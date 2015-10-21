#
#  Copyright 2015 Electric Cloud, Inc.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

##########################
# step.sync.pl
##########################
use ElectricCommander;
use ElectricCommander::PropDB;
use strict;
use JSON;

$::ec = new ElectricCommander();
$::ec->abortOnError(0);
$::pdb = new ElectricCommander::PropDB($::ec);

$| = 1;

my $opts;
my $open_config   = "$[connection_config]";
my $deployments   = '$[deployments]';


$opts->{connection_config}  = q{$[connection_config]};
$[/myProject/procedure_helpers/preamble]

my $json    = JSON->new->allow_nonref->utf8;
$json->relaxed;

sub main {
    print "OpenStack Sync:\n";

    # Validate inputs
    $open_config =~ s/[^A-Za-z0-9_-]//gixms;

    # unpack request
    my $xPath = XML::XPath->new(xml => $deployments);
    my $nodeset = $xPath->find('//Deployment');

    my $instanceList = "";

    # put request in perl hash
    my $deplist;
    foreach my $node ($nodeset->get_nodelist) {

        # for each deployment
        my $i     = $xPath->findvalue('handle',    $node)->string_value;
        my $s     = $xPath->findvalue('state',     $node)->string_value;    # alive
        my $tenant   = $xPath->findvalue('Tenant',       $node)->string_value;
        my $name  = $xPath->findvalue('Name', $node)->string_value;
        my $key    = $xPath->findvalue('KeyPairId',        $node)->string_value;
        print "Input: $i state=$s\n";
        $deplist->{$i}{state}  = "alive";                                   # we only get alive items in list
        $deplist->{$i}{result} = "alive";
        $deplist->{$i}{tenant}    = $tenant;
        $deplist->{$i}{name}   = $name;
        $deplist->{$i}{key}   = $key;
        $instanceList .= "$i\;";
    }

    checkIfAlive($instanceList, $deplist);

    my $xmlout = "";
    addXML(\$xmlout, "<SyncResponse>");
    foreach my $handle (keys %{$deplist}) {
        my $result = $deplist->{$handle}{result};
        my $state  = $deplist->{$handle}{state};

        addXML(\$xmlout, "<Deployment>");
        addXML(\$xmlout, "  <handle>$handle</handle>");
        addXML(\$xmlout, "  <state>$state</state>");
        addXML(\$xmlout, "  <result>$result</result>");
        addXML(\$xmlout, "</Deployment>");
    }
    addXML(\$xmlout, "</SyncResponse>");
    $::ec->setProperty("/myJob/CloudManager/sync", $xmlout);
    print "\n$xmlout\n";
    exit 0;
}

# checks status of instances
# if found to be stopped, it marks the deplist to pending
# otherwise (including errors running api) it assumes it is still running
sub checkIfAlive {
    my ($instances, $deplist) = @_;

    # Initialize
    $openstack->initialize();
    $openstack->initializePropPrefix;
    if ($openstack->opts->{exitcode}) { return; }
    
    my $service_url = $openstack->opts->{service_url};
    $service_url =~ s/http(s*):\/\/(w*)(\.*)//ixms;
    
    
    foreach my $handle (keys %{$deplist}) {
        my $tenant   = $deplist->{$handle}{tenant};
        my $name  = $deplist->{$handle}{name};
        my $key  = $deplist->{$handle}{key};

        # deployment specific response        
        my $url = "http$1://" . $service_url . q{:8774/v} . $openstack->opts->{api_version} . q{/} . $tenant . q{/servers/} . $handle;
        
        my $result = $openstack->rest_request('GET', $url, '', '');
                
        if ($result eq '') { 
            print("Server $deplist->{$handle}{name} in tenant $deplist->{$handle}{tenant} stopped\n");
            $deplist->{$handle}{state}  = "pending";
            $deplist->{$handle}{result} = "success";
            $deplist->{$handle}{mesg}   = "Server was manually stopped or failed";
            next; 
        }
               
               
        my $json_result = $json->decode($result);
        my $status  = $json_result->{server}->{status};
        
        
        my $err = "success";
        my $msg = "";
        if ("$status" eq "ACTIVE") {
            print("Server $deplist->{$handle}{name} in tenant $deplist->{$handle}{tenant} still running\n");
            $deplist->{$handle}{state}  = "alive";
            $deplist->{$handle}{result} = "success";
            $deplist->{$handle}{mesg}   = "Server still running";
        }
        else {
            print("Server $deplist->{$handle}{name} in tenant $deplist->{$handle}{tenant} stopped\n");
            $deplist->{$handle}{state}  = "pending";
            $deplist->{$handle}{result} = "success";
            $deplist->{$handle}{mesg}   = "Server was manually stopped or failed";
        }
    }
    return;
}

sub addXML {
    my ($xml, $text) = @_;
    ## TODO encode
    ## TODO autoindent
    $$xml .= $text;
    $$xml .= "\n";
}

main();

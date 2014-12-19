# -------------------------------------------------------------------------
# Package
#    OpenStack.pm
#
# Purpose
#    A perl library that encapsulates the logic to manipulate Servers from OpenStack
#
#
# Copyright (c) 2014 Electric Cloud, Inc.
# All rights reserved
# -------------------------------------------------------------------------

package OpenStack;

# -------------------------------------------------------------------------
# Includes
# -------------------------------------------------------------------------
use warnings;
use ElectricCommander::PropDB;
use strict;
use LWP::UserAgent;
use MIME::Base64;
use Encode;
use Carp;

use utf8;
use open IO => ':encoding(utf8)';

use JSON;

our $VERSION = 1.0.0;

# -------------------------------------------------------------------------
# Constants
# -------------------------------------------------------------------------

my $ERROR               = 1;
my $SUCCESS             = 0;
my $DEFAULT_DEBUG       = 1;
my $DEFAULT_LOCATION    = q{/myJob/OpenStack/deployed};
my $DEFAULT_RESOURCE    = q{local};
my $DEFAULT_WORKSPACE   = q{default};
my $DEFAULT_QUANTITY    = 1;
my $DEBUG_LEVEL_0       = 0;
my $DEBUG_LEVEL_1       = 1;
my $DEBUG_LEVEL_2       = 2;
my $DEBUG_LEVEL_5       = 5;
my $DEBUG_LEVEL_6       = 6;
my $ALIVE               = 1;
my $NOT_ALIVE           = 0;
my $WAIT_SLEEP_TIME     = 30;
my $TRUE                = 1;
my $FALSE               = 0;
my $EMPTY               = q{};
my $SPACE               = q{ };
my $PER_0700            = '0700';
my $ERRORS = {
    '400' => { 'Fault' => 'BadRequest',            'Description' => 'Malformed request body. The Quantum service is unable to parse the contents of the request body.' },
    '401' => { 'Fault' => 'Unauthorized',          'Description' => 'User has not provided authentication credentials. If authentication is provided by the Keystone identity service, this might mean that either no authentication token has been supplied in the request, or that the token itself is either invalid or expired.' },
    '403' => { 'Fault' => 'Forbidden',             'Description' => 'The user does not have the necessary rights to execute the requested operation.' },
    '404' => { 'Fault' => 'ItemNotFound',          'Description' => 'The requested resource does not exist on the Quantum API server.' },
    '420' => { 'Fault' => 'NetworkNotFound',       'Description' => 'The specified network has not been created or has been removed.' },
    '421' => { 'Fault' => 'NetworkInUse',          'Description' => 'The specified network has attachments plugged into one or more of its ports.' },
    '430' => { 'Fault' => 'PortNotFound',          'Description' => 'The specified port has not been created or has been removed.' },
    '431' => { 'Fault' => 'RequestedStateInvalid', 'Description' => 'Indicates a request to change port to an administrative state not currently supported.' },
    '432' => { 'Fault' => 'PortInUse',             'Description' => 'The specified port cannot be removed as there is an attachment plugged in it.' },
    '440' => { 'Fault' => 'AlreadyAttached',       'Description' => 'Attachment is already plugged into another port.' },
};

# -------------------------------------------------------------------------
my $browser = undef;                           # used to hold the main browser object
my $json    = JSON->new->allow_nonref->utf8;
$json->relaxed;
my $resource_list = $EMPTY;
my $vms_list      = $EMPTY;

############################################################################
# new - Object constructor for OpenStack
#
# Arguments:
#   opts hash
#
# Returns:
#   -
#
############################################################################
sub new {
    my $class = shift;
    my $self = {
                 _cmdr => shift,
                 _opts => shift,
               };
    bless $self, $class;
    return $self;
}

############################################################################
# myCmdr - Get ElectricCommander instance
#
# Arguments:
#   none
#
# Returns:
#   ElectricCommander instance
#
############################################################################
sub myCmdr {
    my ($self) = @_;
    return $self->{_cmdr};
}

############################################################################
# opts - Get opts hash
#
# Arguments:
#   -
#
# Returns:
#   opts hash
#
############################################################################
sub opts {
    my ($self) = @_;
    return $self->{_opts};
}

############################################################################
# ecode - Get exit code
#
# Arguments:
#   -
#
# Returns:
#   exit code number
#
############################################################################
sub ecode {
    my ($self) = @_;
    return $self->opts()->{exitcode};
}

############################################################################
# initialize - Set initial values
#
# Arguments:
#   -
#
# Returns:
#   -
#
############################################################################
sub initialize {
    my ($self) = @_;

    $self->{_props} = ElectricCommander::PropDB->new($self->myCmdr(), $EMPTY);

    # Set defaults
    if ($self->opts->{debug_level} ne $EMPTY) {
        my $g_debug = $self->opts->{debug_level};
    }
    else {
        $self->opts->{debug_level} = $DEFAULT_DEBUG;
    }

    if (defined($self->opts->{resource_workspace}) && $self->opts->{resource_workspace} eq $EMPTY) {
        $self->opts->{resource_workspace} = $DEFAULT_WORKSPACE;
    }

    $self->opts->{exitcode}    = $SUCCESS;
    $self->opts->{JobId}       = $ENV{COMMANDER_JOBID};
    return;
}

############################################################################
# initializePropPrefix - Initialize PropPrefix value and check valid location
#
# Arguments:
#   none
#
# Returns:
#   none
#
############################################################################
sub initializePropPrefix {
    my ($self) = @_;

    # setup the property sheet where information will be exchanged
    if (!defined($self->opts->{location}) || $self->opts->{location} eq $EMPTY) {
        if ($self->opts->{JobStepId} ne q{1}) {
            $self->opts->{location} = $DEFAULT_LOCATION;    # default location to save properties
            $self->debug_msg($DEBUG_LEVEL_5, q{Using default location for results});
        }
        else {
            $self->debug_msg($DEBUG_LEVEL_0, q{Must specify property sheet location when not running in job});
            $self->opts->{exitcode} = $ERROR;
            return;
        }
    }
    $self->opts->{PropPrefix} = $self->opts->{location};
    if (defined($self->opts->{tag}) && $self->opts->{tag} ne $EMPTY) {
        $self->opts->{PropPrefix} .= q{/} . $self->opts->{tag};
    }
    $self->debug_msg($DEBUG_LEVEL_5, q{Results will be in: } . $self->opts->{PropPrefix});

    # test that the location is valid
    if ($self->checkValidLocation) {
        $self->opts->{exitcode} = $ERROR;
        return;
    }
}

############################################################################
# checkValidLocation - Check if location specified in PropPrefix is valid
#
# Arguments:
#   none
#
# Returns:
#   0 - Success
#   1 - Error
#
############################################################################
sub checkValidLocation {
    my ($self) = @_;
    my $location = q{/test-} . $self->opts->{JobStepId};

    # Test set property in location
    my $result = $self->setProp($location, q{Test property});
    if (!defined($result) || $result eq $EMPTY) {
        $self->debug_msg($DEBUG_LEVEL_0, q{Invalid location: } . $self->opts->{PropPrefix});
        return $ERROR;
    }

    # Test get property in location
    $result = $self->getProp($location);
    if (!defined($result) || $result eq $EMPTY) {
        $self->debug_msg($DEBUG_LEVEL_0, q{Invalid location: } . $self->opts->{PropPrefix});
        return $ERROR;
    }

    # Delete property
    $result = $self->deleteProp($location);
    return $SUCCESS;
}

############################################################################
# myProp - Get PropDB
#
# Arguments:
#   none
#
# Returns:
#   PropDB
#
############################################################################
sub myProp {
    my ($self) = @_;
    return $self->{_props};
}

############################################################################
# setProp - Use stored property prefix and PropDB to set a property
#
# Arguments:
#   location - relative location to set the property
#   value    - value of the property
#
# Returns:
#   set_result - result returned by PropDB->setProp
#
############################################################################
sub setProp {
    my ($self, $location, $value) = @_;
    my $set_result = $self->myProp->setProp($self->opts->{PropPrefix} . $location, $value);
    return $set_result;
}

############################################################################
# getProp - Use stored property prefix and PropDB to get a property
#
# Arguments:
#   location - relative location to get the property
#
# Returns:
#   get_result - property value
#
############################################################################
sub getProp {
    my ($self, $location) = @_;
    my $get_result = $self->myProp->getProp($self->opts->{PropPrefix} . $location);
    return $get_result;
}

############################################################################
# deleteProp - Use stored property prefix and PropDB to delete a property
#
# Arguments:
#   location - relative location of the property to delete
#
# Returns:
#   del_result - result returned by PropDB->deleteProp
#
############################################################################
sub deleteProp {
    my ($self, $location) = @_;
    my $del_result = $self->myProp->deleteProp($self->opts->{PropPrefix} . $location);
    return $del_result;
}

############################################################################
# deploy - Call deploy_vm to create new server(s)
#
# Arguments:
#   -
#
# Returns:
#   -
#
############################################################################
sub deploy {
    my ($self) = @_;

    #-----------------------------------------------------------------------------
    # Deploy
    #-----------------------------------------------------------------------------
    if ($self->opts->{quantity} eq $DEFAULT_QUANTITY) {
        $self->deploy_vm();
    }
    else {
        my $vm_prefix = $self->opts->{server_name};
        my $vm_number;
        for (1 .. $self->opts->{quantity}) {
            $vm_number = $_;
            $self->opts->{server_name} = $vm_prefix . q{_} . $vm_number;
            $self->deploy_vm();
        }
    }

    $self->setProp(q{/resourceList}, $resource_list);
    $self->setProp(q{/vmsList},      $vms_list);

    return;
}


sub associate_floating_ip {
    my ($self) = @_;

    $self->debug_msg($DEBUG_LEVEL_1, q{---------------------------------------------------------------------});
    $self->initialize();
    $self->initializePropPrefix;

    if ($self->opts->{exitcode}) {
        return;
    }

    my $compute_service_url = $self->opts->{compute_service_url};
    my $url = $compute_service_url . q{/v2/}  . $self->opts->{tenant_id};
    my $tenant_url = $url;

    $url .= '/servers/' . $self->opts->{server_id};

    my $server_data = $self->rest_request(
        GET => $url,
        undef, undef
    );

    if ($self->opts->{exitcode} && $self->opts->{restcode}) {
        croak "Error occured, can't find desired instance";
    }

    my $public_ip = undef;

    my $allocated_ips_ref = $self->get_allocated_ips($tenant_url);
    if (!$allocated_ips_ref) {
        croak "Can't get allocated ips\n";
    }

    my @free_ips = $self->get_free_ips_by_allocated_ips($allocated_ips_ref);

    unless (@free_ips) {
        croak "There is no free IP for instance\n";
    }

    $public_ip = $free_ips[0];

    if ($self->associate_ip_to_instance($url, $public_ip)) {
        print "IP address successful associated to instance.";
    }
    else {
        croak "Unable associate IP $free_ips[0] to instance: $url\n";
    }

    return ;
}

############################################################################
# deploy_vm - Deploy a new server
#
# Arguments:
#   -
#
# Returns:
#   -
#
############################################################################
sub deploy_vm {
    my ($self) = @_;

    $self->debug_msg($DEBUG_LEVEL_1, q{---------------------------------------------------------------------});
    $self->initialize();
    $self->initializePropPrefix;
    if ($self->opts->{exitcode}) { return; }

    my $message;
    my $result;
    my $body;
    my $data;
    my $public_ip  = $EMPTY;
    my $private_ip = $EMPTY;

    my $keypair    = $self->opts->{keyPairName};


    my $compute_service_url = $self->opts->{compute_service_url};

    my $url = $compute_service_url . q{/v2/}  . $self->opts->{tenant_id}; # . q{/servers};
	my $tenant_url = $url;
    $url .= q{/servers};

    $data->{server}->{name}            = $self->opts->{server_name};
    $data->{server}->{imageRef}        = $self->opts->{image};
    $data->{server}->{flavorRef}       = $self->opts->{flavor};

    $data->{server}->{key_name}        = $keypair;

    # Construct the security group information, if specified
    if (length($self->opts->{security_groups})) {
      my @result =
            $self->constructSecurityGroupArray($self->opts->{security_groups});

      $data->{server}->{security_groups} = \@result;
    }

    $body = to_json($data);

    ## Make POST request
    $result = $self->rest_request('POST', $url, 'application/json', $body);
    if ($self->opts->{exitcode}) { return; }

    my $json_result = $json->decode($result);
    my $progress    = $json_result->{server}->{progress} || '';
    my $status      = $json_result->{server}->{status} || '';
    my $server_id   = $json_result->{server}->{id};

    $self->debug_msg($DEBUG_LEVEL_1, q{Waiting for action to complete...});

    # Describe
    $url = $compute_service_url . q{/v2/}  . $self->opts->{tenant_id} . q{/servers/} . $server_id;
    while ($progress ne '100' && $status ne 'ACTIVE') {

        ## Make GET request
        $result = $self->rest_request('GET', $url, $EMPTY, $EMPTY);
        if ($self->opts->{exitcode}) { return; }

        $json_result = $json->decode($result);
        $status      = $json_result->{server}->{status};
        if ($status eq 'ERROR') {
            $self->opts->{exitcode} = $ERROR;
            return;
        }
        $progress = $json_result->{server}->{progress};
        sleep $WAIT_SLEEP_TIME;
    }

    $self->debug_msg($DEBUG_LEVEL_1, q{Server '} . $self->opts->{server_name} . q{' created.});

    # Now describe them one more time to capture the attributes
    $result = $self->rest_request('GET', $url, $EMPTY, $EMPTY);
    if ($self->opts->{exitcode}) { return; }

    $json_result = $json->decode($result);

    my $id        = $json_result->{server}->{id};
    my $name      = $json_result->{server}->{name};
    my $uuid      = $json_result->{server}->{uuid};
    my $tenant_id = $json_result->{server}->{tenant_id};
    my $state     = $json_result->{server}->{status};
    my $image     = $json_result->{server}->{image};
    my $image_id  = $image->{id};
    my $addresses = $json_result->{server}->{addresses};
    if ($addresses->{public}[0]->{addr})  { $public_ip  = $addresses->{public}[0]->{addr}; }
    if ($addresses->{private}[0]->{addr}) { $private_ip = $addresses->{private}[0]->{addr}; }

    if ("$vms_list" ne $EMPTY) { $vms_list .= q{;}; }
    $vms_list .= $id;

    my $resource = $EMPTY;

    if ($self->opts->{associate_ip}) {
    	$self->debug_msg(1, "Allocating ip to instance: $tenant_url");

        my $allocated_ips_ref = $self->get_allocated_ips($tenant_url);
        if (!$allocated_ips_ref) {
            croak "Can't get allocated ips\n";
        }

        my @free_ips = $self->get_free_ips_by_allocated_ips($allocated_ips_ref);

        unless (@free_ips) {
            croak "There is no free IP for instance\n";
        }

        $public_ip = $free_ips[0];
        if ($self->associate_ip_to_instance($url, $public_ip)) {
            print "IP address successful associated to instance.";
        }
        else {
            croak "Unable associate IP $free_ips[0] to instance: $url\n";
        }
    }

    if ($self->opts->{resource_check} eq $TRUE) {
        $resource = $self->make_new_resource($name . q{-} . $self->opts->{JobId} . q{-} . $self->opts->{tag}, $name, $public_ip);
        $self->setProp("/Server-$id/Resource", "$resource");
        
    }

    #store properties

    $self->setProp("/Server-$id/Name",    "$name");
    $self->setProp("/Server-$id/Tenant",  "$tenant_id");
    $self->setProp("/Server-$id/AMI",     "$image_id");
    $self->setProp("/Server-$id/Address", "$public_ip");
    $self->setProp("/Server-$id/Private", "$private_ip");

    
    $self->debug_msg($DEBUG_LEVEL_1, q{Server } . $name . q{ deployed.});
    return;

}

############################################################################
# cleanup - delete one or more servers and resources
#
# Arguments:
#   -
#
# Returns:
#   -
#
############################################################################
sub cleanup {
    my ($self) = @_;

    $self->debug_msg($DEBUG_LEVEL_1, q{---------------------------------------------------------------------});
    $self->initialize();
    $self->initializePropPrefix;
    if ($self->opts->{exitcode}) { return; }

    my $message;
    my $result;
    my $body;
    my $data;

    my $compute_service_url = $self->opts->{compute_service_url};

    ## Make DELETE request
    my $term_count = 0;
    my @list = split /;/xsm, $self->opts->{server_id};
    foreach (@list) {
        my $id = $_;

        #openstack
        my $url = $compute_service_url . q{/v2/}  . $self->opts->{tenant_id} . q{/servers/} . $id;

        $self->debug_msg($DEBUG_LEVEL_1, "Terminating instance $id");
        $result = $self->rest_request('DELETE', $url, $EMPTY, $EMPTY);
        if ($self->opts->{exitcode} and ($self->opts->{restcode} ne '404')) { return; }
        $self->opts->{exitcode}    = $SUCCESS;
        $self->debug_msg($DEBUG_LEVEL_1, q{Instance '} . $id . q{' has been terminated.});
        
        $term_count++;
    }

    $self->debug_msg($DEBUG_LEVEL_1, "$term_count instances terminated.");

    if ($self->opts->{resource_name} ne $EMPTY) {

        $self->debug_msg($DEBUG_LEVEL_1, q{Deleting resources.});
        my @rlist = split /;/xsm, $self->opts->{resource_name};
        foreach my $res (@rlist) {
            $self->delete_resource($res);
        }

    }

    return;

}

############################################################################
# create_key_pair - Create a new OpenStack Key Pair
#
# Arguments:
#   -
#
# Returns:
#   -
#
############################################################################
sub create_key_pair {
    my ($self) = @_;

    $self->debug_msg($DEBUG_LEVEL_1, '---------------------------------------------------------------------');
    $self->debug_msg($DEBUG_LEVEL_1, '-- Creating KeyPair -------');
    $self->initialize();
    $self->initializePropPrefix;
    if ($self->opts->{exitcode}) { return; }

    my $message;
    my $result;
    my $xml;
    my $body;
    my $data;

    my $compute_service_url = $self->opts->{compute_service_url};


    #openstack
    my $url = $compute_service_url . q{/v2/}  . $self->opts->{tenant_id} . q{/os-keypairs};

    $data->{keypair}->{name} = $self->opts->{keyname};
    $body = to_json($data);

    ## Make POST request
    $result = $self->rest_request('POST', $url, 'application/json', $body);
    if ($self->opts->{exitcode}) { return; }
    $self->setProp('/KeyPairId', $self->opts->{keyname});

    # Extract private key from results
    my $json_result = $json->decode($result);
    my $pem         = $json_result->{keypair}->{private_key};

    $self->extract_keyfile($self->opts->{keyname} . q{.pem}, $pem);
    $self->debug_msg($DEBUG_LEVEL_1, q{KeyPair } . $self->opts->{keyname} . q{ created.});
    return;
}

############################################################################
# delete_key_pair - Delete a new OpenStack Key Pair
#
# Arguments:
#   -
#
# Returns:
#   -
#
############################################################################
sub delete_key_pair {
    my ($self) = @_;

    $self->debug_msg($DEBUG_LEVEL_1, '---------------------------------------------------------------------');
    $self->debug_msg($DEBUG_LEVEL_1, '-- Deleting KeyPair -------');
    $self->initialize();
    $self->initializePropPrefix;
    if ($self->opts->{exitcode}) { return; }

    my $message;
    my $result;
    my $xml;

    my $compute_service_url = $self->opts->{compute_service_url};


    #openstack
    my $url = $compute_service_url . q{/v2/}  . $self->opts->{tenant_id} . q{/os-keypairs};

    # see if a key was created for this tag
    my $keynames = $self->opts->{keyname};
    my @keylist = split /;/xsm, "$keynames";
    foreach my $keyname (@keylist) {

        ## Make DELETE request
        $result = $self->rest_request('DELETE', $url . q{/} . $keyname, $EMPTY, $EMPTY);
        
        if ($self->opts->{exitcode} and ($self->opts->{restcode} ne '500')) { return; }
        $self->opts->{exitcode}    = $SUCCESS;
        $self->debug_msg($DEBUG_LEVEL_1, "KeyPair $keyname deleted\n");
    }

    return;
}

############################################################################
# allocate_ip - Allocate a Floating IP
#
# Arguments:
#   -
#
# Returns:
#   -
#
############################################################################
sub allocate_ip {
    my ($self) = @_;

    $self->debug_msg($DEBUG_LEVEL_1, '---------------------------------------------------------------------');
    $self->debug_msg($DEBUG_LEVEL_1, '-- Allocating IP -------');
    $self->initialize();
    $self->initializePropPrefix;
    if ($self->opts->{exitcode}) { return; }

    my $message;
    my $result;
    my $body;
    my $data;

    my $compute_service_url = $self->opts->{compute_service_url};

    #openstack
    my $url = $compute_service_url . q{/v2/}  . $self->opts->{tenant_id} . q{/os-floating-ips};

    $data->{pool} = $self->opts->{pool};
    $body = to_json($data);

    ## Make POST request
    $result = $self->rest_request('POST', $url, 'application/json', $body);
    if ($self->opts->{exitcode}) { return; }
    my $json_result = $json->decode($result);

    my $id = $json_result->{floating_ip}->{id};
    my $ip = $json_result->{floating_ip}->{ip};

    if ("$ip" eq $EMPTY) {
        $self->debug_msg($DEBUG_LEVEL_1, "Error allocating IP address.\n");
        return;
    }

    #store properties
    $self->setProp(q{/address_id}, "$id");
    $self->setProp(q{/ip},         "$ip");
    $self->debug_msg($DEBUG_LEVEL_1, "Address $ip allocated\n");

    return;
}

############################################################################
# release_ip - Release a Floating IP
#
# Arguments:
#   -
#
# Returns:
#   -
#
############################################################################
sub release_ip {
    my ($self) = @_;

    $self->debug_msg($DEBUG_LEVEL_1, '---------------------------------------------------------------------');
    $self->debug_msg($DEBUG_LEVEL_1, '-- Releasing IP -------');
    $self->initialize();
    $self->initializePropPrefix;
    if ($self->opts->{exitcode}) { return; }

    my $message;
    my $result;
    my $body;
    my $data;

    my $compute_service_url = $self->opts->{compute_service_url};


    #openstack
    my $url = $compute_service_url . q{/v2/}  . $self->opts->{tenant_id} . q{/os-floating-ips/} . $self->opts->{address_id};

    $data->{pool} = $self->opts->{pool};

    $body = to_json($data);

    ## Make POST request
    $result = $self->rest_request('DELETE', $url, $EMPTY, $EMPTY);
    if ($self->opts->{exitcode}) {
        $self->debug_msg($DEBUG_LEVEL_1, q{Error releasing IP address.});
        return;
    }

    $self->debug_msg($DEBUG_LEVEL_1, q{Address } . $self->opts->{address_id} . q{ released.});

    return;
}

# -------------------------------------------------------------------------
# Helper functions
# -------------------------------------------------------------------------

############################################################################
# get_authentication - Login to OpenStack and get X-Auth-Token
#
# Arguments:
#   none
#
# Returns:
#   auth-token - string
#
############################################################################
sub get_authentication {
    my ($self) = @_;

    $self->debug_msg($DEBUG_LEVEL_1, '---------------------------------------------------------------------');
    $self->debug_msg($DEBUG_LEVEL_1, '-- Authenticating with server -------');
    $self->initialize();
    if ($self->opts->{exitcode}) { return; }

    my $keystone_api_version = $self->opts->{keystone_api_version} . '';

    if (!$keystone_api_version) {
        $self->debug_msg(1, "keystone_api_version not found, using default: v3\n");
        $keystone_api_version = '3';
    }
    $self->debug_msg(1, "Using Keystone API version: $keystone_api_version\n");

    if ($keystone_api_version ne '2.0' && $keystone_api_version ne '3') {
        die 'Unsupported api version: ', $keystone_api_version;
    }

    # my $url_tail = q|:35357/v| . $keystone_api_version;
    my $url_tail = '/v' . $keystone_api_version;

    if ($keystone_api_version eq '2.0') {
        $url_tail .= '/tokens';
    }
    else {
        $url_tail .= '/auth/tokens';
    }

    $self->debug_msg(1, "Keystone version is: $keystone_api_version\n");
    $self->debug_msg(1, "Auth URI is: $url_tail");

    #-----------------------------
    # Create new LWP browser
    #-----------------------------
    $browser = LWP::UserAgent->new(agent => 'perl LWP', cookie_jar => {});

    my $identity_service_url = $self->opts->{identity_service_url};

    # openstack old

    # openstack new
    my $auth_url = $identity_service_url . $url_tail;
    #-----------------------------
    # Authentication Request
    #-----------------------------
    $self->debug_msg($DEBUG_LEVEL_1, qq|Authenticating with $auth_url'|);

    # GET /v1.0 HTTP/1.1
    # Host: auth.api.yourcloud.com
    # X-Auth-User: user
    # X-Auth-Key: key
    #-----------------------------
    # GET to url, with X-Auth-User and X-Auth-Key to get auth token
    #-----------------------------
    my $req = HTTP::Request->new(POST => $auth_url);
    #-----------------------------
    # Set headers
    #-----------------------------
    $req->header('content-type' => 'application/json');
    my $post_data;
    if ($keystone_api_version eq '2.0') {
        my $auth_hash = {
            auth    =>  {
                passwordCredentials => {
                    username    =>  '' . $self->opts->{config_user},
                    password    =>  '' . $self->opts->{config_pass},
                },
                tenantId        =>  '' . $self->opts->{tenant_id},
            },
        };
        $post_data = encode_json($auth_hash);
    }
    else {
        my $auth_hash = {
            auth    =>  {
                identity => {
                    methods     =>  [
                        'password'
                    ],
                    password    =>  {
                        user    =>  {
                            name        =>  '' . $self->opts->{config_user},
                            password    =>  '' . $self->opts->{config_pass},
                        },
                    },
                },
                scope   =>  {
                    project =>  {
                        id    =>  '' . $self->opts->{tenant_id},
                    },
                },
            },
        };
        $post_data = encode_json($auth_hash);
    }


    $req->content($post_data);

    my $response = $browser->request($req);

    my $request_as_string = $req->as_string();
    $request_as_string =~ s/"password"\s?:\s?".*?"/"password":"***"/gs;

    $self->debug_msg($DEBUG_LEVEL_6, "\nRequest:" . $request_as_string);

    $self->debug_msg($DEBUG_LEVEL_6, "\nResponse:" . $response->as_string);

    $self->debug_msg($DEBUG_LEVEL_1, q{    Authentication status: } . $response->status_line);

    #-----------------------------
    # Check if successful login
    #-----------------------------
    if ($response->is_error) {
        $self->debug_msg($DEBUG_LEVEL_0, $response->status_line);
        $self->get_error_by_code($response->code);
        $self->opts->{exitcode} = $ERROR;
        return;
    }

	my $json = decode_json $response->decoded_content;
	my $auth_token;

    if ($keystone_api_version eq '2.0') {
        $auth_token = $json->{'access'}->{'token'}->{'id'};    
    }
    else {
        $auth_token = $response->header('x-subject-token');
    }

    $self->opts->{auth_token} = $auth_token;
    return $auth_token;
}

############################################################################
# debug_msg - Print a debug message
#
# Arguments:
#   errorlevel - number compared to $self->opts->{Debug}
#   msg        - string message
#
# Returns:
#   -
#
############################################################################
sub debug_msg {
    my ($self, $errlev, $msg) = @_;

    if ($self->opts->{debug_level} >= $errlev) {
        print "$msg\n";
    }
    return;
}

############################################################################
# get_error_by_code - Print a detailed error message
#
# Arguments:
#   error      - response error code
#
# Returns:
#   -
#
############################################################################
sub get_error_by_code {
    my ($self, $error_code) = @_;

    if (!defined($ERRORS->{$error_code}->{Description})) {
        return;
    }
    $self->debug_msg($DEBUG_LEVEL_6, q{Error: } . $error_code . $SPACE . $ERRORS->{$error_code}->{Fault} . q{ with message '} . $ERRORS->{$error_code}->{Description} . q{'.});
    return;
}

############################################################################
# rest_request - issue the HTTP request, do special processing, and return result
#
# Arguments:
#   req      - the HTTP req
#
# Returns:
#   response - the HTTP response
############################################################################
sub rest_request {
    my ($self, $post_type, $url_text, $content_type, $content) = @_;

    my $url;
    my $req;
    my $response;

    ## Check url
    if ($url_text eq $EMPTY) {
        $self->debug_msg($DEBUG_LEVEL_0, q{Error: blank URL in rest_request.});
        $self->opts->{exitcode} = $ERROR;
        return $EMPTY;
    }

    ## Set Request Method
    $url = URI->new($url_text);
    if ($post_type eq 'POST') {
        $req = HTTP::Request->new(POST => $url);
    }
    elsif ($post_type eq 'DELETE') {
        $req = HTTP::Request->new(DELETE => $url);
    }
    elsif ($post_type eq 'PUT') {
        $req = HTTP::Request->new(PUT => $url);
    }
    else {
        $req = HTTP::Request->new(GET => $url);
    }

    ## Create authorization to server
    $req->header('X-Auth-Token' => $self->opts->{auth_token});

    ## Set Request Content type
    if ($content_type ne $EMPTY) {
        $req->content_type($content_type);
    }
    ## Set Request Content
    if ($content ne $EMPTY) {
        $req->content($content);
    }

    ## Print Request
    $self->debug_msg($DEBUG_LEVEL_5, "HTTP Request:\n" . $req->as_string);

    ## Make Request
    $response = $browser->request($req);

    ## Print Response
    $self->debug_msg($DEBUG_LEVEL_5, "HTTP Response:\n" . $response->content);

    ## Check for errors
    if ($response->is_error) {
        $self->debug_msg($DEBUG_LEVEL_6, $response->status_line);
        $self->get_error_by_code($response->code);
        $self->opts->{exitcode} = $ERROR;
        $self->opts->{restcode} = $response->code;
        return ($EMPTY);
    }

    ## Return Response
    my $xml = $response->content;
    return $xml;
}

############################################################################
# extract_keyfile
#
# extract keyfile for commands that retun key contents to STDOUT
#
# args:
#      filename - the name of the file to put the key text in
#      pem      - contents of key file
############################################################################
sub extract_keyfile {
    my ($self, $filename, $pem) = @_;

    if (open my $FILE, q{>}, $filename) {
        print {$FILE} $pem . "\n";
        close $FILE;
        chmod $PER_0700, $filename;

    }
    else {

        warn "Error: Cannot Open File\n";
        return;
    }
    return;
}

############################################################################
# make_new_resource - Create a new ElectricCommander resource
#
# Arguments:
#   -
#
# Returns:
#   -
#
############################################################################
sub make_new_resource {
    my ($self, $res_name, $server, $host) = @_;
  
    
    # host must be present
    if ("$host" eq $EMPTY) {
        $self->debug_msg($DEBUG_LEVEL_1, q{No host provided to make_new_resource.});
        return $EMPTY;
    }

    #-----------------------------
    # Append a generated pool name to any specified
    #-----------------------------
    my $pool = $self->opts->{resource_pool} . q{ EC-} . $self->opts->{JobStepId};

    # workspace and port can be blank
    $self->debug_msg($DEBUG_LEVEL_1, 'Creating resource for server \'' . $server . '\'...');

    #-------------------------------------
    # Create the resource
    #-------------------------------------
    my $cmdrresult = $self->myCmdr()->createResource(
                                                     $res_name,
                                                     {
                                                        description   => q{Provisioned resource (dynamic) for } . $server,
                                                        workspaceName => $self->opts->{resource_workspace},
                                                        hostName      => "$host",
                                                        pools         => "$pool"
                                                     }
                                                    );

    #-----------------------------
    # Check for error return
    #-----------------------------
    my $err_msg = $self->myCmdr()->checkAllErrors($cmdrresult);
    if ($err_msg ne $EMPTY) {
        if ($err_msg =~ /DuplicateResourceName/sm) {
            $self->debug_msg($DEBUG_LEVEL_0, "resource $res_name exists");
            next;
        }
        else {
            $self->debug_msg($DEBUG_LEVEL_0, "Error: $err_msg");
            $self->opts->{exitcode} = $ERROR;
            return;
        }
    }
    $self->debug_msg($DEBUG_LEVEL_1, "Resource Name:$res_name");

    if ("$resource_list" ne $EMPTY) { $resource_list .= q{;}; }
    $resource_list .= $res_name;

    return $res_name;

}

############################################################################
# delete_resource - Delete an ElectricCommander resource
#
# Arguments:
#   -
#
# Returns:
#   -
#
############################################################################
sub delete_resource {
    my ($self, $resource) = @_;

    # resource must be present
    if ("$resource" eq $EMPTY) {
        $self->debug_msg($DEBUG_LEVEL_1, "No resource provided to delete_resource.\n");
        return;
    }

    $self->debug_msg($DEBUG_LEVEL_1, "Deleting resource $resource\n");

    #-------------------------------------
    # Delete the resource
    #-------------------------------------
    my $cmdrresult = $self->myCmdr()->deleteResource($resource);

    # Check for error return
    my $err_msg = $self->myCmdr()->checkAllErrors($cmdrresult);
    if ($err_msg ne $EMPTY) {
        $self->debug_msg($DEBUG_LEVEL_1, "Error: $err_msg\n");
        return;
    }
    $self->debug_msg($DEBUG_LEVEL_1, "Deleted resource: $resource");
    return;
}

############################################################################
# constructSecurityGroupArray - Constructs security group array
# Arguments:
#   security_groups - new line delimited list of security groups
#
# Returns:
#   Array of hash
# [{"name" : "group1"}, {"name":"group2"}]
############################################################################
sub constructSecurityGroupArray {
    my ($self, $security_groups) = @_;

    # Get each security group
    my @groups = split("\n", $security_groups);

    # Iterate through each security group, adding it as a hash to the array
    my @result = map {{name => $_}} @groups;

    # result is an array of hashes
    return @result;
}

############################################################################
# get_allocated_ips
# Arguments:
#	$compute_api_url_with_tenant_id
#
# Returns:
#	Arrayref of hashrefs with allocated ips
############################################################################
sub get_allocated_ips {
	my ($self, $tenant_url) = @_;

	unless ($tenant_url) {
		croak "Can't get allocated IPs without an url\n";
	}

	$tenant_url .= '/os-floating-ips';
	my $result = $self->rest_request(GET => $tenant_url, undef, undef);
	if (!$result) {
		print "WARNING: Can't get allocated ips\n";
		return undef;
	}

	my $allocated_ips = undef;
	eval {
		$allocated_ips = decode_json($result);
		1;
	} or do {
		print "WARNING: Error occured: $@, can't decode response\n";
	};
	return undef unless $allocated_ips;

	my $retval = $allocated_ips->{floating_ips};
	bless $retval, 'AllocatedIps';
	return $retval;
}

############################################################################
# get_free_ips_by_allocated_ips
# Arguments:
#   $value_from_get_allocated_ips
#
# Returns:
#   Array of free (unassociated ips) ips ('127.0.0.1', '127.0.0.2', 127.0.0.3), for example
############################################################################
sub get_free_ips_by_allocated_ips {
	my ($self, $allocated_ips_ref) = @_;

	if (!$allocated_ips_ref || !ref $allocated_ips_ref || ref $allocated_ips_ref ne 'AllocatedIps') {
		croak "This function accepts blessed allocated_ips reference\n";
	}

	my @free_ips = ();
    @free_ips = map {$_->{ip}} grep {!$_->{instance_id}} @{$allocated_ips_ref};

    return @free_ips;
}

############################################################################
# associate_ip_to_instance
# Arguments:
#   $instance_url, $free_allocated_ip
#
# Returns:
#   1 on success || 0 on error
############################################################################
sub associate_ip_to_instance {
    my ($self, $instance_url, $ip) = @_;

    if (!$instance_url || !$ip) {
        croak "One of two required parameters is missing\n";
    }

    $instance_url .= '/action';
    my $hash = {
        addFloatingIp   =>  {
            address     =>  $ip,
        },
    };

    my $json = encode_json($hash);
    my $result = $self->rest_request(POST => $instance_url,
        'application/json',
        $json
    );

    if ($self->opts->{exitcode} && $self->opts->{restcode}) {
        print "IP $ip was successfully deployed to instance\n";
        return 0;
    }

    return 1;

}

1;


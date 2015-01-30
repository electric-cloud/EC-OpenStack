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
    my $availability_zone = $EMPTY;
    my $customization_script = $EMPTY;

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

    # Assign availability zone, if specified.
    if ($self->opts->{availability_zone}) {
         $data->{server}->{availability_zone} = $self->opts->{availability_zone};
    }

    # Assign customization script, if specified.
    if ($self->opts->{customization_script}) {
         $data->{server}->{customization_script} = MIME::Base64::encode($self->opts->{customization_script});
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
    if ($json_result->{server}->{availability_zone}) { $availability_zone = $json_result->{server}->{availability_zone}; }
    if ($json_result->{server}->{customization_script}) { $customization_script = $json_result->{server}->{customization_script}; }

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
    $self->setProp("/Server-$id/AvailabilityZone", "$availability_zone");
    $self->setProp("/Server-$id/CustomizationScript", "$customization_script");
    
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
# reboot - Reboots an given instance of server
#
# Arguments:
#   -
#
# Returns:
#   -
#
############################################################################
sub reboot {
    my ($self) = @_;

    $self->debug_msg($DEBUG_LEVEL_1, '---------------------------------------------------------------------');
    $self->debug_msg($DEBUG_LEVEL_1, '-- Rebooting an instance -------');
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
    my $url = $compute_service_url . q{/v2/}  . $self->opts->{tenant_id} . q{/servers/} . $self->opts->{server_id} . q{/action};

    #Check reboot type
    if (uc($self->opts->{reboot_type}) eq q{SOFT}){
        $data->{reboot}->{type} = '' . q{SOFT};
    } elsif (uc($self->opts->{reboot_type}) eq q{HARD}){
        $data->{reboot}->{type} = '' . q{HARD};
    } else {
        $self->debug_msg($DEBUG_LEVEL_1,q{Unrecognized reboot type.Terminating ...});
        return;
    }
    $body = to_json($data);

    ## Make POST request
    $result = $self->rest_request('POST', $url, 'application/json', $body);
    if ($self->opts->{exitcode}) {

        $self->debug_msg($DEBUG_LEVEL_1,q{Failed to reboot server});
        return;
    }

    $self->debug_msg($DEBUG_LEVEL_1, q{Server } . $self->opts->{server_id} . q{ rebooted successfully.Reboot type : } . $self->opts->{reboot_type});
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

############################################################################
# create_volume - Create a new volume
#
# Arguments:
#   -
#
# Returns:
#   -
#
############################################################################
sub create_volume {
    my ($self) = @_;

    $self->debug_msg($DEBUG_LEVEL_1, '---------------------------------------------------------------------');
    $self->debug_msg($DEBUG_LEVEL_1, '-- Creating a volume -------');
    $self->initialize();
    $self->initializePropPrefix;
    if ($self->opts->{exitcode}) { return; }

    my $message;
    my $result;
    my $xml;
    my $body;
    my $data;
    my $blockstorage_api_version;
    my $availability_zone = $EMPTY;
    my $volume_type = $EMPTY;
    my $size = $EMPTY;


    my $blockstorage_service_url = $self->opts->{blockstorage_service_url};

    if ($self->opts->{blockstorage_api_version} eq "1") {
        $blockstorage_api_version = q{/v1/};
    } elsif ($self->opts->{blockstorage_api_version} eq "2") {
        $blockstorage_api_version = q{/v2/};
    } else {
        $self->debug_msg($DEBUG_LEVEL_1,q{Unsupported block storage API version.Exiting...});
        return;
    }
    my $url = $blockstorage_service_url . $blockstorage_api_version . $self->opts->{tenant_id};
    my $tenant_url = $url;
    $url .= q{/volumes};

    $data->{volume}->{display_name} = $self->opts->{display_name};
    $data->{volume}->{size} = $self->opts->{size};
    $data->{volume}->{volume_type} = $self->opts->{volume_type};
    $data->{volume}->{availability_zone} = $self->opts->{availability_zone};
    $body = to_json($data);
    
    ## Make POST request
    $result = $self->rest_request('POST', $url, 'application/json', $body);
    if ($self->opts->{exitcode}) { return; }

    my $json_result = $json->decode($result);

    my $volume_id = $json_result->{volume}->{id};
    my $volume_name = $json_result->{volume}->{name};

    if ($json_result->{volume}->{availability_zone} ) { $availability_zone = $json_result->{volume}->{availability_zone};}
    if ($json_result->{volume}->{volume_type} ) { $volume_type = $json_result->{volume}->{volume_type}; }
    if ($json_result->{volume}->{size} ) {  $size = $json_result->{volume}->{size}; }

    $self->setProp("/Volume/ID",    "$volume_id");
    $self->setProp("/Volume/Name",  "$volume_name");
    $self->setProp("/Volume/AvailabilityZone",  "$availability_zone");
    $self->setProp("/Volume/VolumeType",  "$volume_type");
    $self->setProp("/Volume/Size",  "$size");

    $self->debug_msg($DEBUG_LEVEL_1, q{Volume } . $self->opts->{display_name} . q{ created.});
    return;
}

############################################################################
# attach_volume - Attach a volume to given server
#
# Arguments:
#   -
#
# Returns:
#   -
#
############################################################################
sub attach_volume {
    my ($self) = @_;

    $self->debug_msg($DEBUG_LEVEL_1, '---------------------------------------------------------------------');
    $self->debug_msg($DEBUG_LEVEL_1, '-- Attaching a volume -------');
    $self->initialize();
    $self->initializePropPrefix;
    if ($self->opts->{exitcode}) { return; }

    my $message;
    my $result;
    my $xml;
    my $body;
    my $data;

    my $compute_service_url = $self->opts->{compute_service_url};


    #openstack compute URL
    my $url = $compute_service_url . q{/v2/}  . $self->opts->{tenant_id};
   
    my $tenant_url = $url;
    $url = $url . q{/servers/} . $self->opts->{server_id} . q{/os-volume_attachments} ;
   

    $data->{volumeAttachment}->{volumeId} = $self->opts->{volume_id};
   
    # Give device name, if specified
    if (length($self->opts->{device})) {
       $data->{volumeAttachment}->{device} = $self->opts->{device};
    }
    $body = to_json($data);

    ## Make POST request
    $result = $self->rest_request('POST', $url, 'application/json', $body);
    if ($self->opts->{exitcode}) { return; }
    my $json_result = $json->decode($result);

    my $attachment_id = $json_result->{volumeAttachment}->{id};
    my $volume_id = $json_result->{volumeAttachment}->{volumeId};
    my $server_id = $json_result->{volumeAttachment}->{serverId};

    if ("$attachment_id" eq $EMPTY) {
        $self->debug_msg($DEBUG_LEVEL_1, "Error attaching the volume to instance.\n");
        return;
    }

    #store properties
    $self->setProp(q{/VolumeAttachment/ID}, "$attachment_id");
    $self->setProp(q{/VolumeAttachment/VolumeId}, "$volume_id");
    $self->setProp(q{/VolumeAttachment/ServerId}, "$server_id");
    $self->debug_msg($DEBUG_LEVEL_1, "Volume $volume_id attached to server\n");

    return;
}

############################################################################
# detach_volume - Detach a volume from server
#
# Arguments:
#   -
#
# Returns:
#   -
#
############################################################################
sub detach_volume {
    my ($self) = @_;

    $self->debug_msg($DEBUG_LEVEL_1, '---------------------------------------------------------------------');
    $self->debug_msg($DEBUG_LEVEL_1, '-- Detaching a volume -------');
    $self->initialize();
    $self->initializePropPrefix;
    if ($self->opts->{exitcode}) { return; }

    my $message;
    my $result;
    my $xml;
    my $body;
    my $data;

    my $compute_service_url = $self->opts->{compute_service_url};


    #openstack compute URL
    my $url = $compute_service_url . q{/v2/}  . $self->opts->{tenant_id};
   
    my $tenant_url = $url;
    $url = $url . q{/servers/} . $self->opts->{server_id} . q{/os-volume_attachments/} . $self->opts->{attachment_id};
   
    ## Make POST request
    $result = $self->rest_request('DELETE', $url, $EMPTY, $EMPTY);
    if ($self->opts->{exitcode}) {
        $self->debug_msg($DEBUG_LEVEL_1, q{Error detaching volume.});
        return;
    }

    $self->debug_msg($DEBUG_LEVEL_1, q{Volume detached from server } . $self->opts->{server_id} . q{ successfully.});

    return;
}
    
############################################################################
# delete_volume - Delete already existing volume
#
# Arguments:
#   -
#
# Returns:
#   -
#
############################################################################
sub delete_volume {
    my ($self) = @_;
    my $result;

    $self->debug_msg($DEBUG_LEVEL_1, '---------------------------------------------------------------------');
    $self->debug_msg($DEBUG_LEVEL_1, '-- Deleting a volume -------');
    $self->initialize();
    $self->initializePropPrefix;
    if ($self->opts->{exitcode}) { return; }

    #openstack block storage URL
    my $blockstorage_service_url = $self->opts->{blockstorage_service_url};
    my $url = $blockstorage_service_url . q{/} . $self->opts->{blockstorage_api_version} . q{/} . $self->opts->{tenant_id};
    my $tenant_url = $url;
    $url .= q{/volumes/} . $self->opts->{volume_id};


    ## Make DELETE request
    $result = $self->rest_request('DELETE', $url, $EMPTY, $EMPTY);
        
    if ($self->opts->{exitcode}) { 
       if($result ne $EMPTY){
           my $json_result = $json->decode($result);
           my $error_message = $json_result->{badRequest}->{message};
           $self->debug_msg($DEBUG_LEVEL_1, q{Error deleting volume.\nError msg :\n } . $error_message);
       }
       return; 
    }
    
    $self->opts->{exitcode}    = $SUCCESS;
    $self->debug_msg($DEBUG_LEVEL_1, q{Volume } . $self->opts->{volume_id} . q{ deleted successfully.\n});
    

    return;
}


############################################################################
# create_image - Creates a new image
#
# Arguments:
#   -
#
# Returns:
#   -
#
############################################################################
sub create_image {
    my ($self) = @_;

    $self->debug_msg($DEBUG_LEVEL_1, '---------------------------------------------------------------------');
    $self->debug_msg($DEBUG_LEVEL_1, '-- Creating an image -------');
    $self->initialize();
    $self->initializePropPrefix;
    if ($self->opts->{exitcode}) { return; }

    if ( $self->opts->{image_api_version} eq '1') {
            create_image_v1($self);
    } elsif ( $self->opts->{image_api_version} eq '2' ) {
            create_image_v2($self);
    } else {
            $self->debug_msg($DEBUG_LEVEL_1, "Unsupported Image service version");
            return;
    }

}

############################################################################
# create_image_v1 - Creates a new image with GLANCE API v1
#
# Arguments:
#   -
#
# Returns:
#   -
#
############################################################################
sub create_image_v1 {

    my ($self) = @_;

    my $result;
    my $body;
    my %headers;
    my $json_result;
    my $file_contents;
    my $image_service_url = $self->opts->{image_service_url};
    my $image_api_version = q{/v} . $self->opts->{image_api_version} . q{/};
    my $url = $image_service_url . $image_api_version . q{images};
    my $tenant_url = $url;
    my $status = $EMPTY;
    my $response;

    ## Add all image meta-data in HTTP request headers
    ## Since, request body contains raw image itself

    $headers{'x-image-meta-name'} = $self->opts->{name};
    $headers{'x-image-meta-disk_format'} = $self->opts->{disk_format};
    $headers{'x-image-meta-container_format'} = $self->opts->{container_format};

    ##Add optional headers, if specified by user.
    if ($self->opts->{size}) {
        $headers{'x-image-meta-size'} = $self->opts->{size};
    }
    if ($self->opts->{checksum}) {
        $headers{'x-image-meta-checksum'} = $self->opts->{checksum};
    }
    if ($self->opts->{min_ram}) {
        $headers{'x-image-meta-min-ram'} = $self->opts->{min_ram};
    }
    if ($self->opts->{min_disk}) {
        $headers{'x-image-meta-min-disk'} = $self->opts->{min_disk};
    }
    if ($self->opts->{owner_name}) {
        $headers{'x-image-meta-owner'} = $self->opts->{owner_name};
    }



    if($self->opts->{is_local}){

         ## Add code to read from file and attach as a data.
         open FILE, "<", $self->opts->{image_path};
         binmode FILE;

         $file_contents = do { local $/; <FILE> };

    }else {

        ## URL is specified as image location.
        $headers{'x-image-meta-location'} = $self->opts->{image_path};
        $file_contents = $EMPTY;

    }
    ## Make POST request

    $result = $self->rest_request('POST', $url, 'application/octet-stream', $file_contents, \%headers);
    $json_result = $json->decode($result);
    if ($self->opts->{exitcode}) { return; }

    # Wait for action to complete
    $self->debug_msg($DEBUG_LEVEL_1, q{Waiting for action to complete...});
    my $image_id = $json_result->{image}->{id};

    # Describe
    $url = $image_service_url . $image_api_version . q{images/} . $image_id;

    while ($status ne 'active') {

        # Make HEAD request, poll for X-Image-Meta-Status in response header
        $response = $self->rest_request_with_header('HEAD', $url, $EMPTY, $EMPTY);
        $status = $response->header('X-Image-Meta-Status');
        if ($self->opts->{exitcode}) { return; }
            $self->debug_msg($DEBUG_LEVEL_1, q{no exit code...});
        if ($status eq 'ERROR') {
            $self->opts->{exitcode} = $ERROR;
            return;
        }

            sleep $WAIT_SLEEP_TIME;
    }

       #store properties

       $self->setProp(q{/Image/ID}, $image_id);
       if ($response->header('X-Image-Meta-Owner')) { $self->setProp(q{/Image/Owner}, $response->header('X-Image-Meta-Owner'));}
       if ($response->header('X-Image-Meta-Name')) { $self->setProp(q{/Image/Name}, $response->header('X-Image-Meta-Name'));}
       if ($response->header('X-Image-Meta-Container_format')) { $self->setProp(q{/Image/ContainerFormat}, $response->header('X-Image-Meta-Container_format'));}
       if ($response->header('X-Image-Meta-Property-Image_type')) { $self->setProp(q{/Image/PropertyImageType}, $response->header('X-Image-Meta-Property-Image_type'));}
       if ($response->header('X-Image-Meta-Property-Instance_uuid')) { $self->setProp(q{/Image/PropertyInstanceUuid}, $response->header('X-Image-Meta-Property-Instance_uuid'));}
       if ($response->header('X-Image-Meta-Checksum')) { $self->setProp(q{/Image/Checksum}, $response->header('X-Image-Meta-Checksum') );}
       if ($response->header('X-Image-Meta-Size')) { $self->setProp(q{/Image/Size}, $response->header('X-Image-Meta-Size'));}
       if ($response->header('X-Image-Meta-Disk_format')) { $self->setProp(q{/Image/DiskFormat}, $response->header('X-Image-Meta-Disk_format'));}

       $self->debug_msg($DEBUG_LEVEL_1, q{Image  } . $self->opts->{name} . q{ created.});
       return;
}

############################################################################
# create_image_v2 - Creates a new image with GLANCE API v2
#
# Arguments:
#   -
#
# Returns:
#   -
#
############################################################################
sub create_image_v2 {

        my ($self) = @_;

        my $result;
        my $body;
        my %headers;
        my $json_result;
        my $file_contents;
        my $response;
        my $data;

        my $image_service_url = $self->opts->{image_service_url};
        my $image_api_version = q{/v} . $self->opts->{image_api_version} . q{/};
        my $url = $image_service_url . $image_api_version . q{images};
        my $tenant_url = $url;

        $data->{name} = $self->opts->{name};
        $data->{container_format} = $self->opts->{container_format};
        $data->{disk_format} = $self->opts->{disk_format};

        ##Add optional headers, if specified by user.
        if ($self->opts->{size}) {
           $data->{size} = $self->opts->{size};
        }
        if ($self->opts->{checksum}) {
           $data->{checksum}  = $self->opts->{checksum};
        }
        if ($self->opts->{min_ram}) {
           $data->{min_ram} = $self->opts->{min_ram};
        }
        if ($self->opts->{min_disk}) {
           $data->{min_disk} = $self->opts->{min_disk};
        }
        if ($self->opts->{owner_name}) {
           $data->{owner_name} = $self->opts->{owner_name};
        }

        $body = to_json($data);
        #Send POST request
        $result = $self->rest_request('POST', $url, 'application/json', $body, $EMPTY);

        $json_result = $json->decode($result);
        if ($self->opts->{exitcode}) { return; }


        if ($json_result->{id}) { $self->setProp(q{/Image/ID}, $json_result->{id});}
        if ($json_result->{name}) { $self->setProp(q{/Image/Name}, $json_result->{name});}
        if ($json_result->{owner}) { $self->setProp(q{/Image/Owner}, $json_result->{owner});}
        if ($json_result->{container_format}) { $self->setProp(q{/Image/ContainerFormat}, $json_result->{container_format});}
        if ($json_result->{disk_format}) { $self->setProp(q{/Image/DiskFormat}, $json_result->{disk_format} );}

        if($self->opts->{is_local}){

          #Read binary image data from file.
          open FILE, "<", $self->opts->{image_path};
          binmode FILE;

          $file_contents = do { local $/; <FILE> };

        }else {

             ## URL is specified as image location remaining.
             # $headers{'x-image-meta-location'} = $self->opts->{image_path};
             # $file_contents = $EMPTY;

        }

        $url .= q{/} . $json_result->{id} . q{/file};
        ## Make POST request

        $result = $self->rest_request('PUT', $url, 'application/octet-stream', $file_contents);

        if ($self->opts->{exitcode}) { return; }

        $self->debug_msg($DEBUG_LEVEL_1, q{Image  } . $self->opts->{name} . q{ created.});
        return;
}

############################################################################
# take_volume_snapshot - Creates a new snapshot of given volume
#
# Arguments:
#   -
#
# Returns:
#   -
#
############################################################################
sub take_volume_snapshot {
    my ($self) = @_;

    $self->debug_msg($DEBUG_LEVEL_1, '---------------------------------------------------------------------');
    $self->debug_msg($DEBUG_LEVEL_1, '-- Creating a snapshot of a volume-------');
    $self->initialize();
    $self->initializePropPrefix;
    if ($self->opts->{exitcode}) { return; }

    my $message;
    my $result;
    my $xml;
    my $body;
    my $data;

    my $compute_service_url = $self->opts->{compute_service_url};

    my $url = $compute_service_url . q{/v2/}  . $self->opts->{tenant_id};
    my $tenant_url = $url;
    $url .= q{/os-snapshots};

    $data->{snapshot}->{display_name} = $self->opts->{display_name};
    $data->{snapshot}->{display_description} = $self->opts->{display_description};
    $data->{snapshot}->{force} = $self->opts->{force};
    $data->{snapshot}->{volume_id} = $self->opts->{volume_id};
    $body = to_json($data);

    ## Make POST request
    $result = $self->rest_request('POST', $url, 'application/json', $body);
    if ($self->opts->{exitcode}) { return; }

    my $json_result = $json->decode($result);

    my $snapshot_id = $json_result->{snapshot}->{id};
    my $snapshot_name = $json_result->{snapshot}->{name};
    my $volume_id = $json_result->{snapshot}->{volume_id};

    $self->setProp("/VolumeSnapshot/ID",    "$snapshot_id");
    $self->setProp("/VolumeSnapshot/Name",  "$snapshot_name");
    $self->setProp("/VolumeSnapshot/VolumeID",  "$volume_id");

    $self->debug_msg($DEBUG_LEVEL_1, q{Snapshot  } . $self->opts->{display_name} . q{ created.});
    return;
}

############################################################################
# take_instance_snapshot - Creates a new snapshot of given instance
#
# Arguments:
#   -
#
# Returns:
#   -
#
############################################################################
sub take_instance_snapshot {
    my ($self) = @_;

    $self->debug_msg($DEBUG_LEVEL_1, '---------------------------------------------------------------------');
    $self->debug_msg($DEBUG_LEVEL_1, '-- Creating a snapshot of an instance-------');
    $self->initialize();
    $self->initializePropPrefix;
    if ($self->opts->{exitcode}) { return; }

    my $message;
    my $result;
    my $json_result;
    my $xml;
    my $body;
    my $data;
    my $status = $EMPTY;
    my $response;

    my $compute_service_url = $self->opts->{compute_service_url};

    my $url = $compute_service_url . q{/v2/}  . $self->opts->{tenant_id}; # . q{/servers};
    my $tenant_url = $url;
    $url .= q{/servers/} . $self->opts->{server_id} . q{/action};

    #Request body
    $data->{createImage}->{name} = $self->opts->{display_name};

    my %metadata = $self->constructMetadataHash($self->opts->{metadata});
    foreach my $key (keys %metadata) {
            $data->{createImage}->{metadata}->{$key} = $metadata{$key};
    }
    $body = to_json($data);

    ## Make POST request
    $result = $self->rest_request_with_header('POST', $url, 'application/json', $body);
  
    if ($self->opts->{exitcode}) { return; }

    # Retrieve image id from Location header of HTTP response.
    my $snapshot_location = $result->header('Location');
    my ($uri, $image_id) = split "/images/", $snapshot_location;

    # Wait for action to complete
    $self->debug_msg($DEBUG_LEVEL_1, q{Waiting for action to complete...});


    # Describe
    my $image_service_url = $self->opts->{image_service_url};
    my $image_api_version = q{/v} . $self->opts->{image_api_version} . q{/};

    $url = $image_service_url . $image_api_version . q{images/} . $image_id;

    while ($status ne 'active') {

        if ( $self->opts->{image_api_version} eq '1' ) {

            $response = $self->rest_request_with_header('HEAD', $url, $EMPTY, $EMPTY);
            $status = $response->header('X-Image-Meta-Status');
            if ($self->opts->{exitcode}) { return; }
            if ($status eq 'ERROR') {
                   $self->opts->{exitcode} = $ERROR;
                   return;
            }
        } elsif ( $self->opts->{image_api_version} eq '2') {

            $result = $self->rest_request('GET', $url, $EMPTY, $EMPTY);
            if ($self->opts->{exitcode}) { return; }
            $json_result = $json->decode($result);
            $status = $json_result->{status};

            if ($status eq 'ERROR') {
                $self->opts->{exitcode} = $ERROR;
                return;
            }

        }

            sleep $WAIT_SLEEP_TIME;
    }

    $self->setProp("/InstanceSnapshot/ID",  "$image_id");

    if ( $self->opts->{image_api_version} eq '1' ) {

        if ( $response->header('X-Image-Meta-Name') ) { $self->setProp("/InstanceSnapshot/Name",  $response->header('X-Image-Meta-Name')); }
        if ( $response->header('X-Image-Meta-Owner') ) { $self->setProp("/InstanceSnapshot/Owner",  $response->header('X-Image-Meta-Owner')); }
        if ( $response->header('X-Image-Meta-Disk_format') ) { $self->setProp("/InstanceSnapshot/DiskFormat",  $response->header('X-Image-Meta-Disk_format')); }
        if ( $response->header('X-Image-Meta-Container_format') ) { $self->setProp("/InstanceSnapshot/ContainerFormat",  $response->header('X-Image-Meta-Container_format')); }
        if ( $response->header('X-Image-Meta-Min_disk') ) { $self->setProp("/InstanceSnapshot/MinDisk",  $response->header('X-Image-Meta-Min_disk')); }
        if ( $response->header('X-Image-Meta-Min_ram') ) { $self->setProp("/InstanceSnapshot/MinRam",  $response->header('X-Image-Meta-Min_ram')); }

    } elsif ( $self->opts->{image_api_version} eq '2' ) {

        if ( $json_result->{name} ) { $self->setProp("/InstanceSnapshot/Name",  $json_result->{name}); }
        if ( $json_result->{owner} ) { $self->setProp("/InstanceSnapshot/Owner",   $json_result->{owner}); }
        if ( $json_result->{disk_format} ) { $self->setProp("/InstanceSnapshot/DiskFormat",  $json_result->{disk_format}); }
        if ( $json_result->{container_format} ) { $self->setProp("/InstanceSnapshot/ContainerFormat",  $json_result->{container_format}); }
        if ( $json_result->{min_disk} ) { $self->setProp("/InstanceSnapshot/MinDisk",  $json_result->{min_disk}); }
        if ( $json_result->{min_ram} ) { $self->setProp("/InstanceSnapshot/MinRam",  $json_result->{min_ram}); }

    }

    $self->debug_msg($DEBUG_LEVEL_1, q{Snapshot } . $self->opts->{display_name} . q{ created.});
    return;
}

############################################################################
# create_stack - Creates a new heat stack from a template.
#
# Arguments:
#   -
#
# Returns:
#   -
#
############################################################################
sub create_stack {
    my ($self) = @_;

    $self->debug_msg($DEBUG_LEVEL_1, '---------------------------------------------------------------------');
    $self->debug_msg($DEBUG_LEVEL_1, '-- Creating a stack -------');
    $self->initialize();
    $self->initializePropPrefix;
    if ($self->opts->{exitcode}) { return; }


    my $result = $EMPTY;
    my $body = $EMPTY;
    my $data;
    my $json_result = $EMPTY;
    my $status = $EMPTY;
    my $orchestration_service_url = $self->opts->{orchestration_service_url};


    my $url = $orchestration_service_url . q{/v1/} . $self->opts->{tenant_id};
    my $tenant_url = $url;
    $url .= q{/stacks};

    $data->{stack_name} = $self->opts->{stack_name};

    # If user has supplied both template and template_url,
    # the template gets preference over template_url.

    if ( $self->opts->{template} ) {
        $data->{template} = $json->decode($self->opts->{template});
    } elsif ( $self->opts->{template_url} ) {
        $data->{template_url} = $self->opts->{template_url};
    } else {
         $self->debug_msg($DEBUG_LEVEL_1, q{Either of template or template URL must be specified.});
    }

    $body = to_json($data);

    ## Make POST request
    $result = $self->rest_request('POST', $url, 'application/json', $body);
    if ($self->opts->{exitcode}) { return; }

    $json_result = $json->decode($result);

    my $stack_id = $json_result->{stack}->{id};


    $self->debug_msg($DEBUG_LEVEL_1, q{Waiting for complete stack  to get deployed ...});

    # Describe openstack orchestration URL
    $url = $orchestration_service_url . q{/v1/} . $self->opts->{tenant_id} . q{/stacks/} . $self->opts->{stack_name} . q{/} . $stack_id;

    while ( $status ne 'CREATE_COMPLETE') {

            ## Make GET request
            $result = $self->rest_request('GET', $url, $EMPTY, $EMPTY);
            if ($self->opts->{exitcode} ) { return; }

            $json_result = $json->decode($result);
            $status      = $json_result->{stack}->{stack_status};
            if ($status eq 'CREATE_FAILED') {
                $self->opts->{exitcode} = $ERROR;
                return;
            }

            sleep $WAIT_SLEEP_TIME;
    }

    $self->debug_msg($DEBUG_LEVEL_1, q{Stack } . $self->opts->{stack_name} . q{ created.});

    # Now describe them one more time to capture the attributes
    $result = $self->rest_request('GET', $url, $EMPTY, $EMPTY);
    if ($self->opts->{exitcode}) { return; }

    $json_result = $json->decode($result);

    my $stack_status = $json_result->{stack}->{stack_status};

    $self->setProp("/Stack/ID",    "$stack_id");
    $self->setProp("/Stack/StackStatus",    "$stack_status");

    if ( $json_result->{stack}->{disable_rollback} ) { $self->setProp("/Stack/DisableRollback",  $json_result->{stack}->{disable_rollback}); }
    if ( $json_result->{stack}->{parent} ) { $self->setProp("/Stack/Parent",  $json_result->{stack}->{parent}); }
    if ( $json_result->{stack}->{stack_name} ) { $self->setProp("/Stack/StackName",  $json_result->{stack}->{stack_name}); }
    if ( $json_result->{stack}->{stack_owner} ) { $self->setProp("/Stack/StackOwner",  $json_result->{stack}->{stack_owner}); }
    if ( $json_result->{stack}->{creation_time} ) { $self->setProp("/Stack/CreationTime",  $json_result->{stack}->{creation_time}); }
    if ( $json_result->{stack}->{stack_owner} ) { $self->setProp("/Stack/StackOwner",  $json_result->{stack}->{stack_owner}); }
    if ( $json_result->{stack}->{timeout_mins} ) { $self->setProp("/Stack/TimeoutMins", $json_result->{stack}->{timeout_mins}); }
    if ( $json_result->{stack}->{stack_owner} ) { $self->setProp("/Stack/StackOwner",  $json_result->{stack}->{stack_owner}); }

    $self->debug_msg($DEBUG_LEVEL_1, q{Stack } . $self->opts->{stack_name} . q{ created.});
    return;
}

############################################################################
# update_stack - Updates a specified stack.
#
# Arguments:
#   -
#
# Returns:
#   -
#
############################################################################
sub update_stack {
    my ($self) = @_;

    $self->debug_msg($DEBUG_LEVEL_1, '---------------------------------------------------------------------');
    $self->debug_msg($DEBUG_LEVEL_1, '-- Updating a stack -------');
    $self->initialize();
    $self->initializePropPrefix;
    if ($self->opts->{exitcode}) { return; }


    my $result = $EMPTY;
    my $body = $EMPTY;
    my $data;
    my $json_result = $EMPTY;
    my $status = $EMPTY;
    my $orchestration_service_url = $self->opts->{orchestration_service_url};


    my $url = $orchestration_service_url . q{/v1/} . $self->opts->{tenant_id};
    my $tenant_url = $url;
    $url .= q{/stacks/} . $self->opts->{stack_name} . q{/} . $self->opts->{stack_id};

    # If user has supplied both template and template_url,
    # the template gets preference over template_url.

    if ( $self->opts->{template} ) {
        $data->{template} = $json->decode($self->opts->{template});
    } elsif ( $self->opts->{template_url} ) {
        $data->{template_url} = $self->opts->{template_url};
    } else {
         $self->debug_msg($DEBUG_LEVEL_1, q{Either of template or template URL must be specified.});
    }

    $body = to_json($data);

    ## Make POST request
    $result = $self->rest_request('PUT', $url, 'application/json', $body);
    if ($self->opts->{exitcode}) { return; }

    $self->debug_msg($DEBUG_LEVEL_1, q{Stack } . $self->opts->{stack_name} . q{ updated.});
    return;
}

############################################################################
# delete_stack - Deletes a specified stack.
#
# Arguments:
#   -
#
# Returns:
#   -
#
############################################################################
sub delete_stack {
    my ($self) = @_;

    $self->debug_msg($DEBUG_LEVEL_1, '---------------------------------------------------------------------');
    $self->debug_msg($DEBUG_LEVEL_1, '-- Deleting a stack -------');
    $self->initialize();
    $self->initializePropPrefix;
    if ($self->opts->{exitcode}) { return; }


    my $result = $EMPTY;
    my $body = $EMPTY;
    my $data;
    my $json_result = $EMPTY;
    my $status = $EMPTY;
    my $orchestration_service_url = $self->opts->{orchestration_service_url};


    my $url = $orchestration_service_url . q{/v1/} . $self->opts->{tenant_id};
    my $tenant_url = $url;
    $url .= q{/stacks/} . $self->opts->{stack_name} . q{/} . $self->opts->{stack_id};

    ## Make DELETE request
    $result = $self->rest_request('DELETE', $url, $EMPTY, $body);
    if ($self->opts->{exitcode}) { return; }

    $self->debug_msg($DEBUG_LEVEL_1, q{Stack } . $self->opts->{stack_name} . q{ deleted.});
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
                            domain      =>  {
                                                id    =>   'default'
                                            },
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

    my $user = '' . $self->opts->{config_user};
    my $pass = '' . $self->opts->{config_pass};

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
    my ($self, $post_type, $url_text, $content_type, $content, $headers) = @_;

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

    if ( $headers ne $EMPTY) {
        # If additional headers have to be added to request
        foreach my $key ( keys %{$headers}){
            $req->header( $key => $headers->{$key});
        }
    }
    ## Set Request Content type
    if ($content_type ne $EMPTY) {
        $req->content_type($content_type);
    }
    ## Set Request Content
    if ($content ne $EMPTY) {
        $req->content($content);
    }

    ## Print Request
    if ($content_type eq 'application/octet-stream') {

        ## If request body is binary data, no need to print body.Print only the headers.
        $self->debug_msg($DEBUG_LEVEL_5, "HTTP Request:\n");






        $self->debug_msg($DEBUG_LEVEL_5, $req->method . " " . $req->uri);
        $self->debug_msg($DEBUG_LEVEL_5, $req->headers->as_string);
        $self->debug_msg($DEBUG_LEVEL_5, "\<Raw image contents...\>");

    } else {
        ## Plain JSON request, print whole request.
        $self->debug_msg($DEBUG_LEVEL_5, "HTTP Request:\n" . $req->as_string);
    }


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
# rest_request_with_header - issue the HTTP HEAD request, do special processing, and return requested response header
#
# Arguments:
#   req      - the HTTP req
#
# Returns:
#   response - the HTTP response
############################################################################
sub rest_request_with_header {
    my ($self, $post_type, $url_text, $content_type, $content, $request_headers) = @_;

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
    elsif ($post_type eq 'HEAD') {
            $req = HTTP::Request->new(HEAD => $url);
    }
    else {
        $req = HTTP::Request->new(GET => $url);
    }

    ## Create authorization to server
    $req->header('X-Auth-Token' => $self->opts->{auth_token});

    foreach my $key ( keys %{$request_headers}){
        $req->header( $key => $request_headers->{$key});
    }

    ## Set Request Content type
    if ($content_type ne $EMPTY) {
        $req->content_type($content_type);
    }
    ## Set Request Content
    if ($content ne $EMPTY) {
        $req->content($content);
    }

    ## Plain JSON request, print as it is.
    $self->debug_msg($DEBUG_LEVEL_5, "HTTP Request:\n" . $req->as_string);



    ## Make Request
    $response = $browser->request($req);

    ## Print Response
    $self->debug_msg($DEBUG_LEVEL_5, "HTTP Response:\n" . $response->as_string);

    ## Check for errors
    if ($response->is_error) {
        $self->debug_msg($DEBUG_LEVEL_6, $response->status_line);
        $self->get_error_by_code($response->code);
        $self->opts->{exitcode} = $ERROR;
        $self->opts->{restcode} = $response->code;
        return ($EMPTY);
    }

    ## Return the entire response
    return $response;
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
# constructMetadataHash - Constructs key:value pair of metadata hash
# Arguments:
#   metadata - comma separated list of keys and values in the
#              form of key1,value1,key2,value2
#
# Returns:
#    hash
# [{"key1" => "value1"}, {"key2" => "value2"}]
############################################################################
sub constructMetadataHash {
    my ($self, $metadata) = @_;

    # Get each key:value pair
    my @pairs = split(",", $metadata);

    # Convert array of key:values into hash

    my %metadata = @pairs;

    # return the resuling hash
    return %metadata;
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

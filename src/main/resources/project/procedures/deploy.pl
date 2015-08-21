##########################
# deploy.pl
##########################
use warnings;
use strict;
use Encode;
use utf8;
use open IO => ':encoding(utf8)';

my $opts;

# Configuration: A commander configuration previously created.
$opts->{connection_config} = q{$[connection_config]};

# Name: Name for the new server.
$opts->{server_name} = q{$[server_name]};

# Image: ID for the image to use (ami-0as1d341)
$opts->{image} = q{$[image]};

# Flavor: Code for the flavor to use (m1.tiny)
$opts->{flavor} = q{$[flavor]};

# Number of Servers: how many servers to deploy(default 1), if more than 1, a prefix "_#" will be used.
$opts->{quantity} = q{$[quantity]};

# Results location: property path to store server information.
$opts->{location} = q{$[location]};

# Deploy tag: tag to identify this deployment in the resource location
$opts->{tag} = q{$[tag]};

# Create Resource?: checkbox to create a resurce for the new server
$opts->{resource_check} = q{$[resource_check]};

# Associate IP to deployed instance?
$opts->{associate_ip} = q{$[associate_ip]};

# Resource Pool: Optional.
$opts->{resource_pool} = q{$[resource_pool]};

# Commander Workspace: Optional
$opts->{resource_workspace} = q{$[resource_workspace]};

# Tenant: Id of the tenant.
$opts->{tenant_id} = q{$[tenant_id]};

# The key pair name
$opts->{keyPairName} = q{$[keyPairName]};

# The security group(s)
$opts->{security_groups} = q{$[security_groups]};

# The availability zone : The availability zone in which to launch the server.
$opts->{availability_zone} = q{$[availability_zone]};

# Customization Script : Configuration information or scripts to execute upon launch of the server.
$opts->{customization_script} = q{$[customization_script]};

$opts->{zone} = q{$[zone]};

$[/myProject/procedure_helpers/preamble]

$openstack->deploy();
exit($opts->{exitcode});

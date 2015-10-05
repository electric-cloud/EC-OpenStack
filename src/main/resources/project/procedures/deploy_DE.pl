##########################
# deploy_DE.pl
##########################
use strict;
use warnings;

my $opts;

# Configuration: A commander configuration previously created.
$opts->{connection_config} = q{$[connection_config]};

# Image: ID for the image to use (ami-0as1d341)
$opts->{image} = q{$[image]};

# Flavor: Code for the flavor to use (m1.tiny)
$opts->{flavor} = q{$[flavor]};

# Number of OpenStack instances that will be created
$opts->{quantity} = q{$[quantity]};

# Commander Workspace: Optional
$opts->{resource_workspace} = q{$[resource_workspace]};

# The key pair name
$opts->{keyPairName} = q{$[keyPairName]};

# The security group(s)
$opts->{security_groups} = q{$[security_groups]};

# The availability zone : The availability zone in which to launch the server.
$opts->{availability_zone} = q{$[availability_zone]};

# Customization Script : Configuration information or scripts to execute upon launch of the server.
$opts->{customization_script} = q{$[customization_script]};

# Results location: property path to store server information.
$opts->{location} = q{$[location]};

#
# '_DeployDE' deviations from the 'Deploy' procedure being made
# for dynamic environments feature.
#

# Resource Pool: Optional, but if set, flag that corresponding resources will
# be created.
$opts->{resource_pool} = q{$[resource_pool]};
if ($opts->{resource_pool}) {
    # Create Resource if resource pool name was assigned
    $opts->{resource_check} = 1;
}

# Always associate IP to the deployed instances
$opts->{associate_ip} = 1;

# Deploy tag to identify this deployment in the resource location, default to jobStepId.
$opts->{tag} = q{$[jobStepId]};

$[/myProject/procedure_helpers/preamble]

$openstack->deploy();
exit($opts->{exitcode});

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
$opts->{region} = q{$[region]};

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

$opts->{resource_zone} = q{$[resource_zone]};

if (!$opts->{resource_zone}) {
    # Default resource zone to 'default' if nothing was specified.
    $opts->{resource_zone} = "default";
}

$opts->{resource_port} = q{$[resource_port]};

if (!$opts->{resource_port}) {
    # Default the resource_port to 7800
    $opts->{resource_port} = 7800;
}

$[/myProject/procedure_helpers/preamble]

$openstack->deploy();
exit($opts->{exitcode});

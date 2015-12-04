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

# The key pair name
$opts->{keyPairName} = q{$[keyPairName]};

# The security group(s)
$opts->{security_groups} = q{$[security_groups]};

# The availability zone : The availability zone in which to launch the server.
$opts->{availability_zone} = q{$[availability_zone]};

# Customization Script : Configuration information or scripts to execute upon launch of the server.
$opts->{customization_script} = q{$[customization_script]};

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

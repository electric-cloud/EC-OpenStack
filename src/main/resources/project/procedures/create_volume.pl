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
# create.volume.pl
##########################
use warnings;
use strict;
use Encode;
use utf8;
use open IO => ':encoding(utf8)';

my $opts;

# Configuration: A commander configuration previously created.
$opts->{connection_config} = q{$[connection_config]};

# Display name: Display name for the new volume.
$opts->{display_name} = q{$[display_name]};

# Size: Size in GB for the new volume.
$opts->{size} = q{$[size]};

# Volume type: Type of the volume for the new volume.
$opts->{volume_type} = q{$[volume_type]};

# Availability zone: Availability zone for the new volume.
$opts->{availability_zone} = q{$[availability_zone]};

# Results location: property path to store server information.
$opts->{location} = q{$[location]};

# Deploy tag: tag to identify this deployment in the resource location
$opts->{tag} = q{$[tag]};

$[/myProject/procedure_helpers/preamble]

$openstack->create_volume();
exit($opts->{exitcode});
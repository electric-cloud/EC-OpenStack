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
# update.stack.pl
##########################
use warnings;
use strict;
use Encode;
use utf8;
use open IO => ':encoding(utf8)';

my $opts;

# Configuration: A commander configuration previously created.
$opts->{connection_config} = q{$[connection_config]};

# Stack Name: Display name of the stack to update.
$opts->{stack_name} = q{$[stack_name]};

# Stack ID: ID of the stack to update.
$opts->{stack_id} = q{$[stack_id]};

# Template: Updated stack template.
$opts->{template} = q{$[template]};

# Template URL: A URI to the location containing the stack template to instantiate. 
$opts->{template_url} = q{$[template_url]};

# Results location: property path to store server information.
$opts->{location} = q{$[location]};

# Deploy tag: tag to identify this deployment in the resource location
$opts->{tag} = q{$[tag]};

$[/myProject/procedure_helpers/preamble]

$openstack->update_stack();
exit($opts->{exitcode});

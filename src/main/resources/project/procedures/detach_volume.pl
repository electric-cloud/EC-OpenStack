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
# detach.volume.pl
##########################
use warnings;
use strict;
use Encode;
use utf8;
use open IO => ':encoding(utf8)';

my $opts;

# Configuration: A commander configuration previously created.
$opts->{connection_config} = q{$[connection_config]};

# Server Id: ID of the server from which volume to detach.
$opts->{server_id} = q{$[server_id]};

# Volume Id: ID of the volume to attach.
$opts->{volume_id} = q{$[volume_id]};

# Attachment Id: Volume attachment ID. 
$opts->{attachment_id} = q{$[attachment_id]};

$[/myProject/procedure_helpers/preamble]

$openstack->detach_volume();
exit($opts->{exitcode});

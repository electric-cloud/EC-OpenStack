##########################
# create.instancesnapshot.pl
##########################
use warnings;
use strict;
use Encode;
use utf8;
use open IO => ':encoding(utf8)';

my $opts;

# Configuration: A commander configuration previously created.
$opts->{connection_config} = q{$[connection_config]};

# Display name: Display name for the new instance snapshot.
$opts->{display_name} = q{$[display_name]};

# Server ID: ID of the server of which to create snapshot.
$opts->{server_id} = q{$[server_id]};

# Metadata: Metadata for new instance snapshot in key1,value1,key2,value2 ... format.
$opts->{metadata} = q{$[metadata]};

# Results location: property path to store server information.
$opts->{location} = q{$[location]};

# Deploy tag: tag to identify this deployment in the resource location
$opts->{tag} = q{$[tag]};

$[/myProject/procedure_helpers/preamble]

$openstack->take_instance_snapshot();
exit($opts->{exitcode});

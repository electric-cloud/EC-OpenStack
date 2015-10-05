##########################
# create.volumesnapshot.pl
##########################
use warnings;
use strict;
use Encode;
use utf8;
use open IO => ':encoding(utf8)';

my $opts;

# Configuration: A commander configuration previously created.
$opts->{connection_config} = q{$[connection_config]};

# Display name: Display name for the new snapshot.
$opts->{display_name} = q{$[display_name]};

# Display Description: Display description for the new snapshot.
$opts->{display_desc} = q{$[display_desc]};

# Force: Create snapshot forcefully of already attached volume.
$opts->{force} = q{$[force]};

# Volume ID: ID of the volume of which to take the snapshot.
$opts->{volume_id} = q{$[volume_id]};

# Results location: property path to store server information.
$opts->{location} = q{$[location]};

# Deploy tag: tag to identify this deployment in the resource location
$opts->{tag} = q{$[tag]};

$[/myProject/procedure_helpers/preamble]

$openstack->take_volume_snapshot();
exit($opts->{exitcode});

##########################
# delete.volume.pl
##########################
use warnings;
use strict;
use Encode;
use utf8;
use open IO => ':encoding(utf8)';

my $opts;

# Configuration: A commander configuration previously created.
$opts->{connection_config} = q{$[connection_config]};

# Volume ID: ID of the volume to be deleted.
$opts->{volume_id} = q{$[volume_id]};


$[/myProject/procedure_helpers/preamble]

$openstack->delete_volume();
exit($opts->{exitcode});

##########################
# extend_volume.pl
##########################
use warnings;
use strict;
use Encode;
use utf8;
use open IO => ':encoding(utf8)';

my $opts;

# Configuration: A commander configuration previously created.
$opts->{connection_config} = q{$[connection_config]};

# Tenant: Id of the tenant.
$opts->{tenant_id} = q{$[tenant_id]};

# Volume ID : ID of the volume that is to extend.
$opts->{volume_id} = q{$[volume_id]};

# New Size : Size to which the volume to extend.
$opts->{new_size} = q{$[new_size]};

# Results location: property path to store server information.
$opts->{location} = q{$[location]};

# Deploy tag: tag to identify this deployment in the resource location
$opts->{tag} = q{$[tag]};

$[/myProject/procedure_helpers/preamble]

$openstack->extend_volume();
exit($opts->{exitcode});

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

# Tenant: Id of the tenant.
$opts->{tenant_id} = q{$[tenant_id]};

# Name: Name for the new kewypair.
$opts->{display_name} = q{$[display_name]};

# Results location: property path to store information.
$opts->{size} = q{$[size]};

# Results tag: tag to identify this job in the resource location
$opts->{volume_type} = q{$[volume_type]};

$opts->{availability_zone} = q{$[availability_zone]};

$[/myProject/procedure_helpers/preamble]

$openstack->create_volume();
exit($opts->{exitcode});
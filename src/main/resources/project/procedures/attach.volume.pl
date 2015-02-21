##########################
# attach.volume.pl
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
$opts->{server_id} = q{$[server_id]};

# Results location: property path to store information.
$opts->{volume_id} = q{$[volume_id]};

# Results tag: tag to identify this job in the resource location
$opts->{device} = q{$[device]};

$[/myProject/procedure_helpers/preamble]

$openstack->attach_volume();
exit($opts->{exitcode});
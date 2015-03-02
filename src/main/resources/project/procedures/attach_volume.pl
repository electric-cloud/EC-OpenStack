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

# Server Id: ID of the server to which volume to attach.
$opts->{server_id} = q{$[server_id]};

# Volume Id: ID of the volume to attach.
$opts->{volume_id} = q{$[volume_id]};

# Device: Name of the device such as, /dev/vdb after attachment.
$opts->{device} = q{$[device]};

# Results location: property path to store server information.
$opts->{location} = q{$[location]};

# Deploy tag: tag to identify this deployment in the resource location
$opts->{tag} = q{$[tag]};

$[/myProject/procedure_helpers/preamble]

$openstack->attach_volume();
exit($opts->{exitcode});
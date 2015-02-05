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

# Tenant: Id of the tenant.
$opts->{tenant_id} = q{$[tenant_id]};

# Server Id: ID of the server from which volume to detach.
$opts->{server_id} = q{$[server_id]};

# Attachment Id: Volume attachment ID. 
$opts->{attachment_id} = q{$[attachment_id]};

$[/myProject/procedure_helpers/preamble]

$openstack->detach_volume();
exit($opts->{exitcode});

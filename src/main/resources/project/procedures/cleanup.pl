##########################
# cleanup.pl
##########################
use warnings;
use strict;
use Encode;
use utf8;
use open IO => ':encoding(utf8)';

my $opts;

# Configuration: A commander configuration previously created.
$opts->{connection_config} = q{$[connection_config]};

# Server: Id for the server to delete.
$opts->{server_id} = q{$[server_id]};

# Resource: name of the resource to delete.
$opts->{resource_name} = q{$[resource_name]};

# Tenant: Id of the tenant.
$opts->{tenant_id} = q{$[tenant_id]};

$[/myProject/procedure_helpers/preamble]

$openstack->cleanup();
exit($opts->{exitcode});

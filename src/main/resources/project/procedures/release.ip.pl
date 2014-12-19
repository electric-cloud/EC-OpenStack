##########################
# allocate.ip.pl
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

# Floating Ip: Id of the floating ip to release.
$opts->{address_id} = q{$[address_id]};



$[/myProject/procedure_helpers/preamble]

$openstack->release_ip();
exit($opts->{exitcode});

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

# Results location: property path to store information.
$opts->{location} = q{$[location]};

# Results tag: tag to identify this job in the resource location
$opts->{tag} = q{$[tag]};

$[/myProject/procedure_helpers/preamble]

$openstack->allocate_ip();
exit($opts->{exitcode});

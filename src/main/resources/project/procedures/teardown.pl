##########################
# teardown.pl
##########################
use warnings;
use strict;
use Encode;
use utf8;
use open IO => ':encoding(utf8)';

my $opts;

# Resource: name of the resource to delete.
$opts->{resource_name} = q{$[resName]};
# Conneciton config. Higher priority than config from resource's properties.
$opts->{connection_config} = q{$[connection_config]};
# Tenant id. Required for config.
$opts->{tenant_id} = q{$[tenant_id]};

$[/myProject/procedure_helpers/preamble];

$ec ||= ElectricCommander->new();

my $data = OpenStack::getInstancesForTermination($ec, $opts->{resource_name});
@$data = grep {$_->{createdBy} eq 'EC-OpenStack'}@$data;

# No instances for termination
if (!@$data) {
    print 'ERROR : No instances found';
    exit 1;
}

for my $d (@$data) {
    $opts->{tenant_id} = $d->{tenant_id};
    $opts->{server_id} = $d->{instance_id};
    $opts->{connection_config} = $d->{config};
    $opts->{resource_name} = $d->{resource_name};

    $openstack ||= $ec->__get_openstack_instance_by_options($opts);
    $openstack->get_authentication();
    $openstack->cleanup();
}
exit 0;

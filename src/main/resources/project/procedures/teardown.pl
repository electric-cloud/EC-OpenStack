##########################
# teardown.pl
##########################
use warnings;
use strict;
use Encode;
use utf8;
use open IO => ':encoding(utf8)';
use Data::Dumper;

my $opts;

# Resource: name of the resource to delete.
$opts->{resource_name} = q{$[resName]};
print Dumper $opts;


$[/myProject/procedure_helpers/preamble];

$ec ||= ElectricCommander->new();

my $data = OpenStack::getInstancesForTermination($ec, $opts->{resource_name});
@$data = grep {$_->{createdBy} eq 'EC-OpenStack'}@$data;
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

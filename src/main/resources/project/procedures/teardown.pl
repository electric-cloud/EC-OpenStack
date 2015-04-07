##########################
# teardown.pl
##########################
use warnings;
use strict;
use Encode;
use utf8;
use open IO => ':encoding(utf8)';

my $opts;

$[/myProject/procedure_helpers/preamble];

$ec ||= ElectricCommander->new();
$ec->abortOnError(0);

# Resource: name of the resource to delete.
$opts->{resource_name} = $ec->getPropertyValue("resName");
# Connection config. Higher priority than config from resource's properties.
$opts->{connection_config} = $ec->getPropertyValue("connection_config");

# Tenant id. Required for config.
$opts->{tenant_id} = $ec->getPropertyValue("tenant_id");

my $data = OpenStack::getInstancesForTermination($ec, $opts->{resource_name});
@$data = grep {$_->{createdBy} eq 'EC-OpenStack'}@$data;

# No instances for termination
if (!@$data) {
    print "No resource or resource pool with name '$opts->{resource_name}' found for termination. Nothing to do in this case.";
    #ECPREOPSTK-77: This is an acceptable condition since the resource creation may have failed during provisioning.
    #In this case, when the teardown is called as part of cleanup, we will not find any resources for termination.
    exit 0;
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

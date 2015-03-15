use ElectricCommander;
use File::Basename;
use ElectricCommander::PropDB;
use ElectricCommander::PropMod;
use Encode;
use Data::Dumper;
use utf8;

$| = 1;

use constant {
    SUCCESS => 0,
    ERROR   => 1,
};

# Create ElectricCommander instance
my $ec = new ElectricCommander();
$ec->abortOnError(0);

my $pluginKey  = 'EC-OpenStack';
my $xpath      = $ec->getPlugin($pluginKey);
my $pluginName = $xpath->findvalue('//pluginVersion')->value;
# print "Using plugin $pluginKey version $pluginName\n";
$opts->{pluginVer} = $pluginName;
# print "Opts before\n";
# print Dumper $opts;

# loading OpenStack driver
if (!ElectricCommander::PropMod::loadPerlCodeFromProperty( $ec, '/myProject/plugin_driver/OpenStack')) {
    print 'Could not load OpenStack.pm\n';
    exit ERROR;
}

# I have no idea how to do that proper. $opts is too global
*{ElectricCommander::__get_openstack_instance_by_options} = sub {
    my (undef, $options) = @_;
    if (!$opts->{connection_config}) {
	return ($options, undef);
    }

    my $cfgName = $options->{connection_config};
    print "Loading config $cfgName\n";
    $options->{keystone_api_version} = $xpath->findvalue('//keystone_api_version')->value;
    my $proj = "$[/myProject/projectName]";
    my $cfg  = new ElectricCommander::PropDB( $ec, "/projects/$proj/openstack_cfgs");
    my %vals = $cfg->getRow($cfgName);

    # Check if configuration exists
    unless ( keys(%vals) ) {
	print "Configuration [$cfgName] does not exist\n";
	exit ERROR;
    }

    # Add all options from configuration
    foreach my $c ( keys %vals ) {
	print "Adding config $c = $vals{$c}\n";
	$options->{$c} = $vals{$c};
    }

    # Check that credential item exists
    if ( !defined $options->{credential} || $options->{credential} eq "" ) {
	print "Configuration [$cfgName] does not contain a OpenStack credential\n";
	exit ERROR;
    }

    # Get user/password out of credential named in $options->{credential}
    my $xpath = $ec->getFullCredential("$options->{credential}");

    $options->{config_user} = $xpath->findvalue("//userName");
    $options->{config_pass} = $xpath->findvalue("//password");

    # Check for required items
    if (!defined $options->{identity_service_url} || $options->{identity_service_url} eq "" ) {
	print "Configuration [$cfgName] does not contain a OpenStack identity service url\n";
	exit ERROR;
    }

    if ( !defined $options->{compute_service_url} || $options->{compute_service_url} eq "" ) {
	print "Configuration [$cfgName] does not contain a OpenStack compute service url\n";
	exit ERROR;
    }
    if ( !defined $options->{blockstorage_service_url} || $options->{blockstorage_service_url} eq "" ) {
	print "Configuration [$cfgName] does not contain a OpenStack block storage service url\n";
	exit ERROR;
    }

    if ( !defined $options->{image_service_url} || $options->{image_service_url} eq "" ) {
	print "Configuration [$cfgName] does not contain a OpenStack image service url\n";
	exit ERROR;
    }

    $options->{JobStepId} =  '$[/myJobStep/jobStepId]';
    my $openstack = new OpenStack($ec, $options);
    return ($options, $openstack);
};
# $opts->{JobStepId} =  "$[/myJobStep/jobStepId]";

# Load the actual code into this process


# Make an instance of the object, passing in options as a hash

my $openstack;
($opts, $openstack) = ElectricCommander->__get_openstack_instance_by_options($opts);

if ($openstack) {
    $openstack->get_authentication();
}


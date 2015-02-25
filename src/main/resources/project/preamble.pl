use ElectricCommander;
use File::Basename;
use ElectricCommander::PropDB;
use ElectricCommander::PropMod;
use Encode;
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
print "Using plugin $pluginKey version $pluginName\n";
$opts->{pluginVer} = $pluginName;

if ( defined( $opts->{connection_config} ) && $opts->{connection_config} ne "" )
{
    my $cfgName = $opts->{connection_config};
    print "Loading config $cfgName\n";

    $opts->{keystone_api_version} = $xpath->findvalue('//keystone_api_version')->value;

    my $proj = "$[/myProject/projectName]";
    my $cfg  = new ElectricCommander::PropDB( $ec,
        "/projects/$proj/openstack_cfgs" );

    my %vals = $cfg->getRow($cfgName);

    # Check if configuration exists
    unless ( keys(%vals) ) {
        print "Configuration [$cfgName] does not exist\n";
        exit ERROR;
    }

    # Add all options from configuration
    foreach my $c ( keys %vals ) {
        print "Adding config $c = $vals{$c}\n";
        $opts->{$c} = $vals{$c};
    }

    # Check that credential item exists
    if ( !defined $opts->{credential} || $opts->{credential} eq "" ) {
        print
"Configuration [$cfgName] does not contain a OpenStack credential\n";
        exit ERROR;
    }

    # Get user/password out of credential named in $opts->{credential}
    my $xpath = $ec->getFullCredential("$opts->{credential}");

    $opts->{config_user} = $xpath->findvalue("//userName");
    $opts->{config_pass} = $xpath->findvalue("//password");

    # Check for required items
    if ( !defined $opts->{identity_service_url}
        || $opts->{identity_service_url} eq "" )
    {
        print
"Configuration [$cfgName] does not contain a OpenStack identity service url\n";
        exit ERROR;
    }

    if ( !defined $opts->{compute_service_url}
        || $opts->{compute_service_url} eq "" )
    {
        print
"Configuration [$cfgName] does not contain a OpenStack compute service url\n";
        exit ERROR;
    }

    if ( !defined $opts->{blockstorage_service_url}
        || $opts->{blockstorage_service_url} eq "" )
    {
         print
"Configuration [$cfgName] does not contain a OpenStack block storage service url\n";
         exit ERROR;
    }

    if ( !defined $opts->{image_service_url}
        || $opts->{image_service_url} eq "" )
    {
         print
"Configuration [$cfgName] does not contain a OpenStack image service url\n";
         exit ERROR;
    }
}

$opts->{JobStepId} =  "$[/myJobStep/jobStepId]";

# Load the actual code into this process
if (
    !ElectricCommander::PropMod::loadPerlCodeFromProperty(
        $ec, '/myProject/plugin_driver/OpenStack'
    )
  )
{
    print 'Could not load OpenStack.pm\n';
    exit ERROR;
}

# Make an instance of the object, passing in options as a hash
my $openstack = new OpenStack($ec, $opts);

$openstack->get_authentication();




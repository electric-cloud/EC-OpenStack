use ElectricCommander;
use ElectricCommander::PropDB;

$::ec = new ElectricCommander();
$::ec->abortOnError(0);

$| = 1;

my $number   = "$[number]";     # quantity
my $poolName = "$[poolName]";

my $open_config      = "$[connection_config]";
my $open_server_name = "$[server_name]";
my $open_image       = "$[image]";
my $open_flavor      = "$[flavor]";
my $open_tenant_id   = "$[tenant_id]";
my $open_workspace   = "$[resource_workspace]";
my $open_tag         = "$[tag]";

my @deparray = split(/\|/, $deplist);

sub main {
    print "OpenStack Grow:\n";

    # Validate inputs
    $number   =~ s/[^0-9]//gixms;
    $poolName =~ s/[^A-Za-z0-9_-\s].*//gixms;

    $open_config      =~ s/[^A-Za-z0-9_-\s]//gixms;
    $open_server_name =~ s/[^A-Za-z0-9_-\s]//gixms;
    $open_image       =~ s/[^A-Za-z0-9_-\s]//gixms;
    $open_flavor      =~ s/[^A-Za-z0-9_-\s]//gixms;
    $open_tenant_id   =~ s/[^A-Za-z0-9_-\s]//gixms;
    $open_workspace   =~ s/[^A-Za-z0-9_-\s]//gixms;
    $open_tag         =~ s/[^A-Za-z0-9_-\s]//gixms;
    $open_location    =~ s/[^A-Za-z0-9_\-\/\s]//gixms;

    my $xmlout = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n";
    addXML(\$xmlout, "<GrowResponse>");

    ### CREATE Servers ###
    print("Running OpenStack Deploy\n");
    my $proj = "$[/myProject/projectName]";
    my $proc = "Deploy";
    my $xPath = $::ec->runProcedure(
                                    "$proj",
                                    {
                                       procedureName   => "$proc",
                                       pollInterval    => 1,
                                       timeout         => 3600,
                                       actualParameter => [{ actualParameterName => "connection_config", value => "$open_config" }, { actualParameterName => "server_name", value => "$open_server_name-$[jobStepId]" }, { actualParameterName => "image", value => "$open_image" }, { actualParameterName => "flavor", value => "$open_flavor" }, { actualParameterName => "quantity", value => "$number" }, { actualParameterName => "location", value => "/myJob/OpenStack/deployed" }, { actualParameterName => "tag", value => "$open_tag" }, { actualParameterName => "resource_check", value => '1' }, { actualParameterName => "resource_pool", value => "$poolName" }, { actualParameterName => "resource_workspace", value => "$open_workspace" }, { actualParameterName => "tenant_id", value => "$open_tenant_id" },],
                                    }
                                   );
    if ($xPath) {
        my $code = $xPath->findvalue('//code');
        if ($code ne "") {
            my $mesg = $xPath->findvalue('//message');
            print "Run procedure returned code is '$code'\n$mesg\n";
        }
    }
    my $outcome = $xPath->findvalue('//outcome')->string_value;
    if ("$outcome" ne "success") {
        print "OpenStack Deploy job failed.\n";
        exit 1;
    }
    my $jobId = $xPath->findvalue('//jobId')->string_value;
    if (!$jobId) {
        exit 1;
    }

    print "Deploy succeded\n";

    my $depobj = new ElectricCommander::PropDB($::ec, "");
    my $vmList = $depobj->getProp("/jobs/$jobId/OpenStack/deployed/$open_tag/vmsList");
    print "VM list=$vmList\n";
    my @vms = split(/;/, $vmList);
    my $createdList = ();

    foreach my $vm (@vms) {
        addXML(\$xmlout, "  <Deployment>");
        addXML(\$xmlout, "      <handle>$vm</handle>");
        addXML(\$xmlout, "      <hostname>" . $depobj->getProp("/jobs/$jobId/OpenStack/deployed/$open_tag/Server-$vm/Address") . "</hostname>");
        addXML(\$xmlout, "      <Name>" . $depobj->getProp("/jobs/$jobId/OpenStack/deployed/$open_tag/Server-$vm/Name") . "</Name>");
        addXML(\$xmlout, "      <resource>" . $depobj->getProp("/jobs/$jobId/OpenStack/deployed/$open_tag/Server-$vm/Resource") . "</resource>");
        addXML(\$xmlout, "      <Private>" . $depobj->getProp("/jobs/$jobId/OpenStack/deployed/$open_tag/Server-$vm/Private") . "</Private>");
        addXML(\$xmlout, "      <Tenant>" . $depobj->getProp("/jobs/$jobId/OpenStack/deployed/$open_tag/Server-$vm/Tenant") . "</Tenant>");
        addXML(\$xmlout, "      <KeyPairId>" . $depobj->getProp("/jobs/$jobId/OpenStack/deployed/$open_tag/KeyPairId") . "</KeyPairId>");
        addXML(\$xmlout, "      <results>/jobs/$jobId/OpenStack/deployed</results>");
        addXML(\$xmlout, "      <tag>$open_tag</tag>");
        addXML(\$xmlout, "  </Deployment>");
    }

    addXML(\$xmlout, "</GrowResponse>");

    my $prop = "/myJob/CloudManager/grow";
    print "Registering results for $vmList in $prop\n";
    $::ec->setProperty("$prop", $xmlout);
}

sub addXML {
    my ($xml, $text) = @_;
    ## TODO encode
    ## TODO autoindent
    $$xml .= $text;
    $$xml .= "\n";
}

main();

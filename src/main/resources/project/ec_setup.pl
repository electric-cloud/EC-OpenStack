# The plugin is being promoted, create a property reference in the server's property sheet
# Data that drives the create step picker registration for this plugin.
no warnings qw/redefine/;
use XML::Simple;
use ElectricCommander::Util;

my %deploy = (
    label       => "OpenStack - Deploy",
    procedure   => "Deploy",
    description => "Deploy a new Openstack server.",
    category    => "Resource Management"
);

my %cleanup = (
    label       => "OpenStack - Cleanup",
    procedure   => "Cleanup",
    description => "Delete an existing server.",
    category    => "Resource Management"
);
my %teardown = (
    label       => "OpenStack - Teardown",
    procedure   => "Teardown",
    description => "Teardown Resource or resource pool.",
    category    => "Resource Management"
);

my %createkey = (
    label       => "OpenStack - CreateKeyPair",
    procedure   => "CreateKeyPair",
    description => "Create a new key pair.",
    category    => "Resource Management"
);
my %deletekey = (
    label       => "OpenStack - DeleteKeyPair",
    procedure   => "DeleteKeyPair",
    description => "Delete an existing key pair.",
    category    => "Resource Management"
);
my %allocate = (
    label       => "OpenStack - AllocateIP",
    procedure   => "AllocateIP",
    description => "Allocate a new Floating IP.",
    category    => "Resource Management"
);

my %release = (
    label       => "OpenStack - ReleaseIP",
    procedure   => "ReleaseIP",
    description => "Delete an existing Floating IP.",
    category    => "Resource Management"
);

my %associate = (
    label       => "OpenStack - AssociateFloatingIP",
    procedure   => "AssociateFloatingIP",
    description => "Associate a floating IP to an existing instance.",
    category    => "Resource Management"
);

my %createvolume = (
    label       => "OpenStack - CreateVolume",
    procedure   => "CreateVolume",
    description => "Create a new volume.",
    category    => "Resource Management"
);

my %attachvolume = (
    label       => "OpenStack - AttachVolume",
    procedure   => "AttachVolume",
    description => "Attach a volume to a server.",
    category    => "Resource Management"
);

my %extendvolume = (
    label       => "OpenStack - ExtendVolume",
    procedure   => "ExtendVolume",
    description => "Extend the size of existing volume.",
    category    => "Resource Management"
);

my %detachvolume = (
    label       => "OpenStack - DetachVolume",
    procedure   => "DetachVolume",
    description => "Detach a volume from a server.",
    category    => "Resource Management"
);

my %deletevolume = (
    label       => "OpenStack - DeleteVolume",
    procedure   => "DeleteVolume",
    description => "Delete a volume.",
    category    => "Resource Management"
);

my %reboot = (
    label       => "OpenStack - RebootInstance",
    procedure   => "RebootInstance",
    description => "Reboots an instance.",
    category    => "Resource Management"
);

my %createvolumesnapshot = (
    label       => "OpenStack - CreateVolumeSnapshot",
    procedure   => "CreateVolumeSnapshot",
    description => "Creates a new snapshot of a volume.",
    category    => "Resource Management"
);

my %createimage = (
    label       => "OpenStack - CreateImage",
    procedure   => "CreateImage",
    description => "Creates a new image.",
    category    => "Resource Management"
);


my %createinstancesnapshot = (
    label       => "OpenStack - CreateInstanceSnapshot",
    procedure   => "CreateInstanceSnapshot",
    description => "Creates a new snapshot of an instance.",
    category    => "Resource Management"
);

my %createstack = (
    label       => "OpenStack - CreateHeatStack",
    procedure   => "CreateStack",
    description => "Creates a new heat stack from a template.",
    category    => "Resource Management"
);

my %updatestack = (
    label       => "OpenStack - UpdateHeatStack",
    procedure   => "UpdateStack",
    description => "Updates existing stack to specified template.",
    category    => "Resource Management"
);

my %deletestack = (
    label       => "OpenStack - DeleteHeatStack",
    procedure   => "DeleteStack",
    description => "Deletes existing stack.",
    category    => "Resource Management"
);

$batch->deleteProperty("/server/ec_customEditors/pickerStep/OpenStack - Deploy");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/OpenStack - Cleanup");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/OpenStack - CreateKeyPair");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/OpenStack - DeleteKeyPair");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/OpenStack - AllocateIP");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/OpenStack - ReleaseIP");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/OpenStack - AssociateFloatingIP");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/OpenStack - CreateVolume");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/OpenStack - AttachVolume");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/OpenStack - ExtendVolume");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/OpenStack - DetachVolume");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/OpenStack - DeleteVolume");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/OpenStack - RebootInstance");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/OpenStack - CreateVolumeSnapshot");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/OpenStack - CreateImage");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/OpenStack - CreateInstanceSnapshot");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/OpenStack - CreateHeatStack");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/OpenStack - UpdateHeatStack");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/OpenStack - DeleteHeatStack");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/OpenStack - Teardown");

@::createStepPickerSteps = (\%deploy, \%cleanup, \%createkey, \%deletekey, \%allocate, \%release, \%associate, \%createvolume, \%attachvolume, \%extendvolume, \%detachvolume, \%deletevolume, \%reboot, \%createvolumesnapshot, \%createimage, \%createinstancesnapshot, \%createstack, \%updatestack, \%deletestack, \%teardown);

my $pluginName = "@PLUGIN_NAME@";
my $pluginKey  = "@PLUGIN_KEY@";
if ($promoteAction ne '') {
    my @objTypes = ('projects', 'resources', 'workspaces');
    my $query    = $commander->newBatch();
    my @reqs     = map { $query->getAclEntry('user', "project: $pluginName", { systemObjectName => $_ }) } @objTypes;
    push @reqs, $query->getProperty('/server/ec_hooks/promote');
    $query->submit();

    foreach my $type (@objTypes) {
        if ($query->findvalue(shift @reqs, 'code') ne 'NoSuchAclEntry') {
            $batch->deleteAclEntry('user', "project: $pluginName", { systemObjectName => $type });
        }
    }

    if ($promoteAction eq "promote") {
        foreach my $type (@objTypes) {
            $batch->createAclEntry(
                                   'user',
                                   "project: $pluginName",
                                   {
                                      systemObjectName           => $type,
                                      readPrivilege              => 'allow',
                                      modifyPrivilege            => 'allow',
                                      executePrivilege           => 'allow',
                                      changePermissionsPrivilege => 'allow'
                                   }
                                  );
        }
    }
}

if ($upgradeAction eq "upgrade") {
    patch_configs("/plugins/$otherPluginName/project/openstack_cfgs");
    my $query   = $commander->newBatch();
    my $newcfg  = $query->getProperty("/plugins/$pluginName/project/openstack_cfgs");

    my $old_cfgs_path = "/plugins/$otherPluginName/project/openstack_cfgs";
    my $new_cfgs_path = "/plugins/$pluginName/project/openstack_cfgs";
    my $oldcfgs = $query->getProperty($old_cfgs_path);
    my $creds   = $query->getCredentials("\$[/plugins/$otherPluginName]");

    local $self->{abortOnError} = 0;
    $query->submit();

    # if new plugin does not already have cfgs
    if ($query->findvalue($newcfg, "code") eq "NoSuchProperty") {

        # if old cfg has some cfgs to copy
        if ($query->findvalue($oldcfgs, "code") ne "NoSuchProperty") {
            $batch->clone(
                          {
                            path      => "/plugins/$otherPluginName/project/openstack_cfgs",
                            cloneName => "/plugins/$pluginName/project/openstack_cfgs"
                          }
                         );
        }
    }
    # Copy configuration credentials and attach them to the appropriate steps
    my $nodes = $query->find($creds);
    if ($nodes) {
        my @nodes = $nodes->findnodes('credential/credentialName');
        for (@nodes) {
            my $cred = $_->string_value;

            # Clone the credential
            $batch->clone(
                          {
                            path      => "/plugins/$otherPluginName/project/credentials/$cred",
                            cloneName => "/plugins/$pluginName/project/credentials/$cred"
                          }
                         );

            # Make sure the credential has an ACL entry for the new project principal
            my $xpath = $commander->getAclEntry(
                                                "user",
                                                "project: $pluginName",
                                                {
                                                   projectName    => $otherPluginName,
                                                   credentialName => $cred
                                                }
                                               );
            if ($xpath->findvalue("//code") eq "NoSuchAclEntry") {
                $batch->deleteAclEntry(
                                       "user",
                                       "project: $otherPluginName",
                                       {
                                          projectName    => $pluginName,
                                          credentialName => $cred
                                       }
                                      );
                $batch->createAclEntry(
                                       "user",
                                       "project: $pluginName",
                                       {
                                          projectName                => $pluginName,
                                          credentialName             => $cred,
                                          readPrivilege              => 'allow',
                                          modifyPrivilege            => 'allow',
                                          executePrivilege           => 'allow',
                                          changePermissionsPrivilege => 'allow'
                                       }
                                      );
            }

        # Attach the credential to the appropriate steps
        $batch->attachCredential(
                                 "\$[/plugins/$pluginName/project]",
                                 $cred,
                                 {
                                    procedureName => 'Deploy',
                                    stepName      => 'Deploy'
                                 }
                                );
        $batch->attachCredential(
                                 "\$[/plugins/$pluginName/project]",
                                 $cred,
                                 {
                                    procedureName => '_DeployDE',
                                    stepName      => '_DeployDE'
                                 }
                                );
        $batch->attachCredential(
		"\$[/plugins/$pluginName/project]",
		$cred,
		{
		    procedureName => 'Cleanup',
		    stepName      => 'Cleanup'
		}
	    );

	    $batch->attachCredential(
		"\$[/plugins/$pluginName/project]",
		$cred,
		{
		    procedureName => 'Teardown',
		    stepName      => 'Teardown'
		}
	    );

            $batch->attachCredential(
                                     "\$[/plugins/$pluginName/project]",
                                     $cred,
                                     {
                                        procedureName => 'CreateKeyPair',
                                        stepName      => 'CreateKeyPair'
                                     }
                                    );
            $batch->attachCredential(
                                     "\$[/plugins/$pluginName/project]",
                                     $cred,
                                     {
                                        procedureName => 'DeleteKeyPair',
                                        stepName      => 'DeleteKeyPair'
                                     }
                                    );

            $batch->attachCredential(
                                     "\$[/plugins/$pluginName/project]",
                                     $cred,
                                     {
                                        procedureName => 'AllocateIP',
                                        stepName      => 'AllocateIP'
                                     }
                                    );
            $batch->attachCredential(
                                     "\$[/plugins/$pluginName/project]",
                                     $cred,
                                     {
                                        procedureName => 'ReleaseIP',
                                        stepName      => 'ReleaseIP'
                                     }
                                    );
            $batch->attachCredential(
                                     "\$[/plugins/$pluginName/project]",
                                     $cred,
                                     {
                                        procedureName => 'AssociateFloatingIP',
                                        stepName      => 'AssociateFloatingIP'
                                     }
                                    );
            $batch->attachCredential(
                                     "\$[/plugins/$pluginName/project]",
                                     $cred,
                                     {
                                        procedureName => 'CloudManagerGrow',
                                        stepName      => 'grow'
                                     }
                                    );
            $batch->attachCredential(
                                     "\$[/plugins/$pluginName/project]",
                                     $cred,
                                     {
                                        procedureName => 'CloudManagerShrink',
                                        stepName      => 'shrink'
                                     }
                                    );
            $batch->attachCredential(
                                     "\$[/plugins/$pluginName/project]",
                                     $cred,
                                     {
                                        procedureName => 'CloudManagerSync',
                                        stepName      => 'sync'
                                     }
                                    );
            $batch->attachCredential(
                                     "\$[/plugins/$pluginName/project]",
                                     $cred,
                                     {
                                        procedureName => 'CreateVolume',
                                        stepName      => 'CreateVolume'
                                     }
                                    );
            $batch->attachCredential(
                                     "\$[/plugins/$pluginName/project]",
                                     $cred,
                                     {
                                        procedureName => 'AttachVolume',
                                        stepName      => 'AttachVolume'
                                     }
                                    );
            $batch->attachCredential(
                                     "\$[/plugins/$pluginName/project]",
                                     $cred,
                                     {
                                        procedureName => 'ExtendVolume',
                                        stepName      => 'ExtendVolume'
                                     }
                                    );

            $batch->attachCredential(
                                     "\$[/plugins/$pluginName/project]",
                                      $cred,
                                      {
                                        procedureName => 'DetachVolume',
                                        stepName      => 'DetachVolume'
                                     }
                                    );
            $batch->attachCredential(
                                     "\$[/plugins/$pluginName/project]",
                                     $cred,
                                     {
                                        procedureName => 'DeleteVolume',
                                        stepName      => 'DeleteVolume'
                                     }
                                    );
            $batch->attachCredential(
                                     "\$[/plugins/$pluginName/project]",
                                     $cred,
                                     {
                                        procedureName => 'RebootInstance',
                                        stepName      => 'RebootInstance'
                                     }
                                    );

            $batch->attachCredential(
                                     "\$[/plugins/$pluginName/project]",
                                     $cred,
                                     {
                                         procedureName => 'CreateVolumeSnapshot',
                                         stepName      => 'CreateVolumeSnapshot'
                                     }
                                    );

            $batch->attachCredential(
                                     "\$[/plugins/$pluginName/project]",
                                     $cred,
                                     {
                                         procedureName => 'CreateImage',
                                         stepName      => 'CreateImage'
                                     }
                                    );

            $batch->attachCredential(
                                     "\$[/plugins/$pluginName/project]",
                                     $cred,
                                     {
                                         procedureName => 'CreateInstanceSnapshot',
                                         stepName      => 'CreateInstanceSnapshot'
                                     }
                                    );

            $batch->attachCredential(
                                     "\$[/plugins/$pluginName/project]",
                                     $cred,
                                     {
                                          procedureName => 'CreateStack',
                                          stepName      => 'CreateStack'
                                     }
                                    );

            $batch->attachCredential(
                                     "\$[/plugins/$pluginName/project]",
                                     $cred,
                                     {
                                          procedureName => 'UpdateStack',
                                          stepName      => 'UpdateStack'
                                     }
                                    );
            $batch->attachCredential(
                                     "\$[/plugins/$pluginName/project]",
                                     $cred,
                                     {
                                          procedureName => 'DeleteStack',
                                          stepName      => 'DeleteStack'
                                     }
                                    );
        }
    }
}

# Set the credentialProtected flag on the validation and parameterOptions
# property sheets if installing the plugin on EF server 6.1+. Doing this
# programatically allows the plugin to continue to be supported on
# older versions of EF server.
my $xpath = $commander->getVersions();
my $serverVersion = $xpath->findvalue('//version')->string_value();

if (compareMinor($serverVersion, '6.1') >= 0) {
     # Flag the property sheet as being protected by credentials
     # attached to the enclosing procedure's steps.

     $commander->modifyProperty("/projects/$pluginName/procedures/CreateConfiguration/ec_form/validation", {credentialProtected => "1"});
     $commander->modifyProperty("/projects/$pluginName/procedures/CreateConfiguration/ec_form/parameterOptions", {credentialProtected => "1"});
     $commander->modifyProperty("/projects/$pluginName/procedures/_DeployDE/ec_form/parameterOptions", {credentialProtected => "1"});

       if ($upgradeAction eq 'upgrade') {
         # If the disable flags were set on the earlier version of the plugin,
         # then set them when upgrading.
         my $batch = $commander->newBatch();
         my @reqIds = (
             $batch->getProperty("/plugins/$otherPluginName/project/ec_disable_dynamic_operations"),
             $batch->getProperty("/plugins/$otherPluginName/project/procedures/CreateConfiguration/ec_form/disable"),
             $batch->getProperty("/plugins/$otherPluginName/project/procedures/_DeployDE/ec_form/disable"),
         );
         $batch->submit();

         my $disabledOps = $batch->findvalue($reqIds[0], 'property/value')->string_value();
         my $disabledCreateConfigurationOp = $batch->findvalue($reqIds[1], 'property/value')->string_value();
         my $disabledDeployOp = $batch->findvalue($reqIds[2], 'property/value')->string_value();

         if ($disabledOps == '1') {
             my $desc = $batch->findvalue($reqIds[0], 'property/description');
             $commander->setProperty( "/plugins/$pluginName/project/ec_disable_dynamic_operations", { value => '1', description => $desc});
         }

         if ($disabledCreateConfigurationOp == '1') {
             my $desc = $batch->findvalue($reqIds[1], 'property/description');
             $commander->setProperty( "/plugins/$pluginName/project/procedures/CreateConfiguration/ec_form/disable", { value => '1', description => $desc});
         }

         if ($disabledDeployOp == '1') {
             my $desc = $batch->findvalue($reqIds[2], 'property/description');
             $commander->setProperty( "/plugins/$pluginName/project/procedures/_DeployDE/ec_form/disable", { value => '1', description => $desc});
         }

       }
}

removeTenantId();

sub patch_configs {
    my ($config_path) = @_;

    my $configs = '';
    eval {
        my $res = $commander->getProperty($config_path);
        $configs = $res->findvalue('//propertySheetId')->string_value();
    };
    unless ($configs) {
        return;
    }
    my $cfg_list = undef;
    eval {
        my $t = $commander->getProperties({propertySheetId => $configs});
        my $cfg_data = XMLin($t->{_xml});
        $cfg_list = $cfg_data->{response}->{propertySheet}->{property};
        if (ref $cfg_list eq 'HASH') {
            $cfg_list = [$cfg_list];
        }
        if (ref $cfg_list ne 'ARRAY') {
            $cfg_list = [];
        }
    };

    for my $c (@$cfg_list) {
        my $description = $c->{description};
        $description or next;
        eval {
            my $prop = $commander->getProperty($config_path . '/' . $c->{propertyName});
            1;
        } or do {
            next;
        };
        $commander->deleteProperty($config_path . '/' . $c->{propertyName} . '/desc');
        $commander->setProperty($config_path . '/' . $c->{propertyName} . '/desc' => $description);

    }
    return 1;
}

sub removeTenantId {
       my $commander = ElectricCommander->new();
          my $steps = $commander->findObjects("procedureStep", {
              filter => [
                  {
                      propertyName => "subproject",
                      operator     => "equals",
                      operand1     => "/plugins/EC-OpenStack/project"
                  },
                  {
                      propertyName => "subprocedure",
                      operator     => "notEqual",
                      operand1     => "CreateConfiguration"
                  }
              ]
          });

          # Walk through all the steps
          for my $step ($steps->findnodes("//step")) {
              my $projectName = $step->findvalue("projectName");
              my $procedureName = $step->findvalue("procedureName");
              my $stepName = $step->findvalue("stepName");

              my $parameters = $commander->getActualParameters({
                  projectName     => $projectName,
                  procedureName   => $procedureName,
                  stepName        => $stepName});

              my $tenantIdExists = 0;

              for my $parameter ($parameters->findnodes("//actualParameter")) {
                  my $name = $parameter->findvalue("actualParameterName");
                  if ($name eq "tenant_id") {
                      $tenantIdExists = 1;

                  }
              }
              if ($tenantIdExists) {
                  $commander->deleteActualParameter($projectName, $procedureName,
                      $stepName, "tenant_id");
              }
          }
}


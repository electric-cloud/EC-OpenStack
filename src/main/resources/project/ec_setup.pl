
# The plugin is being promoted, create a property reference in the server's property sheet
# Data that drives the create step picker registration for this plugin.
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

@::createStepPickerSteps = (\%deploy, \%cleanup, \%createkey, \%deletekey, \%allocate, \%release, \%associate, \%createvolume, \%attachvolume, \%extendvolume);

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
    my $query   = $commander->newBatch();
    my $newcfg  = $query->getProperty("/plugins/$pluginName/project/openstack_cfgs");
    my $oldcfgs = $query->getProperty("/plugins/$otherPluginName/project/openstack_cfgs");
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
                                        procedureName => 'Cleanup',
                                        stepName      => 'Cleanup'
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
        }
    }
}


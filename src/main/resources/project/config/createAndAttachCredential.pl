##########################
# createAndAttachCredential.pl
##########################

use ElectricCommander;

use constant {
               SUCCESS => 0,
               ERROR   => 1,
             };

my $ec = new ElectricCommander();
$ec->abortOnError(0);

my $credName = "$[/myJob/config]";
my $xpath    = $ec->getFullCredential("credential");
my $userName = $xpath->findvalue("//userName");

my $password = $xpath->findvalue("//password");

# Create credential
my $projName = "$[/myProject/projectName]";

$ec->deleteCredential($projName, $credName);
$xpath = $ec->createCredential($projName, $credName, $userName, $password);
my $errors = $ec->checkAllErrors($xpath);

# Give config the credential's real name
my $configPath = "/projects/$projName/openstack_cfgs/$credName";
$xpath = $ec->setProperty($configPath . "/credential", $credName);
$errors .= $ec->checkAllErrors($xpath);

# Give job launcher full permissions on the credential
my $user = "$[/myJob/launchedByUser]";
$xpath = $ec->createAclEntry(
                             "user", $user,
                             {
                                projectName                => $projName,
                                credentialName             => $credName,
                                readPrivilege              => allow,
                                modifyPrivilege            => allow,
                                executePrivilege           => allow,
                                changePermissionsPrivilege => allow
                             }
                            );
$errors .= $ec->checkAllErrors($xpath);

# Attach credential to steps that will need it
$xpath = $ec->attachCredential(
                               $projName,
                               $credName,
                               {
                                  procedureName => "Deploy",
                                  stepName      => "Deploy"
                               }
                              );
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential(
                               $projName,
                               $credName,
                               {
                                  procedureName => "Cleanup",
                                  stepName      => "Cleanup"
                               }
                              );
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential(
                               $projName,
                               $credName,
                               {
                                  procedureName => "CreateKeyPair",
                                  stepName      => "CreateKeyPair"
                               }
                              );
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential(
                               $projName,
                               $credName,
                               {
                                  procedureName => "DeleteKeyPair",
                                  stepName      => "DeleteKeyPair"
                               }
                              );
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential(
                               $projName,
                               $credName,
                               {
                                  procedureName => "AllocateIP",
                                  stepName      => "AllocateIP"
                               }
                              );
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential(
                               $projName,
                               $credName,
                               {
                                  procedureName => "ReleaseIP",
                                  stepName      => "ReleaseIP"
                               }
                              );
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential(
                               $projName,
                               $credName,
                               {
                                  procedureName => "CreateConfiguration",
                                  stepName      => "CreateConfiguration"
                               }
                              );
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential(
                               $projName,
                               $credName,
                               {
                                  procedureName => "CloudManagerGrow",
                                  stepName      => "grow"
                               }
                              );
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential(
                               $projName,
                               $credName,
                               {
                                  procedureName => "CloudManagerShrink",
                                  stepName      => "shrink"
                               }
                              );
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential(
                               $projName,
                               $credName,
                               {
                                  procedureName => "CloudManagerSync",
                                  stepName      => "sync"
                               }
                              );
$errors .= $ec->checkAllErrors($xpath);


$xpath = $ec->attachCredential(
                               $projName,
                               $credName,
                               {
                                  procedureName => "AssociateFloatingIP",
                                  stepName      => "AssociateFloatingIP"
                               }
                              );
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential(
                               $projName,
                               $credName,
                               {
                                  procedureName => "CreateVolume",
                                  stepName      => "CreateVolume"
                               }
                              );
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential(
                               $projName,
                               $credName,
                               {
                                  procedureName => "AttachVolume",
                                  stepName      => "AttachVolume"
                               }
                              );
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential(
                               $projName,
                               $credName,
                               {
                                  procedureName => "DetachVolume",
                                  stepName      => "DetachVolume"
                               }
                              );
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential(
                               $projName,
                               $credName,
                               {
                                  procedureName => "DeleteVolume",
                                  stepName      => "DeleteVolume"
                               }
                              );
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential(
                               $projName,
                               $credName,
                               {
                                  procedureName => "RebootInstance",
                                  stepName      => "RebootInstance"
                               }
                              );
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential(
                               $projName,
                               $credName,
                               {
                                  procedureName => "CreateVolumeSnapshot",
                                  stepName      => "CreateVolumeSnapshot"
                               }
                              );
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential(
                               $projName,
                               $credName,
                               {
                                  procedureName => "CreateImage",
                                  stepName      => "CreateImage"
                               }
                              );
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential(
                               $projName,
                               $credName,
                               {
                                  procedureName => "CreateInstanceSnapshot",
                                  stepName      => "CreateInstanceSnapshot"
                               }
                              );
$errors .= $ec->checkAllErrors($xpath);


$xpath = $ec->attachCredential(
                               $projName,
                               $credName,
                               {
                                  procedureName => "CreateStack",
                                  stepName      => "CreateStack"
                               }
                              );
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential(
                               $projName,
                               $credName,
                               {
                                  procedureName => "UpdateStack",
                                  stepName      => "UpdateStack"
                               }
                              );
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential(
                               $projName,
                               $credName,
                               {
                                  procedureName => "DeleteStack",
                                  stepName      => "DeleteStack"
                               }
                              );
$errors .= $ec->checkAllErrors($xpath);


if ("$errors" ne "") {

    # Cleanup the partially created configuration we just created
    $ec->deleteProperty($configPath);
    $ec->deleteCredential($projName, $credName);
    my $errMsg = "Error creating configuration credential: " . $errors;
    $ec->setProperty("/myJob/configError", $errMsg);
    print $errMsg;
    exit ERROR;
}

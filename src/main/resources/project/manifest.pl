@files = (
    ['//procedure[procedureName="CreateConfiguration"]/propertySheet/property[propertyName="ec_parameterForm"]/value', 'ui_forms/createConfigForm.xml'],
    ['//property[propertyName="ui_forms"]/propertySheet/property[propertyName="CreateConfigForm"]/value',              'ui_forms/createConfigForm.xml'],
    ['//property[propertyName="ui_forms"]/propertySheet/property[propertyName="EditConfigForm"]/value',                'ui_forms/editConfigForm.xml'],

    ['//property[propertyName="postp_matchers"]/value', 'postp_matchers.pl'],

    ['//procedure[procedureName="Deploy"]/propertySheet/property[propertyName="ec_parameterForm"]/value',             'ui_forms/deploy.xml'],
    ['//step[stepName="Deploy"]/command',                                                                             'procedures/deploy.pl'],
    
    ['//procedure[procedureName="Cleanup"]/propertySheet/property[propertyName="ec_parameterForm"]/value',            'ui_forms/cleanup.xml'],
    ['//step[stepName="Cleanup"]/command',                                                                            'procedures/cleanup.pl'],
    
    ['//procedure[procedureName="CreateKeyPair"]/propertySheet/property[propertyName="ec_parameterForm"]/value',            'ui_forms/create.key.xml'],
    ['//step[stepName="CreateKeyPair"]/command',                                                                            'procedures/create.key.pl'],

    ['//procedure[procedureName="DeleteKeyPair"]/propertySheet/property[propertyName="ec_parameterForm"]/value',            'ui_forms/delete.key.xml'],
    ['//step[stepName="DeleteKeyPair"]/command',                                                                            'procedures/delete.key.pl'],
    
    ['//procedure[procedureName="AllocateIP"]/propertySheet/property[propertyName="ec_parameterForm"]/value',            'ui_forms/allocate.ip.xml'],
    ['//step[stepName="AllocateIP"]/command',                                                                            'procedures/allocate.ip.pl'],

    ['//procedure[procedureName="ReleaseIP"]/propertySheet/property[propertyName="ec_parameterForm"]/value',            'ui_forms/release.ip.xml'],
    ['//step[stepName="ReleaseIP"]/command',                                                                            'procedures/release.ip.pl'],     

    ['//procedure[procedureName="AssociateFloatingIP"]/propertySheet/property[propertyName="ec_parameterForm"]/value',            'ui_forms/associate_ip.xml'],
    ['//step[stepName="AssociateFloatingIP"]/command',                                                                            'procedures/associate_ip.pl'],

    ['//procedure[procedureName="CreateVolume"]/propertySheet/property[propertyName="ec_parameterForm"]/value',            'ui_forms/create_volume.xml'],
    ['//step[stepName="CreateVolume"]/command',                                                                            'procedures/create_volume.pl'],

    ['//procedure[procedureName="AttachVolume"]/propertySheet/property[propertyName="ec_parameterForm"]/value',            'ui_forms/attach_volume.xml'],
    ['//step[stepName="AttachVolume"]/command',                                                                            'procedures/attach_volume.pl'],

    ['//procedure[procedureName="DetachVolume"]/propertySheet/property[propertyName="ec_parameterForm"]/value',            'ui_forms/detach_volume.xml'],
    ['//step[stepName="DetachVolume"]/command',                                                                            'procedures/detach_volume.pl'],

    ['//procedure[procedureName="DeleteVolume"]/propertySheet/property[propertyName="ec_parameterForm"]/value',            'ui_forms/delete_volume.xml'],
    ['//step[stepName="DeleteVolume"]/command',                                                                            'procedures/delete_volume.pl'],

    ['//procedure[procedureName="RebootInstance"]/propertySheet/property[propertyName="ec_parameterForm"]/value',            'ui_forms/reboot.xml'],
    ['//step[stepName="RebootInstance"]/command',                                                                            'procedures/reboot.pl'],

    ['//procedure[procedureName="CreateVolumeSnapshot"]/propertySheet/property[propertyName="ec_parameterForm"]/value',            'ui_forms/create_volumesnapshot.xml'],
    ['//step[stepName="CreateVolumeSnapshot"]/command',                                                                            'procedures/create_volumesnapshot.pl'],

    ['//procedure[procedureName="CreateImage"]/propertySheet/property[propertyName="ec_parameterForm"]/value',            'ui_forms/create_image.xml'],
    ['//step[stepName="CreateImage"]/command',                                                                            'procedures/create_image.pl'],

    ['//procedure[procedureName="CreateInstanceSnapshot"]/propertySheet/property[propertyName="ec_parameterForm"]/value',            'ui_forms/create_instancesnapshot.xml'],
    ['//step[stepName="CreateInstanceSnapshot"]/command',                                                                            'procedures/create_instancesnapshot.pl'],

    ['//procedure[procedureName="CreateStack"]/propertySheet/property[propertyName="ec_parameterForm"]/value',            'ui_forms/create_stack.xml'],
    ['//step[stepName="CreateStack"]/command',                                                                            'procedures/create_stack.pl'],

    ['//procedure[procedureName="UpdateStack"]/propertySheet/property[propertyName="ec_parameterForm"]/value',            'ui_forms/update_stack.xml'],
    ['//step[stepName="UpdateStack"]/command',                                                                            'procedures/update_stack.pl'],

    ['//procedure[procedureName="DeleteStack"]/propertySheet/property[propertyName="ec_parameterForm"]/value',            'ui_forms/delete_stack.xml'],
    ['//step[stepName="DeleteStack"]/command',                                                                            'procedures/delete_stack.pl'],

    ['//procedure[procedureName="CloudManagerGrow"]/propertySheet/property[propertyName="ec_parameterForm"]/value',   'ui_forms/grow.xml'],
    ['//step[stepName="grow"]/command',                                                                               'procedures/step.grow.pl'],

    ['//procedure[procedureName="CloudManagerShrink"]/propertySheet/property[propertyName="ec_parameterForm"]/value', 'ui_forms/shrink.xml'],
    ['//step[stepName="shrink"]/command',                                                                             'procedures/step.shrink.pl'],

    ['//procedure[procedureName="CloudManagerSync"]/propertySheet/property[propertyName="ec_parameterForm"]/value', 'ui_forms/sync.xml'],
    ['//step[stepName="sync"]/command',                                                                             'procedures/step.sync.pl'],    
    
    ['//property[propertyName="preamble"]/value',                                                                     'preamble.pl'],
    ['//property[propertyName="OpenStack"]/value',                                                                    'OpenStack.pm'],
    ['//procedure[procedureName="CreateConfiguration"]/step[stepName="CreateConfiguration"]/command',                 'config/createcfg.pl'],
    ['//procedure[procedureName="CreateConfiguration"]/step[stepName="CreateAndAttachCredential"]/command',           'config/createAndAttachCredential.pl'],
    ['//procedure[procedureName="DeleteConfiguration"]/step[stepName="DeleteConfiguration"]/command',                 'config/deletecfg.pl'],

    ['//property[propertyName="ec_setup"]/value', 'ec_setup.pl'],

         );


EC-OpenStack
============

The ElectricFlow OpenStack integration

## Compile ##

Run gradlew to compile the plugin

`./gradlew`

## Tests ##

#### Creating ecplugin.properties ####
Create an ecplugin.properties at the root of this repository with the following content

    COMMANDER_USER=<COMMANDER_USER>
    COMMANDER_PASSWORD=<COMMANDER_PASSWORD>
    OPENSTACK_USER=<USER> 
    OPENSTACK_PASSWORD=<PASSWORD>
    OPENSTACK_TENANTID=<ID>
    OPENSTACK_IDENTITY_URL=<URL>
    
These represent secrets that **should not** be checked in.

#### Running tests ####
Run the `test` task to run the system tests. You may want to specify the ElectricCommander Server to test against by way of the COMMANDER_SERVER environment variable.

`COMMANDER_SERVER=192.168.158.20 ./gradlew test`


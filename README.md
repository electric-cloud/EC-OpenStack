EC-OpenStack
============

The ElectricCommander OpenStack integration

## Compile ##

Run gradlew to compile the plugin

`./gradlew`

## Tests ##

### Creating openstack-test.properties ###
Create an openstack-test.properties with the following content

    OPENSTACK_USER=<user> 
    OPENSTACK_PASSWORD=<PASSWORD>
    OPENSTACK_TENANTID=<ID>
    OPENSTACK_IDENTITY_URL=<URL>
    
These represent secrets that **should not** be checked in.

### Running tests ###
Run the `test` task to run the system tests. You may want to specify the ElectricCommander Server to test against by way of the COMMANDER_SERVER environment variable.

`COMMANDER_SERVER=192.168.158.20 ./gradlew test`


EC-OpenStack
============

The ElectricCommander OpenStack integration

## Compile ##

Run gradlew to compile the plugin

./gradlew

## Tests ##

Run the test task to run the tests. You may want to specify the ElectricCommander Server to test against by way of the COMMANDER_SERVER environment variable.

COMMANDER_SERVER=192.168.158.20 ./gradlew test


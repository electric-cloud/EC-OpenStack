package ecplugins.openstack

import groovy.json.JsonBuilder
import groovy.json.JsonOutput

class OpenStackOptionsRetrievalTest extends BaseScriptsTestCase {


    void testImagesOptionsRetrievalV1() {

        def json = new JsonBuilder()

        def imageParams = json (
                image_service_url: testProperties.getString(PROP_IMAGE_SVC_URL),
                image_api_version: '1'
        )

        def inputParams = createScriptInputParams(imageParams, 'image')
        assertOptionPresent(inputParams, 'CoreOS', 'image')
    }

    void testFlavorsOptionsRetrieval() {

        def json = new JsonBuilder()

        def computeParams = json (
                compute_service_url: testProperties.getString(PROP_COMPUTE_SVC_URL),
                api_version: '2'
        )

        def inputParams = createScriptInputParams(computeParams, 'flavor')
        assertOptionPresent(inputParams, 'standard.small', 'flavor')
    }

    void testAvailableZonesOptionsRetrieval() {

        def json = new JsonBuilder()

        def computeParams = json (
                compute_service_url: testProperties.getString(PROP_COMPUTE_SVC_URL),
                api_version: '2'
        )

        def inputParams = createScriptInputParams(computeParams, 'availability_zone')
        assertOptionPresent(inputParams, 'az1', 'availability_zone')
    }

    void testSecurityGroupsOptionsRetrieval() {

        def json = new JsonBuilder()

        def params = json (
                compute_service_url: testProperties.getString(PROP_COMPUTE_SVC_URL),
                api_version: '2'
        )

        def inputParams = createScriptInputParams(params, 'security_groups')
        assertOptionPresent(inputParams, 'default', 'security_groups')
    }

    def createScriptInputParams(def inputConfigurationParams, def paramName) {
        def json = new JsonBuilder()
        def credential = json (
                credentialName: "testCredential",
                userName: USER,
                password: PASSWORD
        )

        def actualParams = json (
                connection_config:'test',
                tenant_id: TENANT_ID
        )

        def configurationParams = json (
                identity_service_url : testProperties.getString(PROP_IDENTITY_SVC_URL),
                keystone_api_version: testProperties.getString(PROP_IDENTITY_SVC_VERSION)
        )

        //merge configurationParams with the inputConfigurationParams
        for (prop in inputConfigurationParams) {
            configurationParams.put(prop.key, prop.value);
        }

        def input = json (
                parameters : actualParams,
                configurationParameters : configurationParams,
                credential: [credential],
                formalParameterName: paramName
        )

        input

    }

    def assertOptionPresent(def inputParam, def expectedOption, def paramName) {
        def result = evalScript('project/procedures/form_scripts/parameterOptions/openStackOptions.groovy', inputParam)
        for (option in result?.options) {
            if (option.displayString?.equals(expectedOption)) {
                assertNotNull "Option value is required to be present", option.value
                return
            }
        }
        // fail if we did not find the expected option
        def list = JsonOutput.toJson(result)
        fail("Expected option $expectedOption  not found for $paramName in :\n $list")
    }
}

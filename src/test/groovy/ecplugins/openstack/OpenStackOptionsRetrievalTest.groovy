package ecplugins.openstack

import groovy.json.JsonBuilder
import groovy.json.JsonOutput

class OpenStackOptionsRetrievalTest extends BaseScriptsTestCase {


    void testImagesOptionsRetrievalV1() {

        def json = new JsonBuilder()

        def imageParams = json (
                augmentedAttr_image_service_url: testProperties.getString(PROP_IMAGE_SVC_URL),
                augmentedAttr_image_api_version: '1'
        )

        def inputParams = createScriptInputParams(imageParams, 'image')
        assertOptionPresent(inputParams, 'CoreOS', 'image')
    }

    void testFlavorsOptionsRetrieval() {

        def json = new JsonBuilder()

        def imageParams = json (
                augmentedAttr_compute_service_url: testProperties.getString(PROP_COMPUTE_SVC_URL),
        )

        def inputParams = createScriptInputParams(imageParams, 'flavor')
        assertOptionPresent(inputParams, 'standard.small', 'flavor')
    }

    void testAvailableZonesOptionsRetrieval() {

        def json = new JsonBuilder()

        def imageParams = json (
                augmentedAttr_compute_service_url: testProperties.getString(PROP_COMPUTE_SVC_URL),
        )

        def inputParams = createScriptInputParams(imageParams, 'availability_zone')
        assertOptionPresent(inputParams, 'az1', 'availability_zone')
    }

    def createScriptInputParams(def optionParams, def paramName) {
        def json = new JsonBuilder()
        def actualParams = json (
                augmentedAttr_userName : USER,
                augmentedAttr_password : PASSWORD,
                augmentedAttr_identity_service_url : testProperties.getString(PROP_IDENTITY_SVC_URL),
                augmentedAttr_keystone_api_version: testProperties.getString(PROP_IDENTITY_SVC_VERSION),
                augmentedAttr_config:'test',
                connection_config:'test',
                tenant_id: TENANT_ID
        )

        //merge optionParams with the actualParams
        for (prop in optionParams) {
            actualParams.put(prop.key, prop.value);
        }

        def input = json (
                parameters : actualParams,
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

package ecplugins.openstack

import groovy.json.JsonBuilder
import groovy.json.JsonOutput

class CreateConfigurationValidationTest extends BaseScriptsTestCase {

    void testInputParameterValidations() {

        def json = new JsonBuilder()
        def actualParams = json (
                userName : ''
        )
        def input = json (
                parameters : actualParams
        )

        checkSuccessResponse({})

        checkSuccessResponse(input)

        actualParams = json (
                userName : USER
        )
        input = json (
                parameters : actualParams
        )

        checkSuccessResponse(input)

        actualParams = json (
                userName: USER,
                password: PASSWORD,
                test: "value"
        )
        def inputWithValidCreds = json (
                parameters : actualParams
        )

        checkSuccessResponse(input)

        actualParams = json (
                userName: USER,
                password: PASSWORD,
                identity_service_url : testProperties.getString(PROP_IDENTITY_SVC_URL)
            )

        inputWithValidCreds.parameters = actualParams
        checkSuccessResponse(inputWithValidCreds)

        actualParams = json (
                userName: USER,
                password: PASSWORD,
                identity_service_url : testProperties.getString(PROP_IDENTITY_SVC_URL),
                keystone_api_version: '5'
        )
        inputWithValidCreds.parameters = actualParams
        checkErrorResponse(inputWithValidCreds, 'keystone_api_version', "5 not supported as Keystone API Version value. Supported values are '2.0' and '3'")

    }

    void testInvalidIdentityUrl() {
        checkInvalidIdentityUrl("2.0")
        checkInvalidIdentityUrl("3")
    }

    void testInvalidUserId() {
        checkInvalidUserId("2.0")
        checkInvalidUserId("3")
    }

    void testInvalidPassword() {
        checkInvalidPassword("2.0")
        checkInvalidPassword("3")
    }

    void testInvalidTenantId() {
        checkInvalidTenantId("2.0")
        checkInvalidTenantId("3")
    }

    void testValidConfiguration() {
        checkValidConfiguration("3")
        checkValidConfiguration("2.0")
    }

    void testValidConfigurationWithTenant() {
        checkValidConfiguration("3", TENANT_ID)
        checkValidConfiguration("2.0", TENANT_ID)
    }

    void testInvalidServiceUrls() {
        checkInvalidServiceUrl("3", "compute_service_url", "Compute Service URL", "https://region-b.geo-1.compute.hpcloudsvc.com")
        checkInvalidServiceUrl("2.0", "compute_service_url", "Compute Service URL", "https://region-b.geo-1.compute.hpcloudsvc.com")

        checkInvalidServiceUrl("3", "blockstorage_service_url", "Block Storage URL", "https://region-b.geo-1.block.hpcloudsvc.com")
        checkInvalidServiceUrl("2.0", "blockstorage_service_url", "Block Storage URL", "https://region-b.geo-1.block.hpcloudsvc.com")

        checkInvalidServiceUrl("3", "image_service_url", "Image Service URL", "https://region-b.geo-1.images.hpcloudsvc.com:443")
        checkInvalidServiceUrl("2.0", "image_service_url", "Image Service URL", "https://region-b.geo-1.images.hpcloudsvc.com:443")

        checkUnsupportedServiceUrl("3", "orchestration_service_url", "Orchestration Service URL")
        checkUnsupportedServiceUrl("2.0", "orchestration_service_url", "Orchestration Service URL")
    }

    void testValidServiceUrls() {
        checkValidServiceUrls("3")
        checkValidServiceUrls("2.0")
    }

    void checkUnsupportedServiceUrl(def version, def service, def serviceUrlLabel) {
        def json = new JsonBuilder()

        def actualParams = json (
                userName : USER,
                password : PASSWORD,
                identity_service_url : testProperties.getString(PROP_IDENTITY_SVC_URL),
                keystone_api_version: version,
                tenant_id: TENANT_ID
        )
        actualParams[service] = 'http://some_invalid_service'

        def input = json (
                parameters : actualParams
        )
        checkErrorResponse(input, service, "No ${serviceUrlLabel} is supported by this OpenStack deployment.")
    }

    void checkInvalidServiceUrl(def version, def service, def serviceUrlLabel, def expectedServiceUrl) {
        def json = new JsonBuilder()

        def actualParams = json (
                userName : USER,
                password : PASSWORD,
                identity_service_url : testProperties.getString(PROP_IDENTITY_SVC_URL),
                keystone_api_version: version,
                tenant_id: TENANT_ID
        )
        actualParams[service] = 'http://some_invalid_service'

        def input = json (
                parameters : actualParams
        )
        checkErrorResponse(input, service, "${serviceUrlLabel} is invalid. Enter valid URL: '${expectedServiceUrl}'.")
    }

    void checkValidServiceUrls(def version) {

        def json = new JsonBuilder()

        def actualParams = json (
                userName : USER,
                password : PASSWORD,
                identity_service_url : testProperties.getString(PROP_IDENTITY_SVC_URL),
                keystone_api_version: version,
                tenant_id: TENANT_ID,
                compute_service_url: "https://region-b.geo-1.compute.hpcloudsvc.com",
                blockstorage_service_url: "https://region-b.geo-1.block.hpcloudsvc.com",
                image_service_url: "https://region-b.geo-1.images.hpcloudsvc.com:443"
        )
        def input = json (
                parameters : actualParams
        )

        checkSuccessResponse(input)

    }

    void checkValidConfiguration(def version, def tenantId) {

        def json = new JsonBuilder()

        def actualParams = json (
                userName : USER,
                password : PASSWORD,
                identity_service_url : testProperties.getString(PROP_IDENTITY_SVC_URL),
                keystone_api_version: version,
                tenant_id: tenantId
        )
        def input = json (
                parameters : actualParams
        )

        checkSuccessResponse(input)

    }

    void checkValidConfiguration(def version) {

        checkValidConfiguration(version, /*tenant*/ null)

    }

    void checkInvalidIdentityUrl(def version) {

        def json = new JsonBuilder()

        def actualParams = json (
                userName : USER,
                password : PASSWORD,
                identity_service_url : 'https://invalidserver.hpcloudsvc.com:35357',
                keystone_api_version : version
            )
        def input = json (
                parameters : actualParams
        )

        checkErrorResponse(input, 'identity_service_url', "Identity Service URL is invalid")

    }

    void checkInvalidUserId(def version) {

        def json = new JsonBuilder()

        def actualParams = json (
                userName : 'dummyuser123',
                password : PASSWORD,
                identity_service_url : testProperties.getString(PROP_IDENTITY_SVC_URL),
                keystone_api_version: version
            )

        def input = json (
                parameters : actualParams
        )

        checkErrorResponse(input, 'userName', "Invalid username and password")

    }

    void checkInvalidTenantId(def version) {

        def json = new JsonBuilder()

        def actualParams = json (
                userName : USER,
                password : PASSWORD,
                tenant_id: '0000000abc',
                identity_service_url : testProperties.getString(PROP_IDENTITY_SVC_URL),
                keystone_api_version: version
        )

        def input = json (
                parameters : actualParams
        )

        checkErrorResponse(input, 'tenant_id', "Invalid username and password for tenant")

    }

    void checkInvalidPassword(def version) {

        def json = new JsonBuilder()

        def actualParams = json (
                userName : USER,
                password : 'asfasfdsfsfhr',
                identity_service_url : testProperties.getString(PROP_IDENTITY_SVC_URL),
                keystone_api_version: version
            )
        def input = json (
                parameters : actualParams
        )

        //not passing any error message to validate for invalid password as the actual error message is
        //unpredictable in this case. Sometimes the server returns 401:Unauthorized and other times
        //it return 500: server error. As long as it returns an error, we are ok
        checkErrorResponse(input, 'credential', null)

    }

    void checkSuccessResponse(def inputParam) {

        def result = evalScript('project/procedures/form_scripts/validation/createConfiguration.groovy', inputParam)
        assertEquals "success", result.outcome.toString()
    }

    void checkErrorResponse(def inputParam, def parameter, def expectedError) {
        def result = evalScript('project/procedures/form_scripts/validation/createConfiguration.groovy', inputParam)
        assertEquals "error", result.outcome.toString()
        if (expectedError) {
            def errMsg = findErrorMessage(result.messages, parameter)
            def json = JsonOutput.toJson(inputParam)
            assertEquals "Incorrect error message with input parameters: " + json, parameter, errMsg.parameterName
            assertEquals "Incorrect error message with input parameters: " + json, expectedError, errMsg.message
        }

    }

    def findErrorMessage(def messages, def parameter) {
        for (message in messages) {
            if (message.parameterName.equals(parameter)) {
                return message
            }
        }
        // fail if we did not find the message yet
        fail('Error message not found for parameter: ' + parameter +
                ', in error messages: \n' + JsonOutput.toJson(messages))
    }
}

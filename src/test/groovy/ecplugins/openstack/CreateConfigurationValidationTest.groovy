package ecplugins.openstack

import groovy.json.JsonBuilder
import groovy.json.JsonOutput

class CreateConfigurationValidationTest extends BaseScriptsTestCase {

    void testInputParameterValidations() {

        def json = new JsonBuilder()

        def credential = json (
                credentialName: "testCredential",
                userName: ''
        )

        def actualParams = json (
        )
        def input = json (
                parameters : actualParams,
                credential: [credential]
        )

        checkSuccessResponse({})

        checkSuccessResponse(input)

        credential = json (
                credentialName: "testCredential",
                userName: USER
        )
        actualParams = json (
        )
        input = json (
                parameters : actualParams,
                credential: [credential]
        )

        checkSuccessResponse(input)

        credential = json (
                credentialName: "testCredential",
                userName: USER,
                password: PASSWORD
        )
        actualParams = json (
                test: "value"
        )

        def inputWithValidCreds = json (
                parameters : actualParams,
                credential: [credential]
        )

        checkSuccessResponse(input)

        actualParams = json (
                identity_service_url : testProperties.getString(PROP_IDENTITY_SVC_URL)
        )

        inputWithValidCreds.parameters = actualParams
        checkSuccessResponse(inputWithValidCreds)

        actualParams = json (
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
        checkValidConfiguration("2.0")
        checkValidConfiguration("3")
    }

    void testValidConfigurationWithTenant() {
        checkValidConfiguration("3", TENANT_ID)
        checkValidConfiguration("2.0", TENANT_ID)
    }

    void testInvalidServiceUrls() {
        checkInvalidServiceUrl("3", "compute_service_url", "Compute Service URL", testProperties.getString(PROP_COMPUTE_SVC_URL),
                                "Compute API Version", "2")
        checkInvalidServiceUrl("2.0", "compute_service_url", "Compute Service URL", testProperties.getString(PROP_COMPUTE_SVC_URL),
                                "Compute API Version", "2")

        checkInvalidServiceUrl("3", "blockstorage_service_url", "Block Storage URL", testProperties.getString(PROP_BLOCK_SVC_URL),
                                "Block Storage API Version", "1")
        checkInvalidServiceUrl("2.0", "blockstorage_service_url", "Block Storage URL", testProperties.getString(PROP_BLOCK_SVC_URL),
                                "Block Storage API Version", "1")

        checkInvalidServiceUrl("3", "image_service_url", "Image Service URL", testProperties.getString(PROP_IMAGE_SVC_URL),
                                "Image API Version", "1")
        checkInvalidServiceUrl("2.0", "image_service_url", "Image Service URL", testProperties.getString(PROP_IMAGE_SVC_URL),
                                "Image API Version", "1")

        checkUnsupportedServiceUrl("3", "orchestration_service_url", "Orchestration Service URL")
        checkUnsupportedServiceUrl("2.0", "orchestration_service_url", "Orchestration Service URL")
    }

    void testInvalidServiceAPIVersions() {
        //Check using version2
        checkInvalidServiceVersion("2.0", "api_version", "Compute API Version")
        checkInvalidServiceVersion("2.0", "blockstorage_api_version", "Block Storage API Version")
        checkInvalidServiceVersion("2.0", "image_api_version", "Image API Version")

        //Check using version 3
        checkInvalidServiceVersion("3", "api_version", "Compute API Version")
        checkInvalidServiceVersion("3", "blockstorage_api_version", "Block Storage API Version")
        checkInvalidServiceVersion("3", "image_api_version", "Image API Version")
    }

    void testValidServiceUrls() {
        checkValidServiceUrls("2.0")
        checkValidServiceUrls("3")
    }

    void testCEV8820_AllInvalidValues() {

        def json = new JsonBuilder()

        def credential = json (
                credentialName: "credential",
                userName: "123",
                password: "123"
        )

        def actualParams = json (
                identity_service_url : "http://asdas.com/asda",
                keystone_api_version: "3",
                api_version: "2",
                blockstorage_api_version: "1",
                image_api_version: "1"
        )
        def input = json (
                parameters : actualParams,
                credential: [credential]
        )

        checkErrorResponse(input, 'identity_service_url', 'Identity Service URL is invalid')

    }

    void testCEV8821_AllInvalidValues() {

        def json = new JsonBuilder()

        def credential = json (
                credentialName: "credential",
                userName: "123",
                password: "123"
        )

        def actualParams = json (
                identity_service_url : "https://google.com/[",
                keystone_api_version: "3",
                api_version: "2",
                blockstorage_api_version: "1",
                image_api_version: "1"
        )
        def input = json (
                parameters : actualParams,
                credential: [credential]
        )

        checkErrorResponse(input, 'identity_service_url', 'Identity Service URL is invalid')

    }

    void testCEV8822_AllInvalidValues() {

        def json = new JsonBuilder()

        def credential = json (
                credentialName: "credential",
                userName: "123",
                password: "123"
        )

        def actualParams = json (
                identity_service_url : "https://google.com/;",
                keystone_api_version: "3",
                api_version: "2",
                blockstorage_api_version: "1",
                image_api_version: "1"
        )
        def input = json (
                parameters : actualParams,
                credential: [credential]
        )

        checkErrorResponse(input, 'identity_service_url', 'Identity Service URL is invalid')

    }

    void checkUnsupportedServiceUrl(def version, def service, def serviceUrlLabel) {
        def json = new JsonBuilder()

        def credential = json (
                credentialName: "testCredential",
                userName: USER,
                password: PASSWORD
        )

        def actualParams = json (
                identity_service_url : testProperties.getString(PROP_IDENTITY_SVC_URL),
                keystone_api_version: version,
                tenant_id: TENANT_ID
        )
        actualParams[service] = 'http://some_invalid_service'

        def input = json (
                credential: [credential],
                parameters : actualParams
        )
        checkErrorResponse(input, service, "No ${serviceUrlLabel} is supported by this OpenStack deployment.")
    }

    void checkInvalidServiceVersion(def keystoneAPIVersion, def versionParam, def serviceVersionLabel) {
        def json = new JsonBuilder()

        def credential = json (
                credentialName: "testCredential",
                userName: USER,
                password: PASSWORD
        )

        def actualParams = json (
                identity_service_url : testProperties.getString(PROP_IDENTITY_SVC_URL),
                keystone_api_version: keystoneAPIVersion,
                tenant_id: TENANT_ID,
                compute_service_url: testProperties.getString(PROP_COMPUTE_SVC_URL),
                image_service_url: testProperties.getString(PROP_IMAGE_SVC_URL),
                blockstorage_service_url: testProperties.getString(PROP_BLOCK_SVC_URL)
        )
        actualParams[versionParam] = '67'

        def input = json (
                credential: [credential],
                parameters : actualParams
        )
        checkErrorResponse(input, versionParam, "$serviceVersionLabel '67' is not supported by this OpenStack deployment.")
    }

    void checkInvalidServiceUrl(def version, def service, def serviceUrlLabel, def expectedServiceUrl,
                                def serviceVersionLabel, def serviceVersion) {
        def json = new JsonBuilder()

        def credential = json (
                credentialName: "testCredential",
                userName: USER,
                password: PASSWORD
        )

        def actualParams = json (
                identity_service_url : testProperties.getString(PROP_IDENTITY_SVC_URL),
                keystone_api_version: version,
                tenant_id: TENANT_ID,
                api_version: "2",
                blockstorage_api_version: "1",
                image_api_version: "1"
        )
        actualParams[service] = 'http://some_invalid_service'

        def input = json (
                credential: [credential],
                parameters : actualParams
        )
        checkErrorResponse(input, service, "$expectedServiceUrl is the valid $serviceUrlLabel for $serviceVersionLabel '$serviceVersion'.")
    }

    void checkValidServiceUrls(def version) {

        def json = new JsonBuilder()

        def credential = json (
                credentialName: "testCredential",
                userName: USER,
                password: PASSWORD
        )

        def actualParams = json (
                identity_service_url : testProperties.getString(PROP_IDENTITY_SVC_URL),
                keystone_api_version: version,
                tenant_id: TENANT_ID,
                compute_service_url: testProperties.getString(PROP_COMPUTE_SVC_URL),
                api_version: "2",
                blockstorage_service_url: testProperties.getString(PROP_BLOCK_SVC_URL),
                blockstorage_api_version: "1",
                image_service_url: testProperties.getString(PROP_IMAGE_SVC_URL),
                image_api_version: "1"
        )
        def input = json (
                credential: [credential],
                parameters : actualParams
        )

        checkSuccessResponse(input)

    }

    void checkValidConfiguration(def version, def tenantId) {

        def json = new JsonBuilder()

        def credential = json (
                credentialName: "testCredential",
                userName: USER,
                password: PASSWORD
        )

        def actualParams = json (
                identity_service_url : testProperties.getString(PROP_IDENTITY_SVC_URL),
                keystone_api_version: version,
                tenant_id: tenantId
        )
        def input = json (
                parameters : actualParams,
                credential: [credential]
        )

        checkSuccessResponse(input)

    }

    void checkValidConfiguration(def version) {

        checkValidConfiguration(version, /*tenant*/ null)

    }

    void checkInvalidIdentityUrl(def version) {

        def json = new JsonBuilder()

        def credential = json (
                credentialName: "testCredential",
                userName: USER,
                password: PASSWORD
        )

        def actualParams = json (
                identity_service_url : 'https://invalidserver.hpcloudsvc.com:35357',
                keystone_api_version : version
            )
        def input = json (
                parameters : actualParams,
                credential: [credential]
        )

        checkErrorResponse(input, 'identity_service_url', "Identity Service URL is invalid")

    }

    void checkInvalidUserId(def version) {

        def json = new JsonBuilder()

        def credential = json (
                credentialName: "testCredential",
                userName: 'dummyuser123',
                password: PASSWORD
        )

        def actualParams = json (
                identity_service_url : testProperties.getString(PROP_IDENTITY_SVC_URL),
                keystone_api_version: version
        )

        def input = json (
                credential: [credential],
                parameters : actualParams
        )

        checkErrorResponse(input, 'testCredential.userName', "Invalid username and password")

    }

    void checkInvalidTenantId(def version) {

        def json = new JsonBuilder()

        def credential = json (
                credentialName: "testCredential",
                userName: USER,
                password: PASSWORD
        )

        def actualParams = json (
                tenant_id: '0000000abc',
                identity_service_url : testProperties.getString(PROP_IDENTITY_SVC_URL),
                keystone_api_version: version
        )

        def input = json (
                credential: [credential],
                parameters : actualParams
        )

        checkErrorResponse(input, 'tenant_id', "Invalid username and password for tenant")

    }

    void checkInvalidPassword(def version) {

        def json = new JsonBuilder()

        def credential = json (
                credentialName: "testCredential",
                userName: USER,
                password: 'asfasfdsfsfhr'
        )

        def actualParams = json (
                identity_service_url : testProperties.getString(PROP_IDENTITY_SVC_URL),
                keystone_api_version: version
        )
        def input = json (
                parameters : actualParams,
                credential: [credential]
        )

        //not passing any error message to validate for invalid password as the actual error message is
        //unpredictable in this case. Sometimes the server returns 401:Unauthorized and other times
        //it return 500: server error. As long as it returns an error, we are ok
        checkErrorResponse(input, null, null)

    }

    void checkSuccessResponse(def inputParam) {

        def result = evalScript('project/procedures/form_scripts/validation/createConfiguration.groovy', inputParam)
        assertEquals "Errors found: " + JsonOutput.toJson(result), "success", result.outcome.toString()
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

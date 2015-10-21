/*
 *  Copyright 2015 Electric Cloud, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
import groovy.transform.Field

import org.openstack4j.api.OSClient
import org.openstack4j.api.exceptions.AuthenticationException
import org.openstack4j.api.exceptions.ServerResponseException
import org.openstack4j.core.transport.Config
import org.openstack4j.core.transport.HttpExecutorService
import org.openstack4j.model.common.Identifier
import org.openstack4j.model.identity.v3.TokenV3;
import org.openstack4j.openstack.OSFactory;
import org.openstack4j.openstack.client.OSClientBuilder
import org.openstack4j.openstack.common.functions.EnforceVersionToURL

import com.electriccloud.domain.FormalParameterValidationResult


/**
 * This script validates the OpenStack settings provided to the
 * Create/Edit configuration. It takes the same input parameters as the
 * CreateConfiguration procedure and authenticates the user with the
 * given credentials against the OpenStack Identity service URL.
 * Input parameter: {
 *   "parameters" : {
 *      "keystone_api_version" : "<3|2.0>",
 *      "identity_service_url" : "e.g, https://<server>:<port>",
 *      "credential" : [
 *                      {
 *                         "credentialName" : "<credentialName>",
 *                         "userName" : "<username>",
 *                         "password" : "<pwd>"
 *                      }
 *                     ]
 *   },
 * }
 * Output: {
 *   "outcome" : "success|error"
 *   If error then
 *   "messages" : [
 *                  {
 *                    parameterName : 'param1',
 *                          message : 'error message1'
 *                  }, {
 *                    parameterName : '<credentialName>.userName',
 *                          message : 'error message for invalid userName'
 *                  }, {
 *                    parameterName : '<credentialName>.password',
 *                          message : 'error message for invalid password'
 *                  }
 *               ]
 *   }
 * }
 */
@Field
final String CREDENTIAL_NAME = "credentialName"
@Field
final String USER_NAME = "userName"
@Field
final String PASSWORD = "password"
@Field
final String IDENTITY_SERVICE_URL = "identity_service_url"
@Field
final String IDENTITY_API_VERSION = "keystone_api_version"
@Field
final String TENANT_ID = "tenant_id"
@Field
final String COMPUTE_SERVICE_URL = "compute_service_url"
@Field
final String BLOCK_STORAGE_URL = "blockstorage_service_url"
@Field
final String IMAGE_SERVICE_URL = "image_service_url"
@Field
final String ORCHESTRATION_SERVICE_URL = "orchestration_service_url"
@Field
final String COMPUTE_SERVICE_VERSION = "api_version"
@Field
final String BLOCK_STORAGE_VERSION = "blockstorage_api_version"
@Field
final String IMAGE_SERVICE_VERSION = "image_api_version"
@Field
final boolean DEBUG = false

// Main driver
debug('Begin script')
if (canValidate(args)) {

    // The current context class loader is the one which
    // loaded the DslDelegate class, which is the application's
    // class loader. That is not the class loader we want to use
    // to find the services registered in the external libraries.
    // So we save the contextClassLoader before switching the context
    // class loader on the thread to the Groovy class loader which is
    // the one that can and should be used to find the services
    // registered in the external libraries.
    ClassLoader ctxClassLoader = Thread.currentThread().getContextClassLoader()
    try {
        Thread.currentThread().setContextClassLoader(HttpExecutorService.class.classLoader)
        doValidations(args)

    } finally {
        Thread.currentThread().setContextClassLoader(ctxClassLoader)
    }

} else {
    // simply return success if cannot do validations
    // yet on the given input
    FormalParameterValidationResult.SUCCESS
}

//--------------------Helper classes and functions-----------------------------//
public class InvalidParameterException extends IllegalArgumentException {
    String parameter
    InvalidParameterException(String parameter, String message) {
        super(message)
        this.parameter = parameter
    }
}

boolean canValidate(args) {
    debug('Inside canValidate')
    args?.parameters &&
            args.credential &&
            args.credential.size() > 0 &&
            args.credential[0][USER_NAME] &&
            args.credential[0][PASSWORD] &&
            args.parameters[IDENTITY_SERVICE_URL] &&
            args.parameters[IDENTITY_API_VERSION]
}

def doValidations(args) {

    def result = FormalParameterValidationResult.SUCCESS

    debug('Inside doValidations')


    // We authenticate with OpenStack first.
    // That validates the identity url, username/password and
    // tenant (if provided)

    OSClient osClient

    try {
        osClient = authenticate(args)

    } catch (InvalidParameterException ex) {
        result = buildErrorResponse(ex.parameter, ex.message)

    } catch (AuthenticationException ex) {
        debug("${ex.class.name}: ${ex.message}")
        result = buildAuthenticationError(args)

    } catch (ServerResponseException ex) {
        debug("${ex.class.name}: ${ex.message}")
        result = buildAuthenticationFailureError(args)
    } catch (Exception ex) {
        debug("${ex.class.name}: ${ex.message}")
        result = buildErrorResponse(IDENTITY_SERVICE_URL, 'Identity Service URL is invalid')
    }

    if (osClient) {

        if (!args.parameters[TENANT_ID]) {
            debug('Cannot validate service end-points without tenant_id.')
        } else {

            Map serviceEndPoints = [:]
            //Retrieve the service endpoints from the service
            //catalog for Keystone V2 service version
            osClient.access.serviceCatalog.each { service ->
                debug("****************************")
                debug("V2 Service type: ${service.type}")
                service.endpoints.each { endPoint ->
                    buildAndAddEndPointUrl(endPoint.publicURL.toString(),
                                           args.parameters[TENANT_ID],
                                           service.type,
                                           serviceEndPoints)
                }
            }

            //Retrieve the service endpoints from the service
            //catalog for Keystone V3 service version
            if(osClient.token instanceof TokenV3) {
                def token = (TokenV3) osClient.token
                token.catalog.each { catalog ->
                    debug("****************************")
                    debug("V3 Service type: ${catalog.type}")
                    catalog.endpoints.each { endPoint ->
                        //**
                        buildAndAddEndPointUrl(endPoint.URL.toString(),
                                               args.parameters[TENANT_ID],
                                               catalog.type,
                                               serviceEndPoints)
                    }
                }
            }

            def errorList = []

            validateServiceURLAndVersion(args, serviceEndPoints,
                    COMPUTE_SERVICE_URL, 'Compute Service URL',
                    COMPUTE_SERVICE_VERSION, 'Compute API Version', 'compute', errorList)

            validateServiceURLAndVersion(args, serviceEndPoints,
                    BLOCK_STORAGE_URL, 'Block Storage URL',
                    BLOCK_STORAGE_VERSION, 'Block Storage API Version', 'volume', errorList)

            validateServiceURLAndVersion(args, serviceEndPoints,
                    IMAGE_SERVICE_URL, 'Image Service URL',
                    IMAGE_SERVICE_VERSION, 'Image API Version', 'image', errorList)
            // Orchestration service version is hard-coded in the plugin as '1'. The user is not prompted
            // for the value, so we handle orchestration service end-point validation
            // as a special case.
            validateServiceEndpoint(serviceEndPoints,
                    args.parameters[ORCHESTRATION_SERVICE_URL],
                    ORCHESTRATION_SERVICE_URL,
                    'Orchestration Service URL',
                    /*orchestration API version supported by the plugin*/'1',
                    /*report the version error again the orchestration url parameter */ ORCHESTRATION_SERVICE_URL,
                    'Orchestration Service Version',
                    'orchestration', errorList)

            if (errorList.size() > 0) {
                result = FormalParameterValidationResult.errorResult()
                for (def err : errorList) {
                    result.error(err.param, err.message)
                }
            }

        }
    }

    result
}

OSClient authenticate(args) {

    // Make sure we are ready to authenticate with openstack
    // this method will throw InvalidParameterException to bail out
    preAuthentication(args)

    def url = args.parameters[IDENTITY_SERVICE_URL]
    def version = args.parameters[IDENTITY_API_VERSION]
    def userName = args.credential[0].userName
    def password = args.credential[0].password
    def tenantId = args.parameters[TENANT_ID]

    String endPointUrl = "$url/v$version"

    debug("Endpoint URL $endPointUrl")
    debug("User name $userName")
    debug("Password $password")
    debug("TenantID $tenantId")

    OSClientBuilder osClientBuilder

    Config config = Config.newConfig()
    config.withSSLVerificationDisabled()

    if (version == "2.0") {
        osClientBuilder = OSFactory.builder()
                .withConfig(config)
                .endpoint(endPointUrl)
                .credentials(userName, password)

        if (tenantId) {
            osClientBuilder.tenantId(tenantId)
        }

    } else {
        osClientBuilder = OSFactory.builderV3()
                .withConfig(config)
                .endpoint(endPointUrl)
                .credentials(userName, password, Identifier.byName("default"))

        if (tenantId) {
            osClientBuilder.scopeToProject(Identifier.byId(tenantId), Identifier.byName('default'))
        }

    }

    osClientBuilder.authenticate()
}

void preAuthentication(args) {

    def apiVersion = args.parameters[IDENTITY_API_VERSION]
    if (apiVersion != "2.0" && apiVersion != "3") {
        throw new InvalidParameterException(IDENTITY_API_VERSION,
                apiVersion + " not supported as Keystone API Version value. " +
                        "Supported values are '2.0' and '3'")
    }

}

String stripTenantIdFromUrl(def endPoint, def tenantId) {

    debug("Calling stripTenantIdFromUrl with $endPoint and $tenantId")
    // remove any trailing / in the url
    if (endPoint.endsWith("/")) {
        endPoint = endPoint.substring(0, endPoint.length() - 1)
    }

    //remove tenantId if present
    if (endPoint.endsWith("/${tenantId}")) {
        endPoint = endPoint.substring(0, endPoint.length() - (tenantId.length() + 1))
    }

    debug("Returning endpoint: $endPoint" )
    endPoint
}

private void buildAndAddEndPointUrl(String endPointUrl,
                                    String tenantId,
                                    String serviceType,
                                    Map serviceEndPoints) {

    def url = stripTenantIdFromUrl(endPointUrl, tenantId)
    // For some reason, the Image service has a special logic for the service url
    // So, we handle it here as is done by BaseImageServices
    if (serviceType == 'image') {
        EnforceVersionToURL enforceVersionToURL = EnforceVersionToURL.instance('/v1')
        url = enforceVersionToURL.apply(url)
    }
    def endpoints = serviceEndPoints.getOrDefault(serviceType, [])

    debug("Service type $serviceType: URL $url")
    endpoints.add(url)
    serviceEndPoints.put(serviceType, endpoints)

}

private void validateServiceURLAndVersion(args,
                                          Map serviceEndPoints,
                                          String serviceUrlParam,
                                          String urlLabel,
                                          String serviceVersionParam,
                                          String serviceVersionLabel,
                                          String serviceType,
                                          List errorsList) {


    validateServiceEndpoint(serviceEndPoints,
                            args.parameters[serviceUrlParam],
                            serviceUrlParam,
                            urlLabel,
                            args.parameters[serviceVersionParam],
                            serviceVersionParam,
                            serviceVersionLabel,
                            serviceType,
                            errorsList)

}

private void validateServiceEndpoint(Map serviceEndPoints,
                                     String serviceUrlValue,
                                     String serviceUrlParam,
                                     String urlLabel,
                                     String serviceVersionValue,
                                     String serviceVersionParam,
                                     String versionLabel,
                                     String serviceType,
                                     List errorsList) {

    if (!serviceUrlValue || !serviceVersionValue) {
        return
    }

    def endpointUrls = serviceEndPoints.get(serviceType)
    if (!endpointUrls) {
        def error = [:]
        error.param = serviceUrlParam
        error.message = "No ${urlLabel} is supported by this OpenStack deployment."
        errorsList.add(error)

    } else {
        //check the specified end-point url and version against the endPointUrls from OpenStack
        // find the end-point with the version passed in.
        String serviceUrlWithMatchingVersion = endpointUrls.find {
            it.endsWith("/v$serviceVersionValue")
        }
        if (!serviceUrlWithMatchingVersion) {
            //Specified version is not supported
            def error = [:]
            error.param = serviceVersionParam
            error.message = "$versionLabel '$serviceVersionValue' is not supported by this OpenStack deployment."
            errorsList.add(error)

        } else {
            // For the matching version, check that the user-provided url matches
            String serviceUrlWithoutVersion = serviceUrlWithMatchingVersion.substring(0, serviceUrlWithMatchingVersion.lastIndexOf("/v$serviceVersionValue"))
            if (serviceUrlWithoutVersion != serviceUrlValue) {
                def error = [:]
                error.param = serviceUrlParam
                error.message = "$serviceUrlWithoutVersion is the valid $urlLabel for $versionLabel '$serviceVersionValue'."
                errorsList.add(error)
            }
        }
    }
}

/**
 * Constructs and returns an error response object using the given
 * <code>errorMessage</code>
 */
def buildErrorResponse(String parameter, String errorMessage) {
    def errors = FormalParameterValidationResult.errorResult()
    errors.error(parameter, errorMessage)
}

def buildAuthenticationFailureError(args) {
    def credentialName = args.credential[0][CREDENTIAL_NAME];
    def error = 'Failed to authenticate user with given username and password.'

    result = buildErrorResponse("$credentialName.$USER_NAME", error).
            error("$credentialName.$PASSWORD", error)
    result
}

def buildAuthenticationError(args) {
    def tenantIdProvided = args.parameters[TENANT_ID]
    def credentialName = args.credential[0][CREDENTIAL_NAME];

    def error = tenantIdProvided ?
            'Invalid username and password for tenant' :
            'Invalid username and password'

    result = buildErrorResponse("$credentialName.$USER_NAME", error).
            error("$credentialName.$PASSWORD", error)
    if (tenantIdProvided) {
        result.error(TENANT_ID, error)
    }
    result
}

def debug(String msg) {
    if(DEBUG) System.out.println(msg)
}
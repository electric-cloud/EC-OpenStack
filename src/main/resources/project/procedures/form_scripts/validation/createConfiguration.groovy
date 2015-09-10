import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Field
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.ClientProtocolException
import org.apache.http.conn.HttpHostConnectException
import org.apache.http.conn.scheme.PlainSocketFactory

import java.security.SecureRandom
import java.security.cert.X509Certificate

import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

import org.apache.http.HttpRequest
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.scheme.SchemeRegistry
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.conn.SingleClientConnManager

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
final String CREDENTIAL = "credential"
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

// Main driver
if (canValidate(args)) {
    doValidations(args)
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
    args?.parameters &&
            args.credential &&
            args.credential.size() > 0 &&
            args.credential[0][USER_NAME] &&
            args.credential[0][PASSWORD] &&
            args.parameters[IDENTITY_SERVICE_URL] &&
            args.parameters[IDENTITY_API_VERSION]
}

def doValidations(args) {

    def result
    try {

        // Start with authentication

        // Make sure we are ready to authenticate with openstack
        // this method will throw exceptions to bail out
        preAuthentication(args)

        // build payload for authentication
        def jsonPayload = buildAuthenticationPayload(args)
        HttpEntity payload = new StringEntity(JsonOutput.toJson(jsonPayload))
        payload.setContentType("application/json")

        def response = authenticate(args, payload)

        def statusCode = response.statusLine.statusCode
        if (statusCode < 400) {
            result = validateResponse(args, response.jsonResponse, response.token)
        } else if (statusCode == 401) {
            result = buildAuthenticationError(args)
        } else {
            result = buildErrorResponse(IDENTITY_SERVICE_URL, statusCode + ":" + response.statusLine.reasonPhrase)
        }

    } catch (InvalidParameterException ex) {
        result = buildErrorResponse(ex.parameter, ex.message)
    } catch (HttpHostConnectException|
             ClientProtocolException |
             IllegalStateException |
             UnknownHostException ex) {
        result = buildErrorResponse('identity_service_url', 'Identity Service URL is invalid')
    } catch (Exception ex) {
        //catch-all for any other unknown exceptions encountered during validation
        result = buildErrorResponse(IDENTITY_SERVICE_URL, ex)
    }

    result
}

void preAuthentication(args) {

    def apiVersion = args.parameters[IDENTITY_API_VERSION]
    if (apiVersion != "2.0" && apiVersion != "3") {
        throw new InvalidParameterException(IDENTITY_API_VERSION,
                apiVersion + " not supported as Keystone API Version value. " +
                "Supported values are '2.0' and '3'")
    }

}

def doPost(String url, HttpEntity payload) {
    HttpClient httpClient
    try {
        def httpRequest = createRequest(url, "post", payload)
        httpClient = buildMostTrustingHttpClient(httpRequest)
        HttpResponse response = httpClient.execute(httpRequest)

        // read the response only upon success
        def jsonResponse
        if (response.statusLine.statusCode < 400) {
            jsonResponse = readResponse(response.entity)
        }

        def result = [:]
        result.statusLine = response.statusLine
        result.jsonResponse = jsonResponse

        return result
    } finally {
        httpClient?.getConnectionManager()?.shutdown()
    }
}

def doGet(String url) {

    HttpClient httpClient
    try {
        def httpRequest = new HttpGet(url)
        //httpRequest.setHeader('X-Auth-Token', authToken)

        httpClient = buildMostTrustingHttpClient(httpRequest)
        HttpResponse response = httpClient.execute(httpRequest)

        // read the response only upon success
        def jsonResponse = null
        if (response.statusLine.statusCode < 400) {
            jsonResponse = readResponse(response.entity)
        }

        def result = [:]
        result.statusLine = response.statusLine
        result.jsonResponse = jsonResponse

        return result
    } finally {
        httpClient?.getConnectionManager()?.shutdown()
    }
}

String buildIdentityServiceURL(args) {

    def identityServiceUrl = args.parameters[IDENTITY_SERVICE_URL]
    def keystoneAPIVersion = args.parameters[IDENTITY_API_VERSION]

    identityServiceUrl += "/v" + keystoneAPIVersion
    if (keystoneAPIVersion == "2.0") {
        identityServiceUrl += "/tokens"
    } else{
        identityServiceUrl += "/auth/tokens"
    }
    identityServiceUrl
}

def buildAuthenticationPayload(args) {
    def jsonPayload = [:]
    def keystoneAPIVersion = args.parameters.keystone_api_version
    if (keystoneAPIVersion == "2.0") {
        jsonPayload."auth" = [:]
        jsonPayload."auth"."tenantId" = args.parameters[TENANT_ID] ?: ''
        jsonPayload."auth"."passwordCredentials" = [:]
        jsonPayload."auth"."passwordCredentials"."username" = args.credential[0].userName
        jsonPayload."auth"."passwordCredentials"."password" = args.credential[0].password
    } else {
        jsonPayload."auth" = [:]
        jsonPayload."auth"."identity" = [:]
        jsonPayload."auth"."identity"."methods" = ["password"]
        jsonPayload."auth"."identity"."password" = [:]
        jsonPayload."auth"."identity"."password"."user" = [:]
        jsonPayload."auth"."identity"."password"."user"."name" = args.credential[0].userName
        jsonPayload."auth"."identity"."password"."user"."password" = args.credential[0].password
        jsonPayload."auth"."identity"."password"."user"."domain" = [:]
        jsonPayload."auth"."identity"."password"."user"."domain"."id" = "default"

        if (args.parameters[TENANT_ID]) {
            jsonPayload."auth"."scope" = [:]
            jsonPayload."auth"."scope"."project" = [:]
            jsonPayload."auth"."scope"."project"."id" = args.parameters[TENANT_ID]
        }
    }
    jsonPayload
}

/**
 * Simple helper method to construct the HttpRequest using the given
 * <code>url</code>, <code>httpMethod</code> handling "get" or "post"
 * for now, and the <code>payload</code>.
 */
HttpRequest createRequest(String url, String httpMethod, StringEntity payload) {

    def httpRequest
    switch (httpMethod) {
        case "post":
            httpRequest = new HttpPost(url)
            httpRequest.setEntity(payload)
            break
        case "get"://fall thru to default
        default:
            httpRequest = new HttpGet(url)
            break
    }
    httpRequest
}

/**
 * Like the name suggests, this is the most trusting http client since
 * it establishes a trust manager that trusts everything disregarding
 * presence or absence of any certificate from the server the client
 * tries to connect with using SSL. This is really a hack for
 * SSLPeerUnverifiedException when connecting to OpenStack using
 * Apache HttpClient. Its possible there is an untrusted certificate
 * in the chain which this hack ignores to go through with the connection.
 */
HttpClient buildMostTrustingHttpClient (HttpRequest httpRequest) {
    //Set up a TrustManager that trusts everything
    SSLContext sslContext = SSLContext.getInstance("SSL")
    TrustManager[] trustManagers = [ new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
            return null
        }

        public void checkClientTrusted(X509Certificate[] certs,
                                       String authType) {}

        public void checkServerTrusted(X509Certificate[] certs,
                                       String authType) {}
    } ]

    sslContext.init(null, trustManagers, new SecureRandom())
    SSLSocketFactory sslFactory = new SSLSocketFactory(sslContext,
            SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)

    SchemeRegistry schemeRegistry = new SchemeRegistry()
    schemeRegistry.register(new Scheme("https", 443, sslFactory))
    schemeRegistry.register( new Scheme("http", 80, PlainSocketFactory.getSocketFactory()))

    SingleClientConnManager cm = new SingleClientConnManager(
            httpRequest.getParams(), schemeRegistry)
    return new DefaultHttpClient(cm, httpRequest.getParams())
}


/**
 * Helper method to parse the server JSON response into
 * a basic object instance.
 */
def readResponse(def entity) {

    int bytesRead
    def result = new String()
    byte[] buffer = new byte[1024]
    InputStream inputStream = entity.getContent()
    def bis = new BufferedInputStream(inputStream)
    while ((bytesRead = bis.read(buffer)) != -1) {
        String chunk = new String(buffer, 0, bytesRead)
        chunk.toLowerCase()
        result += chunk
    }
    (new JsonSlurper()).parseText(result)
}

/**
 * Construct and returns an error response object using the given
 * exception. All the relevant details from the exception may be
 * extracted out here such as the exception stack trace. For now,
 * only retrieving the exception class and message.
 */
def buildErrorResponse(String parameter, Exception ex) {
    buildErrorResponse(parameter, ex.getClass().getName() + ":" + ex.getMessage())
}

/**
 * Constructs and returns an error response object using the given
 * <code>errorMessage</code>
 */
def buildErrorResponse(String parameter, String errorMessage) {
    def errors = FormalParameterValidationResult.errorResult()
    errors.error(parameter, errorMessage)
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

def validateResponse(args, jsonResponse, authenticationToken) {
    def result
    if (args.parameters[TENANT_ID]) {
        //If the tenant id was provided, then OpenStack API returns the service catalog information
        // for the tenant (project) that can be used to then validate the other API urls.
        result = validateServiceURLs(args, jsonResponse, authenticationToken)
    }
    result = result ?: FormalParameterValidationResult.SUCCESS

    result
}

private def validateServiceURLs(args, jsonResponse, authenticationToken) {
    def result
    def list = []

    validateServiceURLAndVersion(args, jsonResponse, authenticationToken,
            COMPUTE_SERVICE_URL, 'Compute Service URL',
            COMPUTE_SERVICE_VERSION, 'Compute API Version', 'compute', list)

    validateServiceURLAndVersion(args, jsonResponse, authenticationToken,
            BLOCK_STORAGE_URL, 'Block Storage URL',
            BLOCK_STORAGE_VERSION, 'Block Storage API Version', 'volume', list)

    validateServiceURLAndVersion(args, jsonResponse, authenticationToken,
            IMAGE_SERVICE_URL, 'Image Service URL',
            IMAGE_SERVICE_VERSION, 'Image API Version', 'image', list)
    // Orchestration service version is hard-coded in the plugin as '1'. The user is not prompted
    // for the value, so we handle orchestration service end-point validation
    // as a special case.
    validateServiceEndpoint(args,
            jsonResponse, authenticationToken,
            args.parameters[ORCHESTRATION_SERVICE_URL],
            ORCHESTRATION_SERVICE_URL,
            'Orchestration Service URL',
            /*orchestration API version supported by the plugin*/'1',
            /*report the version error again the orchestration url parameter */ ORCHESTRATION_SERVICE_URL,
            'Orchestration Service Version',
            'orchestration', list)

    if (list.size() > 0) {
        result = FormalParameterValidationResult.errorResult()
        for (def err : list) {
            result.error(err.param, err.message)
        }
    }
    result
}

private void validateServiceURLAndVersion(args, jsonResponse, authenticationToken,
        String serviceUrlParam,
        String urlLabel,
        String serviceVersionParam,
        String serviceVersionLabel,
        String serviceType,
        List list) {


    validateServiceEndpoint(args,
            jsonResponse, authenticationToken, args.parameters[serviceUrlParam], serviceUrlParam, urlLabel,
            args.parameters[serviceVersionParam], serviceVersionParam, serviceVersionLabel,
            serviceType, list)

}

private void validateServiceEndpoint(args,
                                      jsonResponse,
                                      authenticationToken,
                                      String serviceUrlValue,
                                      String serviceUrlParam,
                                      String urlLabel,
                                      String serviceVersionValue,
                                      String serviceVersionParam,
                                      String versionLabel,
                                      String serviceType,
                                      List list) {

    if (!serviceUrlValue || !serviceVersionValue) {
        return
    }

    def serviceUrlToRetrieveVersions = getServiceUrlForVersions(args, jsonResponse, serviceType)
    boolean serviceUrlMatches = serviceUrlToRetrieveVersions == serviceUrlValue
    def serviceUrls = null

    try {
        if (serviceUrlToRetrieveVersions) {

            serviceUrls = getServiceVersionUrls(serviceUrlToRetrieveVersions)
        }

        if (serviceUrlToRetrieveVersions && serviceUrls) {

            // find the end-point with the version passed in.
            def serviceUrl = serviceUrls.find {
                it.endsWith("/v$serviceVersionValue")
            }

            // If no end-point found with the given version
            // then the version is not supported.
            if (!serviceUrl) {
                def error = [:]
                error.param = serviceVersionParam
                error.message = "$versionLabel '$serviceVersionValue' is not supported by this OpenStack deployment."
                list.add(error)
            } else if (!serviceUrlMatches) {
                // For the matching version, check that the user-provided url matches
                String serviceUrlWithoutVersion = serviceUrl.substring(0, serviceUrl.lastIndexOf("/v$serviceVersionValue"))
                if (serviceUrlWithoutVersion != serviceUrlValue) {
                    def error = [:]
                    error.param = serviceUrlParam
                    error.message = "$serviceUrlWithoutVersion is the valid $urlLabel for $versionLabel '$serviceVersionValue'."
                    list.add(error)
                }
            }

        } else {
            def error = [:]
            error.param = serviceUrlParam
            error.message = "No ${urlLabel} is supported by this OpenStack deployment."
            list.add(error)
        }
    } catch (HttpHostConnectException |
        ClientProtocolException |
        IllegalStateException |
        ConnectException |
        UnknownHostException ex) {

        // If we failed to connect to serviceUrlToRetrieveVersions to retrieve
        // the version information, we continue if serviceUrlToRetrieveVersions matched
        // the value entered. Otherwise, we report it as an error.
        if (!serviceUrlMatches) {
            def error = [:]
            error.param = serviceUrlParam
            error.message = "Failed to valid ${urlLabel}. ${ex.class.name}: ${ex.message}"
            list.add(error)
        }
    }

}

def getServiceUrlForVersions(args, jsonResponse, String serviceType) {
    def serviceUrl
    def apiVersion = args.parameters[IDENTITY_API_VERSION]

    if (apiVersion == "2.0") {
        def catalogItem = jsonResponse.access.serviceCatalog.find{ node->
            node["type"] == serviceType
        }

        if (catalogItem) {
            serviceUrl = catalogItem.endpoints.publicURL
        }

    } else { //assuming version 3
        def catalogItem = jsonResponse.token.catalog.find{ node->
            node["type"] == serviceType
        }

        if (catalogItem) {
            serviceUrl = catalogItem.endpoints.url
        }
    }

    return serviceUrl ? stripTenantIdAndVersionFromUrl(serviceUrl[0], args.parameters[TENANT_ID]) : null
}

String stripTenantIdAndVersionFromUrl(String endPoint, String tenantId) {

    // remove any trailing / in the url
    if (endPoint.endsWith("/")) {
        endPoint = endPoint.substring(0, endPoint.length() - 1)
    }

    //remove tenantId if present
    if (endPoint.endsWith("/${tenantId}")) {
        endPoint = endPoint.substring(0, endPoint.length() - (tenantId.length() + 1))
    }

    //finally remove any version string
    // version string can be of different forms, e.g., v3, v2.0,
    // so, we check for any trailing '/vd+(.d+)*'
    def match = endPoint =~ '(.*)/v\\d+(.\\d+)*'
    if (match.find()) {
        endPoint = match.group(1)
    }

    endPoint
}

def getServiceVersionUrls(serviceUrlToRetrieveVersions) {

    def serviceUrls

    // Get the supported versions for the service using the versionList url
    def serviceUrlResponse = doGet(serviceUrlToRetrieveVersions)
    def statusCode = serviceUrlResponse.statusLine.statusCode

    if (statusCode < 400) {

        serviceUrls = serviceUrlResponse.jsonResponse.versions.links.href.collectNested{

            if (it.endsWith("/")) {
                it = it.substring(0, it.length() - 1)
            }
            it
        }.flatten()

    }

    serviceUrls
}

def authenticate(args, HttpEntity payload) {
    String url = buildIdentityServiceURL(args)

    def response = doPost(url, payload)
    if (response.statusLine.statusCode < 400) {

        // Extract auth token
        def token;
        def apiVersion = args.parameters[IDENTITY_API_VERSION]
        if(apiVersion == "2.0") {
            token = response.jsonResponse.access.token.id
        } else {
            def header = response.headers.find {it.name == 'X-Subject-Token'}
            if (header) {
                token = header.value
            }
        }
        response.token = token
    }
    response
}
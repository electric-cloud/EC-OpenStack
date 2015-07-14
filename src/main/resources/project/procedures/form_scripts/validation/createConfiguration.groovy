package ecplugins.openstack.scripts

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
 *      // credential details
 *                  "userName" : "<username>",
 *                  "password" : "<pwd>"
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
 *                    parameterName : 'param2',
 *                          message : 'error message2'
 *                  }
 *               ]
 *   }
 * }
 */
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
    hasValue(args, USER_NAME) &&
        hasValue(args, PASSWORD) &&
        hasValue(args, IDENTITY_SERVICE_URL) &&
        hasValue(args, IDENTITY_API_VERSION)
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

        String url = buildIdentityServiceURL(args)

        def response = doPost(url, payload)

        def statusCode = response.statusLine.statusCode
        if (statusCode < 400) {
            result = validateResponse(args, response.jsonResponse)
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
        def jsonResponse = null
        if (response.statusLine.statusCode < 400) {
            jsonResponse = readResponse(response.entity)
        }

        def result = [:]
        result.statusLine = response.statusLine
        result.jsonResponse = jsonResponse

        return result
    } finally {
        if (httpClient != null) httpClient.getConnectionManager().shutdown()
    }
}

boolean hasValue(args, parameter) {
    args.parameters && args.parameters[parameter] != null && args.parameters[parameter] != ""
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
        jsonPayload."auth"."tenantId" = hasValue(args, TENANT_ID) ? args.parameters[TENANT_ID] : ""
        jsonPayload."auth"."passwordCredentials" = [:]
        jsonPayload."auth"."passwordCredentials"."username" = args.parameters.userName
        jsonPayload."auth"."passwordCredentials"."password" = args.parameters.password
    } else {
        jsonPayload."auth" = [:]
        jsonPayload."auth"."identity" = [:]
        jsonPayload."auth"."identity"."methods" = ["password"]
        jsonPayload."auth"."identity"."password" = [:]
        jsonPayload."auth"."identity"."password"."user" = [:]
        jsonPayload."auth"."identity"."password"."user"."name" = args.parameters.userName
        jsonPayload."auth"."identity"."password"."user"."password" = args.parameters.password
        jsonPayload."auth"."identity"."password"."user"."domain" = [:]
        jsonPayload."auth"."identity"."password"."user"."domain"."id" = "default"

        if (hasValue(args, TENANT_ID)) {
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
    def tenantIdProvided = hasValue(args, TENANT_ID)
    def error = tenantIdProvided ?
            'Invalid username and password for tenant' :
            'Invalid username and password'

    result = buildErrorResponse(USER_NAME, error).
            error(PASSWORD, error)
    if (tenantIdProvided) {
        result.error(TENANT_ID, error)
    }
    result
}

def validateResponse(args, jsonResponse) {
    def result
    if (hasValue(args, TENANT_ID)) {
        //If the tenant id was provided, then OpenStack API returns the service catalog information
        // for the tenant (project) that can be used to then validate the other API urls.
        result = validateServiceURLs(args, jsonResponse)
    }
    if (result == null) result = FormalParameterValidationResult.SUCCESS

    result
}

private def validateServiceURLs(args, jsonResponse) {
    def result
    def list = []
    validateServiceURL(args, jsonResponse, COMPUTE_SERVICE_URL, 'Compute Service URL', 'compute', list)
    validateServiceURL(args, jsonResponse, BLOCK_STORAGE_URL, 'Block Storage URL', 'volume', list)
    validateServiceURL(args, jsonResponse, IMAGE_SERVICE_URL, 'Image Service URL', 'image', list)
    validateServiceURL(args, jsonResponse, ORCHESTRATION_SERVICE_URL, 'Orchestration Service URL', 'orchestration', list)
    if (list.size() > 0) {
        result = FormalParameterValidationResult.errorResult()
        for (def err : list) {
            result.error(err.param, err.message)
        }
    }
    result
}

private def validateServiceURL(args, jsonResponse,
                               String serviceUrlParam,
                               String urlLabel,
                               String serviceType,
                               List list) {

    if (!hasValue(args, serviceUrlParam)) {
        return
    }

    def endPoints
    def apiVersion = args.parameters[IDENTITY_API_VERSION]

    if (apiVersion == "2.0") {
        def catalogItem = jsonResponse.access.serviceCatalog.find{ node->
            node["type"] == serviceType
        }

        if (catalogItem != null && catalogItem.size() > 0) {
            endPoints = catalogItem.endpoints.publicURL
        }

    } else { //assuming version 3
        def catalogItem = jsonResponse.token.catalog.find{ node->
            node["type"] == serviceType
        }

        if (catalogItem != null && catalogItem.size() > 0) {
            endPoints = catalogItem.endpoints.url
        }
    }

    if (endPoints != null && endPoints.size() > 0) {
        def inputUrl = args.parameters[serviceUrlParam]
        for (String endPoint : endPoints) {
            endPoint =
                    stripTenantIdAndVersionFromUrl(endPoints[0],
                            args.parameters[TENANT_ID])

            if (endPoint == inputUrl) {
                // found
                return
            }
        }
        // input service url is not valid
        def endPoint =
                stripTenantIdAndVersionFromUrl(endPoints[0],
                                               args.parameters[TENANT_ID])
        def error = [:]
        error.param = serviceUrlParam
        error.message = "${urlLabel} is invalid. Enter valid URL: '${endPoint}'."
        list.add(error)
    } else {
        def error = [:]
        error.param = serviceUrlParam
        error.message = "No ${urlLabel} is supported by this OpenStack deployment."
        list.add(error)
    }

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
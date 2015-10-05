import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Field

import org.apache.http.HttpEntity
import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.conn.scheme.PlainSocketFactory
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.scheme.SchemeRegistry
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.conn.SingleClientConnManager

import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.SecureRandom
import java.security.cert.X509Certificate

import com.electriccloud.domain.FormalParameterOptionsResult
import com.electriccloud.log.Log;
import com.electriccloud.log.LogFactory;

@Field
final String USER_NAME = "userName"
@Field
final String PASSWORD = "password"
@Field
final String IDENTITY_SERVICE_URL = "identity_service_url"
@Field
final String IDENTITY_API_VERSION = "keystone_api_version"
@Field
final String COMPUTE_SERVICE_URL = "compute_service_url"
@Field
final String COMPUTE_SERVICE_VERSION = "api_version"
@Field
final String IMAGE_SERVICE_URL = "image_service_url"
@Field
final String IMAGE_API_VERSION = "image_api_version"

@Field
final String TENANT_ID = "tenant_id"

def result = new FormalParameterOptionsResult()

def final Log log = LogFactory.getLog(this.class);

if (canGetOptions(args)) {
    def authToken = getAuthToken(args)

    if (authToken) {
        try {
            def options = getOptions(args, authToken)
            options?.sort { it[1] }.each {
                result.add(it[0], it[1])
            }
        } catch (Exception ex) {
            log.warn(ex, 'Failed to retrieve OpenStack options')
        }
    }
}

result

boolean canGetOptions(args) {
    args?.parameters &&
            args.credential &&
            args.credential.size() > 0 &&
            args.credential[0][USER_NAME] &&
            args.credential[0][PASSWORD] &&
            args.configurationParameters[IDENTITY_SERVICE_URL] &&
            args.configurationParameters[IDENTITY_API_VERSION] &&
            args.configurationParameters[TENANT_ID] &&
            canGetOptionsForParameter(args, args.formalParameterName)

}

boolean canGetOptionsForParameter(args, formalParameterName) {
    switch (formalParameterName) {
        case 'flavor':
            return args.configurationParameters[COMPUTE_SERVICE_URL] &&
                    args.configurationParameters[COMPUTE_SERVICE_VERSION]
        case 'availability_zone':
            return args.configurationParameters[COMPUTE_SERVICE_URL]&&
                    args.configurationParameters[COMPUTE_SERVICE_VERSION]
        case 'security_groups':
            return args.configurationParameters[COMPUTE_SERVICE_URL]&&
                    args.configurationParameters[COMPUTE_SERVICE_VERSION]
        case 'keyPairName':
            return args.configurationParameters[COMPUTE_SERVICE_URL]&&
                    args.configurationParameters[COMPUTE_SERVICE_VERSION]
        case 'image':
            return args.configurationParameters[IMAGE_SERVICE_URL] &&
                    args.configurationParameters[IMAGE_API_VERSION]
    }
}

List getOptions(args, authToken) {
    String url = buildServiceURL(args)
    def response = doGet(url, authToken)

    def statusCode = response.statusLine.statusCode
    def list
    if (statusCode < 400) {
        // Read the response based on the parameter we
        // requested info for from OpenStack
        switch (args.formalParameterName) {
            case 'flavor':
                list = response.jsonResponse.flavors.collect {
                    [it.id, it.name]
                }
                break
            case 'availability_zone':
                list = response.jsonResponse.availabilityZoneInfo.collect{
                    [it.zoneName, it.zoneName]
                }
                break
            case 'security_groups':
                list = response.jsonResponse.security_groups.collect{
                    [it.id, it.name]
                }
                break
            case 'keyPairName':
                list = response.jsonResponse.keypairs.collect{
                    [it.keypair.name, it.keypair.name]
                }
                break
            case 'image':
                list = response.jsonResponse.images.collect{
                    [it.id, it.name]
                }
                break
        }
    }
    list
}

String getAuthToken(args) {
    // build payload for authentication
    def jsonPayload = buildAuthenticationPayload(args)
    HttpEntity payload = new StringEntity(JsonOutput.toJson(jsonPayload))
    payload.setContentType("application/json")

    String url = buildIdentityServiceURL(args)
    def response = doPost(url, payload)
    def statusCode = response.statusLine.statusCode
    def token
    if (statusCode < 400) {
        // Extract auth token
        def apiVersion = args.configurationParameters[IDENTITY_API_VERSION]
        if(apiVersion == "2.0") {
            token = response.jsonResponse.access.token.id
        } else {
            def header = response.headers.find {it.name == 'X-Subject-Token'}
            if (header) {
                token = header.value
            }
        }
    }

    token
}

def buildAuthenticationPayload(args) {
    def jsonPayload = [:]
    def keystoneAPIVersion = args.configurationParameters[IDENTITY_API_VERSION]
    if (keystoneAPIVersion == "2.0") {
        jsonPayload."auth" = [:]
        jsonPayload."auth"."tenantId" = args.configurationParameters[TENANT_ID]
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

        jsonPayload."auth"."scope" = [:]
        jsonPayload."auth"."scope"."project" = [:]
        jsonPayload."auth"."scope"."project"."id" = args.configurationParameters[TENANT_ID]

    }
    jsonPayload
}

String buildIdentityServiceURL(args) {

    def identityServiceUrl = args.configurationParameters[IDENTITY_SERVICE_URL]
    def keystoneAPIVersion = args.configurationParameters[IDENTITY_API_VERSION]

    keystoneAPIVersion == "2.0" ?
            "$identityServiceUrl/v$keystoneAPIVersion/tokens" :
            "$identityServiceUrl/v$keystoneAPIVersion/auth/tokens"

}

String buildServiceURL(args) {
    //
    def computeServiceUrl = args.configurationParameters[COMPUTE_SERVICE_URL]
    def computeServiceVersion = args.configurationParameters[COMPUTE_SERVICE_VERSION]

    //
    def imageServiceUrl = args.configurationParameters[IMAGE_SERVICE_URL]
    def imageServiceVersion = args.configurationParameters[IMAGE_API_VERSION]

    //
    def tenantId = args.configurationParameters[TENANT_ID]

    switch (args.formalParameterName) {
        case 'flavor':
            return "$computeServiceUrl/v${computeServiceVersion}/$tenantId/flavors"

        case 'availability_zone':
            return "$computeServiceUrl/v${computeServiceVersion}/$tenantId/os-availability-zone"

        case 'security_groups':
            return "$computeServiceUrl/v${computeServiceVersion}/$tenantId/os-security-groups"

        case 'keyPairName':
            return "$computeServiceUrl/v${computeServiceVersion}/$tenantId/os-keypairs"
        case 'image':
            return imageServiceVersion == '1' ?
                    "$imageServiceUrl/v${imageServiceVersion}/images/detail?status=active":
                    "$imageServiceUrl/v${imageServiceVersion}/images?status=active"

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
        result.headers = response.getAllHeaders()

        return result
    } finally {
        httpClient?.getConnectionManager()?.shutdown()
    }
}

def doGet(String url, String authToken) {
    HttpClient httpClient
    try {
        def httpRequest = new HttpGet(url)
        httpRequest.setHeader('X-Auth-Token', authToken)

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


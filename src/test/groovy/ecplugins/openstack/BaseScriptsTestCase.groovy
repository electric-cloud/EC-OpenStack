package ecplugins.openstack

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

public class BaseScriptsTestCase extends GroovyShellTestCase {

    protected ResourceBundle testProperties;

    // test constants
    protected final String USER = System.getProperty("OPENSTACK_USER");
    protected final String PASSWORD = System.getProperty("OPENSTACK_PASSWORD");
    protected final String TENANT_ID = System.getProperty("OPENSTACK_TENANTID");

    //properties in the test property file
    protected final String PROP_IDENTITY_SVC_URL = 'identity_service_url'
    protected final String PROP_IDENTITY_SVC_VERSION = 'keystone_api_version'
    protected final String PROP_COMPUTE_SVC_URL = 'compute_service_url'
    protected final String PROP_IMAGE_SVC_URL = 'image_service_url'
    protected final String PROP_SCRIPT_JAR_URLS = 'server_jar_urls'

    @Override
    void setUp() {
        super.setUp()

        testProperties =
                new PropertyResourceBundle(new FileInputStream("ecplugin_test.properties"))
        for (String url: testProperties.getString(PROP_SCRIPT_JAR_URLS).split(',')) {
            Thread.currentThread().getContextClassLoader().addURL(new URL(url))
        }
    }

    protected def evalScript(String script, def inputParam) {
        def fileResource = this.class.classLoader.getResourceAsStream(script)
        BufferedReader fileReader = new BufferedReader(new InputStreamReader(fileResource));
        def json = JsonOutput.toJson(inputParam)
        def args = new JsonSlurper().parseText(json)
        def result = withBinding( [args: args] ) {
            shell.evaluate(fileReader)
        }
        result
    }

}

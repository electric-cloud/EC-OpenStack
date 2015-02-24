package ecplugins.openstack;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.PKCS8KeyFile;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.*;

import org.openstack4j.openstack.OSFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("HardCodedStringLiteral")
public class OpenStackProvisionTest {

    private static OSClient m_osClient;
    private final static String COMMANDER_SERVER = System.getProperty("COMMANDER_SERVER");
    private final static String COMMANDER_USER = System.getProperty("COMMANDER_USER");
    private final static String COMMANDER_PASSWORD = System.getProperty("COMMANDER_PASSWORD");
    private final static String IDENTITY_URL = System.getProperty("OPENSTACK_IDENTITY_URL");
    private final static String USER = System.getProperty("OPENSTACK_USER");
    private final static String PASSWORD = System.getProperty("OPENSTACK_PASSWORD");
    private final static String TENANTID = System.getProperty("OPENSTACK_TENANTID");
    private final static String PLUGIN_VERSION = System.getProperty("PLUGIN_VERSION");
    private final static String FLAVOR_ID = "flavor_id";
    private final static String IMAGE_ID = "image_id";
    private final static String KEY_NAME = "key_name";
    private final static String PEM_FILE_LOCATION = "pem_file_location";
    private final static String SECURITY_GROUP = "security_groups";
    private final static String AVAILABILITY_ZONE = "availability_zone";
    private final static String LOGIN_USER_NAME = "instance_login_user_name";
    private final static String IDENTITY_SERVICE_URL = "identity_service_url";
    private final static String COMPUTE_SERVICE_URL = "compute_service_url";
    private final static String BLOCKSTORAGE_SERVICE_URL = "blockstorage_service_url";
    private final static String IMAGE_SERVICE_URL = "image_service_url";
    private final static String ORCHESTRATION_SERVICE_URL = "orchestration_service_url";
    private final static String COMPUTE_SERVICE_VERSION = "compute_api_version";
    private final static String KEYSTONE_API_VERSION = "keystone_api_version";
    private final static long WAIT_TIME = 60000;
    private final static long TIMEOUT_PERIOD_SEC = 180000;    // Timeout period of 3 mins.
    private static String snapshotId = null;
    private static String instanceId = null;
    private static String serverId = null;
    private static Properties prop;

    @BeforeClass
    public static void setup() throws JSONException {

        m_osClient = OSFactory.builder()
                .endpoint(IDENTITY_URL)
                .credentials(USER, PASSWORD)
                .tenantId(TENANTID)
                .authenticate();

        prop = loadProperties();
        deleteConfiguration();
        createConfiguration();
    }

    @Test
    public void testkeyPairCreation() {

        String keyNameToCreate = "automatedTest-testkeyPairCreation";

        // Clean the environment / clean result from previous runs
        m_osClient.compute().keypairs().delete(keyNameToCreate);


        JSONObject jo = new JSONObject();

        try {
            jo.put("projectName", "EC-OpenStack-" + PLUGIN_VERSION);
            jo.put("procedureName", "CreateKeyPair");


            JSONArray actualParameterArray = new JSONArray();
            actualParameterArray.put(new JSONObject()
                    .put("value", "hp")
                    .put("actualParameterName", "connection_config"));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "keyname")
                    .put("value", keyNameToCreate));
            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "tenant_id")
                    .put("value", TENANTID));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "tag")
                    .put("value", "1"));

            jo.put("actualParameter", actualParameterArray);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        String jobId = callRunProcedure(jo);

        String response = waitForJob(jobId);

        // Check job status
        assertEquals("Job completed without errors", "success", response);

        // Get the key pair from OpenStack
        Keypair keypair = m_osClient.compute().keypairs().get(keyNameToCreate);

        // Assert keypair is not null
        assertNotNull(keypair);

        // Grab the keypair name and check its name
        assertEquals("Keypair name is set correctly", keyNameToCreate, keypair.getName());

    }


    @Test
    public void testComputeServices() throws JSONException{


        String snapshotNameToCreate = "automatedTest-testSnapshotCreation";

        // Create keypair and sec. group from openstack4j.
        System.out.println("Creating server [TestServer] to take snapshot of it.");
        ServerCreate testServer = Builders.server().name("TestServer").image(prop.getProperty(IMAGE_ID)).flavor(prop.getProperty(FLAVOR_ID)).build();
        Server server = m_osClient.compute().servers().boot(testServer);
        serverId = server.getId();
        Server.Status serverStatus = m_osClient.compute().servers().get(serverId).getStatus();

        System.out.println("Waiting for server [TestServer] to become active ...");

        long timeTaken = 0;
        while( !serverStatus.toString().equalsIgnoreCase("ACTIVE")) {

            if(timeTaken >= TIMEOUT_PERIOD_SEC) {
                System.out.println("Could not create instance [TestServer] within time.Check the openstack services and re-run the test.");
                // Ensure that the test must fail
                assertTrue(false);
                return;
            }
            try {
                Thread.sleep(WAIT_TIME);
                timeTaken += WAIT_TIME;
                serverStatus = m_osClient.compute().servers().get(serverId).getStatus();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Server [TestServer] became " + serverStatus);

        // Take snapshot of instance [TestServer]

        {

            JSONObject jo = new JSONObject();


            jo.put("projectName", "EC-OpenStack-" + PLUGIN_VERSION);
            jo.put("procedureName", "CreateInstanceSnapshot");


            JSONArray actualParameterArray = new JSONArray();
            actualParameterArray.put(new JSONObject()
                    .put("value", "hp")
                    .put("actualParameterName", "connection_config"));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "display_name")
                    .put("value", snapshotNameToCreate));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "tenant_id")
                    .put("value", TENANTID));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "server_id")
                    .put("value", serverId));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "metadata")
                    .put("value", "desc,testSnapshot"));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "tag")
                    .put("value", "1"));

            jo.put("actualParameter", actualParameterArray);

            System.out.println("Creating a snapshot [" + snapshotNameToCreate + " ] of instance [TestInstance].");
            String jobId = callRunProcedure(jo);

            String response = waitForJob(jobId);

            // Check job status
            assertEquals("Job completed without errors", "success", response);

            Image instanceSnapshot = null;
            // Get the instance snapshot from OpenStack
            for (Image image : m_osClient.compute().images().list()) {
                if (image.getName().equalsIgnoreCase(snapshotNameToCreate)) {
                    instanceSnapshot = image;
                }
            }

            // Assert that instanceSnapshot is not null
            assertNotNull(instanceSnapshot);

            // Assert that image is in fact a snapshot of an instance.
            assertTrue(instanceSnapshot.isSnapshot());

            snapshotId = instanceSnapshot.getId();

            // Grab the instanceSnapshot attributes and verify them
            assertEquals("Instance snapshot name is set correctly", snapshotNameToCreate, instanceSnapshot.getName());
            assertEquals("Instance snapshot status is set correctly", "ACTIVE", instanceSnapshot.getStatus().toString());

            Map<String, String> metadata = m_osClient.images().get(snapshotId).getProperties();

            assertTrue(metadata.containsKey("desc"));
            assertTrue(metadata.containsValue("testSnapshot"));

        }

        {
            // Deploy a new VM from the snapshot created above
            // with given availability zone, customization script and security group

            String instanceNameToCreate = "automatedTest-testSnapshotCreation";

            JSONObject jo = new JSONObject();


            jo.put("projectName", "EC-OpenStack-" + PLUGIN_VERSION);
            jo.put("procedureName", "Deploy");


            JSONArray actualParameterArray = new JSONArray();
            actualParameterArray.put(new JSONObject()
                    .put("value", "hp")
                    .put("actualParameterName", "connection_config"));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "tenant_id")
                    .put("value", TENANTID));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "keyPairName")
                    .put("value", prop.getProperty(KEY_NAME)));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "quantity")
                    .put("value", "1"));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "server_name")
                    .put("value", instanceNameToCreate));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "image")
                    .put("value", snapshotId));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "flavor")
                    .put("value", prop.getProperty(FLAVOR_ID)));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "security_groups")
                    .put("value", prop.getProperty(SECURITY_GROUP)));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "availability_zone")
                    .put("value", prop.getProperty(AVAILABILITY_ZONE)));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "customization_script")
                    .put("value", "#! /bin/bash\nsudo mkdir /home/testDir"));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "associate_ip")
                    .put("value", "1"));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "tag")
                    .put("value", "1"));

            jo.put("actualParameter", actualParameterArray);

            System.out.println("Deploying an instance [ " + instanceNameToCreate + " ] from a snapshot [" + snapshotNameToCreate + " ].");
            String jobId = callRunProcedure(jo);

            String response = waitForJob(jobId);

            // Check job status
            assertEquals("Job completed without errors", "success", response);

            Server instanceFromOpenstack = null;
            // Get the instance from OpenStack
            for (Server instance : m_osClient.compute().servers().list()) {
                if (instance.getName().equalsIgnoreCase(instanceNameToCreate)) {
                    instanceFromOpenstack = instance;
                }
            }

            // Assert that instanceFromOpenstack is not null
            assertNotNull(instanceFromOpenstack);

            instanceId = instanceFromOpenstack.getId();
            String instanceIP = null;

            for (FloatingIP floatingIP : m_osClient.compute().floatingIps().list()) {
                if (instanceId.equals(floatingIP.getInstanceId())) {
                    instanceIP = floatingIP.getFloatingIpAddress();
                }
            }

            // Grab the instanceFromOpenstack attributes and verify them
            assertEquals("Instance  name is set correctly", instanceNameToCreate, instanceFromOpenstack.getName());
            assertEquals("Instance  status is set correctly", "ACTIVE", instanceFromOpenstack.getStatus().toString());
            assertEquals("Instance  availability zone is set correctly", prop.getProperty(AVAILABILITY_ZONE), instanceFromOpenstack.getAvailabilityZone().toString());

            // Verify that directory created in customization script exists.
            String command = "#! /bin/bash\n" +
                    "if [ -d /home/testDir ];\n" +
                    "then\n" +
                    "echo \"/home/testDir exists\";\n" +
                    "else\n" +
                    "echo \"/home/testDir does not exits\";\n" +
                    "fi";

            String responseFromShell = executeOnRemoteMachine(instanceIP, command);
            assertEquals("/home/testDir exists",responseFromShell);
        }

    }

    @AfterClass
    public static void cleanup() throws JSONException {

        System.out.println("Cleaning up Openstack resources.");
        if (serverId != null) {
            m_osClient.compute().servers().delete(serverId);
        }
        if (instanceId != null) {
            m_osClient.compute().servers().delete(instanceId);
        }
        if (snapshotId != null) {
            m_osClient.images().delete(snapshotId);
        }
        System.out.println("Cleaned up the resources.");

    }

    /**
     * callRunProcedure
     *
     * @param jo
     * @return the jobId of the job launched by runProcedure
     */
    public static String callRunProcedure(JSONObject jo) {

        HttpClient httpClient = new DefaultHttpClient();
        JSONObject result = null;
        try {
            HttpPost httpPostRequest = new HttpPost("http://" + COMMANDER_USER
                    + ":" + COMMANDER_PASSWORD + "@" + COMMANDER_SERVER
                    + ":8000/rest/v1.0/jobs?request=runProcedure");
            StringEntity input = new StringEntity(jo.toString());

            input.setContentType("application/json");
            httpPostRequest.setEntity(input);
            HttpResponse httpResponse = httpClient.execute(httpPostRequest);

            result = new JSONObject(EntityUtils.toString(httpResponse.getEntity()));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            httpClient.getConnectionManager().shutdown();
        }

        if (result != null) {
            try {
                return result.getString("jobId");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return "";

    }

    /**
     * getProperty
     *
     * @path a property path
     * @return the value of the property
     */
    public static String getProperty(String path) {

        HttpClient httpClient = new DefaultHttpClient();
        JSONObject result = null;
        try {
            HttpGet httpPostRequest = new HttpGet("http://" + COMMANDER_USER
                    + ":" + COMMANDER_PASSWORD + "@" + COMMANDER_SERVER
                    + ":8000/rest/v1.0/properties/" + path);


            HttpResponse httpResponse = httpClient.execute(httpPostRequest);

            result = new JSONObject(EntityUtils.toString(httpResponse.getEntity()));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            httpClient.getConnectionManager().shutdown();
        }

        if (result != null) {
            try {
                return result.getJSONObject("property").getString("value");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return "";

    }

    /**
     * waitForJob: Waits for job to be completed and reports outcome
     *
     * @param jobId
     * @return outcome of job
     */
    public static String waitForJob(String jobId) {

        String url = "http://" + COMMANDER_USER + ":" + COMMANDER_PASSWORD +
                "@" + COMMANDER_SERVER + ":8000/rest/v1.0/jobs/" +
                jobId + "?request=getJobStatus";
        JSONObject jsonObject = performHTTPGet(url);

        try {
            while (!jsonObject.getString("status").equalsIgnoreCase("completed")) {
                jsonObject = performHTTPGet(url);
            }

            return jsonObject.getString("outcome");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return "";

    }

    /**
     * Wrapper around a HTTP GET to a REST service
     *
     * @param url
     * @return JSONObject
     */
    private static JSONObject performHTTPGet(String url) {

        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse httpResponse = null;

        try {
            HttpGet httpGetRequest = new HttpGet(url);

            httpResponse = httpClient.execute(httpGetRequest);
            return new JSONObject(EntityUtils.toString(httpResponse.getEntity()));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            httpClient.getConnectionManager().shutdown();
        }

        return null;
    }

    /**
     * Delete the openstack configuration used for this test suite (clear previous runs)
     */
    private static void deleteConfiguration() {

        String jobId = "";
        try {
            JSONObject jo = new JSONObject()
                    .put("projectName", "EC-OpenStack-" + PLUGIN_VERSION)
                    .put("procedureName", "DeleteConfiguration");

            JSONArray actualParameterArray = new JSONArray();
            actualParameterArray.put(new JSONObject()
                    .put("value", "hp")
                    .put("actualParameterName", "config"));

            jo.put("actualParameter", actualParameterArray);

            jobId = callRunProcedure(jo);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Block on job completion
        waitForJob(jobId);

        // Do not check job status. Delete will error if it does not exist
        // which is OK since that is the expected state.

    }

    /**
     * Create the openstack configuration used for this test suite
     */
    private static void createConfiguration() throws  JSONException {

        JSONObject jo = new JSONObject();
        JSONArray actualParameterArray = new JSONArray();

        jo.put("projectName", "EC-OpenStack-" + PLUGIN_VERSION);
        jo.put("procedureName", "CreateConfiguration");

        actualParameterArray.put(new JSONObject()
                .put("value", "hp")
                .put("actualParameterName", "config"));

        actualParameterArray.put(new JSONObject()
                .put("actualParameterName", "identity_service_url")
                .put("value", prop.getProperty(IDENTITY_SERVICE_URL)));

        actualParameterArray.put(new JSONObject()
                .put("actualParameterName", "compute_service_url")
                .put("value", prop.getProperty(COMPUTE_SERVICE_URL)));

        actualParameterArray.put(new JSONObject()
                .put("actualParameterName", "api_version")
                .put("value", prop.getProperty(COMPUTE_SERVICE_VERSION)));

        actualParameterArray.put(new JSONObject()
                .put("actualParameterName", "keystone_api_version")
                .put("value", prop.getProperty(KEYSTONE_API_VERSION)));

        actualParameterArray.put(new JSONObject()
                .put("actualParameterName", "debug_level")
                .put("value", "1"));

        actualParameterArray.put(new JSONObject()
                .put("actualParameterName", "credential")
                .put("value", "hp"));

        actualParameterArray.put(new JSONObject()
                .put("actualParameterName", "resource")
                .put("value", "local"));

        actualParameterArray.put(new JSONObject()
                .put("actualParameterName", "workspace")
                .put("value", "default"));

        actualParameterArray.put(new JSONObject()
                .put("actualParameterName", "blockstorage_service_url")
                .put("value", prop.getProperty(BLOCKSTORAGE_SERVICE_URL)));

        actualParameterArray.put(new JSONObject()
                .put("actualParameterName", "blockstorage_api_version")
                .put("value", "1"));

        actualParameterArray.put(new JSONObject()
                .put("actualParameterName", "image_service_url")
                .put("value", prop.getProperty(IMAGE_SERVICE_URL)));

        actualParameterArray.put(new JSONObject()
                .put("actualParameterName", "image_api_version")
                .put("value", "1"));

        actualParameterArray.put(new JSONObject()
                .put("actualParameterName", "orchestration_service_url")
                .put("value", prop.getProperty(ORCHESTRATION_SERVICE_URL)));



        jo.put("actualParameter", actualParameterArray);

        JSONArray credentialArray = new JSONArray();

        JSONObject credentialName = new JSONObject();
        credentialName.put("credentialName", "hp");


        credentialName.put("userName", USER);


        credentialName.put("password", PASSWORD);

        credentialArray.put(credentialName);


        jo.put("credential", credentialArray);


        String jobId = callRunProcedure(jo);

        String response = waitForJob(jobId);

        // Check job status
        assertEquals("Job completed without errors", "success", response);

    }
    /**
     * Load the properties file
     */
    private static Properties loadProperties() {

        Properties prop = new Properties();
        InputStream input = null;

        try {

            input = new FileInputStream("ecplugin_test.properties");
            // load a properties file
            prop.load(input);


        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return prop;
    }

    private static String executeOnRemoteMachine(String hostIp,String command) {
        String response = null;

        System.out.println("Connecting to : " +  hostIp);
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        Session session = null;
        SSHClient sshClient = new SSHClient();
        sshClient.addHostKeyVerifier(new PromiscuousVerifier());

        try {
            sshClient.connect(hostIp);
            PKCS8KeyFile keyFile = new PKCS8KeyFile();
            keyFile.init(new File(prop.getProperty(PEM_FILE_LOCATION)));
            sshClient.authPublickey(prop.getProperty(LOGIN_USER_NAME),keyFile);


            session = sshClient.startSession();
            Session.Command cmd = session.exec(command);
            response = IOUtils.readFully(cmd.getInputStream()).toString();
            cmd.join(10, TimeUnit.SECONDS);


        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            try {
                session.close();
                sshClient.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        return response;

    }
}

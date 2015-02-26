package ecplugins.openstack;

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
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Keypair;

import org.openstack4j.model.image.Image;
import org.openstack4j.openstack.OSFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings("HardCodedStringLiteral")
public class OpenStackProvisionTest {

    private static OSClient m_osClient;
    private static Properties prop;
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
    private final static String IDENTITY_SERVICE_URL = "identity_service_url";
    private final static String COMPUTE_SERVICE_URL = "compute_service_url";
    private final static String BLOCKSTORAGE_SERVICE_URL = "blockstorage_service_url";
    private final static String IMAGE_SERVICE_URL = "image_service_url";
    private final static String ORCHESTRATION_SERVICE_URL = "orchestration_service_url";
    private final static String COMPUTE_SERVICE_VERSION = "compute_api_version";
    private final static String KEYSTONE_API_VERSION = "keystone_api_version";
    private final static String DISK_FORMAT = "disk_format";
    private final static String CONTAINER_FORMAT = "container_format";
    private final static String LOCAL_IMAGE_PATH = "local_image_path";
    private final static long WAIT_TIME = 60000;
    private final static long TIMEOUT_PERIOD_SEC = 180000; // Timeout period of 5 mins.
    private static String imageId = null;

    @BeforeClass
    public static void setup() throws JSONException{

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

    /**
     * To run this test, one must have valid image file available
     * on the machine on which this test is executing and its path
     * must be provided in "local_image_path" property in property file.
     */
    @Test
    public void testImageServices() throws JSONException{

        String imageNameToCreate = "automatedTest-testImageCreation";

        // Clean the environment / clean result from previous runs if exists.
        for (Image image : m_osClient.images().list()) {
            if (image.getName().equalsIgnoreCase(imageNameToCreate)) {
                m_osClient.images().delete(image.getId());
            }
        }


        JSONObject jo = new JSONObject();

        jo.put("projectName", "EC-OpenStack-" + PLUGIN_VERSION);
        jo.put("procedureName", "CreateImage");

        JSONArray actualParameterArray = new JSONArray();
        actualParameterArray.put(new JSONObject()
                .put("value", "hp")
                .put("actualParameterName", "connection_config"));

        actualParameterArray.put(new JSONObject()
                .put("actualParameterName", "tenant_id")
                .put("value", TENANTID));

        actualParameterArray.put(new JSONObject()
                .put("actualParameterName", "name")
                .put("value", imageNameToCreate));

        actualParameterArray.put(new JSONObject()
                .put("actualParameterName", "disk_format")
                .put("value", prop.getProperty(DISK_FORMAT)));

        actualParameterArray.put(new JSONObject()
                .put("actualParameterName", "container_format")
                .put("value", prop.getProperty(CONTAINER_FORMAT)));

        actualParameterArray.put(new JSONObject()
                .put("actualParameterName", "is_local")
                .put("value", "1"));

        actualParameterArray.put(new JSONObject()
                .put("actualParameterName", "image_path")
                .put("value", prop.get(LOCAL_IMAGE_PATH)));

        actualParameterArray.put(new JSONObject()
                .put("actualParameterName", "size")
                .put("value", ""));

        actualParameterArray.put(new JSONObject()
                .put("actualParameterName", "checksum")
                .put("value", ""));

        actualParameterArray.put(new JSONObject()
                .put("actualParameterName", "min_ram")
                .put("value", "1"));

        actualParameterArray.put(new JSONObject()
                .put("actualParameterName", "min_disk")
                .put("value", "1"));

        actualParameterArray.put(new JSONObject()
                .put("actualParameterName", "owner_name")
                .put("value", TENANTID));

        actualParameterArray.put(new JSONObject()
                .put("actualParameterName", "tag")
                .put("value", "1"));

        jo.put("actualParameter", actualParameterArray);

        System.out.println("Creating image [" + imageNameToCreate + "].");
        String jobId = callRunProcedure(jo);

        String response = waitForJob(jobId);

        // Check job status
        assertEquals("Job completed without errors", "success", response);

        // Get the keypair from OpenStack
        Image imageFromOpenstack = null;

        for (Image image : m_osClient.images().list()) {
            if (image.getName().equalsIgnoreCase(imageNameToCreate)) {
                imageFromOpenstack = image;
            }
        }

        // Assert Image with name "automatedTest-testImageCreation" is created
        assertNotNull("Image did not get created on glance.",imageFromOpenstack);

        imageId = imageFromOpenstack.getId();

        // Grab the image attributes and verify it.
        assertEquals("Image name is not set correctly", imageNameToCreate, imageFromOpenstack.getName());
        assertEquals("Disk format is not set correctly", prop.getProperty(DISK_FORMAT), imageFromOpenstack.getDiskFormat().value().toString());
        assertEquals("Container format is not set correctly", prop.getProperty(CONTAINER_FORMAT), imageFromOpenstack.getContainerFormat().value().toString());
        assertEquals("Min-disk is not set correctly", "1", Long.toString(imageFromOpenstack.getMinDisk()));
        assertEquals("Min-ram is not set correctly", "1", Long.toString(imageFromOpenstack.getMinRam()));
        assertEquals("Owner is not set correctly", TENANTID, imageFromOpenstack.getOwner().toString());

    }

    @AfterClass
    public static void cleanup() throws JSONException {

        System.out.println("Cleaning up Openstack resources.");

        if (imageId != null) {
           m_osClient.images().delete(imageId);
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
    private static void deleteConfiguration() throws JSONException{
        String jobId = "";
        JSONObject param1 = new JSONObject();
        JSONObject jo = new JSONObject();
        jo.put("projectName", "EC-OpenStack-" + PLUGIN_VERSION);
        jo.put("procedureName", "DeleteConfiguration");
        JSONArray actualParameterArray = new JSONArray();
        actualParameterArray.put(new JSONObject()
                .put("value", "hp")
                .put("actualParameterName", "config"));
        jo.put("actualParameter", actualParameterArray);
        jobId = callRunProcedure(jo);
        // Block on job completion
        waitForJob(jobId);
        // Do not check job status. Delete will error if it does not exist
        // which is OK since that is the expected state.
    }

    /**
     * Create the openstack configuration used for this test suite
     */
    private static void createConfiguration() throws JSONException {

        String response = "";
        JSONObject parentJSONObject = new JSONObject();
        JSONArray actualParameterArray = new JSONArray();

        parentJSONObject.put("projectName", "EC-OpenStack-" + PLUGIN_VERSION);
        parentJSONObject.put("procedureName", "CreateConfiguration");

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

        parentJSONObject.put("actualParameter", actualParameterArray);

        JSONArray credentialArray = new JSONArray();

        credentialArray.put(new JSONObject()
                .put("credentialName", "hp")
                .put("userName", USER)
                .put("password", PASSWORD));

        parentJSONObject.put("credential", credentialArray);

        String jobId = callRunProcedure(parentJSONObject);

        response = waitForJob(jobId);

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
}

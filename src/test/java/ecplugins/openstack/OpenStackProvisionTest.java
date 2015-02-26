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
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Keypair;

import org.openstack4j.model.image.Image;
import org.openstack4j.openstack.OSFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings("HardCodedStringLiteral")
public class OpenStackProvisionTest {

    private static OSClient m_osClient;
    private static final String COMMANDER_SERVER = System.getProperty("COMMANDER_SERVER");
    private static final String COMMANDER_USER = System.getProperty("COMMANDER_USER");
    private static final String COMMANDER_PASSWORD = System.getProperty("COMMANDER_PASSWORD");
    private static final String IDENTITY_URL = System.getProperty("OPENSTACK_IDENTITY_URL");
    private static final String USER = System.getProperty("OPENSTACK_USER");
    private static final String PASSWORD = System.getProperty("OPENSTACK_PASSWORD");
    private static final String TENANTID = System.getProperty("OPENSTACK_TENANTID");
    private static final String PLUGIN_VERSION = System.getProperty("PLUGIN_VERSION");

    @BeforeClass
    public static void setup() {

        m_osClient = OSFactory.builder()
                .endpoint(IDENTITY_URL)
                .credentials(USER, PASSWORD)
                .tenantId(TENANTID)
                .authenticate();

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
    public void testImageServices(){

        String imageNameToCreate = "automatedTest-testImageCreation";

        // Clean the environment / clean result from previous runs
        for (Image image : m_osClient.images().list()) {
            if (image.getName().equalsIgnoreCase(imageNameToCreate)) {
                m_osClient.images().delete(image.getId());
            }
        }

        JSONObject param1 = new JSONObject();
        JSONObject param2 = new JSONObject();
        JSONObject param3 = new JSONObject();
        JSONObject param4 = new JSONObject();
        JSONObject param5 = new JSONObject();
        JSONObject param6 = new JSONObject();
        JSONObject param7 = new JSONObject();
        JSONObject param8 = new JSONObject();
        JSONObject param9 = new JSONObject();
        JSONObject param10 = new JSONObject();
        JSONObject param11 = new JSONObject();
        JSONObject param12 = new JSONObject();
        JSONObject param13 = new JSONObject();

        JSONObject jo = new JSONObject();

        try {
            jo.put("projectName", "EC-OpenStack-" + PLUGIN_VERSION);
            jo.put("procedureName", "CreateImage");

            param1.put("value", "hp");
            param1.put("actualParameterName", "connection_config");

            param2.put("actualParameterName", "tenant_id");
            param2.put("value", TENANTID);

            param3.put("actualParameterName", "name");
            param3.put("value", imageNameToCreate);

            // temporarily hardcoded.
            param4.put("actualParameterName", "disk_format");
            param4.put("value", "qcow2");

            param5.put("actualParameterName", "container_format");
            param5.put("value", "bare");

            param6.put("actualParameterName", "is_local");
            param6.put("value", "1");

            // temporarily hard coding following values.
            param7.put("actualParameterName", "image_path");
            param7.put("value", "/home/vinayak/electricCloud/Openstack/cirros-0.3.3-x86_64-disk.img");

            param8.put("actualParameterName", "size");
            param8.put("value", "");

            param9.put("actualParameterName", "checksum");
            param9.put("value", "");

            param10.put("actualParameterName", "min_ram");
            param10.put("value", "1");

            param11.put("actualParameterName", "min_disk");
            param11.put("value", "1");

            param12.put("actualParameterName", "owner_name");
            param12.put("value", TENANTID);

            param13.put("actualParameterName", "tag");
            param13.put("value", "1");

            JSONArray actualParameterArray = new JSONArray();
            actualParameterArray.put(param1);
            actualParameterArray.put(param2);
            actualParameterArray.put(param3);
            actualParameterArray.put(param4);
            actualParameterArray.put(param5);
            actualParameterArray.put(param6);
            actualParameterArray.put(param7);
            actualParameterArray.put(param8);
            actualParameterArray.put(param9);
            actualParameterArray.put(param10);
            actualParameterArray.put(param11);
            actualParameterArray.put(param12);
            actualParameterArray.put(param13);


            jo.put("actualParameter", actualParameterArray);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        System.out.println("Creating image [" + imageNameToCreate + "].");
        String jobId = callRunProcedure(jo);

        String response = waitForJob(jobId);

        // Check job status
        assertEquals("Job completed without errors", "warning", response);

        // Get the keypair from OpenStack
        Image imageFromOpenstack = null;

        for (Image image : m_osClient.images().list()) {
            if (image.getName().equalsIgnoreCase(imageNameToCreate)) {
                imageFromOpenstack = image;
            }
        }

        // Assert Image with name "automatedTest-testImageCreation" is created
        assertNotNull(imageFromOpenstack);

        // Grab the image attributes and verify it.
        assertEquals("Image name is set correctly", imageNameToCreate, imageFromOpenstack.getName());
        assertEquals("Disk format is set correctly", "qcow2", imageFromOpenstack.getDiskFormat().value().toString());
        assertEquals("Container format is set correctly", "bare", imageFromOpenstack.getContainerFormat().value().toString());
        assertEquals("Min-disk is set correctly", "1", Long.toString(imageFromOpenstack.getMinDisk()));
        assertEquals("Min-ram is set correctly", "1", Long.toString(imageFromOpenstack.getMinRam()));
        assertEquals("Owner is set correctly", TENANTID, imageFromOpenstack.getOwner().toString());

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
    private static void createConfiguration() {

        String response = "";
        try {
            JSONObject parentJSONObject = new JSONObject()
                    .put("projectName", "EC-OpenStack-" + PLUGIN_VERSION)
                    .put("procedureName", "CreateConfiguration");

            JSONArray actualParameterArray = new JSONArray();

            actualParameterArray.put(new JSONObject()
                    .put("value", "hp")
                    .put("actualParameterName", "config"));
            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "identity_service_url")
                    .put("value", "https://region-a.geo-1.identity.hpcloudsvc.com:35357/"));
            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "compute_service_url")
                    .put("value", "https://region-b.geo-1.compute.hpcloudsvc.com/"));
            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "api_version")
                    .put("value", "2"));
            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "keystone_api_version")
                    .put("value", "2.0"));
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


            parentJSONObject.put("actualParameter", actualParameterArray);

            JSONArray credentialArray = new JSONArray();

            credentialArray.put(new JSONObject()
                    .put("credentialName", "hp")
                    .put("userName", USER)
                    .put("password", PASSWORD));

            parentJSONObject.put("credential", credentialArray);

            String jobId = callRunProcedure(parentJSONObject);

            response = waitForJob(jobId);


        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Check job status
        assertEquals("Job completed without errors", "success", response);

    }
}

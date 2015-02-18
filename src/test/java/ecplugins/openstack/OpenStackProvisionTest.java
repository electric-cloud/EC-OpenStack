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
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Keypair;

import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.openstack.OSFactory;


import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
    private final static long WAIT_TIME = 100;
    
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

        JSONObject param1 = new JSONObject();
        JSONObject param2 = new JSONObject();
        JSONObject param3 = new JSONObject();
        JSONObject param4 = new JSONObject();

        JSONObject jo = new JSONObject();

        try {
            jo.put("projectName", "EC-OpenStack-" + PLUGIN_VERSION);
            jo.put("procedureName", "CreateKeyPair");

            param1.put("value", "hp");
            param1.put("actualParameterName", "connection_config");

            param2.put("actualParameterName", "keyname");
            param2.put("value", keyNameToCreate);

            param3.put("actualParameterName", "tenant_id");
            param3.put("value", TENANTID);

            param4.put("actualParameterName", "tag");
            param4.put("value", "1");

            JSONArray actualParameterArray = new JSONArray();
            actualParameterArray.put(param1);
            actualParameterArray.put(param2);
            actualParameterArray.put(param3);
            actualParameterArray.put(param4);

            jo.put("actualParameter", actualParameterArray);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        String jobId = callRunProcedure(jo);

        String response = waitForJob(jobId);

        // Check job status
        assertEquals("Job completed without errors", "success", response);

        // Get the keypair from OpenStack
        Keypair keypair = m_osClient.compute().keypairs().get(keyNameToCreate);

        // Assert keypair is not null
        assertNotNull(keypair);

        // Grab the keypair name and check its name
        assertEquals("Keypair name is set correctly", keyNameToCreate, keypair.getName());

    }

    @Test
    public void testBlockStorageServices() {

        String volumeNameToCreate = "automatedTest-testVolumeCreation";
        String volumeId;
        Volume volumeFromOpenstack = null;

        List<? extends Volume> listOfVolumes = m_osClient.blockStorage().volumes().list();

        for (Volume volume:listOfVolumes) {
            if (volume.getName().equalsIgnoreCase(volumeNameToCreate)) {
                m_osClient.blockStorage().volumes().delete(volume.getId());
            }
        }

        {
            // limit the variable scope so that same variable names like param1, param2 ...
            // can be used in the same Junit test.
            // Scope : Create Volume

            JSONObject param1 = new JSONObject();
            JSONObject param2 = new JSONObject();
            JSONObject param3 = new JSONObject();
            JSONObject param4 = new JSONObject();
            JSONObject param5 = new JSONObject();
            JSONObject param6 = new JSONObject();
            JSONObject param7 = new JSONObject();

            JSONObject jo = new JSONObject();

            try {
                jo.put("projectName", "EC-OpenStack-" + PLUGIN_VERSION);
                jo.put("procedureName", "CreateVolume");

                param1.put("value", "hp");
                param1.put("actualParameterName", "connection_config");

                param2.put("actualParameterName", "tenant_id");
                param2.put("value", TENANTID);

                param3.put("actualParameterName", "display_name");
                param3.put("value", volumeNameToCreate);

                param4.put("actualParameterName", "size");
                param4.put("value", "1");

                // For time being, hardcoding the type.
                param5.put("actualParameterName", "volume_type");
                param5.put("value", "lvmdriver-1");

                param6.put("actualParameterName", "availability_zone");
                param6.put("value", "nova");

                param7.put("actualParameterName", "tag");
                param7.put("value", "1");

                JSONArray actualParameterArray = new JSONArray();
                actualParameterArray.put(param1);
                actualParameterArray.put(param2);
                actualParameterArray.put(param3);
                actualParameterArray.put(param4);
                actualParameterArray.put(param5);
                actualParameterArray.put(param6);
                actualParameterArray.put(param7);


                jo.put("actualParameter", actualParameterArray);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            System.out.println("Creating volume [" + volumeNameToCreate + "].");
            String jobId = callRunProcedure(jo);

            String response = waitForJob(jobId);

            // Check job status
            assertEquals("Job completed without errors", "success", response);

            // Get the Volume from OpenStack
            listOfVolumes = m_osClient.blockStorage().volumes().list();

            for (Volume volume : listOfVolumes) {

                if (volume.getName().equalsIgnoreCase(volumeNameToCreate)) {
                    // Found the volume with expected name.
                    volumeFromOpenstack = volume;
                }
            }

            // Assert volumeFromOpenstack is not null
            assertNotNull(volumeFromOpenstack);

            // Grab the Volume attributes and verify them
            assertEquals("Volume name is set correctly", volumeNameToCreate, volumeFromOpenstack.getName());
            assertEquals("Volume size is set correctly", 1, volumeFromOpenstack.getSize());
            assertEquals("Volume type is set correctly", "lvmdriver-1", volumeFromOpenstack.getVolumeType());
            assertEquals("Volume availability zone is set correctly", "nova", volumeFromOpenstack.getZone());

            volumeId = volumeFromOpenstack.getId();

        } // end Scope : Create Volume

        // Create a instance to which the created volume can be attached.

        System.out.println("Creating server [TestServer] to attach volume to it.");
        ServerCreate testServer = Builders.server().name("TestServer").image("23af0e46-2127-4d34-a7b3-773e45870917").flavor("42").build();
        Server server = m_osClient.compute().servers().boot(testServer);
        String serverId = server.getId();
        Server.Status serverStatus = m_osClient.compute().servers().get(serverId).getStatus();

        System.out.println("Waiting for server [TestServer] to become active ...");

        while( !serverStatus.toString().equalsIgnoreCase("ACTIVE")) {

            try {
                Thread.sleep(WAIT_TIME);
                serverStatus = m_osClient.compute().servers().get(serverId).getStatus();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Server [TestServer] became " + serverStatus);



        {
            //Scope : Attach Volume

            JSONObject param1 = new JSONObject();
            JSONObject param2 = new JSONObject();
            JSONObject param3 = new JSONObject();
            JSONObject param4 = new JSONObject();
            JSONObject param5 = new JSONObject();
            JSONObject param6 = new JSONObject();


            JSONObject jo = new JSONObject();

            try {
                jo.put("projectName", "EC-OpenStack-" + PLUGIN_VERSION);
                jo.put("procedureName", "AttachVolume");

                param1.put("value", "hp");
                param1.put("actualParameterName", "connection_config");

                param2.put("actualParameterName", "tenant_id");
                param2.put("value", TENANTID);

                param3.put("actualParameterName", "server_id");
                param3.put("value", serverId);

                param4.put("actualParameterName", "volume_id");
                param4.put("value", volumeId);

                param5.put("actualParameterName", "device");
                param5.put("value", "/dev/sdc");

                param6.put("actualParameterName", "tag");
                param6.put("value", "1");

                JSONArray actualParameterArray = new JSONArray();
                actualParameterArray.put(param1);
                actualParameterArray.put(param2);
                actualParameterArray.put(param3);
                actualParameterArray.put(param4);
                actualParameterArray.put(param5);
                actualParameterArray.put(param6);

                jo.put("actualParameter", actualParameterArray);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            System.out.println("Attaching volume [" + volumeNameToCreate + "] to server [TestServer]");
            String jobId = callRunProcedure(jo);

            String response = waitForJob(jobId);

            // Check job status
            assertEquals("Job completed without errors", "success", response);

            // Assert volumeFromOpenstack is not null
            assertNotNull(volumeFromOpenstack);

            // Verify that the volume is in-use indicating it is successfully attached.
            assertEquals("Volume status is set correctly", "in-use", m_osClient.blockStorage().volumes().get(volumeId).getStatus().toString());

        } // end Scope : Attach Volume


        {
            //Scope : Detach Volume

            JSONObject param1 = new JSONObject();
            JSONObject param2 = new JSONObject();
            JSONObject param3 = new JSONObject();
            JSONObject param4 = new JSONObject();
            JSONObject param5 = new JSONObject();

            JSONObject jo = new JSONObject();

            try {
                jo.put("projectName", "EC-OpenStack-" + PLUGIN_VERSION);
                jo.put("procedureName", "DetachVolume");

                param1.put("value", "hp");
                param1.put("actualParameterName", "connection_config");

                param2.put("actualParameterName", "tenant_id");
                param2.put("value", TENANTID);

                param3.put("actualParameterName", "server_id");
                param3.put("value", serverId);

                param4.put("actualParameterName", "volume_id");
                param4.put("value", volumeId);

                // Temporarily hardcoding attachment_id
                param5.put("actualParameterName", "attachment_id");
                param5.put("value", "e67b4c64-2598-4dea-8e09-8a1707bf595b");

                JSONArray actualParameterArray = new JSONArray();
                actualParameterArray.put(param1);
                actualParameterArray.put(param2);
                actualParameterArray.put(param3);
                actualParameterArray.put(param4);
                actualParameterArray.put(param5);

                jo.put("actualParameter", actualParameterArray);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            System.out.println("Detaching [" + volumeNameToCreate + "] from server [TestServer].");
            String jobId = callRunProcedure(jo);

            String response = waitForJob(jobId);

            // Check job status
            assertEquals("Job completed without errors", "success", response);

            // Verify that the volume is available indicating it is successfully detached.
            assertEquals("Volume status is set correctly", "available", m_osClient.blockStorage().volumes().get("e67b4c64-2598-4dea-8e09-8a1707bf595b").getStatus().toString());

        }


        {
            //Scope : Delete Volume

            JSONObject param1 = new JSONObject();
            JSONObject param2 = new JSONObject();
            JSONObject param3 = new JSONObject();

            JSONObject jo = new JSONObject();

            try {
                jo.put("projectName", "EC-OpenStack-" + PLUGIN_VERSION);
                jo.put("procedureName", "DeleteVolume");

                param1.put("value", "hp");
                param1.put("actualParameterName", "connection_config");

                param2.put("actualParameterName", "tenant_id");
                param2.put("value", TENANTID);

                param3.put("actualParameterName", "volume_id");
                param3.put("value", volumeId);

                JSONArray actualParameterArray = new JSONArray();
                actualParameterArray.put(param1);
                actualParameterArray.put(param2);
                actualParameterArray.put(param3);

                jo.put("actualParameter", actualParameterArray);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            System.out.println("Deleting volume [" + volumeNameToCreate + "].");
            String jobId = callRunProcedure(jo);

            String response = waitForJob(jobId);

            // Check job status
            assertEquals("Job completed without errors", "success", response);

            volumeFromOpenstack = null;
            // Check for existance of volume
            volumeFromOpenstack = m_osClient.blockStorage().volumes().get("4bf7eaf3-f96f-40f8-a015-f04f0263144f");


            // Assert volumeFromOpenstack is null
            assertEquals(null, volumeFromOpenstack);

        }


        // Delete the server [TestServer]

        m_osClient.compute().servers().delete(serverId);
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
        JSONObject param1 = new JSONObject();
        JSONObject jo = new JSONObject();

        try {
            jo.put("projectName", "EC-OpenStack-" + PLUGIN_VERSION);
            jo.put("procedureName", "DeleteConfiguration");

            param1.put("value", "hp");
            param1.put("actualParameterName", "config");

            JSONArray actualParameterArray = new JSONArray();
            actualParameterArray.put(param1);

            jo.put("actualParameter", actualParameterArray);

        } catch (JSONException e) {
            e.printStackTrace();
        }


        String jobId = callRunProcedure(jo);

        // Block on job completion
        waitForJob(jobId);

        // Do not check job status. Delete will error if it does not exist
        // which is OK since that is the expected state.

    }

    /**
     * Create the openstack configuration used for this test suite
     */
    private static void createConfiguration() {
        JSONObject param1 = new JSONObject();
        JSONObject param2 = new JSONObject();
        JSONObject param3 = new JSONObject();
        JSONObject param4 = new JSONObject();
        JSONObject param5 = new JSONObject();
        JSONObject param6 = new JSONObject();
        JSONObject param7 = new JSONObject();
        JSONObject param8 = new JSONObject();
        JSONObject param9 = new JSONObject();


        JSONObject jo = new JSONObject();

        try {
            jo.put("projectName", "EC-OpenStack-" + PLUGIN_VERSION);
            jo.put("procedureName", "CreateConfiguration");

            param1.put("value", "hp");
            param1.put("actualParameterName", "config");

            param2.put("actualParameterName", "identity_service_url");
            param2.put("value", "https://region-a.geo-1.identity.hpcloudsvc.com:35357/");

            param3.put("actualParameterName", "compute_service_url");
            param3.put("value", "https://region-b.geo-1.compute.hpcloudsvc.com/");

            param4.put("actualParameterName", "api_version");
            param4.put("value", "2");

            param5.put("actualParameterName", "keystone_api_version");
            param5.put("value", "2.0");

            param6.put("actualParameterName", "debug_level");
            param6.put("value", "1");

            param7.put("actualParameterName", "credential");
            param7.put("value", "hp");

            param8.put("actualParameterName", "resource");
            param8.put("value", "local");

            param9.put("actualParameterName", "workspace");
            param9.put("value", "default");

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


            jo.put("actualParameter", actualParameterArray);

            JSONArray credentialArray = new JSONArray();

            JSONObject credentialName = new JSONObject();
            credentialName.put("credentialName", "hp");


            credentialName.put("userName", USER);


            credentialName.put("password", PASSWORD);

            credentialArray.put(credentialName);


            jo.put("credential", credentialArray);


        } catch (JSONException e) {
            e.printStackTrace();
        }

        String jobId = callRunProcedure(jo);

        String response = waitForJob(jobId);

        // Check job status
        assertEquals("Job completed without errors", "success", response);

    }
}

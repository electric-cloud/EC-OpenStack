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

import org.openstack4j.model.heat.Stack;
import org.openstack4j.openstack.OSFactory;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
    public void testOrchestrationServices() {

        String stackNameToCreate = "automatedTest-testStackCreation";
        Stack stackFromOpenstack = null;
        String stackId = "";

        // Clean the environment / clean result from previous runs
        System.out.println("Cleaning up the environment.");
        for (Stack stack : m_osClient.heat().stacks().list()) {
            if (stack.getName().equalsIgnoreCase(stackNameToCreate)) {
                System.out.println("Found the stack with name [" + stackNameToCreate + "] already exists.Deleting it.");
                m_osClient.heat().stacks().delete(stackNameToCreate, stack.getId());

                // wait for stack to get completely deleted.
                System.out.println("Waiting for stack to get completely deleted.");
                Stack details = m_osClient.heat().stacks().getDetails(stackNameToCreate, stack.getId());
                try {
                    while(!details.getStatus().toString().equalsIgnoreCase("DELETE_COMPLETE")) {

                        Thread.sleep(WAIT_TIME);
                        details = m_osClient.heat().stacks().getDetails(stackNameToCreate, stack.getId());

                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Stack [" + stackNameToCreate + "] deleted successfully.");
            }
        }
        System.out.println("Cleaned up the environment.");

        {
            // limit the variable scope so that same variable names like param1, param2 ...
            // can be used in the same Junit test.
            // Scope : Create Stack

            // Make image_id and key name configurable.
            String template = "{\"heat_template_version\": \"2013-05-23\",\"description\": \"Simple template to test heat commands\", \"parameters\": { \"flavor\": { \"default\": \"m1.tiny\",\"type\": \"string\"}},\"resources\": {\"StackInstance\": {\"type\":\"OS::Nova::Server\",\"properties\": { \"key_name\": \"secondKey\",\"flavor\": {\"get_param\": \"flavor\"},\"image\": \"f6289218-995b-4471-a6e0-8f437f506ecc\",\"user_data\": \"#!/bin/bash -xv\\necho \\\"hello world\\\" &gt; /root/hello-world.txt\\n\"}}}}";


            JSONObject param1 = new JSONObject();
            JSONObject param2 = new JSONObject();
            JSONObject param3 = new JSONObject();
            JSONObject param4 = new JSONObject();
            JSONObject param5 = new JSONObject();
            JSONObject param6 = new JSONObject();

            JSONObject jo = new JSONObject();

            try {
                jo.put("projectName", "EC-OpenStack-" + PLUGIN_VERSION);
                jo.put("procedureName", "CreateStack");

                param1.put("value", "hp");
                param1.put("actualParameterName", "connection_config");

                param2.put("actualParameterName", "tenant_id");
                param2.put("value", TENANTID);

                param3.put("actualParameterName", "stack_name");
                param3.put("value", stackNameToCreate);

                param4.put("actualParameterName", "template");
                param4.put("value", template);

                param5.put("actualParameterName", "template_url");
                param5.put("value", "");

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

            System.out.println("Creating stack [" + stackNameToCreate + "] with template : ." + template);
            String jobId = callRunProcedure(jo);

            String response = waitForJob(jobId);

            // Check job status
            assertEquals("Job completed without errors", "success", response);


            // Get the stack from OpenStack
            for (Stack stack : m_osClient.heat().stacks().list()) {

                if (stack.getName().equalsIgnoreCase(stackNameToCreate)) {

                    stackFromOpenstack = stack;
                    stackId = stackFromOpenstack.getId();
                }
            }


            // Assert stack is not null
            assertNotNull(stackFromOpenstack);

            // Grab the stack details and verify it.
            assertEquals("Stack name is set correctly", stackNameToCreate, stackFromOpenstack.getName());
            assertEquals("Stack status is correct", "CREATE_COMPLETE", stackFromOpenstack.getStatus().toString());

        } // end Scope : Create Stack


        {
            // Scope : Update Stack

            // Make image_id and key name configurable.
            String template = "{\"heat_template_version\": \"2013-05-23\",\"description\": \"Simple template to test heat commands\", \"parameters\": { \"flavor\": { \"default\": \"m1.tiny\",\"type\": \"string\"}},\"resources\": {\"StackInstance\": {\"type\":\"OS::Nova::Server\",\"properties\": { \"key_name\": \"secondKey\",\"flavor\": {\"get_param\": \"flavor\"},\"image\": \"f6289218-995b-4471-a6e0-8f437f506ecc\",\"user_data\": \"#!/bin/bash -xv\\necho \\\"hello world\\\" &gt; /root/hello-world.txt\\n\"}}}}";
            System.out.println("Updating stack to template : " + template);

            // Assert that before update of stack, updated time is null
            assertNull(m_osClient.heat().stacks().getDetails(stackNameToCreate,stackId).getUpdatedTime());

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
                jo.put("procedureName", "UpdateStack");

                param1.put("value", "hp");
                param1.put("actualParameterName", "connection_config");

                param2.put("actualParameterName", "tenant_id");
                param2.put("value", TENANTID);

                param3.put("actualParameterName", "stack_name");
                param3.put("value", stackNameToCreate);

                param4.put("actualParameterName", "stack_id");
                param4.put("value", stackId);

                param5.put("actualParameterName", "template");
                param5.put("value", template);

                param6.put("actualParameterName", "template_url");
                param6.put("value", "");

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

            System.out.println("Updating stack [" + stackNameToCreate + "] to template : " + template);
            String jobId = callRunProcedure(jo);

            String response = waitForJob(jobId);

            // Check job status
            assertEquals("Job completed without errors", "success", response);

            // Assert that after updation of stack , updated time is not null
            assertNotNull(m_osClient.heat().stacks().getDetails(stackNameToCreate, stackId).getUpdatedTime());
            assertEquals("UPDATE_COMPLETE",m_osClient.heat().stacks().getDetails(stackNameToCreate, stackId).getStatus().toString());

        } // end Scope : Update Stack

        {
            // Scope : Delete Stack

            JSONObject param1 = new JSONObject();
            JSONObject param2 = new JSONObject();
            JSONObject param3 = new JSONObject();
            JSONObject param4 = new JSONObject();
            JSONObject param5 = new JSONObject();

            JSONObject jo = new JSONObject();

            try {
                jo.put("projectName", "EC-OpenStack-" + PLUGIN_VERSION);
                jo.put("procedureName", "DeleteStack");

                param1.put("value", "hp");
                param1.put("actualParameterName", "connection_config");

                param2.put("actualParameterName", "tenant_id");
                param2.put("value", TENANTID);

                param3.put("actualParameterName", "stack_name");
                param3.put("value", stackNameToCreate);

                param4.put("actualParameterName", "stack_id");
                param4.put("value", stackId);

                param5.put("actualParameterName", "tag");
                param5.put("value", "1");

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

            System.out.println("Deleting stack [" + stackNameToCreate + "].");
            String jobId = callRunProcedure(jo);

            String response = waitForJob(jobId);

            // Check job status
            assertEquals("Job completed without errors", "success", response);

            // Assert that the stack with name "automatedTest-testStackCreation" no longer exists.
            stackFromOpenstack = null;
            for (Stack stack : m_osClient.heat().stacks().list()) {
                if (stack.getName().equalsIgnoreCase(stackNameToCreate)) {
                    stackFromOpenstack = stack;
                }
            }

            assertNull(stackFromOpenstack);

        } // end Scope : Delete Stack

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

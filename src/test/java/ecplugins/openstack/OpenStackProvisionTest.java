package ecplugins.openstack;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.PKCS8KeyFile;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
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

import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.model.storage.block.Volume;
import static org.junit.Assert.*;
import org.openstack4j.model.heat.Stack;
import org.openstack4j.model.image.Image;
import org.openstack4j.openstack.OSFactory;

import java.io.*;
import java.security.Security;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;
import java.lang.String;
import java.lang.System;
import java.util.List;


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
    private final static String DISK_FORMAT = "disk_format";
    private final static String CONTAINER_FORMAT = "container_format";
    private final static String LOCAL_IMAGE_PATH = "local_image_path";
    private final static String VOLUME_TYPE = "volume_type";
    private final static long WAIT_TIME = 60000;
    private final static long TIMEOUT_PERIOD_SEC = 180000;    // Timeout period of 3 mins.
    private static String snapshotId = null;
    private static String instanceId = null;
    private static String serverId = null;
    private static String imageId = null;
    private static String stackId = null;
    private static String stackNameToCreate = null;
    private static String volumeId = null;
    private static Properties prop;

    @BeforeClass
    public static void setup() throws JSONException, IOException {

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
    public void testCloudProvisioningProps() throws JSONException, IOException {
        String pluginRoot = "/plugins/EC-OpenStack-" + PLUGIN_VERSION + "/project";
        String cloudPropRoot = pluginRoot + "/ec_cloudprovisioning_plugin";

        //Checking top-level properties for ec_cloudprovisioning_plugin
        validatePropertySheetExists(cloudPropRoot);
        validateProperty(cloudPropRoot + "/configurationLocation", "openstack_cfgs");
        validateProperty(cloudPropRoot + "/hasConfiguration", "1");
        validateProperty(cloudPropRoot + "/displayName", "OpenStack");

        String operationsPropRoot = cloudPropRoot + "/operations";
        validatePropertySheetExists(operationsPropRoot);

        //Checking createConfiguration operation properties
        String createConfigPropRoot = operationsPropRoot + "/createConfiguration";
        validatePropertySheetExists(createConfigPropRoot);
        validateProperty(createConfigPropRoot + "/procedureName", "CreateConfiguration");
        validateProperty(createConfigPropRoot + "/ui_formRefs/parameterForm", "ui_forms/CreateConfigForm");

        //Checking deleteConfiguration operation properties
        String deleteConfigPropRoot = operationsPropRoot + "/deleteConfiguration";
        validatePropertySheetExists(deleteConfigPropRoot);
        validateProperty(deleteConfigPropRoot + "/procedureName", "DeleteConfiguration");

        //Checking provision operation properties
        String provisionPropRoot = operationsPropRoot + "/provision";
        validatePropertySheetExists(provisionPropRoot);
        validateProperty(provisionPropRoot + "/procedureName", "_DeployDE");
        validateProperty(provisionPropRoot + "/ui_formRefs/parameterForm", "ec_parameterForm");

        //Checking retireResourcePool operation properties
        String retireRsrcPoolPropRoot = operationsPropRoot + "/retireResourcePool";
        validatePropertySheetExists(retireRsrcPoolPropRoot);
        validateProperty(retireRsrcPoolPropRoot + "/procedureName", "Teardown");

        //Checking retireResource operation properties
        String retireRsrcPropRoot = operationsPropRoot + "/retireResource";
        validatePropertySheetExists(retireRsrcPropRoot);
        validateProperty(retireRsrcPropRoot + "/procedureName", "Teardown");

    }

    @Test
    public void testkeyPairCreation() throws JSONException, IOException {

        String keyNameToCreate = "automatedTest-testkeyPairCreation";

        // Clean the environment / clean result from previous runs
        m_osClient.compute().keypairs().delete(keyNameToCreate);


        JSONObject jo = new JSONObject();


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
    public void testBlockStorageServices() throws JSONException, IOException {

        String volumeNameToCreate = "automatedTest-testVolumeCreation";
        String attachmentID;
        Volume volumeFromOpenstack = null;

        List<? extends Volume> listOfVolumes = m_osClient.blockStorage().volumes().list();

        for (Volume volume : listOfVolumes) {
            if (volume.getName().equalsIgnoreCase(volumeNameToCreate)) {
                m_osClient.blockStorage().volumes().delete(volume.getId());
            }
        }

        {
            // Scope : Create Volume

            JSONObject jo = new JSONObject();

            jo.put("projectName", "EC-OpenStack-" + PLUGIN_VERSION);
            jo.put("procedureName", "CreateVolume");

            JSONArray actualParameterArray = new JSONArray();
            actualParameterArray.put(new JSONObject()
                    .put("value", "hp")
                    .put("actualParameterName", "connection_config"));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "tenant_id")
                    .put("value", TENANTID));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "display_name")
                    .put("value", volumeNameToCreate));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "size")
                    .put("value", "1"));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "volume_type")
                    .put("value", prop.getProperty(VOLUME_TYPE)));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "availability_zone")
                    .put("value", prop.getProperty(AVAILABILITY_ZONE)));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "tag")
                    .put("value", "1"));

            jo.put("actualParameter", actualParameterArray);

            System.out.println("Creating volume [" + volumeNameToCreate + "].");

            String jobId = callRunProcedure(jo);

            String response = waitForJob(jobId);

            assertEquals("Job completed with errors", "success", response);

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
            assertEquals("Volume name is not set correctly", volumeNameToCreate, volumeFromOpenstack.getName());
            assertEquals("Volume size is not set correctly", 1, volumeFromOpenstack.getSize());
            assertEquals("Volume type is not set correctly", prop.getProperty(VOLUME_TYPE), volumeFromOpenstack.getVolumeType());
            assertEquals("Volume availability zone is not set correctly", prop.getProperty(AVAILABILITY_ZONE), volumeFromOpenstack.getZone());

            volumeId = volumeFromOpenstack.getId();

        } // end Scope : Create Volume

        {
            // Scope : Extend Volume

            JSONObject jo = new JSONObject();

            jo.put("projectName", "EC-OpenStack-" + PLUGIN_VERSION);
            jo.put("procedureName", "ExtendVolume");

            JSONArray actualParameterArray = new JSONArray();
            actualParameterArray.put(new JSONObject()
                    .put("value", "hp")
                    .put("actualParameterName", "connection_config"));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "tenant_id")
                    .put("value", TENANTID));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "volume_id")
                    .put("value", volumeId));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "new_size")
                    .put("value", "2"));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "tag")
                    .put("value", "1"));

            jo.put("actualParameter", actualParameterArray);

            System.out.println("Extending volume [" + volumeNameToCreate + "] to 2 GB size.");

            String jobId = callRunProcedure(jo);

            String response = waitForJob(jobId);

            assertEquals("Job completed with errors", "success", response);
            assertEquals("Volume size not extended.", "2", Integer.toString(m_osClient.blockStorage().volumes().get(volumeId).getSize()));

        } // end Scope : Extend Volume

        // Create a instance to which the created volume can be attached.

        System.out.println("Creating server [TestServer] to attach a volume to it.");
        ServerCreate testServer = Builders.server().name("TestServer").image(prop.getProperty(IMAGE_ID)).flavor(prop.getProperty(FLAVOR_ID)).availabilityZone(prop.getProperty(AVAILABILITY_ZONE)).build();
        Server server = m_osClient.compute().servers().boot(testServer);
        serverId = server.getId();
        Server.Status serverStatus = m_osClient.compute().servers().get(serverId).getStatus();
        System.out.println("Waiting for server [TestServer] to become active ...");
        long timeTaken = 0;
        while (!serverStatus.toString().equalsIgnoreCase("ACTIVE")) {
            if (timeTaken >= TIMEOUT_PERIOD_SEC) {
                // Ensure that the test must fail
                fail("Could not create instance [TestServer] within time.Check the openstack services and re-run the test.");
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


        {
            //Scope : Attach Volume

            JSONObject jo = new JSONObject();

            jo.put("projectName", "EC-OpenStack-" + PLUGIN_VERSION);
            jo.put("procedureName", "AttachVolume");

            JSONArray actualParameterArray = new JSONArray();
            actualParameterArray.put(new JSONObject()
                    .put("value", "hp")
                    .put("actualParameterName", "connection_config"));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "tenant_id")
                    .put("value", TENANTID));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "server_id")
                    .put("value", serverId));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "volume_id")
                    .put("value", volumeId));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "device")
                    .put("value", "/dev/sdc"));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "tag")
                    .put("value", "test"));

            jo.put("actualParameter", actualParameterArray);

            System.out.println("Attaching volume [" + volumeNameToCreate + "] to server [TestServer]");

            String jobId = callRunProcedure(jo);

            String response = waitForJob(jobId);

            assertEquals("Job completed with errors", "success", response);

            // Assert volumeFromOpenstack is not null
            assertNotNull(volumeFromOpenstack);

            // Verify that the volume is in-use indicating it is successfully attached.
            assertEquals("Volume status is not set correctly", "in-use", m_osClient.blockStorage().volumes().get(volumeId).getStatus().toString());

            // Get volume attachment ID from commander.
            String propertyPath = "/jobs/" + jobId + "/OpenStack/deployed/test/VolumeAttachment/ID";
            attachmentID = getProperty(propertyPath);


        } // end Scope : Attach Volume


        {
            //Scope : Detach Volume


            JSONObject jo = new JSONObject();

            jo.put("projectName", "EC-OpenStack-" + PLUGIN_VERSION);
            jo.put("procedureName", "DetachVolume");

            JSONArray actualParameterArray = new JSONArray();
            actualParameterArray.put(new JSONObject()
                    .put("value", "hp")
                    .put("actualParameterName", "connection_config"));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "tenant_id")
                    .put("value", TENANTID));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "server_id")
                    .put("value", serverId));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "volume_id")
                    .put("value", volumeId));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "attachment_id")
                    .put("value", attachmentID));

            jo.put("actualParameter", actualParameterArray);

            System.out.println("Detaching [" + volumeNameToCreate + "] from server [TestServer].");
            String jobId = callRunProcedure(jo);

            String response = waitForJob(jobId);

            // Check job status
            assertEquals("Job completed with errors", "success", response);

            // Verify that the volume is available indicating it is successfully detached.
            assertEquals("Volume status is not set correctly", "available", m_osClient.blockStorage().volumes().get(volumeId).getStatus().toString());

        }


        {
            //Scope : Delete Volume

            JSONObject jo = new JSONObject();

            jo.put("projectName", "EC-OpenStack-" + PLUGIN_VERSION);
            jo.put("procedureName", "DeleteVolume");

            JSONArray actualParameterArray = new JSONArray();
            actualParameterArray.put(new JSONObject()
                    .put("value", "hp")
                    .put("actualParameterName", "connection_config"));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "tenant_id")
                    .put("value", TENANTID));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "volume_id")
                    .put("value", volumeId));

            jo.put("actualParameter", actualParameterArray);

            System.out.println("Deleting volume [" + volumeNameToCreate + "].");
            String jobId = callRunProcedure(jo);

            String response = waitForJob(jobId);

            // Check job status
            assertEquals("Job completed with errors", "success", response);

            volumeFromOpenstack = null;
            // Check for existance of volume
            volumeFromOpenstack = m_osClient.blockStorage().volumes().get(volumeId);


            // Assert volumeFromOpenstack is null
            assertNotEquals("Volume did not get deleted successfully.", "available", volumeFromOpenstack.getStatus().toString());

        }
    }

    @Test
    public void testOrchestrationServices() throws JSONException, IOException {

        stackNameToCreate = "automatedTest-testStackCreation";
        Stack stackFromOpenstack = null;


        // Clean the environment / clean result from previous runs
        System.out.println("Cleaning up the environment.");
        for (Stack stack : m_osClient.heat().stacks().list()) {
            if (stack.getName().equalsIgnoreCase(stackNameToCreate)) {
                System.out.println("Found the stack with name [" + stackNameToCreate + "] already exists.Deleting it.");
                m_osClient.heat().stacks().delete(stackNameToCreate, stack.getId());

                // wait for stack to get completely deleted.
                System.out.println("Waiting for stack to get completely deleted.");
                Stack details = m_osClient.heat().stacks().getDetails(stackNameToCreate, stack.getId());
                long timeTaken = 0;
                while(!details.getStatus().toString().equalsIgnoreCase("DELETE_COMPLETE")) {
                    try {
                        Thread.sleep(WAIT_TIME);
                        timeTaken += WAIT_TIME;
                        if(timeTaken >= TIMEOUT_PERIOD_SEC) {
                            fail("Could not to delete the stack [" + stackNameToCreate + "] within time." +
                                    "Delete the stack and re-run the test.");
                            return;
                        }
                        details = m_osClient.heat().stacks().getDetails(stackNameToCreate, stack.getId());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                System.out.println("Stack [" + stackNameToCreate + "] deleted successfully.");
            }
        }
        System.out.println("Cleaned up the environment.");

        {
            // Scope : Create Stack

            String template = "{\"heat_template_version\": \"2013-05-23\",\"description\": \"Simple template to test heat commands\", \"parameters\": { \"flavor\": { \"default\": \"" + prop.get(FLAVOR_ID) + "\",\"type\": \"string\"}},\"resources\": {\"StackInstance\": {\"type\":\"OS::Nova::Server\",\"properties\": { \"key_name\": \"" + prop.get(KEY_NAME) + "\",\"flavor\": {\"get_param\": \"flavor\"},\"image\": \"" + prop.get(IMAGE_ID) + "\",\"user_data\": \"#!/bin/bash -xv\\necho \\\"hello world\\\" &gt; /root/hello-world.txt\\n\"}}}}";

            JSONObject jo = new JSONObject();
            JSONArray actualParameterArray = new JSONArray();

            try {
                jo.put("projectName", "EC-OpenStack-" + PLUGIN_VERSION);
                jo.put("procedureName", "CreateStack");

                actualParameterArray.put(new JSONObject()
                        .put("value", "hp")
                        .put("actualParameterName", "connection_config"));

                actualParameterArray.put(new JSONObject()
                        .put("actualParameterName", "stack_name")
                        .put("value", stackNameToCreate));

                actualParameterArray.put(new JSONObject()
                        .put("actualParameterName", "tenant_id")
                        .put("value", TENANTID));

                actualParameterArray.put(new JSONObject()
                        .put("actualParameterName", "template")
                        .put("value", template));

                actualParameterArray.put(new JSONObject()
                        .put("actualParameterName", "template_url")
                        .put("value", ""));

                actualParameterArray.put(new JSONObject()
                        .put("actualParameterName", "tag")
                        .put("value", "1"));

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

            String updatedTemplate = "{\"heat_template_version\": \"2013-05-23\",\"description\": \"Simple template to test heat commands\", \"parameters\": { \"flavor\": { \"default\": \"" + prop.get(FLAVOR_ID) + "\",\"type\": \"string\"}},\"resources\": {\"hello_world6\": {\"type\":\"OS::Nova::Server\",\"properties\": { \"key_name\": \"" + prop.get(KEY_NAME)+ "\",\"flavor\": {\"get_param\": \"flavor\"},\"image\": \"" + prop.get(IMAGE_ID) + "\",\"user_data\": \"#!/bin/bash -xv\\necho \\\"hello world\\\" &gt; /root/hello-world.txt\\n\"}},\"hello_world7\": {\"type\":\"OS::Nova::Server\",\"properties\": { \"key_name\": \"" + prop.get(KEY_NAME) + "\",\"flavor\": {\"get_param\": \"flavor\"},\"image\": \"" + prop.get(IMAGE_ID) + "\",\"user_data\": \"#!/bin/bash -xv\\necho \\\"hello world\\\" &gt; /root/hello-world.txt\\n\"}}}}";

            // Assert that before update of stack, updated time is null
            assertNull(m_osClient.heat().stacks().getDetails(stackNameToCreate,stackId).getUpdatedTime());

            JSONObject jo = new JSONObject();
            JSONArray actualParameterArray = new JSONArray();

            try {
                jo.put("projectName", "EC-OpenStack-" + PLUGIN_VERSION);
                jo.put("procedureName", "UpdateStack");

                actualParameterArray.put(new JSONObject()
                        .put("value", "hp")
                        .put("actualParameterName", "connection_config"));

                actualParameterArray.put(new JSONObject()
                        .put("actualParameterName", "tenant_id")
                        .put("value", TENANTID));

                actualParameterArray.put(new JSONObject()
                        .put("actualParameterName", "stack_name")
                        .put("value", stackNameToCreate));

                actualParameterArray.put(new JSONObject()
                        .put("actualParameterName", "stack_id")
                        .put("value", stackId));

                actualParameterArray.put(new JSONObject()
                        .put("actualParameterName", "template")
                        .put("value", updatedTemplate));

                actualParameterArray.put(new JSONObject()
                        .put("actualParameterName", "template_url")
                        .put("value", ""));

                actualParameterArray.put(new JSONObject()
                        .put("actualParameterName", "tag")
                        .put("value", "1"));

                jo.put("actualParameter", actualParameterArray);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            System.out.println("Updating stack to template : " + updatedTemplate);
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

            JSONObject jo = new JSONObject();
            JSONArray actualParameterArray = new JSONArray();

            try {
                jo.put("projectName", "EC-OpenStack-" + PLUGIN_VERSION);
                jo.put("procedureName", "DeleteStack");

                actualParameterArray.put(new JSONObject()
                        .put("value", "hp")
                        .put("actualParameterName", "connection_config"));

                actualParameterArray.put(new JSONObject()
                        .put("actualParameterName", "tenant_id")
                        .put("value", TENANTID));

                actualParameterArray.put(new JSONObject()
                        .put("actualParameterName", "stack_name")
                        .put("value", stackNameToCreate));

                actualParameterArray.put(new JSONObject()
                        .put("actualParameterName", "stack_id")
                        .put("value", stackId));

                actualParameterArray.put(new JSONObject()
                        .put("actualParameterName", "tag")
                        .put("value", "1"));

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
     * To run this test, one must have valid image file available
     * on the machine on which this test is executing and its path
     * must be provided in "local_image_path" property in property file.
     */
    @Test
    public void testImageServices() throws JSONException, IOException {

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

    @Test
    public void testComputeServices() throws JSONException, IOException {


        String snapshotNameToCreate = "automatedTest-testSnapshotCreation";
        String instanceNameToCreate = "automatedTest-testInstanceCreation";

        // Create keypair and sec. group from openstack4j.
        System.out.println("Creating server [TestServer] to take snapshot of it.");
        ServerCreate testServer = Builders.server().name("TestServer").image(prop.getProperty(IMAGE_ID)).flavor(prop.getProperty(FLAVOR_ID)).build();
        Server server = m_osClient.compute().servers().boot(testServer);
        serverId = server.getId();
        Server.Status serverStatus = m_osClient.compute().servers().get(serverId).getStatus();

        System.out.println("Waiting for server [TestServer] to become active ...");

        long timeTaken = 0;
        while (!serverStatus.toString().equalsIgnoreCase("ACTIVE")) {

            if (timeTaken >= TIMEOUT_PERIOD_SEC) {
                // Ensure that the test must fail
                fail("Could not create instance [TestServer] within time.Check the openstack services and re-run the test.");
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

            System.out.println("Creating a snapshot [" + snapshotNameToCreate + " ] of instance [ TestServer ].");
            String jobId = callRunProcedure(jo);

            String response = waitForJob(jobId);

            // Check job status
            assertEquals("Job completed without errors", "success", response);

            org.openstack4j.model.compute.Image instanceSnapshot = null;
            // Get the instance snapshot from OpenStack
            for (org.openstack4j.model.compute.Image image : m_osClient.compute().images().list()) {
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
            assertEquals("Instance snapshot name is not set correctly", snapshotNameToCreate, instanceSnapshot.getName());
            assertEquals("Instance snapshot status is not set correctly", "ACTIVE", instanceSnapshot.getStatus().toString());

            Map<String, String> metadata = m_osClient.images().get(snapshotId).getProperties();

            assertEquals("Description not set correctly", "testSnapshot", metadata.get("desc"));

        }

        {
            // Deploy a new VM from the snapshot created above
            // with given availability zone, customization script and security group


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
                    .put("value", "#!/bin/bash\n" +
                            "mkdir /home/testDir"));

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
            assertEquals("Instance  name is not set correctly", instanceNameToCreate, instanceFromOpenstack.getName());
            assertEquals("Instance  status is not set correctly", "ACTIVE", instanceFromOpenstack.getStatus().toString());
            assertEquals("Instance  availability zone is not set correctly", prop.getProperty(AVAILABILITY_ZONE), instanceFromOpenstack.getAvailabilityZone().toString());

            // Verify that directory created in customization script exists.
            String command = "#! /bin/bash\n" +
                    "if [ -d /home/testDir ];\n" +
                    "then\n" +
                    "echo \"/home/testDir exists\";\n" +
                    "else\n" +
                    "echo \"/home/testDir does not exits\";\n" +
                    "fi";

            String responseFromShell = executeOnRemoteMachine(instanceIP, command);
            assertEquals("/home/testDir exists\n", responseFromShell);
        }

        {
            // Reboot the VM that is deployed above

            JSONObject jo = new JSONObject();


            jo.put("projectName", "EC-OpenStack-" + PLUGIN_VERSION);
            jo.put("procedureName", "RebootInstance");


            JSONArray actualParameterArray = new JSONArray();
            actualParameterArray.put(new JSONObject()
                    .put("value", "hp")
                    .put("actualParameterName", "connection_config"));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "tenant_id")
                    .put("value", TENANTID));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "server_id")
                    .put("value", serverId));

            actualParameterArray.put(new JSONObject()
                    .put("actualParameterName", "reboot_type")
                    .put("value", "soft"));

            jo.put("actualParameter", actualParameterArray);

            System.out.println("Rebooting instance [ " + instanceNameToCreate + " ].");
            String jobId = callRunProcedure(jo);

            String response = waitForJob(jobId);

            // Check job status
            assertEquals("Job completed without errors", "success", response);

            // Assert that instance is now ACTIVE after reboot.

            assertEquals("Instance did not become active after reboot.", "ACTIVE", m_osClient.compute().servers().get(serverId).getStatus().toString());
            // Assert that instance has really undergone the reboot action.

            JSONArray instanceActionLogs = getInstanceActionLogs(serverId);
            JSONObject actionLog = null;

            int rebootEvents = 0;
            for (int i = 0; i < instanceActionLogs.length(); i++) {
                actionLog = (JSONObject) instanceActionLogs.get(i);
                if(actionLog.get("action").toString().equalsIgnoreCase("reboot")){
                    rebootEvents ++;
                }
            }

            assertEquals("No of reboot actions does not match", 1, rebootEvents);


        }

    }

    @AfterClass
    public static void cleanup() throws JSONException{

        System.out.println("Cleaning up Openstack resources.");


        if (volumeId != null) {
            m_osClient.blockStorage().volumes().delete(volumeId);
        }
        if (serverId != null) {
            m_osClient.compute().servers().delete(serverId);
        }
        if (imageId != null) {
           m_osClient.images().delete(imageId);
        }
        if (stackId != null) {
            m_osClient.heat().stacks().delete(stackNameToCreate, stackId);
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

    private void validatePropertySheetExists(String propertyPath) throws IOException, JSONException {
        validatePropertyExists(propertyPath, /*expectPropertySheet*/ true);
    }

    private void validatePropertyExists(String propertyPath) throws IOException, JSONException {
        validatePropertyExists(propertyPath, /*expectPropertySheet*/ false);
    }

    private void validatePropertyExists(String propertyPath, boolean expectPropertySheet) throws IOException, JSONException {

        String url = "http://" + COMMANDER_USER + ":" + COMMANDER_PASSWORD +
                "@" + COMMANDER_SERVER + ":8000/rest/v1.0/properties/" + propertyPath;
        JSONObject result = performHTTPGet(url);
        assertTrue("Expect the property " + propertyPath + " to be defined", result.has("property"));

        if (expectPropertySheet) {
            assertTrue("Expect the property " + propertyPath + " to be defined as a property sheet",
                    result.getJSONObject("property").has("propertySheetId"));
        } else {
            assertFalse("Expect the property " + propertyPath + " to be defined as a simple property and *not* a property sheet",
                    result.getJSONObject("property").has("propertySheetId"));
        }
    }

    private void validateProperty(String propertyPath, String expectedValue) throws IOException, JSONException {

        String value = getProperty(propertyPath);
        assertEquals("Incorrect value found for property: " + propertyPath, expectedValue, value);
    }

    /**
     * Retrieves the property value from the commander server. The lookup
     * will fail if the property does not exist or if the property is a
     * property sheet and not a simple property.
     *
     * @return the value of the property. Returns null if the property does not
     * have any value specified.
     * @path a property path
     */
    public static String getProperty(String path) throws IOException, JSONException {

        String url = "http://" + COMMANDER_USER + ":" + COMMANDER_PASSWORD +
                "@" + COMMANDER_SERVER + ":8000/rest/v1.0/properties/" + path;
        JSONObject result = performHTTPGet(url);
        assertTrue("Expect the property " + path + " to be defined", result.has("property"));
        return result.getJSONObject("property").has("value") ?
                result.getJSONObject("property").getString("value") : null;
    }

    /**
     * waitForJob: Waits for job to be completed and reports outcome
     *
     * @param jobId
     * @return outcome of job
     */
    public static String waitForJob(String jobId) throws IOException, JSONException {

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
    private static JSONObject performHTTPGet(String url) throws IOException, JSONException {

        HttpClient httpClient = new DefaultHttpClient();
        try {
            HttpGet httpGetRequest = new HttpGet(url);

            HttpResponse httpResponse = httpClient.execute(httpGetRequest);
            if (httpResponse.getStatusLine().getStatusCode() >= 400) {
                throw new RuntimeException("HTTP GET failed with " +
                        httpResponse.getStatusLine().getStatusCode() + "-" +
                        httpResponse.getStatusLine().getReasonPhrase());
            }
            return new JSONObject(EntityUtils.toString(httpResponse.getEntity()));

        } finally {
            httpClient.getConnectionManager().shutdown();
        }

    }

    /**
     * Delete the openstack configuration used for this test suite (clear previous runs)
     */
    private static void deleteConfiguration() throws JSONException, IOException {
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
    private static void createConfiguration() throws JSONException, IOException {

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
    private static Properties loadProperties() throws IOException {
        InputStream input = null;
        try {
            input = new FileInputStream("ecplugin_test.properties");
            Properties prop = new Properties();
            prop.load(input);
            return prop;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static String executeOnRemoteMachine(String hostIp, String command) {
        String response = null;

        System.out.println("Connecting to : " + hostIp);
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        Session session = null;
        SSHClient sshClient = new SSHClient();
        sshClient.addHostKeyVerifier(new PromiscuousVerifier());
        long timeTaken = 0;

        // Try multiple time after server boots up for first time.
        while(true){

            if(timeTaken > TIMEOUT_PERIOD_SEC){
                fail("Could not make ssh connection within time. Exiting ...");
                break;
            }
            try {
                System.out.println("making connection.");
                sshClient.connect(hostIp);

                PKCS8KeyFile keyFile = new PKCS8KeyFile();
                keyFile.init(new File(prop.getProperty(PEM_FILE_LOCATION)));
                sshClient.authPublickey(prop.getProperty(LOGIN_USER_NAME), keyFile);

                System.out.println("Opening SSH session");
                session = sshClient.startSession();
                Session.Command cmd = session.exec(command);
                response = IOUtils.readFully(cmd.getInputStream()).toString();
                cmd.join(10, TimeUnit.SECONDS);

                // Connection made successful. Break the loop.
                break;

            }catch (Exception e) {

                e.printStackTrace();

                // Wait and try after some more time.
                try {
                    System.out.println("Waiting for " + WAIT_TIME + "millsec.");
                    Thread.sleep(WAIT_TIME);
                    timeTaken += WAIT_TIME;
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }

            } finally {

                try {
                    if(session != null) {
                        System.out.println("Closing the ssh connection.");
                        session.close();
                    }
                    sshClient.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return response;

    }

    /**
     * getInstaceActionLogs
     *
     * @param instanceId
     * @return JSONArray array of actions that happened on instance of instanceId
     */
    public static JSONArray getInstanceActionLogs(String instanceId) throws IOException, JSONException {

        JSONArray actionLogs = null;
        HttpClient httpClient = new DefaultHttpClient();
        String keystoneApiVersion = prop.getProperty(KEYSTONE_API_VERSION);
        String URL = null;
        StringEntity input = null;
        String token = null;
        JSONArray instanceActionLogs = null;

        if (keystoneApiVersion.equalsIgnoreCase("2.0") || keystoneApiVersion.equalsIgnoreCase("v2.0") ) {

            JSONObject passwordCredentials = new JSONObject();
            JSONObject auth = new JSONObject();
            JSONObject requestBody = new JSONObject();


            passwordCredentials.put("password", PASSWORD)
                                .put("username", USER);

            auth.put("tenantId", TENANTID)
                .put("passwordCredentials", passwordCredentials);

            requestBody.put("auth",auth);

            URL = IDENTITY_URL + "/tokens";

            HttpPost httpPostRequest = new HttpPost(URL);
            input = new StringEntity(requestBody.toString());
            input.setContentType("application/json");
            httpPostRequest.setEntity(input);

            HttpResponse response = httpClient.execute(httpPostRequest);
            JSONObject result = new JSONObject(EntityUtils.toString(response.getEntity()));

            if (response.getStatusLine().getStatusCode() == 401) {
                fail("Openstack user is unauthorized.");
            }


            token = result.getJSONObject("access").getJSONObject("token").get("id").toString();


        } else {

            JSONObject project = new JSONObject();
            project.put("id", TENANTID);

            JSONObject scope = new JSONObject();
            scope.put("project", project);

            JSONObject domain = new JSONObject();
            domain.put("id","default");

            JSONObject user = new JSONObject();
            user.put("domain", domain)
                .put("name", USER)
                .put("password", PASSWORD);

            JSONObject password = new JSONObject();
            password.put("user", user);

            JSONArray array = new JSONArray();
            array.put(0, "password");

            JSONObject identity = new JSONObject();
            identity.put("password", password)
                    .put("methods", array);

            JSONObject auth = new JSONObject();
            auth.put("identity",identity)
                .put("scope", scope);

            JSONObject requestBody = new JSONObject();
            requestBody.put("auth", auth);


            URL = IDENTITY_URL + "/auth/tokens";

            HttpPost httpPostRequest = new HttpPost(URL);
            input = new StringEntity(requestBody.toString());
            input.setContentType("application/json");
            httpPostRequest.setEntity(input);

            System.out.println(URL);
            System.out.println(requestBody);

            HttpResponse response = httpClient.execute(httpPostRequest);
            JSONObject result = new JSONObject(EntityUtils.toString(response.getEntity()));

            if (response.getStatusLine().getStatusCode() == 401) {
                fail("Openstack user is unauthorized.");
            }

            HeaderIterator headerIterator = response.headerIterator();
            Header header = null;

            while (headerIterator.hasNext()){

                header = headerIterator.nextHeader();
                if(header.getName().equalsIgnoreCase("X-Subject-Token")){
                    token = header.getValue();
                    break;
                }

            }

        }

        String computeUrl = prop.getProperty(COMPUTE_SERVICE_URL) + "/v" + prop.getProperty(COMPUTE_SERVICE_VERSION) + "/" + TENANTID + "/" + "servers" + "/" + instanceId + "/" + "os-instance-actions";
        HttpGet httpGetRequest = new HttpGet(computeUrl);
        httpGetRequest.setHeader("X-Auth-Token", token);

        HttpResponse response = httpClient.execute(httpGetRequest);

        JSONObject result = new JSONObject(EntityUtils.toString(response.getEntity()));

        instanceActionLogs = result.getJSONArray("instanceActions");
        return instanceActionLogs;

    }

}

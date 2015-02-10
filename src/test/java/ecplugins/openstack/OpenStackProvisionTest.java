package ecplugins.openstack;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
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

import org.openstack4j.openstack.OSFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class OpenStackProvisionTest {

    private static OSClient m_osClient;
    private String COMMANDER_SERVER = System.getProperty("COMMANDER_SERVER");
    private static String IDENTITY_URL = System.getProperty("OPENSTACK_IDENTITY_URL");
    private static String USER = System.getProperty("OPENSTACK_USER");
    private static String PASSWORD = System.getProperty("OPENSTACK_PASSWORD");
    private static String TENANTID = System.getProperty("OPENSTACK_TENANTID");

    @BeforeClass
    public static void setup(){

        m_osClient = OSFactory.builder()
                .endpoint(IDENTITY_URL)
                .credentials(USER,PASSWORD)
                .tenantId(TENANTID)
                .authenticate();
    }

    @Test public void testkeyPairCreation() {


        JSONObject param1 = new JSONObject();
        JSONObject param2 = new JSONObject();
        JSONObject param3 = new JSONObject();
        JSONObject param4 = new JSONObject();

        JSONObject jo = new JSONObject();

        try {
            jo.put("projectName", "EC-OpenStack-1.1.2");
            jo.put("procedureName", "CreateKeyPair");
            jo.put("timeout", "600");
            jo.put("pollInterval", "5");


            param1.put("value", "hp");
            param1.put("actualParameterName", "connection_config");

            param2.put("actualParameterName", "keyname");
            param2.put("value", "automatedTest-commandLine3");

            param3.put("actualParameterName", "tenant_id");
            param3.put("value", "10200973354647");

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

        callRunProcedure(jo);

        // Get the keypair from OpenStack
        Keypair keypair = m_osClient.compute().keypairs().get("automatedTest-commandLine3");

        // Assert keypair is not null
        assertNotNull(keypair);

        // Grab the keypair name and check its name
        assertEquals("Keypair name is","automatedTest-commandLine3",keypair.getName() );

    }

    public void callRunProcedure(JSONObject jo) {

        HttpClient httpClient = new DefaultHttpClient();

        try {
            HttpPost httpPostRequest = new HttpPost("http://admin:changeme@"+ COMMANDER_SERVER + ":8000/rest/v1.0/jobs?request=runProcedure");
            StringEntity input = new StringEntity(jo.toString());

            input.setContentType("application/json");
            httpPostRequest.setEntity(input);
            HttpResponse httpResponse = httpClient.execute(httpPostRequest);

            System.out.println("----------------------------------------");
            System.out.println(httpResponse.getStatusLine());
            System.out.println(EntityUtils.toString(httpResponse.getEntity()));
            System.out.println("----------------------------------------");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }
}

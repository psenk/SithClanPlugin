package sithclanplugin.util;

import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import sithclanplugin.SithClanPluginConfig;

public class SithClanPluginUtil
{

    private static final int TIMEOUT_SECONDS = 60;

    /**
     * Removes Discord emojis from text
     * 
     * @param input
     *                  String text to search for emojis using regex
     * @return String text without emojis
     */
    public static String removeEmojis(String input)
    {
        return input.replaceAll(":[a-zA-Z0-9_]+:", "").trim();
    }

    /**
     * Creates and sends an HTTP GET request
     * 
     * @param client
     *                   OkHttpClient object
     * @param uri
     *                   String GET URI
     * @return String HTTP response body
     */
    public static String sendGetRequest(OkHttpClient client, String uri)
    {
        // build HTTP GET request
        Request request = new Request.Builder()
                .url(uri)
                .get()
                .build();
        try (Response response = client.newCall(request).execute())
        {
            ResponseBody responseBody = response.body();
            if (responseBody == null)
            {
                return null;
            }
            return responseBody.string();
        } catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates and sends HTTP POST request
     * 
     * @param client
     *                   OkHttpClient object
     * @param apiKey
     *                   String auth API key
     * @param data
     *                   String data to post
     * @param uri
     *                   String POST URI
     * @return String HTTP Response body with status code
     */
    public static String sendPostRequest(OkHttpClient client, String apiKey, String data, String uri)
    {
        // new client with longer timeout
        OkHttpClient clientWithTimeout = client.newBuilder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();

        // build HTTP POST request
        Request request = new Request.Builder()
                .url(uri)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(MediaType.parse("application/json"), data))
                .build();

        try (Response response = clientWithTimeout.newCall(request).execute())
        {
            ResponseBody responseBody = response.body();
            if (responseBody == null)
            {
                return null;
            }
            if (response.code() != 200 && response.code() != 201)
            {
                return null;
            }
            return responseBody.string();
        } catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates and sends HTTP PUT request
     * 
     * @param client
     *                   OkHttpClient object
     * @param apiKey
     *                   String auth API key
     * @param data
     *                   String data to put
     * @param uri
     *                   String PUT URI
     * @return String HTTP Response body with status code
     */
    public static String sendPutRequest(OkHttpClient client, String apiKey, String data, String uri)
    {

        // build HTTP PUT request
        Request request = new Request.Builder()
                .url(uri)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .put(RequestBody.create(MediaType.parse("application/json"), data))
                .build();

        try (Response response = client.newCall(request).execute())
        {
            ResponseBody responseBody = response.body();
            if (responseBody == null)
            {
                return null;
            }
            if (response.code() != 200)
            {
                return null;
            }
            return responseBody.string();
        } catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates and sends HTTP DELETE request
     * 
     * @param client
     *                   OkHttpClient object
     * @param apiKey
     *                   String auth API key
     * @param uri
     *                   String DELETE URI
     * @return String HTTP Response body with status code
     */
    public static String sendDeleteRequest(OkHttpClient client, String apiKey, String uri)
    {

        // build HTTP DELETE request
        Request request = new Request.Builder()
                .url(uri)
                .header("Authorization", "Bearer " + apiKey)
                .delete()
                .build();

        try (Response response = client.newCall(request).execute())
        {
            ResponseBody responseBody = response.body();
            if (responseBody == null)
            {
                return null;
            }
            if (response.code() != 200)
            {
                return null;
            }
            return responseBody.string();
        } catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Validates API key in plugin config via HTTP GET request
     * Saves senate member state
     * 
     * @return boolean is API key valid
     */
    public static boolean validateApiKey(OkHttpClient client, SithClanPluginConfig config)
    {
        // create HTTP GET request
        Request request = new Request.Builder()
                .url(SithClanPluginConstants.VALIDATE_URI)
                .header("Authorization", "Bearer " + config.apiKey())
                .get()
                .build();

        try (Response response = client.newCall(request).execute())
        {
            ResponseBody responseBody = response.body();
            if (responseBody == null)
            {
                return false;
            }
            // validate response
            boolean isSenateMember = response.code() == 200;
            return isSenateMember;
        } catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }
}

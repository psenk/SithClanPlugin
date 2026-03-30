package sithclanplugin.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import sithclanplugin.SithClanPluginConfig;

public class SithClanPluginUtil {

    /**
     * Removes Discord emojis from text
     * 
     * @param input String text to search for emojis using regex
     * @return String text without emojis
     */
    public static String removeEmojis(String input) {
        return input.replaceAll(":[a-zA-Z0-9_]+:", "").trim();
    }

    /**
     * Creates and sends an HTTP GET request
     * 
     * @param client HttpClient object
     * @param uri    String GET URI
     * @return String HTTP response body
     */
    public static String sendGetRequest(HttpClient client, String uri) {
        // build HTTP GET request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .GET()
                .build();
        try {
            // send request and process response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates and sends HTTP POST request
     * 
     * @param client HttpClient object
     * @param apiKey String auth API key
     * @param data   String data to post
     * @param uri    String POST URI
     * @return String HTTP Response body with status code
     */
    public static String sendPostRequest(HttpClient client, String apiKey, String data, String uri) {
        // build HTTP POST request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(data))
                .build();

        try {
            // send request and process response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 && response.statusCode() != 201) {
                return null;
            }
            return response.body();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates and sends HTTP PUT request
     * 
     * @param client HttpClient object
     * @param apiKey String auth API key
     * @param data   String data to put
     * @param uri    String PUT URI
     * @return String HTTP Response body with status code
     */
    public static String sendPutRequest(HttpClient client, String apiKey, String data, String uri) {
        // build HTTP PUT request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .PUT(HttpRequest.BodyPublishers.ofString(data))
                .build();

        try {
            // send request and process response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }
            return response.body();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates and sends HTTP DELETE request
     * 
     * @param client HttpClient object
     * @param apiKey String auth API key
     * @param uri    String DELETE URI
     * @return String HTTP Response body with status code
     */
    public static String sendDeleteRequest(HttpClient client, String apiKey, String uri) {
        // build HTTP DELETE request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Authorization", "Bearer " + apiKey)
                .DELETE()
                .build();

        try {
            // send request and process response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }
            return response.body();
        } catch (Exception e) {
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
    public static boolean validateApiKey(HttpClient client, SithClanPluginConfig config) {
        // create HTTP GET request
        HttpRequest validationRequest = HttpRequest.newBuilder()
                .uri(URI.create(SithClanPluginConstants.VALIDATE_URI))
                .header("Authorization", "Bearer " + config.apiKey())
                .GET()
                .build();

        try {
            // send request
            HttpResponse<String> validationResponse = client.send(validationRequest,
                    HttpResponse.BodyHandlers.ofString());
            // validate response
            boolean isSenateMember = validationResponse.statusCode() == 200;
            return isSenateMember;
        } catch (Exception e) {
            return false;
        }
    }
}

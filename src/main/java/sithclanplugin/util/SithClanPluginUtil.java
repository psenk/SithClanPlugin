package sithclanplugin.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.inject.Inject;

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
     * @param uri    String GET uri
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
     * @param uri    String GET uri
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
            // sent request and process response
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
}

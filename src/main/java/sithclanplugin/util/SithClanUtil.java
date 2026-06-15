/*
 * Copyright (c) 2026, Kyanize
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package sithclanplugin.util;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import javax.swing.JLabel;
import javax.swing.Timer;

import com.google.gson.JsonObject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import sithclanplugin.SithClanConfig;

@Slf4j
public class SithClanUtil
{
    private static OkHttpClient cachedTimeoutClient = null;

    private static final int TIMEOUT_SECONDS = 60;

    /**
     * HTTP FUNCTIONS
     */

    /**
     * Create and send an HTTP GET request
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
        return executeRequest(client, request);
    }

    /**
     * Create and send HTTP POST request
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
        // build HTTP POST request
        Request request = new Request.Builder()
                .url(uri)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(MediaType.parse("application/json"), data))
                .build();

        return executeRequest(getTimeoutClient(client), request);
    }

    /**
     * Create and send HTTP PUT request
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

        return executeRequest(client, request);
    }

    /**
     * Create and send HTTP DELETE request
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

        return executeRequest(client, request);
    }

    /**
     * Executes HTTP request
     * 
     * @param client
     *                    OkHttpClient client to send requests
     * @param request
     *                    Request HTTP request object
     * @return String response body
     */
    private static String executeRequest(OkHttpClient client, Request request)
    {
        log.debug("Sending {} request to {}", request.method(), request.url());

        try (Response response = client.newCall(request).execute())
        {
            ResponseBody responseBody = response.body();
            if (responseBody == null || !response.isSuccessful())
            {
                log.warn("Request to {} failed with status code: {}", request.url(), response.code());
                return null;
            }
            String body = responseBody.string();
            log.info("Request to {} succeeded ({} bytes)", request.url(), body.length());
            return body;
        } catch (Exception e)
        {
            log.error("Exception during request to {}: {}", request.url(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Create and send HTTP POST request to Discord webhook
     * 
     * @param client
     *                       OkHttpClient client to send request
     * @param webhookUrl
     *                       String Discord webhook URL
     * @param message
     *                       String content to post
     * @return String HTTP Response body
     */
    public static String sendEventLogToDiscord(OkHttpClient client, String webhookUrl, String message)
    {
        // create JSON string
        JsonObject body = new JsonObject();
        body.addProperty("content", message);
        String jsonBody = body.toString();

        // build HTTP POST request
        Request request = new Request.Builder()
                .url(webhookUrl)
                .post(RequestBody.create(MediaType.parse("application/json"), jsonBody))
                .build();

        return executeRequest(client, request);
    }

    /**
     * Validate API key in plugin config via HTTP GET request
     * Save senate member state
     * 
     * @return boolean is API key valid
     */
    public static boolean validateApiKey(OkHttpClient client, SithClanConfig config)
    {
        // create HTTP GET request
        Request request = new Request.Builder()
                .url(SithClanConstants.VALIDATE_URI)
                .header("Authorization", "Bearer " + config.apiKey())
                .get()
                .build();

        try (Response response = client.newCall(request).execute())
        {
            ResponseBody responseBody = response.body();
            if (responseBody == null)
            {
                log.warn("API key validation returned null response body.");
                return false;
            }
            // validate response
            boolean isSenateMember = response.code() == 200;
            log.info("API key validation result: {} (HTTP {})", isSenateMember ? "VALID" : "INVALID", response.code());
            return isSenateMember;
        } catch (Exception e)
        {
            log.error("Exception during API key validation: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checks if a request is rate limited
     * 
     * @param lastFetched
     *                            ZonedDateTime timestamp of last fetch
     * @param cooldownMinutes
     *                            int rate limiting amount in minutes
     * @param isSenateMember
     *                            boolean if senate bypass rate limiting
     * @return boolean if rate limited
     */
    public static boolean isRateLimited(ZonedDateTime lastFetched, int cooldownMinutes, boolean isSenateMember)
    {
        if (isSenateMember)
            return false;
        return lastFetched != null && ZonedDateTime.now().isBefore(lastFetched.plusMinutes((cooldownMinutes)));
    }

    /**
     * Lazy initialization of OkHttpClient with increased timeout
     * 
     * @param client
     *                   OkHttpClient instance
     * @return OkHttpClient instance with increased timeout
     */
    private static synchronized OkHttpClient getTimeoutClient(OkHttpClient client)
    {
        if (cachedTimeoutClient == null)
        {
            // new client with longer timeout
            cachedTimeoutClient = client.newBuilder()
                    .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build();
        }
        return cachedTimeoutClient;
    }

    /**
     * MISC FUNCTIONS
     */

    /**
     * Remove Discord emojis from text
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
     * Creates timer to clear status label text
     * 
     * @param label
     *                  JLabel status label to clear after timer
     */
    public static void statusTimer(JLabel label)
    {
        Timer timer = new Timer(5000, new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent event)
            {
                label.setText("");
                label.setForeground(ColorScheme.TEXT_COLOR);
            }
        });

        timer.setRepeats(false);
        timer.start();
    }

    /**
     * Helper function to create status message label
     * 
     * @return JLabel created status label
     */
    public static JLabel createStatusLabel()
    {
        JLabel statusLabel = new JLabel();
        statusLabel.setVisible(true);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setPreferredSize(SithClanConstants.STATUS_LABEL_DIMENSION);
        statusLabel.setMinimumSize(SithClanConstants.STATUS_LABEL_DIMENSION);
        statusLabel.setMaximumSize(SithClanConstants.STATUS_LABEL_DIMENSION);
        return statusLabel;
    }
}

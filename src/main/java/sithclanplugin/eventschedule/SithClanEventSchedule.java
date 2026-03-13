package sithclanplugin.eventschedule;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;

import javax.inject.Inject;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Singleton;

import lombok.Getter;
import net.runelite.client.RuneLite;
import sithclanplugin.SithClanPluginConfig;
import sithclanplugin.SithClanPluginConstants;

/**
 * Event Schedule Object
 */

@Getter
@Singleton
public class SithClanEventSchedule {

    private final SithClanPluginConfig config;
    private final SithClanNotificationManager notificationManager;
    private boolean isSenateMember = false;

    private ArrayList<SithClanDaySchedule> schedule;
    private LocalDateTime lastScheduleFetch;
    private final HttpClient httpClient;
    private final File localDirectory;
    private final File storedScheduleFile;

    private static final String EVENT_SCHEDULE_GET_URI = "https://sithclanplugin.psenk168.workers.dev/api/eventschedule";
    private static final String EVENT_SCHEDULE_POST_URI = "https://sithclanplugin.psenk168.workers.dev/api/eventschedule/post";
    private static final String VALIDATE_URI = "https://sithclanplugin.psenk168.workers.dev/api/validate";
    private static final String LOCAL_DIRECTORY_NAME = "sithclanplugin";
    private static final String STORED_SCHEDULE_NAME = "sithclaneventschedule.txt";
    private static final int SCHEDULE_FETCH_COOLDOWN_MINUTES = 5;

    @Inject
    public SithClanEventSchedule(SithClanPluginConfig config, SithClanNotificationManager notificationManager) {
        this.config = config;
        this.notificationManager = notificationManager;

        this.localDirectory = new File(RuneLite.RUNELITE_DIR, LOCAL_DIRECTORY_NAME);
        this.storedScheduleFile = new File(localDirectory, STORED_SCHEDULE_NAME);
        schedule = new ArrayList<>();
        lastScheduleFetch = null;
        httpClient = HttpClient.newHttpClient();
    }

    /**
     * Uses HTTP GET request to obtain event schedule
     * 
     * @return String event schedule
     */
    private String getEventSchedule() {
        // build HTTP GET request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(EVENT_SCHEDULE_GET_URI))
                .GET()
                .build();
        try {
            // send request and return response
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Uses HTTP POST request to post event schedule
     * 
     * @param jsonData String event schedule
     * @return String HTTP response body
     */
    private String postEventSchedule(String jsonData) {
        // build HTTP POST request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(EVENT_SCHEDULE_POST_URI))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .build();
        try {
            // send request and return response
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets event schedule for display on panel
     * Includes 5 min rate limiting
     * Saves schedule locally
     * Schedules notifications
     */
    public int parseScheduleFromGet() {
        // rate limiting, 5 minutes
        boolean rateLimited = lastScheduleFetch != null
                && LocalDateTime.now().isBefore(lastScheduleFetch.plusMinutes(SCHEDULE_FETCH_COOLDOWN_MINUTES));

        // senate members bypass rate limiting
        if (rateLimited && !isSenateMember)
            return SithClanPluginConstants.STATUS_RATE_LIMITED;

        // get fresh schedule
        String jsonSchedule = getEventSchedule();
        if (jsonSchedule == null)
            return SithClanPluginConstants.STATUS_NOT_FOUND;
        // convert schedule to JSON
        Gson gson = new Gson();
        // java generic type erasure workaround
        Type scheduleType = new TypeToken<ArrayList<SithClanDaySchedule>>() {
        }.getType();
        this.schedule = gson.fromJson(jsonSchedule, scheduleType);
        saveScheduleLocally(jsonSchedule);
        this.lastScheduleFetch = LocalDateTime.now();
        notificationManager.scheduleNotifications(schedule);
        return SithClanPluginConstants.STATUS_OK;
    }

    /**
     * Takes String input and converts to JSON format for posting
     * Saves schedule locally
     * Schedules notifications
     * 
     * @param text event schedule as String input from plugin
     * @return String HTTPResponse body
     */
    public int parseScheduleForPost(String text) {
        if (text.isBlank())
            return SithClanPluginConstants.STATUS_BAD_INPUT;

        // split input into list of strings
        String[] scheduleInput = text.split("\\r?\\n");

        ArrayList<SithClanDaySchedule> newSchedule = convertSchedule(scheduleInput);
        if (newSchedule == null || newSchedule.isEmpty())
            return SithClanPluginConstants.STATUS_BAD_INPUT;

        // storing schedule as JSON object
        Gson gson = new Gson();
        String data = gson.toJson(newSchedule);

        // posting schedule online
        String response = postEventSchedule(data);
        if (response == null)
            return SithClanPluginConstants.STATUS_NOT_FOUND;
        this.schedule = newSchedule;
        saveScheduleLocally(data);
        notificationManager.scheduleNotifications(schedule);
        return SithClanPluginConstants.STATUS_OK;
    }

    /**
     * Gets event schedule from local file for display on panel
     */
    public int parseScheduleFromFile() {
        try {
            String jsonSchedule = new String(Files.readAllBytes(storedScheduleFile.toPath()));
            if (jsonSchedule.isBlank())
                return SithClanPluginConstants.STATUS_BAD_INPUT;
            Gson gson = new Gson();
            Type scheduleType = new TypeToken<ArrayList<SithClanDaySchedule>>() {
            }.getType();
            this.schedule = gson.fromJson(jsonSchedule, scheduleType);
            notificationManager.scheduleNotifications(schedule);
            return SithClanPluginConstants.STATUS_OK;
        } catch (Exception e) {
            e.printStackTrace();
            return SithClanPluginConstants.STATUS_NOT_FOUND;
        }
    }

    /**
     * Saves schedule to local file for cached loading
     * 
     * @param data String Schedule as JSON string
     */
    private void saveScheduleLocally(String data) {
        try (FileWriter fileWriter = new FileWriter(storedScheduleFile)) {
            fileWriter.write(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper function, converts event schedule String list into custom object
     * ArrayList output
     * 
     * @param scheduleInput String[] event schedule in list
     * @return ArrayList<SithClanDaySchedule> converted event schedule output
     */
    private ArrayList<SithClanDaySchedule> convertSchedule(String[] scheduleInput) {
        // tracking states
        SithClanDaySchedule currentDay = null;
        SithClanEvent currentEvent = null;
        ArrayList<SithClanDaySchedule> newSchedule = new ArrayList<>();

        // iterate through parsed event list
        for (String line : scheduleInput) {
            line = line.trim();
            if (line.isBlank())
                continue;
            // event date and new day
            if (line.startsWith("--")) {
                // next day in list
                if (currentDay != null)
                    newSchedule.add(currentDay);

                // first day
                currentDay = new SithClanDaySchedule();
                currentDay.setDate(line.substring(2));
                currentDay.setEvents(new ArrayList<>());
            }
            // event title and new event
            else if (line.startsWith("-")) {
                if (currentDay == null)
                    return null;
                // add last event to day
                if (currentEvent != null)
                    currentDay.getEvents().add(currentEvent);

                // first event
                currentEvent = new SithClanEvent();
                currentEvent.setEventTitle(line.substring(1));
                currentEvent.setEventMiscInfo(new ArrayList<>());
            }
            // event time
            else if (currentEvent != null && currentEvent.getEventTime() == null)
                currentEvent.setEventTime(line);

            // event host (optional)
            else if (currentEvent != null && line.startsWith("Hosted by:"))
                currentEvent.setEventHost(line.substring(11));

            // event location
            else if (currentEvent != null && line.startsWith("🌎"))
                currentEvent.setEventLocation(line);

            // event repetition (optional)
            else if (currentEvent != null && line.startsWith("**"))
                currentEvent.setEventRepeated(true);

            // misc event info
            else {
                if (currentEvent == null)
                    continue;
                currentEvent.getEventMiscInfo().add(line);
            }
        }

        // add last event
        if (currentEvent != null) {
            if (currentDay == null)
                return null;
            currentDay.getEvents().add(currentEvent);
        }

        // add last day
        if (currentDay != null)
            newSchedule.add(currentDay);

        return newSchedule;
    }

    /**
     * Validates API key in config
     * Saves senate member state
     * 
     * @return boolean is key valid or not
     */
    public boolean validateApiKey() {
        HttpRequest validationRequest = HttpRequest.newBuilder()
                .uri(URI.create(VALIDATE_URI))
                .header("Authorization", "Bearer " + config.apiKey())
                .GET()
                .build();

        try {
            HttpResponse<String> validationResponse = httpClient.send(validationRequest,
                    HttpResponse.BodyHandlers.ofString());
            this.isSenateMember = validationResponse.statusCode() == 200;
            return this.isSenateMember;
        } catch (Exception e) {
            return false;
        }
    }
}

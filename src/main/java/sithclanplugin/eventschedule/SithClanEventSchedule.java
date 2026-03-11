package sithclanplugin.eventschedule;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;

import javax.inject.Inject;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Singleton;

import lombok.Getter;
import lombok.Setter;
import sithclanplugin.SithClanPluginConfig;

/**
 * Event Schedule Object
 */

@Setter
@Getter
@Singleton
public class SithClanEventSchedule {

    private HttpClient httpClient;
    private SithClanPluginConfig config;
    private ArrayList<SithClanDaySchedule> schedule;
    private LocalDateTime lastScheduleFetch;

    private static final String EVENT_SCHEDULE_GET_URI = "http://127.0.0.1:8787/api/eventschedule";
    private static final String EVENT_SCHEDULE_POST_URI = "http://127.0.0.1:8787/api/eventschedule/post";
    private static final String LOCAL_FOLDER_PATH = "%userprofile%/.runelite/sithclanplugin";
    private static final int SCHEDULE_FETCH_COOLDOWN_MINUTES = 5;

    // TODO: save schedule locally for faster loading
    // TODO: schedule expiration?

    @Inject
    public SithClanEventSchedule(SithClanPluginConfig config) {
        this.config = config;
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
     * Returns event schedule for display on panel
     * Includes 5 min rate limiting
     */
    public void parseScheduleFromGet() {
        // rate limiting, 5 minutes
        // if fetched recently, return
        // this.schedule still has data from last fetch
        if (lastScheduleFetch != null
                && LocalDateTime.now().isBefore(lastScheduleFetch.plusMinutes(SCHEDULE_FETCH_COOLDOWN_MINUTES))) {
            return;
        }

        // get fresh schedule
        String jsonSchedule = getEventSchedule();
        if (jsonSchedule == null)
            return;
        // convert schedule to JSON
        Gson gson = new Gson();
        // java generic type erasure workaround
        Type scheduleType = new TypeToken<ArrayList<SithClanDaySchedule>>() {
        }.getType();
        this.schedule = gson.fromJson(jsonSchedule, scheduleType);
        this.lastScheduleFetch = LocalDateTime.now();
    }

    /**
     * Takes String input and converts to JSON format for posting
     * 
     * @param text event schedule as String input from plugin
     * @return String HTTPResponse
     */
    public String parseScheduleForPost(String text) {
        ArrayList<SithClanDaySchedule> newSchedule = new ArrayList<>();

        // split input into list of strings
        String[] scheduleInput = text.split("\\r?\\n");
        // tracking states
        SithClanDaySchedule currentDay = null;
        SithClanEvent currentEvent = null;

        // iterate through parsed event list
        for (String line : scheduleInput) {
            line = line.trim();
            if (line.isBlank())
                continue;
            // event date and new day
            if (line.startsWith("--")) {
                // next day in list
                if (currentDay != null) {
                    newSchedule.add(currentDay);
                }
                // first day
                currentDay = new SithClanDaySchedule();
                currentDay.setDate(line.substring(2));
                currentDay.setEvents(new ArrayList<>());
            }
            // event title and new event
            else if (line.startsWith("-")) {
                // add last event to day
                if (currentEvent != null) {
                    currentDay.getEvents().add(currentEvent);
                }
                // first event
                currentEvent = new SithClanEvent();
                currentEvent.setEventTitle(line.substring(1));
                currentEvent.setEventMiscInfo(new ArrayList<>());
            }
            // event time
            else if (currentEvent != null && currentEvent.getEventTime() == null) {
                currentEvent.setEventTime(line);
            }
            // event host (optional)
            else if (line.startsWith("Hosted by:")) {
                currentEvent.setEventHost(line.substring(11));
            }
            // event location
            else if (line.startsWith("🌎")) {
                currentEvent.setEventLocation(line);
            }
            // event repetition (optional)
            else if (line.startsWith("**")) {
                currentEvent.setEventRepeated(true);
            }
            // misc event info
            else {
                currentEvent.getEventMiscInfo().add(line);
            }
        }
        // add last event
        if (currentEvent != null) {
            currentDay.getEvents().add(currentEvent);
        }
        // add last day
        if (currentDay != null) {
            newSchedule.add(currentDay);
        }

        // storing schedule as JSON object
        Gson gson = new Gson();
        String data = gson.toJson(newSchedule);

        // posting schedule online
        String response = postEventSchedule(data);
        if (response == null) {
            return "Post request failed.";
        }
        this.schedule = newSchedule;
        return response;
    }

    
}

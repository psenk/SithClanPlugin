package com.sithclanplugin.eventschedule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;

import javax.inject.Inject;

import com.google.gson.Gson;
import com.google.inject.Singleton;
import com.sithclanplugin.SithClanPluginConfig;

import lombok.Getter;
import lombok.Setter;

/**
 * Event Schedule Object
 */

@Setter
@Getter
@Singleton
public class SithClanEventSchedule {

    private String rawSchedule;
    private HttpClient httpClient;
    private SithClanPluginConfig config;
    private ArrayList<SithClanDaySchedule> schedule;
    private LocalDateTime lastScheduleFetch;

    private static final String EVENT_SCHEDULE_GET_URI = "http://127.0.0.1:8787/api/eventschedule";
    private static final String EVENT_SCHEDULE_POST_URI = "http://127.0.0.1:8787/api/eventschedule/post";
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
     * @return event schedule as String
     */
    public String getEventSchedule() {
        // rate limiting, 5 minutes
        if (lastScheduleFetch != null
                && LocalDateTime.now().isBefore(lastScheduleFetch.plusMinutes(SCHEDULE_FETCH_COOLDOWN_MINUTES))) {
            return this.getRawSchedule();
        }
        
        HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(EVENT_SCHEDULE_GET_URI))
        .GET()
        .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            this.rawSchedule = response.body();
            this.lastScheduleFetch = LocalDateTime.now();
            return this.getRawSchedule();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Uses HTTP POST request to post event schedule
     * 
     * @param data event schedule as String
     * @return HTTPResponse
     */
    public HttpResponse<String> postEventSchedule(String data) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(EVENT_SCHEDULE_POST_URI))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(data))
                .build();

        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Takes String input and converts to JSON format for posting
     * 
     * @param text event schedule as String input from plugin
     * @return String HTTPResponse
     */
    public String parseScheduleForPost(String text) {
        // prevent data accumulation
        schedule.clear();
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
                    schedule.add(currentDay);
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
            schedule.add(currentDay);
        }

        // storing schedule as JSON object
        Gson gson = new Gson();
        String data = gson.toJson(schedule);

        // posting schedule online
        HttpResponse<String> response = postEventSchedule(data);
        if (response == null) {
            return "Post request failed.";
        }
        return response.body();
    }

    public String parseScheduleFromGet(String text) {
        getEventSchedule();
        return "";
    }
}

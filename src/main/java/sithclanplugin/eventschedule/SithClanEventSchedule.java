package sithclanplugin.eventschedule;

import java.lang.reflect.Type;
import java.net.http.HttpClient;
import java.time.LocalDateTime;
import java.util.ArrayList;

import javax.inject.Inject;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Singleton;

import lombok.Getter;
import lombok.Setter;
import sithclanplugin.SithClanPluginConfig;
import sithclanplugin.managers.SithClanNotificationManager;
import sithclanplugin.managers.SithClanPluginFileManager;
import sithclanplugin.util.SithClanPluginConstants;
import sithclanplugin.util.SithClanPluginUtil;

/**
 * Event Schedule Object
 */

@Getter
@Singleton
public class SithClanEventSchedule {

    @Inject
    private SithClanPluginConfig config;

    @Inject
    private SithClanPluginFileManager fileManager;

    @Inject
    private SithClanNotificationManager notificationManager;

    @Inject
    private HttpClient httpClient;

    @Setter
    private boolean isSenateMember = false;

    private ArrayList<SithClanDaySchedule> schedule;
    private LocalDateTime lastTimeScheduleFetched;

    private static final int SCHEDULE_FETCH_COOLDOWN_MINUTES = 5;

    public SithClanEventSchedule() {
        schedule = new ArrayList<>();
        lastTimeScheduleFetched = null;
    }

    /**
     * Creates and sends an HTTP GET request to obtain the event schedule
     * 
     * @return String event schedule string in HTTP response body
     */
    private String getEventSchedule() {
        return SithClanPluginUtil.sendGetRequest(httpClient, SithClanPluginConstants.EVENT_SCHEDULE_GET_URI);
    }

    /**
     * Creates and sends HTTP POST request to post new event schedule
     * 
     * @param jsonData String JSON event schedule in string format
     * @return String HTTP Response body with status code
     */
    private String postEventSchedule(String jsonData) {
        return SithClanPluginUtil.sendPostRequest(httpClient, config.apiKey(), jsonData,
                SithClanPluginConstants.EVENT_SCHEDULE_POST_URI);
    }

    /**
     * Gets event schedule
     * Includes 5 min rate limiting
     * Saves schedule locally
     * Schedules event notifications
     * 
     * @return int SithClanPluginConstants status code value
     */
    public int parseScheduleFromGet() {
        // rate limiting, 5 minutes
        boolean rateLimited = lastTimeScheduleFetched != null
                && LocalDateTime.now().isBefore(lastTimeScheduleFetched.plusMinutes(SCHEDULE_FETCH_COOLDOWN_MINUTES));

        // allows senate members to bypass rate limiting
        if (rateLimited && !isSenateMember)
            return SithClanPluginConstants.STATUS_RATE_LIMITED;

        // get fresh event schedule
        String jsonSchedule = getEventSchedule();
        if (jsonSchedule == null)
            return SithClanPluginConstants.STATUS_NOT_FOUND;
        // convert schedule to JSON
        this.schedule = deserializeSchedule(jsonSchedule);
        // saves schedule
        fileManager.saveScheduleLocally(jsonSchedule);
        // refresh rate limiting
        this.lastTimeScheduleFetched = LocalDateTime.now();
        // schedule event notifications
        notificationManager.scheduleNotifications(schedule);
        return SithClanPluginConstants.STATUS_OK;
    }

    /**
     * Takes String input and converts to JSON format for posting
     * Saves schedule locally
     * Schedules notifications
     * 
     * @param scheduleInput String event schedule from plugin text box
     * @return int SithClanPluginConstants status code value
     */
    public int parseScheduleForPost(String scheduleInput) {
        if (scheduleInput.isBlank())
            return SithClanPluginConstants.STATUS_BAD_INPUT;

        // split input into list of strings
        String[] scheduleInputList = scheduleInput.split("\\r?\\n");
        // turn list into event schedule
        ArrayList<SithClanDaySchedule> newSchedule = convertSchedule(scheduleInputList);
        if (newSchedule == null || newSchedule.isEmpty())
            return SithClanPluginConstants.STATUS_BAD_INPUT;

        // store schedule as JSON object
        Gson gson = new Gson();
        String data = gson.toJson(newSchedule);

        // post schedule
        String response = postEventSchedule(data);
        if (response == null)
            return SithClanPluginConstants.STATUS_NOT_FOUND;
        this.schedule = newSchedule;
        // save schedule
        fileManager.saveScheduleLocally(data);
        // schedule event notifications
        notificationManager.scheduleNotifications(schedule);
        return SithClanPluginConstants.STATUS_OK;
    }

    /**
     * Gets event schedule from local file
     * 
     * @return int SithClanPluginConstants status code value
     */
    public int parseScheduleFromFile() {
        try {
            // load schedule from local file
            String jsonSchedule = fileManager.readScheduleFile();
            if (jsonSchedule == null || jsonSchedule.isBlank())
                return SithClanPluginConstants.STATUS_BAD_INPUT;
            // convert schedule to JSON
            this.schedule = deserializeSchedule(jsonSchedule);
            // schedule event notifications
            notificationManager.scheduleNotifications(schedule);
            return SithClanPluginConstants.STATUS_OK;
        } catch (Exception e) {
            e.printStackTrace();
            return SithClanPluginConstants.STATUS_NOT_FOUND;
        }
    }

    /**
     * Converts event schedule String list into custom object list
     * 
     * @param scheduleInput event schedule in String[] list
     * @return ArrayList<SithClanDaySchedule> converted event schedule
     */
    private ArrayList<SithClanDaySchedule> convertSchedule(String[] scheduleInput) {
        // tracking states
        SithClanDaySchedule currentDay = null;
        SithClanEvent currentEvent = null;

        ArrayList<SithClanDaySchedule> newSchedule = new ArrayList<>();

        // iterate through schedule list
        for (String line : scheduleInput) {
            if (line.isBlank())
                continue;
            line = line.trim();
            // parse event date
            if (line.startsWith("--")) {
                // add previous day created
                if (currentDay != null) {
                    if (currentEvent != null) {
                        currentDay.getEvents().add(currentEvent);
                        currentEvent = null;
                    }
                    newSchedule.add(currentDay);
                }

                // create and setup day
                currentDay = new SithClanDaySchedule();
                currentDay.setDate(line.substring(2));
                currentDay.setEvents(new ArrayList<>());
            }
            // parse event title
            else if (line.startsWith("-")) {
                if (currentDay == null)
                    return null;
                // add previous event created to day
                if (currentEvent != null)
                    currentDay.getEvents().add(currentEvent);

                // create and setup event
                currentEvent = new SithClanEvent();
                currentEvent.setEventTitle(line.substring(1));
                currentEvent.setEventMiscInfo(new ArrayList<>());
            }
            // parse event time
            else if (currentEvent != null && currentEvent.getEventTime() == null)
                currentEvent.setEventTime(line);

            // parse event host (optional info)
            else if (currentEvent != null && line.startsWith("Hosted by:"))
                currentEvent.setEventHost(line.substring(11));

            // parse event location
            else if (currentEvent != null && line.startsWith("🌎"))
                currentEvent.setEventLocation(line);

            // parse event repetition (optional info)
            else if (currentEvent != null && line.startsWith("**"))
                currentEvent.setEventRepeated(true);

            // parse misc event info (optional info)
            else {
                if (currentEvent == null)
                    continue;
                currentEvent.getEventMiscInfo().add(line);
            }
        }

        // add final event
        if (currentEvent != null) {
            if (currentDay == null)
                return null;
            currentDay.getEvents().add(currentEvent);
        }

        // add final day
        if (currentDay != null)
            newSchedule.add(currentDay);

        return newSchedule;
    }

    /**
     * Deserializes JSON string to ArrayList of SithClanDaySchedule objects
     * 
     * @param jsonSchedule JSON String of event schedule
     * @return ArrayList<SithClanDaySchedule> deserialized event schedule
     */
    private ArrayList<SithClanDaySchedule> deserializeSchedule(String jsonSchedule) {
        // convert schedule to JSON
        Gson gson = new Gson();
        // java generic type erasure workaround
        Type scheduleType = new TypeToken<ArrayList<SithClanDaySchedule>>() {
        }.getType();
        return gson.fromJson(jsonSchedule, scheduleType);
    }
}

package sithclanplugin.util;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class SithClanPluginConstants {

    // clan info
    public static final String CLAN_NAME = "Sith";

    // status codes
    public static final int STATUS_OK = 200;
    public static final int STATUS_BAD_INPUT = 400;
    public static final int STATUS_NOT_FOUND = 404;
    public static final int STATUS_UNPROCESSABLE = 422;
    public static final int STATUS_RATE_LIMITED = 429;
    public static final int STATUS_SERVICE_UNAVAILABLE = 503;

    // file structure
    public static final String LOCAL_DIRECTORY_NAME = "sithclanplugin";
    public static final String STORED_SCHEDULE_NAME = "sithclaneventschedule.txt";
    public static final String STORED_SUBSCRIPTIONS_NAME = "sithclannotifiedevents.txt";

    // uris
    public static final String EVENT_SCHEDULE_GET_URI = "https://sithclanplugin.psenk168.workers.dev/api/eventschedule";
    public static final String EVENT_SCHEDULE_POST_URI = "https://sithclanplugin.psenk168.workers.dev/api/eventschedule/post";
    public static final String VALIDATE_URI = "https://sithclanplugin.psenk168.workers.dev/api/validate";

    // discord
    public static final String SITH_DISCORD_SERVER_ID = "741153043776667658";
    public static final String DISCORD_CHANNEL_URI = "https://discord.com/channels/" + SITH_DISCORD_SERVER_ID + "/";

    // timing
    private static final String DATE_FORMAT = "M/d/yyyy";
    private static final String TIME_FORMAT = "h:mm a";
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_FORMAT);
    public static final ZoneId EST_ZONE = ZoneId.of("America/New_York");
}

package sithclanplugin.util;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class SithClanPluginConstants
{
        // testing
        // TODO: set false before release
        public static final boolean BYPASS_CLAN_CHECK = true;
        // public static final String TEST_PREFIX = "http://127.0.0.1:8787";
        // public static final String EVENT_SCHEDULE_URI = TEST_PREFIX +
        // "/api/eventschedule";
        // public static final String MEMBER_ROSTER_URI = TEST_PREFIX +
        // "/api/memberroster";
        // public static final String ANNOUNCEMENTS_URI = TEST_PREFIX +
        // "/api/announcements";
        // public static final String ANNOUNCEMENTS_EDIT_URI = TEST_PREFIX +
        // "/api/announcements/edit/";
        // public static final String ANNOUNCEMENTS_DELETE_URI = TEST_PREFIX +
        // "/api/announcements/delete/";
        // public static final String VALIDATE_URI = TEST_PREFIX + "/api/validate";

        // clan info
        // name
        public static final String CLAN_NAME = "Sith";

        // discord
        public static final String SITH_DISCORD_SERVER_ID = "741153043776667658";
        public static final String DISCORD_CHANNEL_URI = "https://discord.com/channels/" + SITH_DISCORD_SERVER_ID + "/";

        // ranks
        public static final String[] CLAN_RANKS =
        {
                        "Children of the Watch",
                        "Sith Loyalist",
                        "TIE Pilot",
                        "Sith Trooper",
                        "Death Trooper",
                        "Sovereign Protector",
                        "Sovereign Champion",
                        "Sith Acolyte",
                        "Sith Knight",
                        "Sith Marauder",
                        "Sith Lord",
                        "Sith Overseer",
                        "Imperial Inquisitor",
                        "Grand Inquisitor",
                        "Shadow Hand"
        };

        // credits to promote
        public static final int[] CREDITS_TO_PROMOTE =
        {
                        0, // children of the watch, start as this rank
                        10, // loyalist
                        30, // tie pilot
                        65, // sith trooper
                        100, // death trooper
                        200, // sovereign protector
                        275, // sovereign champion
                        400, // sith acolyte
                        500, // sith knight
                        600, // sith marauder
                        0, // sith lord, no credit req
        };

        // time to promote in days
        public static final int[] DAYS_TO_PROMOTE =
        {
                        0, // children of the watch, start as this rank
                        0, // loyalist
                        0, // tie pilot
                        0, // sith trooper
                        90, // death trooper
                        180, // sovereign protector
                        180, // sovereign champion
                        270, // sith acolyte
                        365, // sith knight
                        1000, // sith marauder
                        0 // sith lord
        };

        // status codes
        public static final int STATUS_OK = 200;
        public static final int STATUS_RESOURCE_CREATED = 201;
        public static final int STATUS_BAD_INPUT = 400;
        public static final int STATUS_NOT_FOUND = 404;
        public static final int STATUS_RATE_LIMITED = 429;

        // file structure
        public static final String LOCAL_DIRECTORY_NAME = "sithclanplugin";
        public static final String STORED_SCHEDULE_NAME = "sithclaneventschedule.txt";
        public static final String STORED_SUBSCRIPTIONS_NAME = "sithclannotifiedevents.txt";

        // endpoints
        public static final String URI_PREFIX = "https://sithclanplugin.psenk168.workers.dev";
        public static final String STARTUP_URI = URI_PREFIX + "/api/startup";
        public static final String EVENT_SCHEDULE_URI = URI_PREFIX + "/api/eventschedule";
        public static final String MEMBER_ROSTER_URI = URI_PREFIX + "/api/memberroster";
        public static final String ANNOUNCEMENTS_URI = URI_PREFIX + "/api/announcements";
        public static final String ANNOUNCEMENTS_EDIT_URI = URI_PREFIX + "/api/announcements/edit/";
        public static final String ANNOUNCEMENTS_DELETE_URI = URI_PREFIX + "/api/announcements/delete/";
        public static final String VALIDATE_URI = URI_PREFIX + "/api/validate";

        // timing
        private static final String DATE_FORMAT = "M/d/yyyy";
        private static final String SHORT_DATE_FORMAT = "M/d/yy";
        private static final String TIME_FORMAT = "h:mm a";
        public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);
        public static final DateTimeFormatter SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern(SHORT_DATE_FORMAT);
        public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_FORMAT);
        public static final ZoneId EST_ZONE = ZoneId.of("America/New_York");
}

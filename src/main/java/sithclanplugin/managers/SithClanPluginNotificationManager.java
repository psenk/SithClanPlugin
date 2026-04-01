package sithclanplugin.managers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import net.runelite.client.Notifier;
import sithclanplugin.SithClanPluginConfig;
import sithclanplugin.eventschedule.SithClanDaySchedule;
import sithclanplugin.eventschedule.SithClanEvent;
import sithclanplugin.util.SithClanPluginConstants;
import sithclanplugin.util.SithClanPluginUtil;

@Singleton
public class SithClanPluginNotificationManager
{
    @Inject
    private SithClanPluginFileManager fileManager;

    private final Notifier notifier;
    private final SithClanPluginConfig config;
    private final ScheduledExecutorService scheduler;

    private final List<ScheduledFuture<?>> scheduledNotifications = new ArrayList<>();

    private static final String EVENT_NOTIFICATION = "Clan event starting soon: ";

    @Inject
    public SithClanPluginNotificationManager(SithClanPluginConfig config, Notifier notifier)
    {
        this.config = config;
        this.notifier = notifier;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    // for testing
    public SithClanPluginNotificationManager(SithClanPluginConfig config, Notifier notifier,
            ScheduledExecutorService scheduler, SithClanPluginFileManager fileManager)
    {
        this.config = config;
        this.notifier = notifier;
        this.scheduler = scheduler;
        this.fileManager = fileManager;
    }

    /**
     * Schedules event notifications
     * 
     * @param schedule
     *                     ArrayList<SithClanDaySchedule> clan event schedule
     */
    public void scheduleNotifications(ArrayList<SithClanDaySchedule> schedule)
    {
        // dont continue if setting off in configs
        if (!config.eventNotifications())
        {
            return;
        }
        // fresh start
        cancelAllNotifications();

        // iterate thru schedule
        for (SithClanDaySchedule day : schedule)
        {
            String currentDay = day.getDate();
            for (SithClanEvent event : day.getEvents())
            {
                String eventTitle = SithClanPluginUtil.removeEmojis(event.getEventTitle());
                // checks if event subscribed to in config file
                if (!fileManager.isSubscribed(eventTitle))
                {
                    continue;
                }
                String currentTime = event.getEventTime();

                try
                {
                    // convert to users local time
                    ZonedDateTime estTime = ZonedDateTime.of(
                            LocalDate.parse(currentDay, SithClanPluginConstants.DATE_FORMATTER),
                            LocalTime.parse(currentTime, SithClanPluginConstants.TIME_FORMATTER),
                            SithClanPluginConstants.EST_ZONE);
                    ZonedDateTime localDateTime = estTime.withZoneSameInstant(ZoneId.systemDefault());

                    // checks if event has already passed
                    long delay = ChronoUnit.MINUTES.between(ZonedDateTime.now(), localDateTime)
                            - config.notificationTimeBuffer();

                    // schedule event
                    if (delay >= 0)
                    {
                        ScheduledFuture<?> future = scheduler.schedule(() ->
                        {
                            notifier.notify(EVENT_NOTIFICATION + eventTitle);
                        }, delay, TimeUnit.MINUTES);
                        scheduledNotifications.add(future);
                    }
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Cancels all active notifications and clears scheduled notifications
     */
    private void cancelAllNotifications()
    {
        for (ScheduledFuture<?> future : scheduledNotifications)
        {
            future.cancel(false);
        }
        scheduledNotifications.clear();
    }

    /**
     * Shuts the scheduler down
     */
    public void shutDown()
    {
        cancelAllNotifications();
        scheduler.shutdown();
    }
}

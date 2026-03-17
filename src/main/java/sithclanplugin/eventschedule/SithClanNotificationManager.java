package sithclanplugin.eventschedule;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

@Singleton
public class SithClanNotificationManager {

    private final Notifier notifier;
    private final SithClanPluginConfig config;
    private final ScheduledExecutorService scheduler;

    private final List<ScheduledFuture<?>> scheduledNotifications = new ArrayList<>();

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("M/d/yyyy h:mm a");

    @Inject
    public SithClanNotificationManager(SithClanPluginConfig config, Notifier notifier) {
        this.config = config;
        this.notifier = notifier;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    // for testing
    // TODO: remove after testing
    public SithClanNotificationManager(SithClanPluginConfig config, Notifier notifier,
            ScheduledExecutorService scheduler) {
        this.config = config;
        this.notifier = notifier;
        this.scheduler = scheduler;
    }

    /**
     * Schedules event notifications
     * 
     * @param schedule ArrayList<SithClanDaySchedule> clan event schedule
     */
    public void scheduleNotifications(ArrayList<SithClanDaySchedule> schedule) {
        if (!config.eventNotifications())
            return;
        cancelAllNotifications();

        // iterate thru schedule
        for (SithClanDaySchedule day : schedule) {
            String currentDay = day.getDate();
            for (SithClanEvent event : day.getEvents()) {
                String currentTime = event.getEventTime();
                String combinedDateTime = currentDay + " " + currentTime;

                try {
                    // has event past yet?
                    LocalDateTime eventDateTime = LocalDateTime.parse(combinedDateTime, FORMATTER);
                    long delay = ChronoUnit.MINUTES.between(LocalDateTime.now(), eventDateTime)
                            - config.notificationTimeBuffer();

                    // schedule event
                    if (delay >= 0) {
                        ScheduledFuture<?> future = scheduler.schedule(() -> {
                            notifier.notify("Clan event starting soon: " + event.getEventTitle());
                        }, delay, TimeUnit.MINUTES);
                        scheduledNotifications.add(future);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Cancels all active notifications and clears scheduled notifications
     */
    private void cancelAllNotifications() {
        for (ScheduledFuture<?> future : scheduledNotifications) {
            future.cancel(false);
        }
        scheduledNotifications.clear();
    }

    /**
     * Shuts the scheduler down
     */
    public void shutDown() {
        cancelAllNotifications();
        scheduler.shutdown();
    }
}

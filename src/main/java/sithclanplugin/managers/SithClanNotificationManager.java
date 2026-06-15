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

package sithclanplugin.managers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.Notifier;
import sithclanplugin.SithClanConfig;
import sithclanplugin.eventschedule.SithClanDaySchedule;
import sithclanplugin.eventschedule.SithClanEvent;
import sithclanplugin.util.SithClanConstants;
import sithclanplugin.util.SithClanUtil;

@Slf4j
@Singleton
public class SithClanNotificationManager
{
    @Inject
    private ScheduledExecutorService executor;

    @Inject
    private Notifier notifier;

    @Inject
    private SithClanFileManager fileManager;

    @Inject
    private SithClanConfig config;

    private final Map<String, ScheduledFuture<?>> scheduledNotifications = new HashMap<>();

    private static final String EVENT_NOTIFICATION = "Clan event starting soon: "; // trailing space intentional

    /**
     * Schedule event notifications
     * 
     * @param schedule
     *                     ArrayList<SithClanDaySchedule> clan event schedule
     */
    public void scheduleNotifications(ArrayList<SithClanDaySchedule> schedule)
    {
        // check if enabled in config
        if (!config.eventNotifications())
        {
            return;
        }

        log.info("Scheduling event notifications..");
        // cancel notifications if no longer in schedule
        Set<String> incomingEvents = new HashSet<>();
        for (SithClanDaySchedule day : schedule)
        {
            for (SithClanEvent event : day.getEvents())
            {
                incomingEvents.add(SithClanUtil.removeEmojis(event.getEventTitle()));
            }
        }

        scheduledNotifications.entrySet().removeIf(entry ->
        {
            if (!incomingEvents.contains(entry.getKey()))
            {
                entry.getValue().cancel(false);
                return true;
            }
            return false;
        });
        // iterate thru schedule
        for (SithClanDaySchedule day : schedule)
        {
            String currentDay = day.getDate();
            for (SithClanEvent event : day.getEvents())
            {
                String eventTitle = SithClanUtil.removeEmojis(event.getEventTitle());

                // skip if already scheduled
                if (scheduledNotifications.containsKey(eventTitle))
                {
                    continue;
                }

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
                            LocalDate.parse(currentDay, SithClanConstants.DATE_FORMATTER),
                            LocalTime.parse(currentTime, SithClanConstants.TIME_FORMATTER),
                            SithClanConstants.EST_ZONE);
                    ZonedDateTime localDateTime = estTime.withZoneSameInstant(ZoneId.systemDefault());

                    // checks if event has already passed
                    long delay = ChronoUnit.MINUTES.between(ZonedDateTime.now(), localDateTime)
                            - config.notificationTimeBuffer();

                    // schedule event
                    if (delay >= 0)
                    {
                        ScheduledFuture<?> future = executor.schedule(() ->
                        {
                            notifier.notify(EVENT_NOTIFICATION + eventTitle);
                            scheduledNotifications.remove(eventTitle);
                        }, delay, TimeUnit.MINUTES);
                        scheduledNotifications.put(eventTitle, future);
                    }
                } catch (Exception e)
                {
                    log.error("Exception while scheduling event notifications: {}", e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Cancel all active notifications and clears scheduled notifications
     */
    private void cancelAllNotifications()
    {
        for (ScheduledFuture<?> future : scheduledNotifications.values())
        {
            future.cancel(false);
        }
        scheduledNotifications.clear();
    }

    /**
     * Cancel all notifications
     */
    public void shutDown()
    {
        cancelAllNotifications();
    }
}

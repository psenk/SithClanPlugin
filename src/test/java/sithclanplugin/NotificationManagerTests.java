package sithclanplugin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import net.runelite.client.Notifier;
import sithclanplugin.eventschedule.SithClanDaySchedule;
import sithclanplugin.eventschedule.SithClanEvent;
import sithclanplugin.managers.SithClanPluginNotificationManager;
import sithclanplugin.managers.SithClanPluginFileManager;
import sithclanplugin.util.SithClanPluginConstants;

public class NotificationManagerTests {

    @Mock
    private Notifier notifier;

    @Mock
    private ScheduledExecutorService scheduler;

    @Mock
    private SithClanPluginConfig config;

    @Mock
    private SithClanPluginFileManager fileManager;

    private SithClanPluginNotificationManager notificationManager;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(config.eventNotifications()).thenReturn(true);
        when(config.notificationTimeBuffer()).thenReturn(15);

        when(scheduler.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class)))
                .thenAnswer(invocation -> {
                    Runnable task = invocation.getArgument(0);
                    long delay = invocation.getArgument(1);
                    if (delay >= 0)
                        task.run();
                    return mock(ScheduledFuture.class);
                });
        notificationManager = new SithClanPluginNotificationManager(config, notifier, scheduler, fileManager);
    }

    @Test
    public void testPastEventsNotScheduled() {
        // create expired schedule
        ArrayList<SithClanDaySchedule> schedule = createTestSchedule("1/1/2020", "12:00 PM", "Past Event");

        notificationManager.scheduleNotifications(schedule);

        // notifier should not fire
        verify(notifier, never()).notify(anyString());
    }

    @Test
    public void testNotificationsDisabled() {
        when(config.eventNotifications()).thenReturn(false);
        ArrayList<SithClanDaySchedule> schedule = new ArrayList<>();
        notificationManager.scheduleNotifications(schedule);
        verify(notifier, never()).notify(anyString());
    }

    @Test
    public void testFutureEventScheduled() {
        // create future event
        ZonedDateTime futureEvent = ZonedDateTime.now(SithClanPluginConstants.EST_ZONE).plusMinutes(30);
        String date = futureEvent.format(DateTimeFormatter.ofPattern("M/d/yyyy"));
        String time = futureEvent.format(DateTimeFormatter.ofPattern("h:mm a"));

        ArrayList<SithClanDaySchedule> schedule = createTestSchedule(date, time, "Test Event");
        when(config.notificationTimeBuffer()).thenReturn(0);

        notificationManager.scheduleNotifications(schedule);
        verify(notifier, times(1)).notify("Clan event starting soon: Test Event");
    }

    @Test
    public void testEventAtBufferThreshold() {
        ZonedDateTime thresholdEvent = ZonedDateTime.now(SithClanPluginConstants.EST_ZONE).plusMinutes(16);
        String date = thresholdEvent.format(DateTimeFormatter.ofPattern("M/d/yyyy"));
        String time = thresholdEvent.format(DateTimeFormatter.ofPattern("h:mm a"));

        when(config.notificationTimeBuffer()).thenReturn(15);

        ArrayList<SithClanDaySchedule> schedule = createTestSchedule(date, time, "Threshold Event");
        notificationManager.scheduleNotifications(schedule);

        verify(notifier, times(1)).notify("Clan event starting soon: Threshold Event");
    }

    @Test
    public void testMultipleEventsOnSameDay() {
        // create future event
        ZonedDateTime futureEvent = ZonedDateTime.now(SithClanPluginConstants.EST_ZONE).plusMinutes(30);
        ZonedDateTime futureEventTwo = ZonedDateTime.now(SithClanPluginConstants.EST_ZONE).plusMinutes(40);
        String date = futureEvent.format(DateTimeFormatter.ofPattern("M/d/yyyy"));
        String time = futureEvent.format(DateTimeFormatter.ofPattern("h:mm a"));

        ArrayList<SithClanDaySchedule> schedule = createTestSchedule(date, time, "Test Event");
        SithClanEvent eventTwo = new SithClanEvent();
        eventTwo.setEventTitle("Second Event");
        eventTwo.setEventTime(futureEventTwo.format(DateTimeFormatter.ofPattern("h:mm a")));
        schedule.get(0).getEvents().add(eventTwo);
        when(config.notificationTimeBuffer()).thenReturn(0);

        notificationManager.scheduleNotifications(schedule);
        verify(notifier, times(1)).notify("Clan event starting soon: Test Event");
        verify(notifier, times(1)).notify("Clan event starting soon: Second Event");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCancelNotifications() {
        @SuppressWarnings("rawtypes")
        ScheduledFuture future = mock(ScheduledFuture.class);

        when(scheduler.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class)))
                .thenReturn(future)
                .thenAnswer(invocation -> mock(ScheduledFuture.class));

        ZonedDateTime futureEvent = ZonedDateTime.now(SithClanPluginConstants.EST_ZONE).plusMinutes(30);
        String date = futureEvent.format(DateTimeFormatter.ofPattern("M/d/yyyy"));
        String time = futureEvent.format(DateTimeFormatter.ofPattern("h:mm a"));

        ArrayList<SithClanDaySchedule> schedule = createTestSchedule(date, time, "Test Event");
        when(config.notificationTimeBuffer()).thenReturn(0);

        // schedule notifications twice
        notificationManager.scheduleNotifications(schedule);
        notificationManager.scheduleNotifications(schedule);

        // notifier should only fire once, not twice
        verify(future, times(1)).cancel(false);
    }

    private ArrayList<SithClanDaySchedule> createTestSchedule(String date, String time, String title) {
        ArrayList<SithClanDaySchedule> schedule = new ArrayList<>();
        SithClanDaySchedule day = new SithClanDaySchedule();
        day.setDate(date);
        SithClanEvent event = new SithClanEvent();
        event.setEventTitle(title);
        event.setEventTime(time);
        ArrayList<SithClanEvent> events = new ArrayList<>();
        events.add(event);
        day.setEvents(events);
        schedule.add(day);
        return schedule;
    }

}

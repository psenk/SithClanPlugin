package sithclanplugin.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;
import sithclanplugin.SithClanPlugin;
import sithclanplugin.SithClanPluginConfig;
import sithclanplugin.eventschedule.SithClanDaySchedule;
import sithclanplugin.eventschedule.SithClanEvent;
import sithclanplugin.eventschedule.SithClanEventSchedule;
import sithclanplugin.managers.SithClanPluginFileManager;
import sithclanplugin.managers.SithClanPluginNotificationManager;
import sithclanplugin.util.SithClanPluginConstants;
import sithclanplugin.util.SithClanPluginUtil;

@Slf4j
@Singleton
public class SithClanSchedulePanel extends JPanel
{
    @Inject
    private ScheduledExecutorService executor;

    @Inject
    private SithClanPlugin plugin;

    @Inject
    private SithClanPluginConfig config;

    @Inject
    private SithClanPluginFileManager fileManager;

    @Inject
    private SithClanPluginNotificationManager notificationManager;

    @Inject
    private SithClanEventSchedule eventSchedule;

    private final JLabel scheduleExpiredLabel;
    private final JLabel nextEventLabel;
    private final Component scheduleExpiredSpace;
    private final JPanel scheduleContainer;
    private final Icon rightArrowIcon;
    private final Icon downArrowIcon;
    private ScheduledFuture<?> nextEventRefreshTask;
    private Runnable onRefreshCallback;

    private static final String EVENT_SCHEDULE = "Event Schedule";
    private static final String SCHEDULE_EXPIRED_WARNING = "Schedule is expired! Please refresh";
    private static final String REFRESH_SCHEDULE_BUTTON = "Refresh Schedule";
    private static final String ARROW_RIGHT_IMG_PATH = "/arrow_right.png";
    private static final String ARROW_DOWN_IMG_PATH = "/arrow_down.png";
    private static final String RATE_LIMITED_WARNING = "The schedule has been retrieved too recently.  Try again in a few minutes.";
    private static final String SCHEDULE_UNOBTAINABLE_WARNING = "Unable to obtain schedule.";
    private static final String CHECKBOX_TOOLTIP = "Check box to receive notification before event start.";
    private static final String REPEATED_WEEKLY = "Repeated Weekly";
    private static final String NO_NEXT_EVENT = "Next Event: None";
    private static final String NEXT_EVENT = "Next Event";

    private static final String NO_UPCOMING_EVENTS = "No upcoming events";

    SithClanSchedulePanel()
    {
        // load collapse/expand arrow imgs
        rightArrowIcon = new ImageIcon(ImageUtil.loadImageResource(getClass(), ARROW_RIGHT_IMG_PATH));
        downArrowIcon = new ImageIcon(ImageUtil.loadImageResource(getClass(), ARROW_DOWN_IMG_PATH));
        // main panel layout
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // top panel title
        JLabel schedulePanelLabel = new JLabel(EVENT_SCHEDULE);
        schedulePanelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // warning if schedule is expired
        scheduleExpiredLabel = new JLabel(SCHEDULE_EXPIRED_WARNING);
        scheduleExpiredLabel.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
        scheduleExpiredLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        scheduleExpiredLabel.setVisible(false);
        scheduleExpiredSpace = Box.createRigidArea(new Dimension(0, 5));
        scheduleExpiredSpace.setVisible(false);
        this.add(scheduleExpiredSpace);

        // organization, contains panel title and expiration warning
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(schedulePanelLabel);
        topPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        topPanel.add(scheduleExpiredLabel);

        this.add(topPanel);

        // next event panel
        nextEventLabel = new JLabel(NO_NEXT_EVENT);
        nextEventLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        nextEventLabel.setForeground(ColorScheme.BRAND_ORANGE);
        nextEventLabel.setVisible(false);

        JPanel nextEventPanel = new JPanel();
        nextEventPanel.setLayout(new BoxLayout(nextEventPanel, BoxLayout.Y_AXIS));
        nextEventPanel.setBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(ColorScheme.BORDER_COLOR), NEXT_EVENT));
        nextEventPanel.add(nextEventLabel);
        nextEventPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        this.add(nextEventPanel);
        this.add(Box.createRigidArea(new Dimension(0, 5)));

        // contains event schedule
        scheduleContainer = new JPanel();
        scheduleContainer.setLayout(new BoxLayout(scheduleContainer, BoxLayout.Y_AXIS));
        scheduleContainer.setVisible(false);
        scheduleContainer.setOpaque(true);
        scheduleContainer.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.add(scheduleContainer);

        // button to refresh schedule
        JButton scheduleRefreshScheduleButton = new JButton(REFRESH_SCHEDULE_BUTTON);
        scheduleRefreshScheduleButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // refresh button
        this.add(Box.createRigidArea(new Dimension(0, 10)));
        this.add(scheduleRefreshScheduleButton);

        // refresh event schedule button action
        scheduleRefreshScheduleButton.addActionListener(e ->
        {
            executor.submit(() ->
            {
                // get and store schedule
                int status = eventSchedule.parseScheduleFromGet();
                SwingUtilities.invokeLater(() ->
                {
                    handleScheduleStatus(status);
                    // display schedule on panel
                    displaySchedule();
                    // callback to reveal senate options button if API key added later
                    if (onRefreshCallback != null)
                    {
                        onRefreshCallback.run();
                    }
                });
            });
        });

        this.setVisible(true);
    }

    /**
     * DISPLAY FUNCTIONS
     */

    /**
     * Display event schedule to panel
     */
    public void displaySchedule()
    {
        String currentDay = "";
        // fresh start
        scheduleContainer.removeAll();

        // if there is no schedule
        if (eventSchedule.getSchedule() == null || eventSchedule.getSchedule().isEmpty())
        {
            scheduleContainer.setVisible(false);
            scheduleContainer.revalidate();
            scheduleContainer.repaint();
            return;
        }

        scheduleContainer.setVisible(true);

        // iterate through all days in schedule
        for (SithClanDaySchedule day : eventSchedule.getSchedule())
        {
            currentDay = day.getDate();
            // create panel for each days events
            JPanel dailyEvents = createDailyEventsPanel();
            // create interactable date label to collapse/expand days events
            JLabel dateLabel = createDateLabel(currentDay, dailyEvents);

            scheduleContainer.add(dateLabel);
            scheduleContainer.add(dailyEvents);

            // sort events by time
            ArrayList<SithClanEvent> events = new ArrayList<>(day.getEvents());
            events.sort((e1, e2) -> LocalTime.parse(e1.getEventTime(), SithClanPluginConstants.TIME_FORMATTER)
                    .compareTo(LocalTime.parse(e2.getEventTime(), SithClanPluginConstants.TIME_FORMATTER)));

            // iterate through all events in day
            for (SithClanEvent event : events)
            {
                // create each event
                JPanel singleEvent = createEvent(event, day.getDate());

                dailyEvents.add(singleEvent);
                dailyEvents.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        }
        // check if schedule is expired
        checkScheduleExpired(currentDay);

        // display next event
        updateNextEventDisplay();

        scheduleContainer.revalidate();
        scheduleContainer.repaint();
    }

    /**
     * Find and display next event
     */
    public void updateNextEventDisplay()
    {
        if (eventSchedule.getSchedule() == null || eventSchedule.getSchedule().isEmpty())
        {
            nextEventLabel.setVisible(false);
            return;
        }

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime nextEventTime = null;
        String nextEventName = null;

        // loop through schedule
        for (SithClanDaySchedule day : eventSchedule.getSchedule())
        {
            for (SithClanEvent event : day.getEvents())
            {
                try
                {
                    ZonedDateTime estTime = ZonedDateTime.of(
                            LocalDate.parse(day.getDate(), SithClanPluginConstants.DATE_FORMATTER),
                            LocalTime.parse(event.getEventTime(), SithClanPluginConstants.TIME_FORMATTER),
                            SithClanPluginConstants.EST_ZONE);
                    ZonedDateTime localTime = estTime.withZoneSameInstant(ZoneId.systemDefault());

                    // check event hasn't happened yet
                    if (localTime.isAfter(now))
                    {
                        if (nextEventTime == null || localTime.isBefore(nextEventTime))
                        {
                            nextEventTime = localTime;
                            nextEventName = SithClanPluginUtil.removeEmojis(event.getEventTitle());
                        }
                    }
                } catch (Exception e)
                {
                    log.error("Exception while updating next event display: {}", e.getMessage(), e);
                }
            }
        }

        // no future events found
        if (nextEventTime == null)
        {
            nextEventLabel.setText(NO_UPCOMING_EVENTS);
            nextEventLabel.setVisible(true);
            return;
        }

        // calculate how long until event
        long minutesUntil = ChronoUnit.MINUTES.between(now, nextEventTime);
        long hours = minutesUntil / 60;
        long minutes = minutesUntil % 60;

        // display time
        String countdown;
        if (hours > 0)
        {
            countdown = "in " + hours + "h " + minutes + "m";
        } else
        {
            countdown = "in " + minutes + "m";
        }

        String timeString = nextEventTime.format(SithClanPluginConstants.TIME_FORMATTER);

        nextEventLabel.setText("<html><b>" + nextEventName + "</b><br />" + timeString + " (" + countdown + ")</html>");
        nextEventLabel.setVisible(true);
    }

    /**
     * CREATE FUNCTIONS
     */

    /**
     * Create panel to hold each days events
     * 
     * @return JPanel panel that holds events
     */
    private JPanel createDailyEventsPanel()
    {
        JPanel dailyEvents = new JPanel();
        dailyEvents.setLayout(new BoxLayout(dailyEvents, BoxLayout.Y_AXIS));
        dailyEvents.setAlignmentX(Component.LEFT_ALIGNMENT);
        dailyEvents.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, ColorScheme.BORDER_COLOR));
        dailyEvents.setVisible(false);
        return dailyEvents;
    }

    /**
     * Create interactive date label for each day on schedule
     * 
     * @param date
     *                        String date of events for label
     * @param dailyEvents
     *                        JPanel panel that holds events
     * @return JLabel created date label
     */
    private JLabel createDateLabel(String date, JPanel dailyEvents)
    {
        JLabel dateLabel = new JLabel(date);

        dateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        dateLabel.setOpaque(true);
        // collapsed by default
        dateLabel.setIcon(rightArrowIcon);
        dateLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        dateLabel
                .setMaximumSize(new Dimension(Integer.MAX_VALUE, dateLabel.getPreferredSize().height));

        // expand/collapse action
        dateLabel.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                boolean isVisible = !dailyEvents.isVisible();
                dailyEvents.setVisible(isVisible);
                // change arrow icon
                if (isVisible)
                {
                    dateLabel.setIcon(downArrowIcon);
                } else
                {
                    dateLabel.setIcon(rightArrowIcon);
                }
                revalidate();
                repaint();
            }
        });
        return dateLabel;
    }

    /**
     * Create panel for single event
     * 
     * @param event
     *                  SithClanEvent object with all event data
     * @param day
     *                  String day of event
     * @return JPanel containing single event
     */
    private JPanel createEvent(SithClanEvent event, String day)
    {
        String eventTitleString = SithClanPluginUtil.removeEmojis(event.getEventTitle());

        // container for event and notification checkbox
        JPanel eventContainer = new JPanel();
        eventContainer.setLayout(new BorderLayout());

        // container for one event
        JPanel singleEvent = new JPanel();
        singleEvent.setLayout(new BoxLayout(singleEvent, BoxLayout.Y_AXIS));
        singleEvent.setAlignmentX(Component.LEFT_ALIGNMENT);
        singleEvent.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        // checkbox to subscribe to event notifications
        JCheckBox notificationCheckbox = new JCheckBox();
        notificationCheckbox.setAlignmentX(Component.RIGHT_ALIGNMENT);
        notificationCheckbox.setToolTipText(CHECKBOX_TOOLTIP);
        notificationCheckbox.setEnabled(config.eventNotifications());
        notificationCheckbox.setSelected(fileManager.isSubscribed(eventTitleString));

        // action listener for checkbox
        notificationCheckbox.addActionListener(e ->
        {
            if (notificationCheckbox.isSelected())
            {
                fileManager.addSubscription(eventTitleString);
            } else
            {
                fileManager.removeSubscription(eventTitleString);
            }
            // reschedule notifications after change
            notificationManager.scheduleNotifications(eventSchedule.getSchedule());
        });

        eventContainer.add(singleEvent, BorderLayout.WEST);
        eventContainer.add(notificationCheckbox, BorderLayout.EAST);

        // event title
        JLabel eventTitle = new JLabel(eventTitleString);
        eventTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        singleEvent.add(eventTitle);

        // event time, converted to user local time
        ZonedDateTime estTime = ZonedDateTime.of(
                LocalDate.parse(day, SithClanPluginConstants.DATE_FORMATTER),
                LocalTime.parse(event.getEventTime(), SithClanPluginConstants.TIME_FORMATTER),
                SithClanPluginConstants.EST_ZONE);
        ZonedDateTime localTime = estTime.withZoneSameInstant(ZoneId.systemDefault());
        JLabel eventTime = new JLabel(localTime.format(SithClanPluginConstants.TIME_FORMATTER));
        eventTime.setAlignmentX(Component.LEFT_ALIGNMENT);
        singleEvent.add(eventTime);

        // event host (optional info)
        if (event.getEventHost() != null && !event.getEventHost().isBlank())
        {
            JLabel eventHost = new JLabel("Hosted by: " + SithClanPluginUtil.removeEmojis(event.getEventHost()));
            eventHost.setAlignmentX(Component.LEFT_ALIGNMENT);
            singleEvent.add(eventHost);
        }

        // event misc info (optional info)
        if (!event.getEventMiscInfo().isEmpty())
        {
            for (String info : event.getEventMiscInfo())
            {
                // creating link to travel to clan discord channels
                JLabel eventInfo = createDiscordLink(SithClanPluginUtil.removeEmojis(info));
                eventInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
                singleEvent.add(eventInfo);
            }
        }

        // event location
        // creating world hop link for location
        JLabel eventLocation = createWorldLink(event.getEventLocation());
        eventLocation.setAlignmentX(Component.LEFT_ALIGNMENT);
        singleEvent.add(eventLocation);

        // event repeated (optional info)
        if (event.isEventRepeated())
        {
            JLabel eventRepeated = new JLabel(REPEATED_WEEKLY);
            eventRepeated.setAlignmentX(Component.LEFT_ALIGNMENT);
            singleEvent.add(eventRepeated);
        }

        // size constraints after children added
        eventContainer.setMinimumSize(new Dimension(PluginPanel.PANEL_WIDTH - 10, 100));
        eventContainer.setMaximumSize(new Dimension(Short.MAX_VALUE, eventContainer.getPreferredSize().height));

        return eventContainer;
    }

    /**
     * Convert Discord channel IDs into links
     * 
     * @param text
     *                 String text to search for Discord channel IDs
     * @return JLabel output unchanged text or Discord channel link
     */
    private JLabel createDiscordLink(String text)
    {
        // check text for Discord link format
        Matcher matcher = Pattern.compile("<#(\\d+)>").matcher(text);

        if (matcher.find())
        {
            String channelId = matcher.group(1);
            // create Discord channel URL
            String channelUrl = SithClanPluginConstants.DISCORD_CHANNEL_URI + channelId;
            // creating link
            JLabel channelLink = new JLabel(
                    "<html>" + text.replaceAll("<#\\d+>", "<a href=''>Discord Channel</a>") + "</html>");
            channelLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            channelLink.addMouseListener(new MouseAdapter()
            {
                public void mouseClicked(MouseEvent e)
                {
                    try
                    {
                        LinkBrowser.browse(channelUrl);
                    } catch (Exception ex)
                    {
                        ex.printStackTrace();
                    }
                }
            });
            return channelLink;
        }
        return new JLabel(text);
    }

    /**
     * Turn world location into quick world hop link
     * 
     * @param location
     *                     event location
     * @return JLabel world quick hop link
     */
    private JLabel createWorldLink(String location)
    {
        // search for runescape world
        Matcher matcher = Pattern.compile("W(\\d{3}$)").matcher(location);
        if (!matcher.find())
        {
            return new JLabel(location);
        }
        String worldId = matcher.group(1);
        // create clickable link
        JLabel worldLink = new JLabel(
                "<html>" + location.replaceAll("W\\d{3}$", "<a href=''>W" + worldId + "</a>") + "</html>");
        worldLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        worldLink.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                try
                {
                    plugin.hopTo(Integer.parseInt(worldId));
                } catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            }
        });
        return worldLink;
    }

    /**
     * MISC FUNCTIONS
     */

    /**
     * Check if event schedule is expired
     * 
     * @param inputDay
     *                     String last day of the current event schedule
     */
    private void checkScheduleExpired(String inputDay)
    {
        if (inputDay.isBlank())
        {
            return;
        }
        LocalDate finalDate = LocalDate.parse(inputDay, SithClanPluginConstants.DATE_FORMATTER);
        if (finalDate.isBefore(LocalDate.now()))
        {
            scheduleExpiredLabel.setVisible(true);
            scheduleExpiredSpace.setVisible(true);
        } else
        {
            scheduleExpiredLabel.setVisible(false);
            scheduleExpiredSpace.setVisible(false);
        }
    }

    /**
     * Handle the returned status of the schedule
     * 
     * @param status
     *                   int status code
     */
    private void handleScheduleStatus(int status)
    {
        switch (status)
        {
            case SithClanPluginConstants.STATUS_RATE_LIMITED:
                JOptionPane.showMessageDialog(null,
                        RATE_LIMITED_WARNING);
                break;
            case SithClanPluginConstants.STATUS_NOT_FOUND:
                JOptionPane.showMessageDialog(null,
                        SCHEDULE_UNOBTAINABLE_WARNING);
                break;
            default:
                break;
        }
    }

    /**
     * Callback function for refresh schedule button
     * 
     * @param callback
     *                     Runnable callback function
     */
    public void setOnRefreshCallback(Runnable callback)
    {
        this.onRefreshCallback = callback;
    }

    /**
     * Starts task to refresh next event display
     */
    public void startNextEventRefresh()
    {
        nextEventRefreshTask = executor.scheduleAtFixedRate(
                () -> SwingUtilities.invokeLater(this::updateNextEventDisplay),
                1, 1, TimeUnit.MINUTES);
    }

    /**
     * Ends task to refresh next event display
     */
    public void stopNextEventRefresh()
    {
        if (nextEventRefreshTask != null)
        {
            nextEventRefreshTask.cancel(false);
        }
    }
}

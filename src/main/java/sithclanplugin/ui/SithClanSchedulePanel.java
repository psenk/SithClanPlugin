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
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;
import sithclanplugin.SithClanPlugin;
import sithclanplugin.SithClanPluginConfig;
import sithclanplugin.eventschedule.SithClanDaySchedule;
import sithclanplugin.eventschedule.SithClanEvent;
import sithclanplugin.eventschedule.SithClanEventSchedule;
import sithclanplugin.managers.SithClanNotificationManager;
import sithclanplugin.managers.SithClanPluginFileManager;
import sithclanplugin.util.SithClanPluginConstants;
import sithclanplugin.util.SithClanPluginUtil;

@Singleton
public class SithClanSchedulePanel extends JPanel {

    @Inject
    private SithClanPlugin plugin;

    @Inject
    private SithClanPluginConfig config;

    @Inject
    private SithClanPluginFileManager fileManager;

    @Inject
    private SithClanNotificationManager notificationManager;

    @Inject
    private SithClanEventSchedule eventSchedule;

    private final JLabel scheduleExpiredLabel;
    private final JPanel scheduleContainer;
    private Runnable onRefreshCallback;

    private final Icon rightArrowIcon;
    private final Icon downArrowIcon;

    private static final String EVENT_SCHEDULE = "Event Schedule";
    private static final String SCHEDULE_EXPIRED_WARNING = "This schedule is expired! Please refresh";
    private static final String REFRESH_SCHEDULE_BUTTON = "Refresh Schedule";
    private static final String ARROW_RIGHT_IMG_PATH = "/arrow_right.png";
    private static final String ARROW_DOWN_IMG_PATH = "/arrow_down.png";
    private static final String RATE_LIMITED_WARNING = "The schedule has been retrieved too recently.  Try again in a few minutes.";
    private static final String SCHEDULE_UNOBTAINABLE_WARNING = "Unable to obtain schedule.";
    private static final String CHECKBOX_TOOLTIP = "Check box to receive notification before event start.";
    private static final String REPEATED_WEEKLY = "Repeated Weekly";

    SithClanSchedulePanel() {
        // load collapse/expand arrow imgs
        rightArrowIcon = new ImageIcon(ImageUtil.loadImageResource(getClass(), ARROW_RIGHT_IMG_PATH));
        downArrowIcon = new ImageIcon(ImageUtil.loadImageResource(getClass(), ARROW_DOWN_IMG_PATH));
        // main panel layout
        this.setLayout(new BorderLayout());

        // top panel title
        JLabel schedulePanelLabel = new JLabel(EVENT_SCHEDULE);
        schedulePanelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // warning if schedule is expired
        scheduleExpiredLabel = new JLabel(SCHEDULE_EXPIRED_WARNING);
        scheduleExpiredLabel.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
        scheduleExpiredLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        scheduleExpiredLabel.setVisible(false);

        // organization, contains panel title and expiration warning
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(schedulePanelLabel);
        topPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        topPanel.add(scheduleExpiredLabel);

        this.add(topPanel, BorderLayout.NORTH);

        // contains event schedule
        scheduleContainer = new JPanel();
        scheduleContainer.setLayout(new BoxLayout(scheduleContainer, BoxLayout.Y_AXIS));
        scheduleContainer.setVisible(true);
        scheduleContainer.setOpaque(true);
        scheduleContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        // allows schedule to scroll
        JScrollPane scheduleContainerScrollPane = new JScrollPane(scheduleContainer);
        scheduleContainerScrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);

        this.add(scheduleContainerScrollPane, BorderLayout.CENTER);

        // button to refresh schedule
        JButton scheduleRefreshScheduleButton = new JButton(REFRESH_SCHEDULE_BUTTON);
        scheduleRefreshScheduleButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // organization, contains refresh button
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        bottomPanel.add(scheduleRefreshScheduleButton);

        this.add(bottomPanel, BorderLayout.SOUTH);
        this.setVisible(true);

        // refresh event schedule button action
        scheduleRefreshScheduleButton.addActionListener(e -> {
            new Thread(() -> {
                // get and store schedule
                int status = eventSchedule.parseScheduleFromGet();
                SwingUtilities.invokeLater(() -> {
                    handleScheduleStatus(status);
                    // display schedule on panel
                    displaySchedule();
                    // callback to reveal senate options button if API key added later
                    if (onRefreshCallback != null) {
                        onRefreshCallback.run();
                    }
                });
            }).start();
        });
    }

    /**
     * Displays event schedule to panel
     */
    public void displaySchedule() {
        String currentDay = "";

        // get and store schedule if we don't have it and try again
        if (eventSchedule.getSchedule() == null || eventSchedule.getSchedule().isEmpty()) {
            new Thread(() -> {
                int status = eventSchedule.parseScheduleFromGet();
                SwingUtilities.invokeLater(() -> {
                    handleScheduleStatus(status);
                    // display schedule on panel
                    displaySchedule();
                });
            }).start();
            return;
        }
        // fresh start
        scheduleContainer.removeAll();

        // iterate through all days in schedule
        for (SithClanDaySchedule day : eventSchedule.getSchedule()) {
            currentDay = day.getDate();
            // create panel for each days events
            JPanel dailyEvents = createDailyEventsPanel();
            // create interactable date label to collapse/expand days events
            JLabel dateLabel = createDateLabel(currentDay, dailyEvents);

            scheduleContainer.add(dateLabel);
            scheduleContainer.add(dailyEvents);

            // iterate through all events in day
            for (SithClanEvent event : day.getEvents()) {
                // create each event
                JPanel singleEvent = createEvent(event, day.getDate());

                dailyEvents.add(singleEvent);
                dailyEvents.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        }
        // check if schedule is expired
        checkScheduleExpired(currentDay);

        scheduleContainer.revalidate();
        scheduleContainer.repaint();
    }

    /**
     * Creates panel to hold each days events
     * 
     * @return JPanel panel that holds events
     */
    private JPanel createDailyEventsPanel() {
        JPanel dailyEvents = new JPanel();
        dailyEvents.setLayout(new BoxLayout(dailyEvents, BoxLayout.Y_AXIS));
        dailyEvents.setAlignmentX(Component.LEFT_ALIGNMENT);
        dailyEvents.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        dailyEvents.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, ColorScheme.BORDER_COLOR));
        dailyEvents.setVisible(false);
        return dailyEvents;
    }

    /**
     * Creates interactive date label for each day on schedule
     * 
     * @param date        String date of events for label
     * @param dailyEvents JPanel panel that holds events
     * @return JLabel created date label
     */
    private JLabel createDateLabel(String date, JPanel dailyEvents) {
        JLabel dateLabel = new JLabel(date);

        dateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        dateLabel.setOpaque(true);
        // collapsed by default
        dateLabel.setIcon(rightArrowIcon);
        dateLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        dateLabel
                .setMaximumSize(new Dimension(Integer.MAX_VALUE, dateLabel.getPreferredSize().height));

        // expand/collapse action
        dateLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                boolean isVisible = !dailyEvents.isVisible();
                dailyEvents.setVisible(isVisible);
                // change arrow icon
                if (isVisible)
                    dateLabel.setIcon(downArrowIcon);
                else
                    dateLabel.setIcon(rightArrowIcon);
                revalidate();
                repaint();
            }
        });
        return dateLabel;
    }

    /**
     * Creates panel for single event
     * 
     * @param event SithClanEvent object with all event data
     * @param day   String day of event
     * @return JPanel containing single event
     */
    private JPanel createEvent(SithClanEvent event, String day) {
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
        notificationCheckbox.addActionListener(e -> {
            if (notificationCheckbox.isSelected()) {
                fileManager.addSubscription(eventTitleString);
            } else {
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
        if (event.getEventHost() != null && !event.getEventHost().isBlank()) {
            JLabel eventHost = new JLabel("Hosted by: " + event.getEventHost());
            eventHost.setAlignmentX(Component.LEFT_ALIGNMENT);
            singleEvent.add(eventHost);
        }

        // event misc info (optional info)
        if (!event.getEventMiscInfo().isEmpty()) {
            for (String info : event.getEventMiscInfo()) {
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
        if (event.isEventRepeated()) {
            JLabel eventRepeated = new JLabel(REPEATED_WEEKLY);
            eventRepeated.setAlignmentX(Component.LEFT_ALIGNMENT);
            singleEvent.add(eventRepeated);
        }
        return eventContainer;
    }

    /**
     * Converts Discord channel IDs into links
     * 
     * @param text String text to search for Discord channel IDs
     * @return JLabel output unchanged text or Discord channel link
     */
    private JLabel createDiscordLink(String text) {
        // check text for Discord link format
        Matcher matcher = Pattern.compile("<#(\\d+)>").matcher(text);

        if (matcher.find()) {
            String channelId = matcher.group(1);
            // create Discord channel URL
            String channelUrl = SithClanPluginConstants.DISCORD_CHANNEL_URI + channelId;
            // creating link
            JLabel channelLink = new JLabel(
                    "<html>" + text.replaceAll("<#\\d+>", "<a href=''>Discord Channel</a>") + "</html>");
            channelLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            channelLink.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    try {
                        LinkBrowser.browse(channelUrl);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            return channelLink;
        }
        return new JLabel(text);
    }

    /**
     * Turns world location into quick world hop link
     * 
     * @param location event location
     * @return JLabel world quick hop link
     */
    private JLabel createWorldLink(String location) {
        // search for runescape world
        Matcher matcher = Pattern.compile("W(\\d{3}$)").matcher(location);
        if (!matcher.find())
            return new JLabel(location);
        String worldId = matcher.group(1);
        // create clickable link
        JLabel worldLink = new JLabel(
                "<html>" + location.replaceAll("W\\d{3}$", "<a href=''>W" + worldId + "</a>") + "</html>");
        worldLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        worldLink.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                try {
                    plugin.hopTo(Integer.parseInt(worldId));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        return worldLink;
    }

    /**
     * Checks if event schedule is expired
     * 
     * @param inputDay String last day of the current event schedule
     */
    private void checkScheduleExpired(String inputDay) {
        if (inputDay.isBlank())
            return;
        LocalDate finalDate = LocalDate.parse(inputDay, SithClanPluginConstants.DATE_FORMATTER);
        if (finalDate.isBefore(LocalDate.now()))
            scheduleExpiredLabel.setVisible(true);
        else
            scheduleExpiredLabel.setVisible(false);
    }

    /**
     * Handles the returned status of the schedule
     * 
     * @param status int status code
     */
    private void handleScheduleStatus(int status) {
        switch (status) {
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
     * @param callback Runnable callback function
     */
    public void setOnRefreshCallback(Runnable callback) {
        this.onRefreshCallback = callback;
    }
}

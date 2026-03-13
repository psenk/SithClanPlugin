package sithclanplugin.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;
import sithclanplugin.eventschedule.SithClanDaySchedule;
import sithclanplugin.eventschedule.SithClanEvent;
import sithclanplugin.eventschedule.SithClanEventSchedule;

@Singleton
public class SithClanEventSchedulePanel extends JPanel {

    private final JLabel schedulePanelLabel;
    private final JLabel scheduleExpiredLabel;
    private final JButton scheduleGetEventScheduleButton;
    private final JPanel scheduleContainer;
    private final JScrollPane scheduleContainerScrollPane;
    private Runnable onRefreshCallback;

    private SithClanEventSchedule eventSchedule;

    private final Icon rightArrowIcon;
    private final Icon downArrowIcon;

    private static final String SITH_DISCORD_SERVER_ID = "741153043776667658";
    private static final String DISCORD_CHANNEL_URL = "https://discord.com/channels/" + SITH_DISCORD_SERVER_ID + "/";
    private static final String EVENT_SCHEDULE = "Event Schedule";
    private static final String SCHEDULE_EXPIRED = "Schedule Expired! Please Refresh.";
    private static final String GET_SCHEDULE_BUTTON = "Refresh Schedule";
    private static final String ARROW_RIGHT_PATH = "/arrow_right.png";
    private static final String ARROW_DOWN_PATH = "/arrow_down.png";
    private static final String REPEATED_WEEKLY = "Repeated Weekly";
    private static final String DATE_FORMAT = "M/d/yyyy";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    @Inject
    SithClanEventSchedulePanel(SithClanEventSchedule eventSchedule) {

        this.eventSchedule = eventSchedule;
        rightArrowIcon = new ImageIcon(ImageUtil.loadImageResource(getClass(), ARROW_RIGHT_PATH));
        downArrowIcon = new ImageIcon(ImageUtil.loadImageResource(getClass(), ARROW_DOWN_PATH));
        this.setLayout(new BorderLayout());

        // contains event schedule
        scheduleContainer = new JPanel();
        scheduleContainer.setLayout(new BoxLayout(scheduleContainer, BoxLayout.Y_AXIS));
        scheduleContainerScrollPane = new JScrollPane(scheduleContainer);
        scheduleContainer.setVisible(true);
        scheduleContainer.setOpaque(true);
        scheduleContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        schedulePanelLabel = new JLabel(EVENT_SCHEDULE);
        schedulePanelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        scheduleGetEventScheduleButton = new JButton(GET_SCHEDULE_BUTTON);
        scheduleGetEventScheduleButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        scheduleContainerScrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);

        scheduleExpiredLabel = new JLabel(SCHEDULE_EXPIRED);
        scheduleExpiredLabel.setForeground(Color.RED);
        scheduleExpiredLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        scheduleExpiredLabel.setVisible(false);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(schedulePanelLabel);
        topPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        topPanel.add(scheduleExpiredLabel);

        this.add(topPanel, BorderLayout.NORTH);
        this.add(scheduleContainerScrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        bottomPanel.add(scheduleGetEventScheduleButton);

        this.add(bottomPanel, BorderLayout.SOUTH);
        this.setVisible(true);

        // get event schedule action
        scheduleGetEventScheduleButton.addActionListener(e -> {
            new Thread(() -> {
                eventSchedule.parseScheduleFromGet();
                SwingUtilities.invokeLater(() -> {
                    displaySchedule();
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
        if (eventSchedule.getSchedule() == null || eventSchedule.getSchedule().isEmpty()) {
            new Thread(() -> {
                eventSchedule.parseScheduleFromGet();
                SwingUtilities.invokeLater(() -> displaySchedule());
            }).start();
            return;
        }
        // fresh start
        scheduleContainer.removeAll();

        for (SithClanDaySchedule day : eventSchedule.getSchedule()) {
            currentDay = day.getDate();
            JPanel dailyEvents = createDailyEventsPanel();
            JLabel dateLabel = createDateLabel(currentDay, dailyEvents);

            scheduleContainer.add(dateLabel);
            scheduleContainer.add(dailyEvents);

            for (SithClanEvent event : day.getEvents()) {
                JPanel singleEvent = createEvent(event);

                dailyEvents.add(singleEvent);
                dailyEvents.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        }
        checkScheduleExpired(currentDay);

        scheduleContainer.revalidate();
        scheduleContainer.repaint();
    }

    /**
     * Helper method, creates each days event panel
     * 
     * @return JPanel panel that holds each days events
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
     * Helper method, creates schedule date label
     * 
     * @param date        String date to be displayed on label
     * @param dailyEvents JPanel that displays the days events
     * @return JLabel created date label
     */
    private JLabel createDateLabel(String date, JPanel dailyEvents) {
        JLabel dateLabel = new JLabel(date);

        // date label properties
        dateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        dateLabel.setOpaque(true);
        dateLabel.setIcon(rightArrowIcon);
        dateLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        dateLabel
                .setMaximumSize(new Dimension(Integer.MAX_VALUE, dateLabel.getPreferredSize().height));

        dateLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                boolean isVisible = !dailyEvents.isVisible();
                dailyEvents.setVisible(isVisible);
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
     * Helper method, creates single event object
     * 
     * @param event SithClanEvent object with all event data
     * @return JPanel containing single event
     */
    private JPanel createEvent(SithClanEvent event) {
        JPanel singleEvent = new JPanel();
        singleEvent.setLayout(new BoxLayout(singleEvent, BoxLayout.Y_AXIS));
        singleEvent.setAlignmentX(Component.LEFT_ALIGNMENT);
        singleEvent.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        String title = event.getEventTitle();
        JLabel eventTitle = new JLabel(removeEmojis(title));
        eventTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        singleEvent.add(eventTitle);

        JLabel eventTime = new JLabel(event.getEventTime());
        eventTime.setAlignmentX(Component.LEFT_ALIGNMENT);
        singleEvent.add(eventTime);

        if (event.getEventHost() != null && !event.getEventHost().isBlank()) {
            JLabel eventHost = new JLabel("Hosted by: " + event.getEventHost());
            eventHost.setAlignmentX(Component.LEFT_ALIGNMENT);
            singleEvent.add(eventHost);
        }

        if (!event.getEventMiscInfo().isEmpty()) {
            for (String info : event.getEventMiscInfo()) {
                JLabel eventInfo = createDiscordLink(removeEmojis(info));
                eventInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
                singleEvent.add(eventInfo);
            }
        }

        JLabel eventLocation = new JLabel(event.getEventLocation());
        eventLocation.setAlignmentX(Component.LEFT_ALIGNMENT);
        singleEvent.add(eventLocation);

        if (event.isEventRepeated()) {
            JLabel eventRepeated = new JLabel(REPEATED_WEEKLY);
            eventRepeated.setAlignmentX(Component.LEFT_ALIGNMENT);
            singleEvent.add(eventRepeated);
        }
        return singleEvent;
    }

    /**
     * Helper method, changes Discord channel IDs into links
     * 
     * @param text String text in to search for Discord channel IDs
     * @return JLabel output
     */
    private JLabel createDiscordLink(String text) {
        // check text for Discord link format
        Matcher matcher = Pattern.compile("<#(\\d+)>").matcher(text);

        if (matcher.find()) {
            String channelId = matcher.group(1);
            String channelUrl = DISCORD_CHANNEL_URL + channelId;
            // replace <#id> with "View Channel"
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
     * Helper method, removes Discord emojis from text
     * 
     * @param text String text in to search for emojis using regex
     * @return String output without emojis
     */
    private String removeEmojis(String text) {
        return text.replaceAll(":[a-zA-Z0-9_]+:", "").trim();
    }

    /**
     * Checks if event schedule is expired
     * 
     * @param inputDay String last day of the event schedule
     */
    private void checkScheduleExpired(String inputDay) {
        LocalDate finalDate = LocalDate.parse(inputDay, FORMATTER);
        if (finalDate.isBefore(LocalDate.now()))
            scheduleExpiredLabel.setVisible(true);
        else
            scheduleExpiredLabel.setVisible(false);
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

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

package sithclanplugin.ui;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Locale;
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
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import com.google.common.html.HtmlEscapers;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;
import sithclanplugin.SithClanConfig;
import sithclanplugin.SithClanPlugin;
import sithclanplugin.eventschedule.SithClanDaySchedule;
import sithclanplugin.eventschedule.SithClanEvent;
import sithclanplugin.eventschedule.SithClanEventSchedule;
import sithclanplugin.managers.SithClanFileManager;
import sithclanplugin.managers.SithClanNotificationManager;
import sithclanplugin.util.SithClanConstants;
import sithclanplugin.util.SithClanUtil;

// refactored on july 4

@Slf4j
@Singleton
public class SithClanSchedulePanel extends JPanel
{

    @Inject
    private ScheduledExecutorService executor;

    @Inject
    private SithClanPlugin plugin;

    @Inject
    private SithClanConfig config;

    @Inject
    private SithClanEventSchedule eventSchedule;

    @Inject
    private SithClanFileManager fileManager;

    @Inject
    private SithClanNotificationManager notificationManager;

    private final Icon rightArrowIcon;
    private final Icon downArrowIcon;
    private final JPanel statusPanel;
    private final JLabel statusLabel;
    private final JLabel scheduleExpiredLabel;
    private final JPanel scheduleContainer;
    private final JLabel nextEventLabel;
    private final JPanel nextEventPanel;
    private ScheduledFuture<?> nextEventRefreshTask;

    private static final String EVENT_SCHEDULE = "Event Schedule";
    private static final String SCHEDULE_EXPIRED_WARNING = "<html><center>Schedule expired! Try refreshing.</center></html>";
    private static final String NEXT_EVENT = "Next Event";
    private static final String REFRESH_SCHEDULE_BUTTON = "Refresh Schedule";
    private static final String EXPAND_ALL_BUTTON = "Expand All";
    private static final String COLLAPSE_ALL_BUTTON = "Collapse All";
    private static final String RATE_LIMITED_WARNING = "<html><center>Please wait and try again in a few minutes.</center></html>";
    private static final String SCHEDULE_ERROR = "Unable to obtain schedule.";
    private static final String NO_EVENTS_SCHEDULED_TODAY = "No events scheduled today.";
    private static final String CHECKBOX_TOOLTIP = "Check box to receive notification before event start.";
    private static final String REPEATED_WEEKLY = "Repeated Weekly";
    private static final String NO_UPCOMING_EVENTS = "No upcoming events.";
    private static final String EVENT_PASSED = "Event has already started or has passed.";
    private static final String M_UNTIL_THIS_EVENT = "m until this event.";
    private static final int LABEL_WRAP_WIDTH = PluginPanel.PANEL_WIDTH - 100;

    SithClanSchedulePanel()
    {
        // for dropdown label ui
        rightArrowIcon = new ImageIcon(ImageUtil.loadImageResource(getClass(), SithClanConstants.ARROW_RIGHT_PATH));
        downArrowIcon = new ImageIcon(ImageUtil.loadImageResource(getClass(), SithClanConstants.ARROW_DOWN_PATH));

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JLabel mainPanelLabel = new JLabel(EVENT_SCHEDULE);
        mainPanelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // shows status and error messages
        statusLabel = SithClanUtil.createStatusLabel();
        statusPanel = SithClanUtil.createStatusPanel(statusLabel);
        this.add(statusPanel);

        // warning if schedule is expired
        scheduleExpiredLabel = new JLabel(SCHEDULE_EXPIRED_WARNING);
        scheduleExpiredLabel.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
        scheduleExpiredLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        scheduleExpiredLabel.setVisible(false);

        // container for title and expiration warning
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(mainPanelLabel);
        topPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        topPanel.add(scheduleExpiredLabel);
        topPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        this.add(topPanel);

        // next event area
        nextEventLabel = new JLabel();
        nextEventLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        nextEventLabel.setForeground(ColorScheme.BRAND_ORANGE);
        nextEventLabel.setVisible(false);

        nextEventPanel = new JPanel();
        nextEventPanel.setLayout(new BoxLayout(nextEventPanel, BoxLayout.Y_AXIS));
        nextEventPanel.setBorder(BorderFactory
                .createTitledBorder(
                        BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ColorScheme.BORDER_COLOR),
                                BorderFactory.createEmptyBorder(4, 8, 4, 8)),
                        NEXT_EVENT));
        nextEventPanel.add(nextEventLabel);
        nextEventPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        this.add(nextEventPanel);
        this.add(Box.createRigidArea(new Dimension(0, 10)));

        JButton refreshScheduleButton = new JButton(REFRESH_SCHEDULE_BUTTON);
        refreshScheduleButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.add(refreshScheduleButton);
        this.add(Box.createRigidArea(new Dimension(0, 10)));

        JButton expandAllButton = new JButton(EXPAND_ALL_BUTTON);
        JButton collapseAllButton = new JButton(COLLAPSE_ALL_BUTTON);

        JPanel expandCollapseButtonPanel = new JPanel();
        expandCollapseButtonPanel.setLayout(new BoxLayout(expandCollapseButtonPanel, BoxLayout.X_AXIS));
        expandCollapseButtonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        expandCollapseButtonPanel.add(expandAllButton);
        expandCollapseButtonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        expandCollapseButtonPanel.add(collapseAllButton);

        this.add(expandCollapseButtonPanel);
        this.add(Box.createRigidArea(new Dimension(0, 10)));

        // contains all of event schedule
        scheduleContainer = new JPanel();
        scheduleContainer.setLayout(new BoxLayout(scheduleContainer, BoxLayout.Y_AXIS));
        scheduleContainer.setVisible(false);
        scheduleContainer.setOpaque(true);
        scheduleContainer.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.add(scheduleContainer);

        // refresh schedule action
        refreshScheduleButton.addActionListener(e ->
        {
            executor.submit(() ->
            {
                // get and store schedule
                int status = eventSchedule.parseScheduleFromGet();
                SwingUtilities.invokeLater(() ->
                {
                    handleScheduleStatus(status);
                    displaySchedule();
                });
            });
        });

        // expand all action
        expandAllButton.addActionListener(e -> setAllDaysExpanded(true));

        // collapse all action
        collapseAllButton.addActionListener(e -> setAllDaysExpanded(false));

        this.setVisible(true);
    }

    /**
     * DISPLAY FUNCTIONS
     */

    /**
     * Display event schedule
     */
    public void displaySchedule()
    {
        String currentDay = "";
        // fresh start
        scheduleContainer.removeAll();
        ArrayList<SithClanDaySchedule> schedule = eventSchedule.getSchedule();

        // no schedule uploaded
        if (schedule == null || schedule.isEmpty())
        {
            // log.debug("No schedule available to display");
            scheduleContainer.setVisible(false);
            scheduleContainer.revalidate();
            scheduleContainer.repaint();
            return;
        }

        scheduleContainer.setVisible(true);

        // iterate through schedule
        for (SithClanDaySchedule day : schedule)
        {
            currentDay = day.getDate();

            // container for all events each day
            JPanel dailyEvents = createDailyEventsPanel();
            // interactable dropdown for daily events
            LocalDate parsedDate = LocalDate.parse(currentDay, SithClanConstants.DATE_FORMATTER);
            String dayOfWeekName = parsedDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.US);
            String labelText = dayOfWeekName + " - " + currentDay;
            JLabel dateLabel = createDateLabel(labelText, dailyEvents);

            scheduleContainer.add(dateLabel);
            scheduleContainer.add(dailyEvents);

            // sort events by time
            // add blank list if no events on day
            ArrayList<SithClanEvent> rawEvents = day.getEvents();
            ArrayList<SithClanEvent> events = (rawEvents != null) ? new ArrayList<>(rawEvents) : new ArrayList<>();
            events.sort((e1, e2) -> LocalTime.parse(e1.getEventTime(), SithClanConstants.TIME_FORMATTER)
                    .compareTo(LocalTime.parse(e2.getEventTime(), SithClanConstants.TIME_FORMATTER)));

            // no scheduled events this day
            if (events.isEmpty())
            {
                JLabel noEventsLabel = new JLabel(NO_EVENTS_SCHEDULED_TODAY);
                noEventsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                noEventsLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
                noEventsLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                noEventsLabel.setMaximumSize(new Dimension(Short.MAX_VALUE, noEventsLabel.getPreferredSize().height));
                dailyEvents.add(noEventsLabel);
                dailyEvents.setMaximumSize(new Dimension(Short.MAX_VALUE, dailyEvents.getPreferredSize().height));
            } else
            {
                // iterate through daily events
                for (SithClanEvent event : events)
                {
                    // create each event
                    JPanel singleEvent = createEvent(event, day.getDate());
                    dailyEvents.add(singleEvent);
                    dailyEvents.add(Box.createRigidArea(new Dimension(0, 10)));
                    dailyEvents.setMaximumSize(new Dimension(Short.MAX_VALUE, dailyEvents.getPreferredSize().height));
                }
            }
        }

        checkScheduleExpired(currentDay);
        updateNextEventDisplay();

        scheduleContainer.setMaximumSize(new Dimension(Short.MAX_VALUE, scheduleContainer.getPreferredSize().height));
        scheduleContainer.revalidate();
        scheduleContainer.repaint();
    }

    /**
     * Find and display next event and time remaining
     */
    public void updateNextEventDisplay()
    {
        ArrayList<SithClanDaySchedule> schedule = eventSchedule.getSchedule();

        // no schedule
        if (schedule == null || schedule.isEmpty())
        {
            nextEventPanel.setVisible(false);
            return;
        }

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime nextEventTime = null;
        String nextEventName = null;

        // iterate through schedule
        for (SithClanDaySchedule day : schedule)
        {
            for (SithClanEvent event : day.getEvents())
            {
                try
                {
                    ZonedDateTime estTime = ZonedDateTime.of(
                            LocalDate.parse(day.getDate(), SithClanConstants.DATE_FORMATTER),
                            LocalTime.parse(event.getEventTime(), SithClanConstants.TIME_FORMATTER),
                            SithClanConstants.EST_ZONE);
                    ZonedDateTime localTime = estTime.withZoneSameInstant(ZoneId.systemDefault());

                    // check event hasn't happened
                    if (localTime.isAfter(now))
                    {
                        if (nextEventTime == null || localTime.isBefore(nextEventTime))
                        {
                            nextEventTime = localTime;
                            nextEventName = SithClanUtil.removeEmojis(event.getEventTitle());
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
            nextEventPanel.setVisible(true);
            return;
        }

        // how long until event
        long minutesUntil = ChronoUnit.MINUTES.between(now, nextEventTime);
        long hours = minutesUntil / 60;
        long minutes = minutesUntil % 60;

        String countdown;
        if (hours > 0)
        {
            countdown = "in " + hours + "h " + minutes + "m";
        } else
        {
            countdown = "in " + minutes + "m";
        }

        String timeString = nextEventTime.format(SithClanConstants.TIME_FORMATTER);

        nextEventLabel.setText("<html><b>" + HtmlEscapers.htmlEscaper().escape(nextEventName) + "</b><br />"
                + timeString + " (" + countdown + ")</html>");
        nextEventLabel.setVisible(true);
        nextEventPanel.setVisible(true);
    }

    /**
     * Expand or collapse all days in schedule
     * 
     * @param expand
     *                   boolean expand or collapse days
     */
    private void setAllDaysExpanded(boolean expand)
    {
        Component[] components = scheduleContainer.getComponents();

        // alternates date label, events...
        for (int i = 0; i + 1 < components.length; i += 2)
        {
            if (!(components[i] instanceof JLabel) || !(components[i + 1] instanceof JPanel))
            {
                continue;
            }
            JLabel dateLabel = (JLabel) components[i];
            JPanel dailyEvents = (JPanel) components[i + 1];

            dailyEvents.setVisible(expand);
            dateLabel.setIcon(expand ? downArrowIcon : rightArrowIcon);
        }

        scheduleContainer.setMaximumSize(new Dimension(Short.MAX_VALUE, scheduleContainer.getPreferredSize().height));
        repaint();
        revalidate();
    }

    /**
     * Enable or disable event notification checkboxes
     * 
     * @param enabled
     *                    boolean enable checkboxes
     */
    public void setCheckboxesEnabled(boolean enabled)
    {
        for (Component dayComponent : scheduleContainer.getComponents())
        {
            if (!(dayComponent instanceof JPanel))
            {
                continue;
            }
            JPanel dailyEvents = (JPanel) dayComponent;

            for (Component eventComponent : dailyEvents.getComponents())
            {
                if (!(eventComponent instanceof JPanel))
                {
                    continue;
                }
                JPanel eventContainer = (JPanel) eventComponent;

                for (Component inner : eventContainer.getComponents())
                {
                    if (inner instanceof JCheckBox)
                    {
                        ((JCheckBox) inner).setEnabled(enabled);
                    }
                }
            }
        }
    }

    /**
     * CREATE FUNCTIONS
     */

    /**
     * Create panel to hold each all events on day
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
     * Create interactive date label for each day
     * 
     * @param date
     *                        String date of events for label
     * @param dailyEvents
     *                        JPanel daily events container
     * @return JLabel created interactive date label
     */
    private JLabel createDateLabel(String date, JPanel dailyEvents)
    {
        JLabel dateLabel = SithClanUtil.createCollapsibleLabel(date, rightArrowIcon);
        dateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        dateLabel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, ColorScheme.BORDER_COLOR));

        // expand/collapse action
        dateLabel.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                boolean isVisible = !dailyEvents.isVisible();
                dailyEvents.setVisible(isVisible);
                // change arrow icon
                dateLabel.setIcon(isVisible ? downArrowIcon : rightArrowIcon);
                scheduleContainer
                        .setMaximumSize(new Dimension(Short.MAX_VALUE, scheduleContainer.getPreferredSize().height));
                repaint();
                revalidate();
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
     * @return JPanel single event container
     */
    private JPanel createEvent(SithClanEvent event, String day)
    {
        String eventTitle = SithClanUtil.removeEmojis(event.getEventTitle());

        // main container (info and checkbox)
        JPanel eventContainer = new JPanel();
        eventContainer.setLayout(new BoxLayout(eventContainer, BoxLayout.X_AXIS));

        // for tooltip time until next event
        final ZonedDateTime[] eventLocalTime =
        { null };

        // event info container
        JPanel singleEvent = new JPanel()
        {
            // show time until event
            @Override
            public String getToolTipText(MouseEvent event)
            {
                if (eventLocalTime[0] == null)
                {
                    return null;
                }

                ZonedDateTime now = ZonedDateTime.now();

                // event over or ongoing
                if (!eventLocalTime[0].isAfter(now))
                {
                    return EVENT_PASSED;
                }

                // calc time remaining
                long minutesUntil = ChronoUnit.MINUTES.between(now, eventLocalTime[0]);
                long hours = minutesUntil / 60;
                long minutes = minutesUntil % 60;

                if (hours > 0)
                {
                    return hours + "h " + minutes + M_UNTIL_THIS_EVENT;
                } else
                {
                    return minutes + M_UNTIL_THIS_EVENT;
                }
            }
        };
        singleEvent.setLayout(new BoxLayout(singleEvent, BoxLayout.Y_AXIS));
        singleEvent.setAlignmentX(Component.LEFT_ALIGNMENT);
        singleEvent.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        // register tooltip
        ToolTipManager.sharedInstance().registerComponent(singleEvent);

        // subscribe to event notifications
        JCheckBox notificationCheckbox = new JCheckBox();
        notificationCheckbox.setAlignmentX(Component.RIGHT_ALIGNMENT);
        notificationCheckbox.setToolTipText(CHECKBOX_TOOLTIP);
        // enabled per config
        notificationCheckbox.setEnabled(config.eventNotifications());
        notificationCheckbox.setSelected(fileManager.isSubscribed(eventTitle));

        // checkbox action
        notificationCheckbox.addActionListener(e ->
        {
            if (notificationCheckbox.isSelected())
            {
                fileManager.addSubscription(eventTitle);
            } else
            {
                fileManager.removeSubscription(eventTitle);
            }
            // reschedule notifications after change
            notificationManager.scheduleNotifications(eventSchedule.getSchedule());
        });

        JLabel mainEventTitle = new JLabel(wrapHtml(HtmlEscapers.htmlEscaper().escape(eventTitle)));
        mainEventTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        singleEvent.add(mainEventTitle);

        try
        {
            // event time as user local time
            ZonedDateTime estTime = ZonedDateTime.of(
                    LocalDate.parse(day, SithClanConstants.DATE_FORMATTER),
                    LocalTime.parse(event.getEventTime(), SithClanConstants.TIME_FORMATTER),
                    SithClanConstants.EST_ZONE);
            ZonedDateTime localTime = estTime.withZoneSameInstant(ZoneId.systemDefault());
            JLabel eventTime = new JLabel(localTime.format(SithClanConstants.TIME_FORMATTER));
            eventTime.setAlignmentX(Component.LEFT_ALIGNMENT);
            singleEvent.add(eventTime);
            eventLocalTime[0] = localTime;
        } catch (Exception e)
        {
            log.error("Exception while creating event panel: {}", e.getMessage(), e);
        }

        // event host (optional info)
        if (event.getEventHost() != null && !event.getEventHost().isBlank())
        {
            JLabel eventHost = new JLabel(wrapHtml(HtmlEscapers.htmlEscaper()
                    .escape("Hosted by: " + SithClanUtil.removeEmojis(event.getEventHost()))));
            eventHost.setAlignmentX(Component.LEFT_ALIGNMENT);
            singleEvent.add(eventHost);
        }

        // event misc info (optional info)
        if (!event.getEventMiscInfo().isEmpty())
        {
            for (String info : event.getEventMiscInfo())
            {
                // creating link to travel to clan discord channels
                JLabel eventInfo = createDiscordLink(SithClanUtil.removeEmojis(info));
                eventInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
                singleEvent.add(eventInfo);
            }
        }

        // event location
        // create location world hop link
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
        eventContainer.add(singleEvent);
        eventContainer.add(notificationCheckbox);

        singleEvent.setMaximumSize(new Dimension(Short.MAX_VALUE, singleEvent.getPreferredSize().height));
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
        // check text for Discord link
        Matcher matcher = Pattern.compile("<#(\\d+)>").matcher(text);

        if (matcher.find())
        {
            String channelId = matcher.group(1);
            String channelUrl = SithClanConstants.DISCORD_CHANNEL_URI + channelId;
            // create link
            String escaped = HtmlEscapers.htmlEscaper().escape(text);
            String withLink = escaped.replaceAll("&lt;#\\d+&gt;", "<a href=''>Discord</a>");
            JLabel channelLink = new JLabel(wrapHtml(withLink));
            channelLink.setPreferredSize(
                    new Dimension(channelLink.getPreferredSize().width - 80, channelLink.getPreferredSize().height));
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
                        log.error("Exception while creating Discord link: {}", ex.getMessage(), ex);
                    }
                }
            });
            return channelLink;
        }
        return new JLabel(wrapHtml(HtmlEscapers.htmlEscaper().escape(text)));
    }

    /**
     * Turn world location into quick world hop link
     * 
     * @param location
     *                     String event location
     * @return JLabel world quick hop link
     */
    private JLabel createWorldLink(String location)
    {
        // search for runescape world in text
        Matcher matcher = Pattern.compile("W(\\d{3}$)").matcher(location);
        if (!matcher.find())
        {
            return new JLabel(location);
        }
        String worldId = matcher.group(1);
        // create link
        String escaped = HtmlEscapers.htmlEscaper().escape(location);
        String withLink = escaped.replaceAll("W\\d{3}$", "<a href=''>W" + worldId + "</a>");
        JLabel worldLink = new JLabel(wrapHtml(withLink));
        worldLink
                .setMaximumSize(
                        new Dimension(worldLink.getPreferredSize().width - 100, worldLink.getPreferredSize().height));
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
                    log.error("Exception while creating world link: {}", ex.getMessage(), ex);
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
        try
        {
            LocalDate finalDate = LocalDate.parse(inputDay, SithClanConstants.DATE_FORMATTER);
            if (finalDate.isBefore(LocalDate.now()))
            {
                log.warn("Event schedule is expired, last date was: {}", inputDay);
                scheduleExpiredLabel.setVisible(true);
            } else
            {
                scheduleExpiredLabel.setVisible(false);
            }
        } catch (Exception e)
        {
            log.error("Exception while checking schedule expiration: {}", e.getMessage(), e);
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
            case SithClanConstants.STATUS_RATE_LIMITED:
                log.warn("Schedule fetch rate limited");
                statusLabel.setForeground(ColorScheme.BRAND_ORANGE);
                statusLabel.setText(RATE_LIMITED_WARNING);
                SithClanUtil.statusTimer(statusLabel);
                break;
            case SithClanConstants.STATUS_NOT_FOUND:
                log.error("Schedule fetch failed with status: {}", status);
                statusLabel.setForeground(ColorScheme.BRAND_ORANGE);
                statusLabel.setText(SCHEDULE_ERROR);
                SithClanUtil.statusTimer(statusLabel);
                break;
            default:
                break;
        }
    }

    /**
     * Starts task to refresh next event display
     */
    public void startNextEventRefresh()
    {
        // log.debug("Starting next event refresh task");
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
            // log.debug("Stopping next event refresh task");
            nextEventRefreshTask.cancel(false);
        }
    }

    /**
     * Wraps pre-built HTML in a fixed-width body
     *
     * @param html
     *                 String HTML fragment to wrap
     * @return String fragment wrapped in width-constrained HTML
     */
    private String wrapHtml(String html)
    {
        return "<html><body style='width:" + LABEL_WRAP_WIDTH + "px'>" + html + "</body></html>";
    }
}

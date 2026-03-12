package sithclanplugin.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;
import sithclanplugin.eventschedule.SithClanDaySchedule;
import sithclanplugin.eventschedule.SithClanEvent;
import sithclanplugin.eventschedule.SithClanEventSchedule;

public class SithClanEventSchedulePanel extends JPanel {

    private final JLabel schedulePanelLabel;
    private final JButton scheduleGetEventScheduleButton;
    private final JPanel scheduleContainer;
    private final JScrollPane scheduleContainerScrollPane;

    private SithClanEventSchedule eventSchedule;

    private final Icon rightArrowIcon;
    private final Icon downArrowIcon;

    private static final String EVENT_SCHEDULE = "Event Schedule";
    private static final String GET_SCHEDULE_BUTTON = "Refresh Schedule";
    private static final String ARROW_RIGHT_PATH = "/arrow_right.png";
    private static final String ARROW_DOWN_PATH = "/arrow_down.png";
    private static final String REPEATED_WEEKLY = "Repeated Weekly";

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
        scheduleContainerScrollPane
                .setMaximumSize(
                        new Dimension(Integer.MAX_VALUE, scheduleContainerScrollPane.getPreferredSize().height));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(schedulePanelLabel);
        topPanel.add(Box.createRigidArea(new Dimension(0, 10)));

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
                SwingUtilities.invokeLater(() -> displaySchedule());
            }).start();
        });
    }

    // TODO: doc for function
    // TODO: refactor giant function
    public void displaySchedule() {
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

            JLabel dateLabel = new JLabel(day.getDate());

            JPanel dailyEvents = new JPanel();
            dailyEvents.setLayout(new BoxLayout(dailyEvents, BoxLayout.Y_AXIS));
            dailyEvents.setAlignmentX(Component.LEFT_ALIGNMENT);
            dailyEvents.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            dailyEvents.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, ColorScheme.BORDER_COLOR));
            dailyEvents.setVisible(false);

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

            scheduleContainer.add(dateLabel);
            scheduleContainer.add(dailyEvents);

            for (SithClanEvent event : day.getEvents()) {
                JPanel singleEvent = new JPanel();
                singleEvent.setLayout(new BoxLayout(singleEvent, BoxLayout.Y_AXIS));
                singleEvent.setAlignmentX(Component.LEFT_ALIGNMENT);
                singleEvent.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

                JLabel eventTitle = new JLabel(event.getEventTitle());
                eventTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
                singleEvent.add(eventTitle);

                JLabel eventTime = new JLabel(event.getEventTime());
                eventTime.setAlignmentX(Component.LEFT_ALIGNMENT);
                singleEvent.add(eventTime);

                if (event.getEventHost() != null && !event.getEventHost().isBlank()) {
                    JLabel eventHost = new JLabel(event.getEventHost());
                    eventHost.setAlignmentX(Component.LEFT_ALIGNMENT);
                    singleEvent.add(eventHost);
                }

                if (!event.getEventMiscInfo().isEmpty()) {
                    for (String info : event.getEventMiscInfo()) {
                        JLabel eventInfo = new JLabel(info);
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

                dailyEvents.add(singleEvent);
                dailyEvents.add(Box.createRigidArea(new Dimension(0, 10)));

            }
        }

        scheduleContainer.revalidate();
        scheduleContainer.repaint();
    }
}

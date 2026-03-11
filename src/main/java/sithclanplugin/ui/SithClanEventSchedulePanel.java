package sithclanplugin.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.google.inject.Inject;

import sithclanplugin.eventschedule.SithClanEventSchedule;

public class SithClanEventSchedulePanel extends JPanel {

    private final JLabel schedulePanelLabel;
    private final JButton scheduleGetEventScheduleButton;

    private SithClanEventSchedule eventSchedule;

    private static final String EVENT_SCHEDULE = "Event Schedule";
    private static final String GET_SCHEDULE_BUTTON = "Refresh Schedule";

    @Inject
    SithClanEventSchedulePanel(SithClanEventSchedule eventSchedule) {

        this.eventSchedule = eventSchedule;

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        schedulePanelLabel = new JLabel(EVENT_SCHEDULE);
        schedulePanelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        scheduleGetEventScheduleButton = new JButton(GET_SCHEDULE_BUTTON);
        scheduleGetEventScheduleButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        this.add(schedulePanelLabel);
        this.add(Box.createRigidArea(new Dimension(0, 10)));
        this.add(scheduleGetEventScheduleButton);
        this.setVisible(true);

        // get event schedule action
        scheduleGetEventScheduleButton.addActionListener((new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                displaySchedule();
            }
        }));
    }

    private void displaySchedule() {
        if (eventSchedule.getSchedule() == null || eventSchedule.getSchedule().isEmpty()) {
            eventSchedule.parseScheduleFromGet();
        }
        System.out.println(eventSchedule.getSchedule());
    }
}

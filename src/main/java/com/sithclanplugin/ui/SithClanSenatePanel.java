package com.sithclanplugin.ui;

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
import com.google.inject.Singleton;
import com.sithclanplugin.eventschedule.SithClanEventSchedule;

@Singleton
public class SithClanSenatePanel extends JPanel {

    private final JLabel senatePanelLabel;
    private final JButton senatePostScheduleButton;

    private SithClanEventSchedule eventSchedule;

    private static final String SENATE_OPTIONS_LABEL = "Senate Options";
    private static final String UPLOAD_SCHEDULE_BUTTON = "Post Schedule";
    private static final String testData = "[{\"test\":\"data\"}]";

    @Inject
    SithClanSenatePanel(SithClanEventSchedule eventSchedule) {

        this.eventSchedule = eventSchedule;

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        senatePanelLabel = new JLabel(SENATE_OPTIONS_LABEL);
        senatePanelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        senatePostScheduleButton = new JButton(UPLOAD_SCHEDULE_BUTTON);
        senatePostScheduleButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        this.add(senatePanelLabel);
        this.add(Box.createRigidArea(new Dimension(0, 10)));
        this.add(senatePostScheduleButton);
        this.setVisible(true);

        // post event schedule action
        senatePostScheduleButton.addActionListener((new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                eventSchedule.postEventSchedule(testData);
            }
        }));
    }

}

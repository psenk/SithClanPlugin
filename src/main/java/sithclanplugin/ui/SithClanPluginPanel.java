package sithclanplugin.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.inject.Inject;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.google.inject.Singleton;

import lombok.Getter;
import net.runelite.client.ui.PluginPanel;
import sithclanplugin.eventschedule.SithClanEventSchedule;

@Singleton
@Getter
public class SithClanPluginPanel extends PluginPanel {

    private SithClanEventSchedule eventSchedule;
    private final SithClanEventSchedulePanel schedulePanel;
    private final SithClanSenatePanel senatePanel;

    private final JPanel cardPanel;
    private final JPanel navPanel;
    private final JPanel buttonPanel;
    private final JButton scheduleButton;
    private final JButton senateButton;
    private CardLayout cardLayout;

    private static final String EVENT_SCHEDULE = "Event Schedule";
    private static final String SENATE_OPTIONS = "Senate Options";
    private static final String SCHEDULE_TITLE = "schedule";
    private static final String SENATE_TITLE = "senate";

    @Inject
    SithClanPluginPanel(SithClanEventSchedule eventSchedule, SithClanEventSchedulePanel schedulePanel,
            SithClanSenatePanel senatePanel) {
        this.setLayout(new BorderLayout());
        getScrollPane().setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // create card panel and layout manager
        cardPanel = new JPanel();
        cardLayout = new CardLayout();
        cardPanel.setLayout(cardLayout);

        // create cards and button panel
        this.schedulePanel = schedulePanel;
        this.senatePanel = senatePanel;
        buttonPanel = new JPanel();
        navPanel = new JPanel();

        // configure panel layouts
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        navPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

        // create panel buttons
        scheduleButton = new JButton(EVENT_SCHEDULE);
        senateButton = new JButton(SENATE_OPTIONS);

        // add cards to card panel
        cardPanel.add(schedulePanel, SCHEDULE_TITLE);
        cardPanel.add(senatePanel, SENATE_TITLE);

        // adding buttons to button panel
        buttonPanel.add(scheduleButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(senateButton);

        // add panels to main panel
        navPanel.add(buttonPanel);
        this.add(navPanel, BorderLayout.NORTH);
        this.add(cardPanel, BorderLayout.CENTER);

        // action listeners
        scheduleButton.addActionListener(e -> {
            cardLayout.show(cardPanel, SCHEDULE_TITLE);
        });

        senateButton.addActionListener(e -> {
            cardLayout.show(cardPanel, SENATE_TITLE);
        });

        schedulePanel.setOnRefreshCallback(() -> {
            new Thread(() -> {
                boolean isSenateMember = eventSchedule.validateApiKey();
                SwingUtilities.invokeLater(() -> senateButton.setVisible(isSenateMember));
            }).start();
        });
    }
}

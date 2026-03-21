package sithclanplugin.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.inject.Inject;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.google.inject.Singleton;

import lombok.Getter;
import net.runelite.client.ui.PluginPanel;
import sithclanplugin.eventschedule.SithClanEventSchedule;

@Singleton
public class SithClanPluginPanel extends PluginPanel {

    @Inject
    private SithClanEventSchedule eventSchedule;

    @Getter
    private final SithClanEventSchedulePanel schedulePanel;

    @Getter
    private final SithClanSenatePanel senatePanel;

    private final JPanel cardPanel;
    private final JPanel navPanel;
    private final JPanel buttonPanel;
    private final JButton scheduleButton;
    @Getter
    private final JButton senateButton;
    private CardLayout cardLayout;
    private final JPanel notClanMemberPanel;
    private final JLabel notClanMemberLabel;

    private static final String EVENT_SCHEDULE = "Event Schedule";
    private static final String SENATE_OPTIONS = "Senate Options";
    private static final String SCHEDULE_TITLE = "schedule";
    private static final String SENATE_TITLE = "senate";
    private static final String NON_MEMBER_MESSAGE = "Sorry, this plugin is for members of the Sith clan only.";

    @Inject
    SithClanPluginPanel(SithClanEventSchedulePanel schedulePanel,
            SithClanSenatePanel senatePanel) {
        this.schedulePanel = schedulePanel;
        this.senatePanel = senatePanel;

        this.setLayout(new BorderLayout());
        getScrollPane().setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // main panel
        cardLayout = new CardLayout();
        cardPanel = new JPanel();
        cardPanel.setLayout(cardLayout);
        cardPanel.add(schedulePanel, SCHEDULE_TITLE);
        cardPanel.add(senatePanel, SENATE_TITLE);

        // create container panels
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        navPanel = new JPanel();
        navPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        navPanel.add(buttonPanel);

        // create panel buttons
        scheduleButton = new JButton(EVENT_SCHEDULE);
        senateButton = new JButton(SENATE_OPTIONS);

        // adding buttons to button panel
        buttonPanel.add(scheduleButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(senateButton);

        this.add(navPanel, BorderLayout.NORTH);
        this.add(cardPanel, BorderLayout.CENTER);

        // action listeners
        scheduleButton.addActionListener(e -> {
            cardLayout.show(cardPanel, SCHEDULE_TITLE);
        });

        senateButton.addActionListener(e -> {
            cardLayout.show(cardPanel, SENATE_TITLE);
        });

        // show senate button if API key added
        schedulePanel.setOnRefreshCallback(() -> {
            new Thread(() -> {
                boolean isSenateMember = eventSchedule.validateApiKey();
                SwingUtilities.invokeLater(() -> senateButton.setVisible(isSenateMember));
            }).start();
        });

        // panel for users not in the clan
        notClanMemberPanel = new JPanel();
        notClanMemberLabel = new JLabel(NON_MEMBER_MESSAGE);
        notClanMemberPanel.add(notClanMemberLabel);
        notClanMemberPanel.setVisible(false);
        this.add(notClanMemberPanel, BorderLayout.CENTER);
    }

    /**
     * Hide all if user is not in clan
     */
    public void userNotInClan() {
        cardPanel.setVisible(false);
        buttonPanel.setVisible(false);
        navPanel.setVisible(false);
        notClanMemberPanel.setVisible(true);
    }
}

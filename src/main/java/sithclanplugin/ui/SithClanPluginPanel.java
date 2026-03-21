package sithclanplugin.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
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
    private final SithClanSchedulePanel schedulePanel;

    @Getter
    private final SithClanMembersPanel membersPanel;

    @Getter
    private final SithClanSenatePanel senatePanel;

    @Getter
    private final JButton senateButton;

    private final JPanel cardPanel;
    private final JPanel navPanel;
    private final JPanel buttonPanel;
    private CardLayout cardLayout;
    private final JPanel notClanMemberPanel;

    private static final String PLUGIN_LABEL = "Sith Clan Plugin";
    private static final String SCHEDULE_BUTTON = "Event Schedule";
    private static final String MEMBERS_BUTTON = "Member Info";
    private static final String SENATE_BUTTON = "Senate Options";
    private static final String SCHEDULE_TITLE = "schedule";
    private static final String MEMBERS_TITLE = "members";
    private static final String SENATE_TITLE = "senate";
    private static final String NON_MEMBER_MESSAGE = "Sorry, this plugin is for members of the Sith clan only.";

    @Inject
    SithClanPluginPanel(SithClanSchedulePanel schedulePanel,
            SithClanSenatePanel senatePanel, SithClanMembersPanel membersPanel) {
        this.schedulePanel = schedulePanel;
        this.membersPanel = membersPanel;
        this.senatePanel = senatePanel;

        // main panel
        cardLayout = new CardLayout();

        this.setLayout(new BorderLayout());
        getScrollPane().setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // cards for each panel
        cardPanel = new JPanel();
        cardPanel.setLayout(cardLayout);
        cardPanel.add(schedulePanel, SCHEDULE_TITLE);
        cardPanel.add(membersPanel, MEMBERS_TITLE);
        cardPanel.add(senatePanel, SENATE_TITLE);

        // button container
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        // navigation area
        navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));

        // main plugin title
        JLabel pluginLabel = new JLabel(PLUGIN_LABEL);
        pluginLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // card buttons
        JButton scheduleButton = new JButton(SCHEDULE_BUTTON);
        scheduleButton.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        JButton membersButton = new JButton(MEMBERS_BUTTON);
        membersButton.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        senateButton = new JButton(SENATE_BUTTON);
        senateButton.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        senateButton.setVisible(false);
        buttonPanel.add(scheduleButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(membersButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(senateButton);

        // button container to center buttons
        JPanel buttonContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonContainer.add(buttonPanel);

        // top panel
        navPanel.add(pluginLabel);
        navPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        navPanel.add(buttonContainer);

        this.add(navPanel, BorderLayout.NORTH);
        this.add(cardPanel, BorderLayout.CENTER);

        // action listeners
        scheduleButton.addActionListener(e -> {
            cardLayout.show(cardPanel, SCHEDULE_TITLE);
        });

        membersButton.addActionListener(e -> {
            cardLayout.show(cardPanel, MEMBERS_TITLE);
        });

        senateButton.addActionListener(e -> {
            cardLayout.show(cardPanel, SENATE_TITLE);
        });

        // show senate button if API key added LATER
        schedulePanel.setOnRefreshCallback(() -> {
            new Thread(() -> {
                boolean isSenateMember = eventSchedule.validateApiKey();
                SwingUtilities.invokeLater(() -> senateButton.setVisible(isSenateMember));
            }).start();
        });

        // panel for users not in the clan
        notClanMemberPanel = new JPanel();
        JLabel notClanMemberLabel = new JLabel(NON_MEMBER_MESSAGE);
        notClanMemberPanel.add(notClanMemberLabel);
        notClanMemberPanel.setVisible(false);
    }

    /**
     * Hide all if user is not in clan
     */
    public void userNotInClan() {
        cardPanel.setVisible(false);
        buttonPanel.setVisible(false);
        navPanel.setVisible(false);
        this.add(notClanMemberPanel, BorderLayout.CENTER);
        notClanMemberPanel.setVisible(true);
        revalidate();
        repaint();
    }
}

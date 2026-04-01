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
import okhttp3.OkHttpClient;
import sithclanplugin.SithClanPluginConfig;
import sithclanplugin.util.SithClanPluginUtil;

@Singleton
public class SithClanPluginPanel extends PluginPanel
{
    @Inject
    private SithClanPluginConfig config;

    @Inject
    private OkHttpClient httpClient;

    @Getter
    private final SithClanSchedulePanel schedulePanel;

    @Getter
    private final SithClanMembersPanel membersPanel;

    @Getter
    private final SithClanSenatePanel senatePanel;

    @Getter
    private final JButton senateButton;

    private CardLayout cardLayout;
    private final JPanel cardPanel;
    private final JPanel navPanel;
    private final JPanel buttonPanel;
    private final JPanel notLoggedInPanel;
    private final JPanel notClanMemberPanel;
    private final CardLayout outerCardLayout = new CardLayout();
    private final JPanel outerCardPanel = new JPanel(outerCardLayout);

    private static final String PLUGIN_LABEL = "Sith Clan Plugin";
    private static final String SCHEDULE_BUTTON = "Event Schedule";
    private static final String MEMBERS_BUTTON = "Member Info";
    private static final String SENATE_BUTTON = "Senate Options";
    private static final String MAIN_CARD = "main";
    private static final String NOT_LOGGED_IN_CARD = "notLoggedIn";
    private static final String NOT_CLAN_MEMBER_CARD = "notClanMember";
    private static final String SCHEDULE_CARD = "schedule";
    private static final String MEMBERS_CARD = "members";
    private static final String SENATE_CARD = "senate";
    private static final String NOT_LOGGED_IN_MESSAGE = "<html><center>Please log in and turn on clan chat to use this plugin.</center></html>";
    private static final String NON_MEMBER_MESSAGE = "<html><center>Sorry, this plugin is for members of the Sith clan only.  Message Kyanize in-game or kyanize. in Discord for joining info!</center></html>";

    @Inject
    SithClanPluginPanel(SithClanSchedulePanel schedulePanel,
            SithClanSenatePanel senatePanel, SithClanMembersPanel membersPanel)
    {
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
        cardPanel.add(schedulePanel, SCHEDULE_CARD);
        cardPanel.add(membersPanel, MEMBERS_CARD);
        cardPanel.add(senatePanel, SENATE_CARD);

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
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // button container to center buttons
        JPanel buttonContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonContainer.add(buttonPanel);

        // top panel
        navPanel.add(pluginLabel);
        navPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        navPanel.add(buttonContainer);

        this.add(navPanel, BorderLayout.NORTH);

        // action listeners
        scheduleButton.addActionListener(e ->
        {
            cardLayout.show(cardPanel, SCHEDULE_CARD);
        });

        membersButton.addActionListener(e ->
        {
            cardLayout.show(cardPanel, MEMBERS_CARD);
        });

        senateButton.addActionListener(e ->
        {
            cardLayout.show(cardPanel, SENATE_CARD);
        });

        // show senate button if API key added LATER
        schedulePanel.setOnRefreshCallback(() ->
        {
            new Thread(() ->
            {
                boolean isSenateMember = SithClanPluginUtil.validateApiKey(httpClient, config);
                SwingUtilities.invokeLater(() -> senateButton.setVisible(isSenateMember));
            }).start();
        });

        // panel for users not logged in
        notLoggedInPanel = new JPanel();
        notLoggedInPanel.setLayout(new BoxLayout(notLoggedInPanel, BoxLayout.Y_AXIS));
        JLabel notLoggedInLabel = new JLabel(NOT_LOGGED_IN_MESSAGE);
        notLoggedInLabel.setMaximumSize(
                new Dimension(PluginPanel.PANEL_WIDTH - 10, notLoggedInLabel.getPreferredSize().height * 2));
        notLoggedInPanel.add(notLoggedInLabel);

        // panel for users not in the clan
        notClanMemberPanel = new JPanel();
        notClanMemberPanel.setLayout(new BoxLayout(notClanMemberPanel, BoxLayout.Y_AXIS));
        JLabel notClanMemberLabel = new JLabel(NON_MEMBER_MESSAGE);
        notClanMemberLabel.setMaximumSize(
                new Dimension(PluginPanel.PANEL_WIDTH - 10, notClanMemberLabel.getPreferredSize().height * 2));
        notClanMemberPanel.add(notClanMemberLabel);

        // add center state cards into outer panel
        outerCardPanel.add(notLoggedInPanel, NOT_LOGGED_IN_CARD);
        outerCardPanel.add(notClanMemberPanel, NOT_CLAN_MEMBER_CARD);
        outerCardPanel.add(cardPanel, MAIN_CARD);

        // default card
        outerCardLayout.show(outerCardPanel, NOT_LOGGED_IN_CARD);

        this.add(outerCardPanel, BorderLayout.CENTER);
    }

    /**
     * Shows main panel after successful clan verification
     */
    public void showMainPanel()
    {
        navPanel.setVisible(true);
        buttonPanel.setVisible(true);
        outerCardLayout.show(outerCardPanel, MAIN_CARD);
        revalidate();
        repaint();
    }

    /**
     * Hide all if user is not in clan
     */
    public void userNotInClan()
    {
        navPanel.setVisible(false);
        buttonPanel.setVisible(false);
        outerCardLayout.show(outerCardPanel, NOT_CLAN_MEMBER_CARD);
        revalidate();
        repaint();
    }
}

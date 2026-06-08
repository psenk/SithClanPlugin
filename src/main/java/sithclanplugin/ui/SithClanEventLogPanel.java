package sithclanplugin.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import net.runelite.client.ui.ColorScheme;
import okhttp3.OkHttpClient;
import sithclanplugin.SithClanPluginConfig;
import sithclanplugin.members.SithClanMemberRoster;
import sithclanplugin.util.SithClanPluginConstants;
import sithclanplugin.util.SithClanPluginUtil;

@Singleton
public class SithClanEventLogPanel extends JPanel
{
    @Inject
    private OkHttpClient httpClient;

    @Inject
    private ScheduledExecutorService executor;

    @Inject
    private SithClanMemberRoster memberRoster;

    @Inject
    private SithClanPluginConfig config;

    private final JTextArea eventLogTextArea;
    private final JPanel statusPanel;
    private final JLabel successLabel;
    private final JLabel failureLabel;
    private final JLabel noWebhookLabel;
    private final JLabel invalidWebhookLabel;
    private final JLabel rosterErrorLabel;
    private final JLabel noNameLabel;
    private final JLabel noHostLabel;

    private static final String PANEL_LABEL = "Post Event Log";
    private static final String SUBMIT_BUTTON = "Submit";
    private static final String TEXT_AREA_DEFAULT = "Post Event Log Here";
    private static final String EVENT_NAME_PREFIX = "Event name: "; // trailing space intentional
    private static final String EVENT_HOST_PREFIX = "Hosted by: "; // trailing space intentional
    private static final String TABLE_HEADER = "Name         | Time   | Late"; // spaces intentional
    private static final String NO_WEBHOOK_URL_WARNING = "No Discord webhook URL set in config";
    private static final String NO_EVENT_NAME_WARNING = "Event name missing";
    private static final String NO_EVENT_HOST_WARNING = "Event host missing";
    private static final String NON_MEMBER_WARNING = "The following names were not found in the clan roster:\n";
    private static final String ROSTER_ERROR_WARNING = "Unable to load member roster.";
    private static final String POST_SUCCESS = "Event log posted to Discord";
    private static final String POST_FAILURE = "Failed to post event log to Discord";
    private static final String DISCORD_WEBHOOK_PREFIX = "https://discord.com/";
    private static final String INVALID_WEBHOOK_URL = "Invalid Discord webhook URL";
    private static final int DISCORD_MAX_LENGTH = 2000;

    SithClanEventLogPanel()
    {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // panel title
        JLabel panelLabel = new JLabel(PANEL_LABEL);
        panelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.add(panelLabel);
        this.add(Box.createRigidArea(new Dimension(0, 5)));

        // status label panel
        statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));

        // success status label
        successLabel = new JLabel(POST_SUCCESS);
        successLabel.setVisible(false);
        successLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // failure status label
        failureLabel = new JLabel(POST_FAILURE);
        failureLabel.setVisible(false);
        failureLabel.setForeground(ColorScheme.BRAND_ORANGE);
        failureLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // no webhook status label
        noWebhookLabel = new JLabel(NO_WEBHOOK_URL_WARNING);
        noWebhookLabel.setVisible(false);
        noWebhookLabel.setForeground(ColorScheme.BRAND_ORANGE);
        noWebhookLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // invalid webhook status label
        invalidWebhookLabel = new JLabel(INVALID_WEBHOOK_URL);
        invalidWebhookLabel.setVisible(false);
        invalidWebhookLabel.setForeground(ColorScheme.BRAND_ORANGE);
        invalidWebhookLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // roster error status label
        rosterErrorLabel = new JLabel(ROSTER_ERROR_WARNING);
        rosterErrorLabel.setVisible(false);
        rosterErrorLabel.setForeground(ColorScheme.BRAND_ORANGE);
        rosterErrorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // no event name status label
        noNameLabel = new JLabel(NO_EVENT_NAME_WARNING);
        noNameLabel.setVisible(false);
        noNameLabel.setForeground(ColorScheme.BRAND_ORANGE);
        noNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // no event host status label
        noHostLabel = new JLabel(NO_EVENT_HOST_WARNING);
        noHostLabel.setVisible(false);
        noHostLabel.setForeground(ColorScheme.BRAND_ORANGE);
        noHostLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        statusPanel.add(successLabel);
        statusPanel.add(failureLabel);
        statusPanel.add(noWebhookLabel);
        statusPanel.add(invalidWebhookLabel);
        statusPanel.add(rosterErrorLabel);
        statusPanel.add(noNameLabel);
        statusPanel.add(noHostLabel);
        this.add(statusPanel);
        this.add(Box.createRigidArea(new Dimension(0, 5)));

        // text area to paste log
        eventLogTextArea = new JTextArea(TEXT_AREA_DEFAULT);
        eventLogTextArea.setRows(20);

        // highlights all text when box focused
        eventLogTextArea.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                eventLogTextArea.selectAll();
            }
        });

        JScrollPane scrollPane = new JScrollPane(eventLogTextArea);
        scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, scrollPane.getPreferredSize().height));
        this.add(scrollPane);

        // submit button
        JButton submitButton = new JButton(SUBMIT_BUTTON);
        submitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        submitButton.setMaximumSize(new Dimension(Short.MAX_VALUE, submitButton.getPreferredSize().height));
        this.add(Box.createRigidArea(new Dimension(0, 10)));
        this.add(submitButton);

        // submit button action
        submitButton.addActionListener(e ->
        {
            executor.submit(() -> validateEventLog(eventLogTextArea.getText()));
        });
    }

    /**
     * VALIDATION FUNCTIONS
     */

    /**
     * Validate event log, check for event name, host, clan membership
     * 
     * @param eventLog
     *                     String raw event log input
     */
    private void validateEventLog(String eventLog)
    {
        // check webhook URL configured
        String webhookUrl = config.eventLogWebhook();
        if (webhookUrl == null || webhookUrl.isBlank())
        {
            SwingUtilities.invokeLater(() ->
            {
                noWebhookLabel.setVisible(true);
                SithClanPluginUtil.statusTimer(noWebhookLabel);
            });
            return;
        }

        // validate webhook URL
        if (!webhookUrl.startsWith(DISCORD_WEBHOOK_PREFIX))
        {
            SwingUtilities.invokeLater(() ->
            {
                invalidWebhookLabel.setVisible(true);
                SithClanPluginUtil.statusTimer(invalidWebhookLabel);
            });
            return;
        }

        // split event log
        String[] lines = eventLog.split("\\r?\\n");

        // validate event name and host
        if (!validateHeader(lines))
        {
            return;
        }

        // validate members
        if (!validateMembers(lines))
        {
            return;
        }

        ArrayList<String> messages = buildDiscordMessages(eventLog);
        for (String message : messages)
        {
            // post to Discord
            String response = SithClanPluginUtil.sendEventLogToDiscord(httpClient, webhookUrl, message);
            if (response == null)
            {
                SwingUtilities.invokeLater(() ->
                {
                    failureLabel.setVisible(true);
                    SithClanPluginUtil.statusTimer(failureLabel);
                });
                return;
            }
        }
        SwingUtilities.invokeLater(() ->
        {
            successLabel.setVisible(true);
            SithClanPluginUtil.statusTimer(successLabel);
        });
    }

    /**
     * Validate event name and event host
     * 
     * @param eventLog
     *                     raw event log as array
     * @return boolean is header correct
     */
    private boolean validateHeader(String[] eventLog)
    {
        String eventName = "";
        String eventHost = "";
        for (String line : eventLog)
        {
            if (line.startsWith(EVENT_NAME_PREFIX))
            {
                eventName = line.substring(EVENT_NAME_PREFIX.length()).trim();
            }
            if (line.startsWith(EVENT_HOST_PREFIX))
            {
                eventHost = line.substring(EVENT_HOST_PREFIX.length()).trim();
            }
        }

        // verify event name
        if (eventName.isBlank())
        {
            SwingUtilities.invokeLater(() ->
            {
                noNameLabel.setVisible(true);
                SithClanPluginUtil.statusTimer(noNameLabel);
            });
            return false;
        }

        // verify event host
        if (eventHost.isBlank())
        {
            SwingUtilities.invokeLater(() ->
            {
                noHostLabel.setVisible(true);
                SithClanPluginUtil.statusTimer(noHostLabel);
            });
            return false;
        }
        return true;
    }

    /**
     * Validate members in clan
     * 
     * @param eventLog
     *                     raw event log as array
     * @return boolean are all members in clan
     */
    private boolean validateMembers(String[] eventLog)
    {
        // load roster if not cached
        if (memberRoster.getRoster().isEmpty())
        {
            int status = memberRoster.parseRosterFromGet();
            if (status != SithClanPluginConstants.STATUS_OK)
            {
                SwingUtilities.invokeLater(() ->
                {
                    rosterErrorLabel.setVisible(true);
                    SithClanPluginUtil.statusTimer(rosterErrorLabel);
                });
                return false;
            }
        }

        // find members in table
        boolean inTable = false;
        ArrayList<String> nonMembers = new ArrayList<>();

        for (String line : eventLog)
        {
            if (line.trim().startsWith(TABLE_HEADER.trim()))
            {
                inTable = true;
                continue;
            }
            if (!inTable || line.isBlank())
            {
                continue;
            }
            if (line.trim().equals("```"))
            {
                break;
            }

            // extract player name
            String memberName = line.substring(0, line.indexOf("|")).trim();
            // member not in clan
            if (memberRoster.getMemberByName(memberName) == null)
            {
                nonMembers.add(memberName);
            }
        }

        // report non-members
        if (!nonMembers.isEmpty())
        {
            String warningMessage = NON_MEMBER_WARNING + String.join("\n", nonMembers);
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, warningMessage));
            return false;
        }
        return true;
    }

    /**
     * CREATE FUNCTIONS
     */

    /**
     * Builds Discord messages
     * Splits message if required
     * 
     * @param eventLog
     *                     String raw event log
     * @return ArrayList<String> list of messages to send
     */
    private ArrayList<String> buildDiscordMessages(String eventLog)
    {
        // only one post needed
        if (eventLog.length() <= DISCORD_MAX_LENGTH)
        {
            ArrayList<String> singleMessage = new ArrayList<>();
            singleMessage.add(eventLog + "\n*Sent from Sith Clan Plugin, message 1 of 1*");
            return singleMessage;
        }

        // split event log
        String[] lines = eventLog.split("\\r?\\n");

        // separate header, members, and footer
        StringBuilder header = new StringBuilder();
        StringBuilder tableHeader = new StringBuilder();
        ArrayList<String> memberLines = new ArrayList<>();
        StringBuilder footer = new StringBuilder();
        boolean inTable = false;
        boolean tableEnded = false;

        // build header and messages
        for (String line : lines)
        {
            if (!inTable && line.trim().equals("```"))
            {
                inTable = true;
                continue;
            }
            if (!inTable)
            {
                header.append(line).append("\n");
                continue;
            }

            if (inTable && !tableEnded)
            {
                if (line.trim().equals("```"))
                {
                    tableEnded = true;
                    continue;
                }
                if (line.trim().startsWith(TABLE_HEADER.trim()) || line.trim().startsWith("---"))
                {
                    tableHeader.append(line).append("\n");
                    continue;
                }
                if (!line.isBlank())
                {
                    memberLines.add(line);
                }
            }
            if (tableEnded)
            {
                footer.append(line).append("\n");
            }
        }

        // split members in half
        int mid = memberLines.size() / 2;
        ArrayList<String> firstHalf = new ArrayList<>(memberLines.subList(0, mid));
        ArrayList<String> secondHalf = new ArrayList<>(memberLines.subList(mid, memberLines.size()));

        // reconstruct two messages
        ArrayList<String> messages = new ArrayList<>();
        messages.add(buildMessage(header.toString(), tableHeader.toString(), firstHalf, footer.toString(), 1, 2));
        messages.add(buildMessage(header.toString(), tableHeader.toString(), secondHalf, footer.toString(), 2, 2));
        return messages;
    }

    /**
     * Reconstruct event log from header and member list
     * 
     * @param header
     *                          String raw event log header
     * @param tableHeader
     *                          String column header and separator
     * @param memberLines
     *                          ArrayList<String> list of members in message
     * @param footer
     *                          String footer of message
     * @param messageNumber
     *                          int index of message
     * @param totalMessages
     *                          int total number of messages being sent
     * @return String raw message output
     */
    private String buildMessage(String header, String tableHeader, ArrayList<String> memberLines, String footer,
            int messageNumber, int totalMessages)
    {
        StringBuilder message = new StringBuilder();
        message.append(header);
        message.append("```\n");
        message.append(tableHeader);
        for (String line : memberLines)
        {
            message.append(line).append("\n");
        }
        message.append("```\n");
        message.append(footer);
        message.append("\n\n**Sent from Sith Clan Plugin, message ").append(messageNumber).append(" of ")
                .append(totalMessages).append("**");
        return message.toString();
    }
}
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

    SithClanEventLogPanel()
    {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // panel title
        JLabel panelLabel = new JLabel(PANEL_LABEL);
        panelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.add(panelLabel);
        this.add(Box.createRigidArea(new Dimension(0, 10)));

        // text area to paste log
        eventLogTextArea = new JTextArea(TEXT_AREA_DEFAULT);
        eventLogTextArea.setRows(10);

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
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, NO_WEBHOOK_URL_WARNING));
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

        // post to Discord
        String response = SithClanPluginUtil.sendEventLogToDiscord(httpClient, webhookUrl, eventLog);
        System.out.println(response);
        if (response == null)
        {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, POST_FAILURE));
            return;
        }
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, POST_SUCCESS));
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
                JOptionPane.showMessageDialog(null, NO_EVENT_NAME_WARNING);
            });
            return false;
        }

        // verify event host
        if (eventHost.isBlank())
        {
            SwingUtilities.invokeLater(() ->
            {
                JOptionPane.showMessageDialog(null, NO_EVENT_HOST_WARNING);
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
        if (memberRoster.getRoster() == null || memberRoster.getRoster().isEmpty())
        {
            int status = memberRoster.parseRosterFromGet();
            if (status != SithClanPluginConstants.STATUS_OK)
            {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, ROSTER_ERROR_WARNING));
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
}

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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import net.runelite.client.ui.ColorScheme;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import sithclanplugin.SithClanConfig;
import sithclanplugin.managers.SithClanFileManager;
import sithclanplugin.members.SithClanMemberRoster;
import sithclanplugin.util.SithClanConstants;
import sithclanplugin.util.SithClanUtil;

// refactored june 16

@Slf4j
@Singleton
public class SithClanEventLogPanel extends JPanel
{

    @Inject
    private OkHttpClient httpClient;

    @Inject
    private ScheduledExecutorService executor;

    @Inject
    private SithClanConfig config;

    @Inject
    private SithClanMemberRoster memberRoster;

    @Inject
    private SithClanFileManager fileManager;

    private final JPanel statusPanel;
    private final JLabel statusLabel;
    private final JTextArea eventLogTextArea;
    private final JButton importButton;
    private final JButton importOtherButton;

    private static final String PANEL_LABEL = "Post Event Log";
    private static final String TEXT_AREA_DEFAULT = "Post Event Log Here";
    private static final String SUBMIT_BUTTON = "Submit";
    private static final String NO_WEBHOOK_URL_WARNING = "No Discord webhook URL set in config.";
    private static final String DISCORD_WEBHOOK_PREFIX = "https://discord.com/";
    private static final String INVALID_WEBHOOK_URL = "Invalid Discord webhook URL.";
    private static final String POST_SUCCESS = "Event log posted to Discord.";
    private static final String POST_FAILURE = "Failed to post event log to Discord.";
    private static final String EVENT_NAME_PREFIX = "Event name: "; // trailing space intentional
    private static final String EVENT_HOST_PREFIX = "Hosted by: "; // trailing space intentional
    private static final String NO_EVENT_NAME_WARNING = "Event name missing";
    private static final String NO_EVENT_HOST_WARNING = "Event host missing";
    private static final String ROSTER_ERROR_WARNING = "Unable to load member roster.";
    private static final String TABLE_HEADER = "Name         | Time   | Late"; // spaces intentional
    private static final String NON_MEMBER_WARNING = "The following names were not found in the clan roster:\n";
    private static final String IMPORT_BUTTON = "Import Latest Event";
    private static final String IMPORT_OTHER_BUTTON = "Import Other Event";
    private static final String IMPORT_NO_FILE_WARNING = "No Clan Event Attendance file found.";
    private static final String SELECT_EVENT_LOG_FILE = "Select Event Log File";
    private static final String FILE_FILTER_DESCRIPTION = "Text files (*.txt)";
    private static final String EVENT_LOG_FILE_EXTENSION = "txt";
    private static final int DISCORD_MAX_LENGTH = 2000;

    SithClanEventLogPanel()
    {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // panel title
        JLabel eventLogPanelLabel = new JLabel(PANEL_LABEL);
        eventLogPanelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.add(eventLogPanelLabel);
        this.add(Box.createRigidArea(new Dimension(0, 5)));

        // status label panel and label
        statusLabel = SithClanUtil.createStatusLabel();
        statusPanel = SithClanUtil.createStatusPanel(statusLabel);
        this.add(statusPanel);

        // text area to paste log
        eventLogTextArea = new JTextArea(TEXT_AREA_DEFAULT);
        eventLogTextArea.setRows(20);

        JScrollPane scrollPane = new JScrollPane(eventLogTextArea);
        scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        scrollPane.setMaximumSize(new Dimension(Short.MAX_VALUE, scrollPane.getPreferredSize().height));
        this.add(scrollPane);

        // panel for all buttons
        JPanel buttonContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        // import button
        importButton = createButton(IMPORT_BUTTON);
        importButton.setVisible(false);

        // import other button
        importOtherButton = createButton(IMPORT_OTHER_BUTTON);
        importOtherButton.setVisible(false);

        // submit button
        JButton submitButton = createButton(SUBMIT_BUTTON);

        // import button action
        importButton.addActionListener(e -> importLatestEventFile());

        // import other button action
        importOtherButton.addActionListener(e -> importOtherEventFile());

        // submit button action
        submitButton.addActionListener(e -> executor.submit(() -> validateEventLog(eventLogTextArea.getText())));

        buttonPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        buttonPanel.add(importButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(importOtherButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        buttonPanel.add(submitButton);
        buttonContainer.add(buttonPanel);
        this.add(buttonContainer);
    }

    /**
     * VALIDATION FUNCTIONS
     */

    /**
     * Check for event name, host, clan membership in log
     * 
     * @param eventLog
     *                     String raw event log input
     */
    private void validateEventLog(String eventLog)
    {
        // normalize line breaks to fix spacing
        eventLog = SithClanUtil.normalizeLineEndings(eventLog);

        // check webhook URL configured
        String webhookUrl = config.eventLogWebhook();
        if (webhookUrl == null || webhookUrl.isBlank())
        {
            log.warn("Event log submission failed: no webhook URL configured.");
            SwingUtilities.invokeLater(() -> showError(NO_WEBHOOK_URL_WARNING));
            return;
        }

        // validate webhook URL
        if (!webhookUrl.startsWith(DISCORD_WEBHOOK_PREFIX))
        {
            log.warn("Event log submission failed: invalid webhook URL '{}'", webhookUrl);
            SwingUtilities.invokeLater(() -> showError(INVALID_WEBHOOK_URL));
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
            String response = sendEventLogToDiscord(message);
            if (response == null)
            {
                SwingUtilities.invokeLater(() -> showError(POST_FAILURE));
                return;
            }
        }
        log.info("Event log posted to Discord successfully ({} message(s)).", messages.size());
        SwingUtilities.invokeLater(() ->
        {
            eventLogTextArea.setText(TEXT_AREA_DEFAULT);
            statusLabel.setText(POST_SUCCESS);
            SithClanUtil.statusTimer(statusLabel);
        });
    }

    /**
     * Validate event name and event host
     * 
     * @param eventLog
     *                     String[] raw event log
     * @return boolean is header correct
     */
    private boolean validateHeader(String[] eventLog)
    {
        String eventName = "";
        String eventHost = "";

        for (String line : eventLog)
        {
            if (!eventName.isBlank() && !eventHost.isBlank())
            {
                break;
            }
            if (line.startsWith(EVENT_NAME_PREFIX))
            {
                eventName = line.substring(EVENT_NAME_PREFIX.length()).replace("\r", "").trim();
                continue;
            }
            if (line.startsWith(EVENT_HOST_PREFIX))
            {
                eventHost = line.substring(EVENT_HOST_PREFIX.length()).replace("\r", "").trim();
                continue;
            }
        }

        // verify event name
        if (eventName.isBlank())
        {
            log.warn("Event log validation failed: missing event name.");
            SwingUtilities.invokeLater(() -> showError(NO_EVENT_NAME_WARNING));
            return false;
        }

        // verify event host
        if (eventHost.isBlank())
        {
            log.warn("Event log validation failed: missing event host.");
            SwingUtilities.invokeLater(() -> showError(NO_EVENT_HOST_WARNING));
            return false;
        }

        return true;
    }

    /**
     * Validate members in clan
     * 
     * @param eventLog
     *                     String[] raw event log
     * @return boolean are all members in clan
     */
    private boolean validateMembers(String[] eventLog)
    {
        // load roster if not cached
        if (memberRoster.getRoster().isEmpty())
        {
            int status = memberRoster.parseRosterFromGet();

            if (status != SithClanConstants.STATUS_OK)
            {
                SwingUtilities.invokeLater(() -> showError(ROSTER_ERROR_WARNING));
                return false;
            }
        }

        // find members in table
        boolean inTable = false;
        ArrayList<String> nonMembers = new ArrayList<>();

        for (String line : eventLog)
        {
            // blank line ends the current table section
            if (line.isBlank())
            {
                inTable = false;
                continue;
            }

            // table header line arms inTable
            if (line.trim().startsWith(TABLE_HEADER.trim()))
            {
                inTable = true;
                continue;
            }

            if (!inTable)
            {
                continue;
            }

            // closing code fence ends the table
            if (line.trim().equals("```"))
            {
                break;
            }

            // skip lines that aren't member rows (e.g. "Below Threshold (10:00)", dashes)
            if (!line.contains("|"))
            {
                continue;
            }

            // extract player name
            String memberName = line.substring(0, line.indexOf("|")).trim();
            if (memberRoster.getMemberByName(memberName) == null)
            {
                nonMembers.add(memberName);
            }
        }

        // report non-members
        if (!nonMembers.isEmpty())
        {
            log.warn("Event log validation failed: non-members found: {}", nonMembers);
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
            singleMessage.add(eventLog + "\n*Sent from Sith Clan RuneLite Plugin, message 1 of 1*");
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
     *                          String event log header
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
        message.append("\n\n*Sent from Sith Clan RuneLite Plugin, message ").append(messageNumber).append(" of ")
                .append(totalMessages).append("*");
        return message.toString();
    }

    /**
     * MISC FUNCTIONS
     */

    /**
     * Create and send HTTP POST request to Discord webhook
     * 
     * @param message
     *                    String content to post
     * @return String HTTP Response body
     */
    private String sendEventLogToDiscord(String message)
    {
        // create JSON string
        JsonObject body = new JsonObject();
        body.addProperty("content", message);
        String jsonBody = body.toString();

        // build HTTP POST request
        Request request = new Request.Builder()
                .url(config.eventLogWebhook())
                .post(RequestBody.create(MediaType.parse("application/json"), jsonBody))
                .build();

        String response = SithClanUtil.executeRequest(httpClient, request);
        if (response == null)
        {
            log.error("Failed to post event log to Discord.");
        }
        return response;
    }

    /**
     * Helper function to update status label with error message
     * 
     * @param message
     *                    String error message to show
     */
    private void showError(String message)
    {
        statusLabel.setForeground(ColorScheme.BRAND_ORANGE);
        statusLabel.setText(message);
        SithClanUtil.statusTimer(statusLabel);
    }

    /**
     * Import the data from the latest Clan Event Attendance file
     */
    private void importLatestEventFile()
    {
        // read file and save to string
        String result = fileManager.readLatestAttendanceFile();
        handleImportResult(result);
    }

    /**
     * Import the data from a specific Clan Event Attendance file
     */
    private void importOtherEventFile()
    {
        // get clan event attendance plugin directory
        File eventLogDirectory = new File(RuneLite.RUNELITE_DIR, SithClanConstants.CLAN_EVENT_ATTENDANCE_DIR);

        // open file chooser for user, only txt files
        JFileChooser chooser = new JFileChooser(eventLogDirectory.exists() ? eventLogDirectory : null);
        chooser.setDialogTitle(SELECT_EVENT_LOG_FILE);
        chooser.setFileFilter(new FileNameExtensionFilter(FILE_FILTER_DESCRIPTION, EVENT_LOG_FILE_EXTENSION));

        // handle X or cancel
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
        {
            return;
        }

        // read file and save to string
        String result = fileManager.readAttendanceFile(chooser.getSelectedFile());
        handleImportResult(result);
    }

    /**
     * Handle result of event log file import
     * 
     * @param result
     *                   String event log to handle
     */
    private void handleImportResult(String result)
    {
        if (result == null)
        {
            showError(IMPORT_NO_FILE_WARNING);
            return;
        }

        // add log to text area
        SwingUtilities.invokeLater(() -> eventLogTextArea.setText(result));
    }

    /**
     * Hide or show import event log buttons based on config
     * 
     * @param enabled
     *                    boolean hide or show buttons
     */
    public void setImportEnabled(boolean enabled)
    {
        importButton.setVisible(enabled);
        importOtherButton.setVisible(enabled);
    }

    /**
     * Helper method to create buttons
     * 
     * @param buttonText
     *                       String text to go on button
     * @return JButton created button
     */
    private JButton createButton(String buttonText)
    {
        JButton button = new JButton(buttonText);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(Short.MAX_VALUE, button.getPreferredSize().height));
        return button;
    }
}
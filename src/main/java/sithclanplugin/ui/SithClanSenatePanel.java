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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;
import sithclanplugin.eventschedule.SithClanEventSchedule;
import sithclanplugin.members.SithClanMemberRoster;
import sithclanplugin.util.SithClanConstants;
import sithclanplugin.util.SithClanUtil;

// refactored june 15

@Slf4j
@Singleton
public class SithClanSenatePanel extends JPanel
{
    @Inject
    private ScheduledExecutorService executor;

    @Inject
    private SithClanEventSchedule eventSchedule;

    @Inject
    private SithClanMemberRoster memberRoster;

    @SuppressWarnings("unused")
    private final SithClanAnnouncementsPanelSenate announcementsPanelSenate;
    private final JPanel statusPanel;
    private final JLabel statusLabel;
    private final JTextArea senatePostScheduleTextArea;
    private final JTextArea senatePostRosterTextArea;

    private static final String SENATE_OPTIONS_LABEL = "Senate Options";
    private static final String UPDATE_BUTTON = "Update";
    private static final String UPDATE_SCHEDULE_LABEL = "Post Event Schedule";
    private static final String EVENT_TEXT_AREA_DEFAULT = "Post Event Schedule Here";
    private static final String UPDATE_ROSTER_LABEL = "Post Member Roster";
    private static final String ROSTER_TEXT_AREA_DEFAULT = "Post Member Roster Here";
    private static final String UPLOADING = "Uploading...";
    private static final String SUCCESSFUL_POST = "Posted successfully.";
    private static final String BAD_INPUT_WARNING = "There is a problem with your input.";
    private static final String ERROR_WARNING = "Unable to post.";

    @Inject
    SithClanSenatePanel(SithClanAnnouncementsPanelSenate announcementsPanelSenate)
    {
        this.announcementsPanelSenate = announcementsPanelSenate;

        final Icon rightArrowIcon = new ImageIcon(
                ImageUtil.loadImageResource(getClass(), SithClanConstants.ARROW_RIGHT_PATH));
        final Icon downArrowIcon = new ImageIcon(
                ImageUtil.loadImageResource(getClass(), SithClanConstants.ARROW_DOWN_PATH));

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));

        // title panel label at top
        JLabel senatePanelLabel = new JLabel(SENATE_OPTIONS_LABEL);
        senatePanelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.add(senatePanelLabel);
        this.add(Box.createRigidArea(new Dimension(0, 10)));

        // status label panel and label
        statusLabel = SithClanUtil.createStatusLabel();
        statusPanel = SithClanUtil.createStatusPanel(statusLabel);
        this.add(statusPanel);

        // post event schedule functionality
        senatePostScheduleTextArea = new JTextArea();
        JButton senatePostScheduleButton = new JButton(UPDATE_BUTTON);
        senatePostScheduleButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // post event schedule section
        this.add(createCollapsiblePanel(UPDATE_SCHEDULE_LABEL, EVENT_TEXT_AREA_DEFAULT, senatePostScheduleTextArea,
                senatePostScheduleButton, rightArrowIcon, downArrowIcon));

        // post event schedule action
        senatePostScheduleButton.addActionListener(e ->
        {
            statusLabel.setText(UPLOADING);
            executor.submit(() ->
            {
                int status = eventSchedule.parseScheduleForPost(senatePostScheduleTextArea.getText());
                SwingUtilities.invokeLater(() ->
                {
                    handlePostStatus(status, senatePostScheduleTextArea, EVENT_TEXT_AREA_DEFAULT);
                });
            });
        });

        this.add(Box.createRigidArea(new Dimension(0, 10)));

        // post member roster functionality
        senatePostRosterTextArea = new JTextArea();
        JButton senatePostRosterButton = new JButton(UPDATE_BUTTON);
        senatePostRosterButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // post member roster section
        this.add(createCollapsiblePanel(UPDATE_ROSTER_LABEL, ROSTER_TEXT_AREA_DEFAULT, senatePostRosterTextArea,
                senatePostRosterButton, rightArrowIcon, downArrowIcon));

        // post member roster action
        senatePostRosterButton.addActionListener(e ->
        {
            statusLabel.setText(UPLOADING);
            executor.submit(() ->
            {
                int status = memberRoster.parseRosterForPost(senatePostRosterTextArea.getText());
                SwingUtilities.invokeLater(() ->
                {
                    handlePostStatus(status, senatePostRosterTextArea, ROSTER_TEXT_AREA_DEFAULT);
                });
            });
        });

        this.add(Box.createRigidArea(new Dimension(0, 10)));

        // post announcements functionality
        this.add(announcementsPanelSenate);
    }

    /**
     * Create collapsible text area panel
     * 
     * @param labelText
     *                        String text of collapsible panel label
     * @param defaultText
     *                        String default text of text area
     * @param textArea
     *                        JTextArea text area to put text for posting
     * @param button
     *                        JButton button to post text
     * @param rightIcon
     *                        Icon right arrow to display collapsed state
     * @param downIcon
     *                        Icon down arrow to display expanded state
     * @return JPanel created collapsible panel
     */
    private JPanel createCollapsiblePanel(String labelText, String defaultText, JTextArea textArea,
            JButton button, Icon rightIcon, Icon downIcon)
    {
        // main panel container
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        // setup text area
        textArea.setText(defaultText);
        textArea.setRows(20);
        textArea.setLineWrap(false);

        // highlights all text when box focused
        SithClanUtil.attachSelectAllOnFocus(textArea);

        JScrollPane scrollPane = new JScrollPane(textArea);

        // setup label
        JLabel panelLabel = new JLabel(labelText);
        panelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelLabel.setIcon(rightIcon);
        panelLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panelLabel.setOpaque(true);
        panelLabel
                .setMaximumSize(new Dimension(Short.MAX_VALUE, panelLabel.getPreferredSize().height));

        // setup main panel
        JPanel collapsiblePanel = new JPanel();
        collapsiblePanel.setLayout((new BoxLayout(collapsiblePanel, BoxLayout.Y_AXIS)));
        collapsiblePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        collapsiblePanel.add(scrollPane);
        collapsiblePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        collapsiblePanel.add(button);
        collapsiblePanel.setVisible(false);

        // expand/collapse panel
        panelLabel.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                boolean isVisible = !collapsiblePanel.isVisible();
                collapsiblePanel.setVisible(isVisible);
                // change arrow icon
                panelLabel.setIcon(isVisible ? downIcon : rightIcon);
                revalidate();
                repaint();
            }
        });

        container.add(panelLabel);
        container.add(collapsiblePanel);

        return container;
    }

    /**
     * Handle response status of post event
     * 
     * @param statusCode
     *                        int returned status code of post
     * @param textArea
     *                        JTextArea text area where post came from to reset to
     *                        default
     * @param defaultText
     *                        String default text for text area
     */
    private void handlePostStatus(int statusCode, JTextArea textArea, String defaultText)
    {
        switch (statusCode)
        {
            case SithClanConstants.STATUS_OK:
                log.debug("Post completed successfully");
                statusLabel.setText(SUCCESSFUL_POST);
                textArea.setText(defaultText);
                SithClanUtil.statusTimer(statusLabel);
                break;
            case SithClanConstants.STATUS_BAD_INPUT:
                log.warn("Post rejected due to bad input");
                statusLabel.setForeground(ColorScheme.BRAND_ORANGE);
                statusLabel.setText(BAD_INPUT_WARNING);
                SithClanUtil.statusTimer(statusLabel);
                break;
            case SithClanConstants.STATUS_NOT_FOUND:
            default:
                log.error("Post failed with status code: {}", statusCode);
                statusLabel.setForeground(ColorScheme.BRAND_ORANGE);
                statusLabel.setText(ERROR_WARNING);
                SithClanUtil.statusTimer(statusLabel);
                break;
        }
    }
}
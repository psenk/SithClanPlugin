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
import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.BorderFactory;
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
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import sithclanplugin.announcements.SithClanAnnouncement;
import sithclanplugin.announcements.SithClanAnnouncements;
import sithclanplugin.util.SithClanConstants;
import sithclanplugin.util.SithClanUtil;

// refactored on june 16

@Slf4j
@Singleton
public class SithClanAnnouncementsPanelSenate extends JPanel
{
    @Inject
    private ScheduledExecutorService executor;

    @Inject
    private SithClanAnnouncements announcements;

    private final JPanel statusPanel;
    private final JLabel statusLabel;
    private final JPanel announcementsListPanel;
    private final JScrollPane announcementsScrollPane;
    private final JTextArea newAnnouncementTextArea;

    private static final String ANNOUNCEMENTS_LABEL = "Update Announcements";
    private static final String ADD_NEW_ANNOUNCEMENT = "Add New";
    private static final String NEW_ANNOUNCEMENT_DEFAULT_TEXT = "Type Announcement Here";
    private static final String POST_ANNOUNCEMENT_BUTTON = "Post";
    private static final String CANCEL_ANNOUNCEMENT_BUTTON = "Cancel";
    private static final String EDIT_ANNOUNCEMENT_BUTTON = "Edit";
    private static final String SAVE_ANNOUNCEMENT_BUTTON = "Save";
    private static final String DELETE_ANNOUNCEMENT_BUTTON = "Delete";
    private static final String ANNOUNCEMENT_POSTED = "Announcement Posted.";
    private static final String ANNOUNCEMENT_UPDATED = "Announcement Updated.";
    private static final String ANNOUNCEMENT_DELETED = "Announcement Deleted.";
    private static final String ANNOUNCEMENT_ERROR = "Announcement Error.";

    SithClanAnnouncementsPanelSenate()
    {
        final Icon rightArrowIcon = new ImageIcon(
                ImageUtil.loadImageResource(getClass(), SithClanConstants.ARROW_RIGHT_PATH));
        final Icon downArrowIcon = new ImageIcon(
                ImageUtil.loadImageResource(getClass(), SithClanConstants.ARROW_DOWN_PATH));

        // this panel
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));

        // main panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));

        // status label panel
        statusLabel = SithClanUtil.createStatusLabel();
        statusPanel = SithClanUtil.createStatusPanel(statusLabel);

        // announcements interactive label
        JLabel announcementsPanelLabel = new JLabel(ANNOUNCEMENTS_LABEL);
        announcementsPanelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        announcementsPanelLabel.setIcon(rightArrowIcon);
        announcementsPanelLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        announcementsPanelLabel.setOpaque(true);
        announcementsPanelLabel
                .setMaximumSize(new Dimension(Short.MAX_VALUE, announcementsPanelLabel.getPreferredSize().height));

        // top buttons
        JPanel topButtonPanel = new JPanel();
        topButtonPanel.setLayout(new BoxLayout(topButtonPanel, BoxLayout.X_AXIS));
        JButton addNewButton = new JButton(ADD_NEW_ANNOUNCEMENT);

        topButtonPanel.add(addNewButton);

        // panel displaying all announcements
        announcementsListPanel = new JPanel();
        announcementsListPanel.setLayout(new BoxLayout(announcementsListPanel, BoxLayout.Y_AXIS));
        announcementsListPanel.setPreferredSize(
                new Dimension(PluginPanel.PANEL_WIDTH - 10, announcementsListPanel.getPreferredSize().height));

        // scroll pane for announcements
        announcementsScrollPane = new JScrollPane(announcementsListPanel);
        announcementsScrollPane.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 10, 300));
        announcementsScrollPane.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1,
                ColorScheme.BORDER_COLOR));
        announcementsScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        announcementsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // panel for new announcements
        JPanel newAnnouncementPanel = new JPanel();
        newAnnouncementPanel.setLayout(new BoxLayout(newAnnouncementPanel, BoxLayout.Y_AXIS));
        newAnnouncementTextArea = new JTextArea(NEW_ANNOUNCEMENT_DEFAULT_TEXT);
        newAnnouncementTextArea.setRows(1);
        newAnnouncementTextArea.setLineWrap(true);
        newAnnouncementTextArea.setWrapStyleWord(true);

        // highlights all text when box focused
        SithClanUtil.attachSelectAllOnFocus(newAnnouncementTextArea);

        // panel for new announcement buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        JButton postAnnouncementButton = new JButton(POST_ANNOUNCEMENT_BUTTON);
        postAnnouncementButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton cancelAnnouncementButton = new JButton(CANCEL_ANNOUNCEMENT_BUTTON);
        cancelAnnouncementButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        buttonPanel.add(postAnnouncementButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPanel.add(cancelAnnouncementButton);

        newAnnouncementPanel.add(newAnnouncementTextArea);
        newAnnouncementPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        newAnnouncementPanel.add(buttonPanel);
        newAnnouncementPanel.setVisible(false);

        // setup collapsible panel
        JPanel announcementsCollapsiblePanel = new JPanel();
        announcementsCollapsiblePanel.setLayout((new BoxLayout(announcementsCollapsiblePanel, BoxLayout.Y_AXIS)));
        announcementsCollapsiblePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        announcementsCollapsiblePanel.add(statusPanel);
        announcementsCollapsiblePanel.add(topButtonPanel);
        announcementsCollapsiblePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        announcementsCollapsiblePanel.add(newAnnouncementPanel);
        announcementsCollapsiblePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        announcementsCollapsiblePanel.add(announcementsScrollPane);
        announcementsCollapsiblePanel.setVisible(false);

        // expand/collapse panel
        announcementsPanelLabel.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                boolean isVisible = !announcementsCollapsiblePanel.isVisible();
                announcementsCollapsiblePanel.setVisible(isVisible);
                // change arrow icon
                announcementsPanelLabel.setIcon(isVisible ? downArrowIcon : rightArrowIcon);
                if (isVisible)
                {
                    displayAnnouncements(announcements.getAnnouncementsList());
                }
                revalidate();
                repaint();
            }
        });

        // add new announcement
        addNewButton.addActionListener(e ->
        {
            newAnnouncementPanel.setVisible(true);
            announcementsListPanel.revalidate();
            announcementsListPanel.repaint();
        });

        // post announcement action listener
        postAnnouncementButton.addActionListener(e ->
        {
            executor.submit(() ->
            {
                int status = announcements.parseAnnouncementForPost(newAnnouncementTextArea.getText());
                SwingUtilities.invokeLater(() ->
                {
                    handleAnnouncementStatus(status);
                    displayAnnouncements(announcements.getAnnouncementsList());
                    newAnnouncementPanel.setVisible(false);
                });
            });
        });

        // cancel announcement action listener
        cancelAnnouncementButton.addActionListener(e ->
        {
            newAnnouncementTextArea.setText(NEW_ANNOUNCEMENT_DEFAULT_TEXT);
            newAnnouncementPanel.setVisible(false);
            announcementsListPanel.revalidate();
            announcementsListPanel.repaint();
        });

        mainPanel.add(announcementsPanelLabel);
        mainPanel.add(announcementsCollapsiblePanel);
        this.add(mainPanel);
    }

    /**
     * CREATE FUNCTIONS
     */

    /**
     * Create single announcement panel for display
     * Allow edit and delete of announcements
     * 
     * @param announcement
     *                         SithClanAnnouncement announcement to display
     * @return JPanel single announcement panel
     */
    private JPanel createSingleAnnouncement(SithClanAnnouncement announcement)
    {
        // create announcement panel
        JPanel singleAnnouncementPanel = new JPanel();
        singleAnnouncementPanel.setLayout(new BoxLayout(singleAnnouncementPanel, BoxLayout.Y_AXIS));
        singleAnnouncementPanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1,
                ColorScheme.BORDER_COLOR));

        // create text area to type announcement
        JTextArea announcementTextArea = new JTextArea(announcement.getAnnouncementText());
        announcementTextArea.setEditable(false);
        announcementTextArea.setLineWrap(true);
        announcementTextArea.setWrapStyleWord(true);
        announcementTextArea.setColumns(1);
        int textAreaWidth = PluginPanel.PANEL_WIDTH - 10;
        announcementTextArea.setSize(textAreaWidth, Short.MAX_VALUE);
        int textAreaHeight = announcementTextArea.getPreferredSize().height;
        announcementTextArea.setPreferredSize(new Dimension(textAreaWidth, textAreaHeight));
        announcementTextArea.setMaximumSize(new Dimension(textAreaWidth, textAreaHeight));
        announcementTextArea.setMinimumSize(new Dimension(textAreaWidth, textAreaHeight));

        // row of buttons, edit and delete announcements
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        JButton editAnnouncementButton = new JButton(EDIT_ANNOUNCEMENT_BUTTON);
        JButton deleteAnnouncementButton = new JButton(DELETE_ANNOUNCEMENT_BUTTON);

        // edit button action
        editAnnouncementButton.addActionListener(e ->
        {
            // edit announcement
            if (!announcementTextArea.isEditable())
            {
                announcementTextArea.setEditable(true);
                announcementTextArea.requestFocus();
                editAnnouncementButton.setText(SAVE_ANNOUNCEMENT_BUTTON);
            } else
            {
                // save announcement
                executor.submit(() ->
                {
                    int status = announcements.parseAnnouncementForEdit(announcement.getAnnouncementId(),
                            announcementTextArea.getText());
                    SwingUtilities.invokeLater(() ->
                    {
                        if (status == SithClanConstants.STATUS_OK)
                        {
                            // reset button state, lock text area
                            announcementTextArea.setEditable(false);
                            editAnnouncementButton.setText(EDIT_ANNOUNCEMENT_BUTTON);
                            displayAnnouncements(announcements.getAnnouncementsList());
                        }
                        handleAnnouncementStatus(status);
                    });
                });
            }
        });

        // delete button action
        deleteAnnouncementButton.addActionListener(e ->
        {
            executor.submit(() ->
            {
                int status = announcements.parseAnnouncementForDelete(announcement.getAnnouncementId());
                SwingUtilities.invokeLater(() ->
                {
                    handleAnnouncementStatus(status);
                    displayAnnouncements(announcements.getAnnouncementsList());
                });
            });
        });

        buttonPanel.add(editAnnouncementButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPanel.add(deleteAnnouncementButton);

        singleAnnouncementPanel.add(announcementTextArea);
        singleAnnouncementPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        singleAnnouncementPanel.add(buttonPanel);

        return singleAnnouncementPanel;
    }

    /**
     * DISPLAY FUNCTIONS
     */

    /**
     * Create and post all announcements to display
     * 
     * @param announcements
     *                          ArrayList<SithClanAnnouncement> list of
     *                          announcements to
     *                          display
     */
    private void displayAnnouncements(ArrayList<SithClanAnnouncement> announcements)
    {
        // start fresh
        announcementsListPanel.removeAll();

        // iterate through announcements and create/post each
        for (SithClanAnnouncement announcement : announcements)
        {
            JPanel announcementPanel = createSingleAnnouncement(announcement);
            announcementsListPanel.add(announcementPanel);
            announcementsListPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        }
        announcementsListPanel.revalidate();
        announcementsListPanel.repaint();

        // scroll back up
        SwingUtilities.invokeLater(() -> announcementsScrollPane.getVerticalScrollBar().setValue(0));
    }

    /**
     * MISC FUNCTIONS
     */

    /**
     * Handle response status of announcement event
     * 
     * @param statusCode
     *                       int returned status code
     */
    private void handleAnnouncementStatus(int statusCode)
    {
        switch (statusCode)
        {
            case SithClanConstants.STATUS_RESOURCE_CREATED:
                statusLabel.setText(ANNOUNCEMENT_POSTED);
                SithClanUtil.statusTimer(statusLabel);
                break;
            case SithClanConstants.STATUS_RESOURCE_DELETED:
                statusLabel.setText(ANNOUNCEMENT_DELETED);
                SithClanUtil.statusTimer(statusLabel);
                break;
            case SithClanConstants.STATUS_OK:
                statusLabel.setText(ANNOUNCEMENT_UPDATED);
                SithClanUtil.statusTimer(statusLabel);
                break;
            case SithClanConstants.STATUS_BAD_INPUT:
            case SithClanConstants.STATUS_NOT_FOUND:
            default:
                log.error("Announcement action failed.");
                statusLabel.setForeground(ColorScheme.BRAND_ORANGE);
                statusLabel.setText(ANNOUNCEMENT_ERROR);
                SithClanUtil.statusTimer(statusLabel);
                break;
        }
    }
}

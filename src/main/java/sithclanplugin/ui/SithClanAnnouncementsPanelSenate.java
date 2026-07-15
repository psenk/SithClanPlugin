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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

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

// refactored on july 3

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
    private final JTextArea createAnnouncementTextArea;

    private static final String ANNOUNCEMENTS_LABEL = "Update Announcements";
    private static final String NEW_ANNOUNCEMENT = "Add New";
    private static final String REFRESH_ANNOUNCEMENTS = "Refresh";
    private static final String NEW_ANNOUNCEMENT_DEFAULT_TEXT = "Type announcement here";
    private static final String POST_ANNOUNCEMENT_BUTTON = "Post";
    private static final String CANCEL_ANNOUNCEMENT_BUTTON = "Cancel";
    private static final String EDIT_ANNOUNCEMENT_BUTTON = "Edit";
    private static final String SAVE_ANNOUNCEMENT_BUTTON = "Save";
    private static final String DELETE_ANNOUNCEMENT_BUTTON = "Delete";
    private static final String ANNOUNCEMENT_POSTED = "Announcement Posted.";
    private static final String ANNOUNCEMENT_UPDATED = "Announcement Updated.";
    private static final String ANNOUNCEMENT_DELETED = "Announcement Deleted.";
    private static final String ANNOUNCEMENT_ERROR = "Announcement Error.";
    private static final int SCROLL_PANE_HEIGHT = 300;

    SithClanAnnouncementsPanelSenate()
    {
        // for dropdown label ui
        final Icon rightArrowIcon = new ImageIcon(
                ImageUtil.loadImageResource(getClass(), SithClanConstants.ARROW_RIGHT_PATH));
        final Icon downArrowIcon = new ImageIcon(
                ImageUtil.loadImageResource(getClass(), SithClanConstants.ARROW_DOWN_PATH));

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // shows status and error messages
        statusLabel = SithClanUtil.createStatusLabel();
        statusPanel = SithClanUtil.createStatusPanel(statusLabel);

        // announcements dropdown label
        JLabel announcementsPanelLabel = SithClanUtil.createCollapsibleLabel(ANNOUNCEMENTS_LABEL, rightArrowIcon);

        // button container
        JPanel topButtonPanel = new JPanel();
        topButtonPanel.setLayout(new BoxLayout(topButtonPanel, BoxLayout.Y_AXIS));

        JButton newAnnouncementButton = new JButton(NEW_ANNOUNCEMENT);
        newAnnouncementButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton refreshAnnouncementsButton = new JButton(REFRESH_ANNOUNCEMENTS);
        refreshAnnouncementsButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        topButtonPanel.add(newAnnouncementButton);
        topButtonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        topButtonPanel.add(refreshAnnouncementsButton);

        // panel and scroll pane to display all announcements
        announcementsListPanel = new JPanel();
        announcementsListPanel.setLayout(new BoxLayout(announcementsListPanel, BoxLayout.Y_AXIS));
        announcementsScrollPane = SithClanUtil.createScrollPane(announcementsListPanel, SCROLL_PANE_HEIGHT);

        // create new announcement panel
        JPanel createAnnouncementPanel = new JPanel();
        createAnnouncementPanel.setLayout(new BoxLayout(createAnnouncementPanel, BoxLayout.Y_AXIS));
        createAnnouncementTextArea = new JTextArea(NEW_ANNOUNCEMENT_DEFAULT_TEXT);
        createAnnouncementTextArea.setRows(1);
        createAnnouncementTextArea.setLineWrap(true);
        createAnnouncementTextArea.setWrapStyleWord(true);

        // highlights all text when box focused
        SithClanUtil.attachSelectAllOnFocus(createAnnouncementTextArea);

        // create announcement button panel (post and cancel)
        JPanel createAnnouncementButtonPanel = new JPanel();
        createAnnouncementButtonPanel.setLayout(new BoxLayout(createAnnouncementButtonPanel, BoxLayout.X_AXIS));

        JButton postAnnouncementButton = new JButton(POST_ANNOUNCEMENT_BUTTON);
        postAnnouncementButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton cancelAnnouncementButton = new JButton(CANCEL_ANNOUNCEMENT_BUTTON);
        cancelAnnouncementButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        createAnnouncementButtonPanel.add(postAnnouncementButton);
        createAnnouncementButtonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        createAnnouncementButtonPanel.add(cancelAnnouncementButton);

        createAnnouncementPanel.add(createAnnouncementTextArea);
        createAnnouncementPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        createAnnouncementPanel.add(createAnnouncementButtonPanel);
        createAnnouncementPanel.setVisible(false);

        // setup collapsible panel under dropdown
        JPanel announcementsCollapsiblePanel = new JPanel();
        announcementsCollapsiblePanel.setLayout((new BoxLayout(announcementsCollapsiblePanel, BoxLayout.Y_AXIS)));
        announcementsCollapsiblePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        announcementsCollapsiblePanel.add(statusPanel);
        announcementsCollapsiblePanel.add(topButtonPanel);
        announcementsCollapsiblePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        announcementsCollapsiblePanel.add(createAnnouncementPanel);
        announcementsCollapsiblePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        announcementsCollapsiblePanel.add(announcementsScrollPane);
        announcementsCollapsiblePanel.setVisible(false);

        // expand/collapse panel action
        announcementsPanelLabel.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                boolean isVisible = !announcementsCollapsiblePanel.isVisible();
                announcementsCollapsiblePanel.setVisible(isVisible);
                announcementsPanelLabel.setIcon(isVisible ? downArrowIcon : rightArrowIcon);
                if (isVisible)
                {
                    displayAnnouncements(announcements.getAnnouncementsList());
                }
                revalidate();
                repaint();
            }
        });

        newAnnouncementButton.addActionListener(e ->
        {
            createAnnouncementPanel.setVisible(true);
            announcementsListPanel.revalidate();
            announcementsListPanel.repaint();
        });

        refreshAnnouncementsButton.addActionListener(e ->
        {
            executor.submit(() ->
            {
                int status = announcements.parseAnnouncementsFromGet();
                SwingUtilities.invokeLater(() ->
                {
                    handleAnnouncementStatus(status);
                    displayAnnouncements(announcements.getAnnouncementsList());
                });
            });
        });

        postAnnouncementButton.addActionListener(e ->
        {
            executor.submit(() ->
            {
                int status = announcements.parseAnnouncementForPost(createAnnouncementTextArea.getText());
                SwingUtilities.invokeLater(() ->
                {
                    handleAnnouncementStatus(status);
                    displayAnnouncements(announcements.getAnnouncementsList());
                    createAnnouncementPanel.setVisible(false);
                });
            });
        });

        cancelAnnouncementButton.addActionListener(e ->
        {
            createAnnouncementTextArea.setText(NEW_ANNOUNCEMENT_DEFAULT_TEXT);
            createAnnouncementPanel.setVisible(false);
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
     * Allow edit and deletion of announcements
     * 
     * @param announcement
     *                         SithClanAnnouncement announcement to display
     * @return JPanel panel containing single announcement
     */
    private JPanel createSingleAnnouncement(SithClanAnnouncement announcement)
    {
        // create announcement panel
        JPanel singleAnnouncementPanel = new JPanel();
        singleAnnouncementPanel.setLayout(new BoxLayout(singleAnnouncementPanel, BoxLayout.Y_AXIS));
        singleAnnouncementPanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1,
                ColorScheme.BORDER_COLOR));

        // save announcement text for cancel
        String originalAnnouncementText = announcement.getAnnouncementText();

        // create text area to type announcement
        JTextArea announcementTextArea = new JTextArea(originalAnnouncementText);
        announcementTextArea.setEditable(false);
        announcementTextArea.setLineWrap(true);
        announcementTextArea.setWrapStyleWord(true);
        announcementTextArea.setColumns(1);
        lockTextAreaHeight(announcementTextArea);

        // button container (edit and delete)
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        JButton editAnnouncementButton = new JButton(EDIT_ANNOUNCEMENT_BUTTON);
        JButton deleteAnnouncementButton = new JButton(DELETE_ANNOUNCEMENT_BUTTON);

        editAnnouncementButton.addActionListener(e ->
        {

            // edit announcement
            if (!announcementTextArea.isEditable())
            {
                announcementTextArea.setEditable(true);
                announcementTextArea.requestFocus();
                editAnnouncementButton.setText(SAVE_ANNOUNCEMENT_BUTTON);
                deleteAnnouncementButton.setText(CANCEL_ANNOUNCEMENT_BUTTON);
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
                            deleteAnnouncementButton.setText(DELETE_ANNOUNCEMENT_BUTTON);
                            displayAnnouncements(announcements.getAnnouncementsList());
                        }
                        handleAnnouncementStatus(status);
                    });
                });
            }
        });

        deleteAnnouncementButton.addActionListener(e ->
        {
            // cancel edit action
            if (announcementTextArea.isEditable())
            {
                announcementTextArea.setText(originalAnnouncementText);
                announcementTextArea.setEditable(false);
                editAnnouncementButton.setText(EDIT_ANNOUNCEMENT_BUTTON);
                deleteAnnouncementButton.setText(DELETE_ANNOUNCEMENT_BUTTON);
                lockTextAreaHeight(announcementTextArea);
                singleAnnouncementPanel.revalidate();
                singleAnnouncementPanel.repaint();
            } else
            {
                // delete announcement
                executor.submit(() ->
                {
                    int status = announcements.parseAnnouncementForDelete(announcement.getAnnouncementId());
                    SwingUtilities.invokeLater(() ->
                    {
                        handleAnnouncementStatus(status);
                        displayAnnouncements(announcements.getAnnouncementsList());
                    });
                });
            }
        });

        // allows dynamic resizing of text area
        announcementTextArea.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                resize();
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                resize();
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                resize();
            }

            private void resize()
            {
                SwingUtilities.invokeLater(() ->
                {
                    lockTextAreaHeight(announcementTextArea);
                    singleAnnouncementPanel.revalidate();
                    singleAnnouncementPanel.repaint();
                });
            }
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
     *                          announcements to display
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
     * Locks size of text area
     * 
     * @param textArea
     *                     JTextArea to have size locked
     */
    private void lockTextAreaHeight(JTextArea textArea)
    {
        int textAreaWidth = PluginPanel.PANEL_WIDTH - 10;
        textArea.setPreferredSize(null);
        textArea.setSize(textAreaWidth, Short.MAX_VALUE);
        int textAreaHeight = textArea.getPreferredSize().height;
        textArea.setPreferredSize(new Dimension(textAreaWidth, textAreaHeight));
        textArea.setMaximumSize(new Dimension(textAreaWidth, textAreaHeight));
    }

    /**
     * Handle response of announcement event
     * 
     * @param statusCode
     *                       int response status code
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

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;

import com.google.common.html.HtmlEscapers;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;
import sithclanplugin.announcements.SithClanAnnouncement;
import sithclanplugin.announcements.SithClanAnnouncements;
import sithclanplugin.util.SithClanConstants;
import sithclanplugin.util.SithClanUtil;

// refactored on june 16

@Slf4j
@Singleton
public class SithClanAnnouncementsPanel extends JPanel
{

    @Inject
    private ScheduledExecutorService executor;

    @Inject
    private SithClanAnnouncements announcements;

    private final JPanel statusPanel;
    private final JLabel statusLabel;
    private final JPanel announcementsListPanel;
    private final JScrollPane announcementsScrollPane;

    private static final String ANNOUNCEMENTS_LABEL = "Clan Announcements";
    private static final String REFRESH_ANNOUNCEMENTS = "Refresh Announcements";
    private static final String NO_ANNOUNCEMENTS_LABEL = "No Announcements Currently.";
    private static final String RATE_LIMITED_WARNING = "Announcements have been retrieved too recently. Try again in a few minutes.";
    private static final String ANNOUNCEMENTS_ERROR = "Error obtaining announcements.";
    private static final int SCROLL_PANE_HEIGHT = 150;

    SithClanAnnouncementsPanel()
    {
        final Icon rightArrowIcon = new ImageIcon(
                ImageUtil.loadImageResource(getClass(), SithClanConstants.ARROW_RIGHT_PATH));
        final Icon downArrowIcon = new ImageIcon(
                ImageUtil.loadImageResource(getClass(), SithClanConstants.ARROW_DOWN_PATH));

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // collapsible panel label
        JLabel announcementsPanelLabel = SithClanUtil.createCollapsibleLabel(ANNOUNCEMENTS_LABEL, downArrowIcon);

        // collapsible panel
        JPanel collapsiblePanel = new JPanel();
        collapsiblePanel.setLayout(new BoxLayout(collapsiblePanel, BoxLayout.Y_AXIS));

        // status label panel and label
        statusLabel = SithClanUtil.createStatusLabel();
        statusPanel = SithClanUtil.createStatusPanel(statusLabel);

        // announcements list panel
        announcementsListPanel = new JPanel();
        announcementsListPanel.setLayout(new BoxLayout(announcementsListPanel, BoxLayout.Y_AXIS));
        announcementsListPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        // create scroll pane
        announcementsScrollPane = SithClanUtil.createScrollPane(announcementsListPanel, SCROLL_PANE_HEIGHT);

        // refresh announcements button
        JButton refreshButton = new JButton(REFRESH_ANNOUNCEMENTS);
        refreshButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // action for refresh announcements button
        refreshButton.addActionListener(e ->
        {
            executor.submit(() ->
            {
                int status = announcements.parseAnnouncementsFromGet();
                SwingUtilities.invokeLater(() ->
                {
                    handleAnnouncementStatus(status);
                    displayAnnouncements();
                });
            });
        });

        // action for collapsible panel
        announcementsPanelLabel.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                boolean isVisible = !collapsiblePanel.isVisible();
                collapsiblePanel.setVisible(isVisible);
                announcementsPanelLabel.setIcon(isVisible ? downArrowIcon : rightArrowIcon);
                revalidate();
                repaint();
            }
        });

        // assembling collapsible panel
        collapsiblePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        collapsiblePanel.add(statusPanel);
        collapsiblePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        collapsiblePanel.add(announcementsScrollPane);
        collapsiblePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        collapsiblePanel.add(refreshButton);
        collapsiblePanel.add(Box.createRigidArea(new Dimension(0, 5)));

        this.add(announcementsPanelLabel);
        this.add(collapsiblePanel);
        this.add(Box.createRigidArea(new Dimension(0, 5)));
        this.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.BORDER_COLOR));
    }

    /**
     * DISPLAY FUNCTIONS
     */

    /**
     * Create announcements and display
     */
    public void displayAnnouncements()
    {
        SwingUtilities.invokeLater(() ->
        {
            // start fresh
            announcementsListPanel.removeAll();

            // get current announcements
            ArrayList<SithClanAnnouncement> announcementsList = announcements.getAnnouncementsList();

            // display if no announcements
            if (announcementsList == null || announcementsList.isEmpty())
            {
                JLabel noAnnouncementsLabel = new JLabel(NO_ANNOUNCEMENTS_LABEL);
                noAnnouncementsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                announcementsListPanel.add(noAnnouncementsLabel);
            } else
            {
                // create each announcement, newest announcement first
                for (int i = announcementsList.size() - 1; i >= 0; i--)
                {
                    JEditorPane editorPane = new JEditorPane();
                    editorPane.setContentType("text/html");
                    editorPane.setText(convertLinks(announcementsList.get(i).getAnnouncementText()));
                    editorPane.setEditable(false);
                    editorPane.setOpaque(false);
                    editorPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5),
                            BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.BORDER_COLOR)));
                    editorPane.setSize(PluginPanel.PANEL_WIDTH - 35, Short.MAX_VALUE);
                    editorPane.setPreferredSize(
                            new Dimension(PluginPanel.PANEL_WIDTH - 10, editorPane.getPreferredSize().height));

                    // open links
                    editorPane.addHyperlinkListener(e ->
                    {
                        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
                        {
                            LinkBrowser.browse(e.getURL().toString());
                        }
                    });

                    // add to announcement list
                    announcementsListPanel.add(editorPane);
                    announcementsListPanel.add(Box.createRigidArea(new Dimension(0, 5)));
                }
            }
            announcementsListPanel.revalidate();
            announcementsListPanel.repaint();
        });
    }

    /**
     * MISC FUNCTIONS
     */

    /**
     * Handle response status of event
     * 
     * @param status
     *                   int returned status code
     */
    private void handleAnnouncementStatus(int status)
    {
        switch (status)
        {
            case SithClanConstants.STATUS_RATE_LIMITED:
                log.warn("Announcements fetch rate limited");
                statusLabel.setForeground(ColorScheme.BRAND_ORANGE);
                statusLabel.setText(RATE_LIMITED_WARNING);
                SithClanUtil.statusTimer(statusLabel);
                break;
            case SithClanConstants.STATUS_NOT_FOUND:
                log.error("Announcements fetch failed with status: {}", status);
                statusLabel.setForeground(ColorScheme.BRAND_ORANGE);
                statusLabel.setText(ANNOUNCEMENTS_ERROR);
                SithClanUtil.statusTimer(statusLabel);
                break;
            default:
                break;
        }
    }

    /**
     * Convert URL to clickable link
     * 
     * @param text
     *                 String input text
     * @return String output HTML link for display
     */
    private String convertLinks(String text)
    {
        String urlPattern = "(https?://\\S+)";
        String[] parts = text.split(urlPattern, -1);
        Matcher matcher = Pattern.compile(urlPattern).matcher(text);

        StringBuilder result = new StringBuilder("<html>");
        for (String part : parts)
        {
            // escape and add the non-URL text segment
            result.append(HtmlEscapers.htmlEscaper().escape(part).replace("\n", "<br>"));
            // if there's a matching URL for this gap, append it as a link
            if (matcher.find())
            {
                String url = matcher.group();
                result.append("<a href='").append(url).append("'>Click here").append("</a>");
            }
        }
        result.append("</html>");
        return result.toString();
    }
}

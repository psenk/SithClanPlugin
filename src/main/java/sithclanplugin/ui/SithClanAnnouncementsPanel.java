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
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.HyperlinkEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;
import sithclanplugin.announcements.SithClanAnnouncement;
import sithclanplugin.announcements.SithClanAnnouncements;
import sithclanplugin.util.SithClanPluginConstants;

@Singleton
public class SithClanAnnouncementsPanel extends JPanel
{
    @Inject
    private SithClanAnnouncements announcements;

    @Inject
    private ScheduledExecutorService executor;

    private final JPanel announcementsListPanel;
    private final JScrollPane announcementsScrollPane;

    private static final String ARROW_RIGHT_PATH = "/arrow_right.png";
    private static final String ARROW_DOWN_PATH = "/arrow_down.png";
    private static final String ANNOUNCEMENTS_LABEL = "Clan Announcements";
    private static final String REFRESH_ANNOUNCEMENTS = "Refresh Announcements";
    private static final String RATE_LIMITED_WARNING = "Announcements have been retrieved too recently. Try again in a few minutes.";
    private static final String NO_ANNOUNCEMENTS_LABEL = "No Announcements Currently";
    private static final String ANNOUNCEMENTS_UNOBTAINABLE = "Unable to obtain announcements.";
    private static final Border ANNOUNCEMENT_BORDER = BorderFactory.createMatteBorder(1, 1, 1, 1,
            ColorScheme.BORDER_COLOR);

    SithClanAnnouncementsPanel()
    {
        final Icon rightArrowIcon = new ImageIcon(ImageUtil.loadImageResource(getClass(), ARROW_RIGHT_PATH));
        final Icon downArrowIcon = new ImageIcon(ImageUtil.loadImageResource(getClass(), ARROW_DOWN_PATH));

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // collapsible panel label
        JLabel announcementsPanelLabel = new JLabel(ANNOUNCEMENTS_LABEL);
        announcementsPanelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        announcementsPanelLabel.setIcon(downArrowIcon);
        announcementsPanelLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        announcementsPanelLabel.setOpaque(true);
        announcementsPanelLabel
                .setMaximumSize(new Dimension(Integer.MAX_VALUE, announcementsPanelLabel.getPreferredSize().height));

        // collapsible panel
        JPanel collapsiblePanel = new JPanel();
        collapsiblePanel.setLayout(new BoxLayout(collapsiblePanel, BoxLayout.Y_AXIS));
        collapsiblePanel.setVisible(true);

        // announcements list panel
        announcementsListPanel = new JPanel();
        announcementsListPanel.setLayout(new BoxLayout(announcementsListPanel, BoxLayout.Y_AXIS));

        announcementsScrollPane = new JScrollPane(announcementsListPanel);
        announcementsScrollPane.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 10, 150));
        announcementsScrollPane.setBorder(ANNOUNCEMENT_BORDER);
        announcementsScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        announcementsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

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
        collapsiblePanel.add(announcementsScrollPane);
        collapsiblePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        collapsiblePanel.add(refreshButton);
        collapsiblePanel.add(Box.createRigidArea(new Dimension(0, 5)));

        this.add(announcementsPanelLabel);
        this.add(collapsiblePanel);
        this.add(Box.createRigidArea(new Dimension(0, 10)));
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
                // create each announcement
                for (SithClanAnnouncement announcement : announcementsList)
                {
                    JEditorPane editorPane = new JEditorPane();
                    editorPane.setContentType("text/html");
                    editorPane.setText(convertLinks(announcement.getAnnouncementText()));
                    editorPane.setEditable(false);
                    editorPane.setOpaque(false);
                    editorPane.setBorder(ANNOUNCEMENT_BORDER);
                    editorPane.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH, 60));
                    editorPane.setMinimumSize(new Dimension(PluginPanel.PANEL_WIDTH, 60));

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
     * @param statusCode
     *                       int returned status code
     */
    private void handleAnnouncementStatus(int statusCode)
    {
        switch (statusCode)
        {
            case SithClanPluginConstants.STATUS_RATE_LIMITED:
                JOptionPane.showMessageDialog(null, RATE_LIMITED_WARNING);
                break;
            case SithClanPluginConstants.STATUS_NOT_FOUND:
                JOptionPane.showMessageDialog(null, ANNOUNCEMENTS_UNOBTAINABLE);
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
        // detect urls
        String urlPattern = "(https?://\\S+)";

        // replace characters
        String escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        String withBreaks = escaped.replace("\n", "<br>");

        // add HTML tags
        String withLinks = withBreaks.replaceAll(urlPattern, "<a href='$1'>$1</a>");
        return "<html>" + withLinks + "</html>";
    }
}

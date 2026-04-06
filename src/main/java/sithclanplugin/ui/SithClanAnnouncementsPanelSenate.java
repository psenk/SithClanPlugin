package sithclanplugin.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import sithclanplugin.announcements.SithClanAnnouncement;
import sithclanplugin.announcements.SithClanAnnouncements;
import sithclanplugin.util.SithClanPluginConstants;

@Singleton
public class SithClanAnnouncementsPanelSenate extends JPanel
{
    @Inject
    private ScheduledExecutorService executor;

    @Inject
    private SithClanAnnouncements announcements;

    private final JPanel announcementsListPanel;
    private final JTextArea newAnnouncementTextArea;
    private final JScrollPane announcementsScrollPane;

    private static final String ARROW_RIGHT_PATH = "/arrow_right.png";
    private static final String ARROW_DOWN_PATH = "/arrow_down.png";
    private static final String ANNOUNCEMENTS_LABEL = "Update Announcements";
    private static final String ADD_NEW_ANNOUNCEMENT = "Add New";
    private static final String NEW_ANNOUNCEMENT_DEFAULT_TEXT = "Type Announcement Here";
    private static final String POST_ANNOUNCEMENT_BUTTON = "Post";
    private static final String CANCEL_ANNOUNCEMENT_BUTTON = "Cancel";
    private static final String EDIT_ANNOUNCEMENT_BUTTON = "Edit";
    private static final String SAVE_ANNOUNCEMENT_BUTTON = "Save";
    private static final String DELETE_ANNOUNCEMENT_BUTTON = "Delete";
    private static final String SUCCESSFUL_POST = "Announcement Updated";
    private static final String RESOURCE_CREATED = "Announcement Created";
    private static final String BAD_INPUT_WARNING = "There is a problem with your input.";
    private static final String NOT_FOUND_WARNING = "Unable to post.";
    private static final Border ANNOUNCEMENT_BORDER = BorderFactory.createMatteBorder(1, 1, 1, 1,
            ColorScheme.BORDER_COLOR);

    SithClanAnnouncementsPanelSenate()
    {
        final Icon rightArrowIcon = new ImageIcon(ImageUtil.loadImageResource(getClass(), ARROW_RIGHT_PATH));
        final Icon downArrowIcon = new ImageIcon(ImageUtil.loadImageResource(getClass(), ARROW_DOWN_PATH));

        // this panel
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // main panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // announcements interactive label
        JLabel announcementsPanelLabel = new JLabel(ANNOUNCEMENTS_LABEL);
        announcementsPanelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        announcementsPanelLabel.setIcon(rightArrowIcon);
        announcementsPanelLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        announcementsPanelLabel.setOpaque(true);
        announcementsPanelLabel
                .setMaximumSize(new Dimension(Integer.MAX_VALUE, announcementsPanelLabel.getPreferredSize().height));

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
        announcementsScrollPane.setBorder(ANNOUNCEMENT_BORDER);
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
        newAnnouncementTextArea.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                newAnnouncementTextArea.selectAll();
            }
        });

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
        singleAnnouncementPanel
                .setPreferredSize(
                        new Dimension(PluginPanel.PANEL_WIDTH, 90));
        singleAnnouncementPanel.setMinimumSize(new Dimension(PluginPanel.PANEL_WIDTH, 90));
        singleAnnouncementPanel.setMaximumSize(new Dimension(PluginPanel.PANEL_WIDTH, 90));
        singleAnnouncementPanel.setBorder(ANNOUNCEMENT_BORDER);

        // create text area to type announcement
        JTextArea announcementTextArea = new JTextArea(announcement.getAnnouncementText());
        announcementTextArea.setEditable(false);
        announcementTextArea.setLineWrap(true);
        announcementTextArea.setWrapStyleWord(true);
        announcementTextArea.setRows(3);

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
                        if (status == SithClanPluginConstants.STATUS_OK)
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
                    if (status == SithClanPluginConstants.STATUS_OK)
                    {
                        JOptionPane.showMessageDialog(null, "Announcement Deleted");
                    } else
                    {
                        handleAnnouncementStatus(status);
                    }
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
        SwingUtilities.invokeLater(() ->
        {
            announcementsScrollPane.getVerticalScrollBar().setValue(0);
        });
    }

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
            case SithClanPluginConstants.STATUS_OK:
                JOptionPane.showMessageDialog(null,
                        SUCCESSFUL_POST);
                break;
            case SithClanPluginConstants.STATUS_RESOURCE_CREATED:
                JOptionPane.showMessageDialog(null, RESOURCE_CREATED);
                break;
            case SithClanPluginConstants.STATUS_BAD_INPUT:
                JOptionPane.showMessageDialog(null,
                        BAD_INPUT_WARNING);
                break;
            case SithClanPluginConstants.STATUS_NOT_FOUND:
                JOptionPane.showMessageDialog(null,
                        NOT_FOUND_WARNING);
                break;
            default:
                break;
        }
    }
}

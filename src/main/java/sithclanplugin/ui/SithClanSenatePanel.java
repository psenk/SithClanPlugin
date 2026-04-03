package sithclanplugin.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ScheduledExecutorService;

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

import com.google.inject.Inject;
import com.google.inject.Singleton;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;
import sithclanplugin.eventschedule.SithClanEventSchedule;
import sithclanplugin.members.SithClanMemberRoster;
import sithclanplugin.util.SithClanPluginConstants;

@Singleton
public class SithClanSenatePanel extends JPanel
{
    @Inject
    private ScheduledExecutorService executor;

    @Inject
    private SithClanEventSchedule eventSchedule;

    @Inject
    private SithClanMemberRoster memberRoster;

    private final SithClanAnnouncementsPanelSenate announcementsPanelSenate;
    private final JTextArea senatePostScheduleTextArea;
    private final JTextArea senatePostRosterTextArea;
    private final JPanel statusPanel;
    private final JLabel uploadingLabel;

    private static final String SENATE_OPTIONS_LABEL = "Senate Options";
    private static final String UPDATE_SCHEDULE_LABEL = "Event Schedule";
    private static final String UPDATE_ROSTER_LABEL = "Member Roster";
    private static final String UPDATE_BUTTON = "Update";
    private static final String EVENT_TEXT_AREA_DEFAULT = "Post Event Schedule Here";
    private static final String ROSTER_TEXT_AREA_DEFAULT = "Post Member Roster Here";
    private static final String ARROW_RIGHT_PATH = "/arrow_right.png";
    private static final String ARROW_DOWN_PATH = "/arrow_down.png";
    private static final String SUCCESSFUL_POST = "Posted successfully.";
    private static final String BAD_INPUT_WARNING = "There is a problem with your input.";
    private static final String NOT_FOUND_WARNING = "Unable to post.";
    private static final String UPLOADING = "Uploading...";

    @Inject
    SithClanSenatePanel(SithClanAnnouncementsPanelSenate announcementsPanelSenate)
    {

        this.announcementsPanelSenate = announcementsPanelSenate;

        final Icon rightArrowIcon = new ImageIcon(ImageUtil.loadImageResource(getClass(), ARROW_RIGHT_PATH));
        final Icon downArrowIcon = new ImageIcon(ImageUtil.loadImageResource(getClass(), ARROW_DOWN_PATH));

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // title panel label at top
        JLabel senatePanelLabel = new JLabel(SENATE_OPTIONS_LABEL);
        senatePanelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.add(senatePanelLabel);
        this.add(Box.createRigidArea(new Dimension(0, 10)));

        // status label panel
        statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));

        // uploading schedule/roster
        uploadingLabel = new JLabel(UPLOADING);
        uploadingLabel.setVisible(false);
        uploadingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        statusPanel.add(uploadingLabel);
        statusPanel.add(Box.createRigidArea(new Dimension(0, 10)));
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
            executor.submit(() ->
            {
                uploadingLabel.setVisible(true);
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
            executor.submit(() ->
            {
                uploadingLabel.setVisible(true);
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
        textArea.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                textArea.selectAll();
            }
        });

        JScrollPane scrollPane = new JScrollPane(textArea);

        // setup label
        JLabel panelLabel = new JLabel(labelText);
        panelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelLabel.setIcon(rightIcon);
        panelLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panelLabel.setOpaque(true);
        panelLabel
                .setMaximumSize(new Dimension(Integer.MAX_VALUE, panelLabel.getPreferredSize().height));

        // setup main panel
        JPanel collapsiblePanel = new JPanel();
        collapsiblePanel.setLayout((new BoxLayout(collapsiblePanel, BoxLayout.Y_AXIS)));
        collapsiblePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        collapsiblePanel.add(scrollPane);
        collapsiblePanel.add(Box.createRigidArea(new Dimension(0, 5)));
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
            case SithClanPluginConstants.STATUS_OK:
                JOptionPane.showMessageDialog(null,
                        SUCCESSFUL_POST);
                textArea.setText(defaultText);
                uploadingLabel.setVisible(false);
                break;
            case SithClanPluginConstants.STATUS_BAD_INPUT:
                JOptionPane.showMessageDialog(null,
                        BAD_INPUT_WARNING);
                uploadingLabel.setVisible(false);
                break;
            case SithClanPluginConstants.STATUS_NOT_FOUND:
                JOptionPane.showMessageDialog(null,
                        NOT_FOUND_WARNING);
                uploadingLabel.setVisible(false);
                break;
            default:
                break;
        }
    }
}
package sithclanplugin.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import net.runelite.client.ui.ColorScheme;
import sithclanplugin.members.SithClanMember;
import sithclanplugin.members.SithClanMemberRoster;

/**
 * TODOS:
 * - post membership file function
 * - get membership file function
 * - parsing data from JSON
 * - parsing data to JSON
 * - update database schema
 * - update worker
 * - create endpoints
 * 
 * TODO: FEATURES
 * - member search
 * - show specific member info
 */

@Singleton
public class SithClanMembersPanel extends JPanel {

    @Inject
    private SithClanMemberRoster memberRoster;

    private final JTextField membersSearchTextField;
    private final JButton membersSearchButton;
    private final JButton membersShowAllButton;
    private final JPanel membersAreaPanel;

    private static final String MEMBERS_PANEL_TITLE = "Sith Member Info";
    private static final String MEMBERS_SEARCH_BUTTON = "Search Members";
    private static final String MEMBERS_SHOW_ALL_BUTTON = "Show All Members";
    private static final String MEMBERS_AREA_LABEL = "Members";

    SithClanMembersPanel() {
        this.setLayout(new BorderLayout());

        // top panel title
        JLabel membersPanelLabel = new JLabel(MEMBERS_PANEL_TITLE);
        membersPanelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // contains search area and search button
        JPanel membersSearchArea = new JPanel();
        membersSearchArea.setLayout(new BoxLayout(membersSearchArea, BoxLayout.Y_AXIS));
        membersSearchArea.setVisible(true);
        membersSearchArea.setOpaque(true);
        membersSearchArea.setAlignmentX(Component.CENTER_ALIGNMENT);

        membersSearchTextField = new JTextField();

        membersSearchButton = new JButton(MEMBERS_SEARCH_BUTTON);
        membersSearchButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        membersSearchButton
                .setPreferredSize(new Dimension(Short.MAX_VALUE, membersSearchButton.getPreferredSize().height));
        membersSearchArea.add(membersSearchTextField);
        membersSearchArea.add(Box.createRigidArea(new Dimension(0, 5)));
        membersSearchArea.add(membersSearchButton);

        // button to show all members
        membersShowAllButton = new JButton(MEMBERS_SHOW_ALL_BUTTON);
        membersShowAllButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        membersShowAllButton
                .setPreferredSize(new Dimension(Short.MAX_VALUE, membersShowAllButton.getPreferredSize().height));
        // contains search area and buttons
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(membersPanelLabel);
        topPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        topPanel.add(membersSearchArea);
        topPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        topPanel.add(membersShowAllButton);
        topPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        this.add(topPanel, BorderLayout.NORTH);

        // members title
        JLabel membersAreaLabel = new JLabel(MEMBERS_AREA_LABEL);
        membersAreaLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // panel that displays members
        membersAreaPanel = new JPanel();
        membersAreaPanel.setLayout(new BoxLayout(membersAreaPanel, BoxLayout.Y_AXIS));
        membersAreaPanel.setVisible(true);
        membersAreaPanel.setOpaque(true);
        membersAreaPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JScrollPane membersAreaScrollPane = new JScrollPane(membersAreaPanel);
        membersAreaScrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        membersAreaScrollPane.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, ColorScheme.BORDER_COLOR));

        // members label and scroll pane
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.add(membersAreaLabel);
        bottomPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        bottomPanel.add(membersAreaScrollPane);

        this.add(bottomPanel, BorderLayout.CENTER);

        // get individual member
        membersSearchButton.addActionListener(e -> {
            new Thread(() -> {
                // get specific member
                SithClanMember member = memberRoster.getMemberByName(membersSearchTextField.getText());
                SwingUtilities.invokeLater(() -> {
                    displaySingleMember(member);
                });
            }).start();
        });

        // show all members
        membersShowAllButton.addActionListener(e -> {
            new Thread(() -> {
                // get all members
                HashMap<String, SithClanMember> roster = memberRoster.getRoster();
                SwingUtilities.invokeLater(() -> {
                    displayAllMembers(roster);
                });
            }).start();
        });

        this.setVisible(true);
    }

    /**
     * TODO: FUNCTIONALITY
     * TODO: JAVADOC
     * 
     * @param member
     */
    private void displaySingleMember(SithClanMember member) {
        // create member panel
        // create name
        // discord pfp?
        // display all info
        // add panel to membersAreaPanel
    }

    /**
     * TODO: FUNCTIONALITY
     * TODO: JAVADOC
     */
    private void displayAllMembers(HashMap<String, SithClanMember> roster) {

    }

}

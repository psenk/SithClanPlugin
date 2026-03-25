package sithclanplugin.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import sithclanplugin.members.SithClanMember;
import sithclanplugin.members.SithClanMemberRoster;
import sithclanplugin.util.SithClanPluginConstants;

@Singleton
public class SithClanMembersPanel extends JPanel {

    @Inject
    private SithClanMemberRoster memberRoster;

    private Icon[] rankIcons;
    private final JScrollPane membersAreaScrollPane;
    private final JTextField membersSearchTextField;
    private final JButton membersSearchButton;
    private final JButton membersShowAllButton;
    private final JPanel membersAreaPanel;
    private ArrayList<SithClanMember> rosterList;
    private int pageIndex;
    private boolean isLoading;

    private static final String CURRENT_GOLD_KEY = "Faca";

    private static final String MEMBERS_PANEL_TITLE = "Sith Member Info";
    private static final String MEMBERS_SEARCH_BUTTON = "Search Members";
    private static final String MEMBERS_SHOW_ALL_BUTTON = "Show All Members";
    private static final String MEMBERS_AREA_LABEL = "Members";
    private static final String ROSTER_UNOBTAINABLE_WARNING = "Unable to obtain roster.";
    private static final String MEMBER_DOES_NOT_EXIST = "Member does not exist.";
    private static final String MEMBER_RANK = "Rank: "; // trailing space intentional
    private static final String MEMBER_PROMOTED = "Promoted On: "; // trailing space intentional
    private static final String MEMBER_CREDITS = " Imperial Credits"; // leading space intentional
    private static final String MEMBER_JOINED = "Joined: "; // leading space intentional
    private static final String MEMBER_ALT = "Alt: "; // leading space intentional
    private static final String MEMBER_UNKNOWN_DATA = "Unknown";
    private static final int AVATAR_SIZE = 64;
    private static final int PAGE_SIZE = 5;

    // rank icons
    private static final String ICON_CHILDREN_OF_THE_WATCH = "/children_of_the_watch.png";
    private static final String ICON_SITH_LOYALIST = "/sith_loyalist.png";
    private static final String ICON_TIE_PILOT = "/tie_pilot.png";
    private static final String ICON_SITH_TROOPER = "/storm_trooper.png";
    private static final String ICON_DEATH_TROOPER = "/death_trooper.png";
    private static final String ICON_SOVEREIGN_PROTECTOR = "/sovereign_protector.png";
    private static final String ICON_SOVEREIGN_CHAMPION = "/sovereign_champion.png";
    private static final String ICON_SITH_ACOLYTE = "/sith_acolyte.png";
    private static final String ICON_SITH_KNIGHT = "/sith_knight.png";
    private static final String ICON_SITH_MARAUDER = "/sith_marauder.png";
    private static final String ICON_SITH_LORD = "/sith_lord.png";
    private static final String ICON_SITH_OVERSEER = "/sith_overseer.png";
    private static final String ICON_IMPERIAL_INQUISITOR = "/imperial_inquisitor.png";
    private static final String ICON_GRAND_INQUISITOR = "/grand_inquisitor.png";
    private static final String ICON_SILVER_KEY = "/silver_key.png";
    private static final String ICON_GOLD_KEY = "/gold_key.png";

    SithClanMembersPanel() {
        rankIcons = new Icon[] {
                new ImageIcon(ImageUtil.loadImageResource(getClass(), ICON_CHILDREN_OF_THE_WATCH)),
                new ImageIcon(ImageUtil.loadImageResource(getClass(), ICON_SITH_LOYALIST)),
                new ImageIcon(ImageUtil.loadImageResource(getClass(), ICON_TIE_PILOT)),
                new ImageIcon(ImageUtil.loadImageResource(getClass(), ICON_SITH_TROOPER)),
                new ImageIcon(ImageUtil.loadImageResource(getClass(), ICON_DEATH_TROOPER)),
                new ImageIcon(ImageUtil.loadImageResource(getClass(), ICON_SOVEREIGN_PROTECTOR)),
                new ImageIcon(ImageUtil.loadImageResource(getClass(), ICON_SOVEREIGN_CHAMPION)),
                new ImageIcon(ImageUtil.loadImageResource(getClass(), ICON_SITH_ACOLYTE)),
                new ImageIcon(ImageUtil.loadImageResource(getClass(), ICON_SITH_KNIGHT)),
                new ImageIcon(ImageUtil.loadImageResource(getClass(), ICON_SITH_MARAUDER)),
                new ImageIcon(ImageUtil.loadImageResource(getClass(), ICON_SITH_LORD)),
                new ImageIcon(ImageUtil.loadImageResource(getClass(), ICON_SITH_OVERSEER)),
                new ImageIcon(ImageUtil.loadImageResource(getClass(), ICON_IMPERIAL_INQUISITOR)),
                new ImageIcon(ImageUtil.loadImageResource(getClass(), ICON_GRAND_INQUISITOR)),
                new ImageIcon(ImageUtil.loadImageResource(getClass(), ICON_SILVER_KEY)),
                new ImageIcon(ImageUtil.loadImageResource(getClass(), ICON_GOLD_KEY)) };

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

        membersAreaScrollPane = new JScrollPane(membersAreaPanel);
        membersAreaScrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        membersAreaScrollPane.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, ColorScheme.BORDER_COLOR));
        membersAreaScrollPane.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 10, 400));

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
                // get roster if not already in memory
                if (memberRoster.getRoster().isEmpty()) {
                    int status = memberRoster.parseRosterFromGet();
                    if (status == SithClanPluginConstants.STATUS_NOT_FOUND) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(null, ROSTER_UNOBTAINABLE_WARNING);
                        });
                        return;
                    }
                }
                // get specific member
                SithClanMember member = memberRoster.getMemberByName(membersSearchTextField.getText());
                SwingUtilities.invokeLater(() -> {
                    if (member == null) {
                        JOptionPane.showMessageDialog(null, MEMBER_DOES_NOT_EXIST);
                    } else {
                        displaySingleMember(member);
                    }
                });
            }).start();
        });

        // show all members
        membersShowAllButton.addActionListener(e -> {
            new Thread(() -> {
                // get roster if not already in memory
                if (memberRoster.getRoster().isEmpty()) {
                    int status = memberRoster.parseRosterFromGet();
                    if (status == SithClanPluginConstants.STATUS_NOT_FOUND) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(null, ROSTER_UNOBTAINABLE_WARNING);
                        });
                        return;
                    }
                }
                // get all members
                SwingUtilities.invokeLater(() -> {
                    displayAllMembers(memberRoster.getRoster().values());
                });
            }).start();
        });

        // pagination for show all members
        JScrollBar membersAreaScrollBar = membersAreaScrollPane.getVerticalScrollBar();
        membersAreaScrollBar.addAdjustmentListener(e -> {
            // if already loading stop
            if (isLoading) {
                return;
            }
            // if no list or everything loaded
            if (rosterList == null || pageIndex >= rosterList.size()) {
                return;
            }
            // load next page if at the bottom
            if (membersAreaScrollBar.getValue() + membersAreaScrollBar.getVisibleAmount() >= membersAreaScrollBar
                    .getMaximum()) {
                loadNextPage();
            }
        });

        this.setVisible(true);
    }

    /**
     * Creates a single member card panel for display
     * 
     * @param member SithClanMember member to place on card
     * @return JPanel built member card panel
     */
    private JPanel buildMemberCard(SithClanMember member) {
        // container for all info
        JPanel singleMemberPanel = new JPanel();
        singleMemberPanel.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH, 130));
        singleMemberPanel.setMaximumSize(new Dimension(PluginPanel.PANEL_WIDTH, 130));
        singleMemberPanel.setMinimumSize(new Dimension(PluginPanel.PANEL_WIDTH, 130));
        singleMemberPanel.setLayout(new BoxLayout(singleMemberPanel, BoxLayout.X_AXIS));
        singleMemberPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, ColorScheme.BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 1, 4, 1)));
        singleMemberPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        singleMemberPanel.setOpaque(true);
        singleMemberPanel.setVisible(true);

        // avatar panel (rank icon right now)
        JLabel avatar;
        JPanel memberAvatar = new JPanel();
        memberAvatar.setLayout(new BorderLayout());
        memberAvatar.setAlignmentY(Component.CENTER_ALIGNMENT);
        memberAvatar.setPreferredSize(new Dimension(AVATAR_SIZE - 5, 130));
        memberAvatar.setMaximumSize(new Dimension(AVATAR_SIZE - 5, 130));
        memberAvatar.setMinimumSize(new Dimension(AVATAR_SIZE - 5, 130));
        int rankInt = member.getMemberRank();
        memberAvatar.setOpaque(false);
        if (rankInt == 15 && member.getMemberName().equalsIgnoreCase(CURRENT_GOLD_KEY)) {
            avatar = new JLabel(rankIcons[15]);
        } else {
            avatar = new JLabel(rankIcons[rankInt - 1]);
        }
        avatar.setPreferredSize(new Dimension(AVATAR_SIZE, AVATAR_SIZE));
        memberAvatar.add(avatar, BorderLayout.CENTER);
        singleMemberPanel.add(memberAvatar);

        // member info container
        JPanel rightPanel = new JPanel();
        rightPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        rightPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

        // member name
        JLabel memberName = new JLabel(member.getMemberName());
        Font customFont = memberName.getFont().deriveFont(Font.BOLD, 16);
        memberName.setFont(customFont);
        rightPanel.add(memberName);

        // member rank
        JLabel memberRank = new JLabel(MEMBER_RANK + SithClanPluginConstants.CLAN_RANKS[rankInt - 1]);
        rightPanel.add(memberRank);

        // member promoted date
        String promotionDate = member.getMemberDatePromoted();
        JLabel memberPromoted = new JLabel(MEMBER_PROMOTED
                + (promotionDate == null ? MEMBER_UNKNOWN_DATA : promotionDate));

        rightPanel.add(memberPromoted);

        // member credits
        JLabel memberCredits = new JLabel(member.getMemberCredits() + MEMBER_CREDITS);
        rightPanel.add(memberCredits);

        // member date joined
        JLabel memberJoined = new JLabel(MEMBER_JOINED + member.getMemberDateJoined());
        rightPanel.add(memberJoined);

        // member alt accounts
        String altName = member.getMemberAltName();
        if (altName != null && !altName.isBlank()) {
            JLabel memberAlt = new JLabel(MEMBER_ALT + altName);
            rightPanel.add(memberAlt);
        }

        singleMemberPanel.add(rightPanel);
        return singleMemberPanel;
    }

    /**
     * Adds single member card to display
     * 
     * @param member SithClanMember member to display
     */
    private void displaySingleMember(SithClanMember member) {
        // fresh panel
        membersAreaPanel.removeAll();

        JPanel singleMemberPanel = buildMemberCard(member);

        // add for display
        membersAreaPanel.add(singleMemberPanel);
        membersAreaPanel.revalidate();
        membersAreaPanel.repaint();
    }

    /**
     * Lazily displays a list of all members in clan
     * 
     * @param rosterCollection collection of clan members
     */
    private void displayAllMembers(Collection<SithClanMember> rosterCollection) {
        // fresh panel
        membersAreaPanel.removeAll();

        // sort roster by clan rank descending
        rosterList = new ArrayList<>(rosterCollection);
        rosterList.sort(Comparator.comparingInt(SithClanMember::getMemberRank).reversed());

        // reset pagination and load first page
        pageIndex = 0;
        loadNextPage();

    }

    /**
     * Loads the next page of member cards
     */
    private void loadNextPage() {
        int pageEnd = Math.min(pageIndex + PAGE_SIZE, rosterList.size());

        isLoading = true;
        // create member card and display page
        for (int i = pageIndex; i < pageEnd; i++) {
            membersAreaPanel.add(buildMemberCard(rosterList.get(i)));
        }
        pageIndex = pageEnd;
        membersAreaPanel.revalidate();
        membersAreaPanel.repaint();
        isLoading = false;
    }
}

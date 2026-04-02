package sithclanplugin.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import sithclanplugin.members.SithClanMember;
import sithclanplugin.members.SithClanMemberRoster;
import sithclanplugin.util.SithClanPluginConstants;

@Singleton
public class SithClanMembersPanel extends JPanel
{
    @Inject
    private ScheduledExecutorService executor;

    @Inject
    private SithClanMemberRoster memberRoster;

    private final JScrollPane membersAreaScrollPane;
    private final JTextField membersSearchTextField;
    private final JButton membersSearchButton;
    private final JButton membersShowAllButton;
    private final JPanel membersAreaPanel;
    private final JPanel statusPanel;
    private final JLabel rosterDateLabel;
    private final JLabel memberDoesNotExistLabel;
    private final Icon[] rankIcons;
    private ArrayList<SithClanMember> rosterList;
    private int pageIndex;
    private boolean isLoading;

    private static final String CURRENT_GOLD_KEY = "Faca";
    private static final String MEMBERS_PANEL_TITLE = "Sith Member Info";
    private static final String MEMBERS_SEARCH_BUTTON = "Search Members";
    private static final String MEMBERS_SHOW_ALL_BUTTON = "Show All Members";
    private static final String MEMBERS_AREA_LABEL = "Member(s)";
    private static final String ROSTER_UNOBTAINABLE_WARNING = "Unable to obtain roster.";
    private static final String MEMBER_DOES_NOT_EXIST = "Member does not exist!";
    private static final String MEMBER_RANK = "Rank: "; // trailing space intentional
    private static final String MEMBER_PROMOTED = "Promoted On: "; // trailing space intentional
    private static final String MEMBER_CREDITS_NEEDED = "Credits until promotion: "; // trailing space intentional
    private static final String MEMBER_DAYS_NEEDED = "Days until promotion: "; // trailing space intentional
    private static final String MEMBER_NONE_NEEDED = "None! Coming soon..";
    private static final String MEMBER_CREDITS = " Imperial Credits"; // leading space intentional
    private static final String MEMBER_JOINED = "Joined: "; // trailing space intentional
    private static final String MEMBER_ALT = "Alt: "; // trailing space intentional
    private static final String MEMBER_UNKNOWN_DATA = "Unknown";
    private static final String ROSTER_DATE_PREFIX = "Roster last updated on "; // trailing space intentional
    private static final int AVATAR_SIZE = 64;
    private static final int PAGE_SIZE = 6;
    private static final Font MEMBER_NAME_FONT = new JLabel().getFont().deriveFont(Font.BOLD, 16f);
    private static final Border MEMBER_CARD_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 1, 1, 1, ColorScheme.BORDER_COLOR),
            BorderFactory.createEmptyBorder(4, 1, 4, 1));

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

    SithClanMembersPanel()
    {
        rankIcons = new Icon[]
        {
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

        // status label panel
        statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));

        // current roster date
        rosterDateLabel = new JLabel();
        rosterDateLabel.setVisible(false);
        rosterDateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        memberDoesNotExistLabel = new JLabel(MEMBER_DOES_NOT_EXIST);
        memberDoesNotExistLabel.setVisible(false);
        memberDoesNotExistLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        statusPanel.add(rosterDateLabel);
        statusPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        statusPanel.add(memberDoesNotExistLabel);

        // contains search area and search button
        JPanel membersSearchArea = new JPanel();
        membersSearchArea.setLayout(new BoxLayout(membersSearchArea, BoxLayout.Y_AXIS));
        membersSearchArea.setVisible(true);
        membersSearchArea.setOpaque(true);
        membersSearchArea.setAlignmentX(Component.CENTER_ALIGNMENT);

        membersSearchTextField = new JTextField();
        // highlights all text when box focused
        membersSearchTextField.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                membersSearchTextField.selectAll();
            }
        });

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
        topPanel.add(statusPanel);
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
        membersAreaScrollPane.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 10, 600));

        // members label and scroll pane
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.add(membersAreaLabel);
        bottomPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        bottomPanel.add(membersAreaScrollPane);

        this.add(bottomPanel, BorderLayout.CENTER);

        // get individual member
        membersSearchButton.addActionListener(e ->
        {
            executor.submit(() ->
            {
                // get roster if not already in memory
                if (memberRoster.getRoster().isEmpty())
                {
                    int status = memberRoster.parseRosterFromGet();

                    if (status == SithClanPluginConstants.STATUS_NOT_FOUND)
                    {
                        SwingUtilities.invokeLater(() ->
                        {
                            JOptionPane.showMessageDialog(null, ROSTER_UNOBTAINABLE_WARNING);
                        });
                        return;
                    }
                }
                // get specific member
                SithClanMember member = memberRoster.getMemberByName(membersSearchTextField.getText());
                SwingUtilities.invokeLater(() ->
                {
                    if (member == null)
                    {
                        memberDoesNotExistLabel.setVisible(true);
                    } else
                    {
                        memberDoesNotExistLabel.setVisible(false);
                        updateRosterDateLabel(memberRoster.getDateRosterPosted());
                        displaySingleMember(member);
                    }
                });
            });
        });

        // show all members
        membersShowAllButton.addActionListener(e ->
        {
            executor.submit(() ->
            {
                // get roster if not already in memory
                if (memberRoster.getRoster().isEmpty())
                {
                    int status = memberRoster.parseRosterFromGet();
                    if (status == SithClanPluginConstants.STATUS_NOT_FOUND)
                    {
                        SwingUtilities.invokeLater(() ->
                        {
                            JOptionPane.showMessageDialog(null, ROSTER_UNOBTAINABLE_WARNING);
                        });
                        return;
                    }
                }
                // get all members
                SwingUtilities.invokeLater(() ->
                {
                    updateRosterDateLabel(memberRoster.getDateRosterPosted());
                    displayAllMembers(memberRoster.getRoster().values());
                });
            });
        });

        // pagination for show all members
        JScrollBar membersAreaScrollBar = membersAreaScrollPane.getVerticalScrollBar();
        membersAreaScrollBar.addAdjustmentListener(e ->
        {
            // if already loading stop
            if (isLoading)
            {
                return;
            }
            // if no list or everything loaded
            if (rosterList == null || pageIndex >= rosterList.size())
            {
                return;
            }
            // load next page if at the bottom
            if (membersAreaScrollBar.getValue() + membersAreaScrollBar.getVisibleAmount() >= membersAreaScrollBar
                    .getMaximum())
            {
                loadNextPage();
            }
        });

        this.setVisible(true);
    }

    /**
     * Creates a single member card panel for display
     * 
     * @param member
     *                   SithClanMember member to place on card
     * @return JPanel built member card panel
     */
    private JPanel buildMemberCard(SithClanMember member)
    {
        // container for all info
        JPanel singleMemberPanel = new JPanel();
        singleMemberPanel.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 10, 160));
        singleMemberPanel.setMaximumSize(new Dimension(PluginPanel.PANEL_WIDTH - 10, 160));
        singleMemberPanel.setMinimumSize(new Dimension(PluginPanel.PANEL_WIDTH - 10, 160));
        singleMemberPanel.setLayout(new BoxLayout(singleMemberPanel, BoxLayout.X_AXIS));
        singleMemberPanel.setBorder(MEMBER_CARD_BORDER);
        singleMemberPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        singleMemberPanel.setOpaque(true);
        singleMemberPanel.setVisible(true);

        // avatar panel (rank icon right now)
        JLabel avatar;
        JPanel memberAvatar = new JPanel();
        String memberName = member.getMemberName();
        int rankInt = member.getMemberRank();
        int creditsInt = member.getMemberCredits();
        String promotionDate = member.getMemberDatePromoted();

        memberAvatar.setLayout(new BorderLayout());
        memberAvatar.setAlignmentY(Component.CENTER_ALIGNMENT);
        memberAvatar.setPreferredSize(new Dimension(AVATAR_SIZE - 5, 130));
        memberAvatar.setMaximumSize(new Dimension(AVATAR_SIZE - 5, 130));
        memberAvatar.setMinimumSize(new Dimension(AVATAR_SIZE - 5, 130));

        memberAvatar.setOpaque(false);
        if (rankInt == 15 && memberName.equalsIgnoreCase(CURRENT_GOLD_KEY))
        {
            avatar = new JLabel(rankIcons[15]);
        } else
        {
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
        JLabel memberNameLabel = new JLabel(memberName);
        memberNameLabel.setFont(MEMBER_NAME_FONT);
        rightPanel.add(memberNameLabel);

        // member rank
        JLabel memberRank = new JLabel(MEMBER_RANK + SithClanPluginConstants.CLAN_RANKS[rankInt - 1]);
        rightPanel.add(memberRank);

        // member credits
        JLabel memberCredits = new JLabel(creditsInt + MEMBER_CREDITS);
        rightPanel.add(memberCredits);

        // member promoted date
        JLabel memberPromoted = new JLabel(MEMBER_PROMOTED
                + (promotionDate == null ? MEMBER_UNKNOWN_DATA : promotionDate));

        rightPanel.add(memberPromoted);

        // until next promotion
        if (rankInt <= 10)
        { // sith marauder and below
          // credits
            int creditsNeeded = SithClanPluginConstants.CREDITS_TO_PROMOTE[rankInt] - creditsInt;
            if (creditsNeeded > 0)
            {
                JLabel creditsUntilPromotion = new JLabel(MEMBER_CREDITS_NEEDED + creditsNeeded);
                rightPanel.add(creditsUntilPromotion);
            } else
            {
                if (rankInt >= 5 && rankInt <= 10)
                {// between death trooper and sith marauder
                    JLabel daysUntilPromotion = null;
                    if (promotionDate == null)
                    {
                        daysUntilPromotion = new JLabel(MEMBER_DAYS_NEEDED + MEMBER_UNKNOWN_DATA);
                    } else
                    {
                        long daysInRank = ChronoUnit.DAYS.between(
                                LocalDate.parse(promotionDate, SithClanPluginConstants.SHORT_DATE_FORMATTER),
                                LocalDate.now());
                        long daysNeeded = SithClanPluginConstants.DAYS_TO_PROMOTE[rankInt - 1] - daysInRank;
                        if (daysNeeded <= 0)
                        {
                            daysUntilPromotion = new JLabel(MEMBER_DAYS_NEEDED + MEMBER_NONE_NEEDED);
                        } else
                        {
                            daysUntilPromotion = new JLabel(MEMBER_DAYS_NEEDED + daysNeeded);
                        }
                    }
                    rightPanel.add(daysUntilPromotion);
                } else
                {
                    JLabel noneNeeded = new JLabel(MEMBER_CREDITS_NEEDED + MEMBER_NONE_NEEDED);
                    rightPanel.add(noneNeeded);
                }
            }
        }

        // member date joined
        JLabel memberJoined = new JLabel(MEMBER_JOINED + member.getMemberDateJoined());
        rightPanel.add(memberJoined);

        // member alt accounts
        String altName = member.getMemberAltName();
        if (altName != null && !altName.isBlank())
        {
            JLabel memberAlt = new JLabel(MEMBER_ALT + altName);
            rightPanel.add(memberAlt);
        }

        singleMemberPanel.add(rightPanel);
        return singleMemberPanel;
    }

    /**
     * Adds single member card to display
     * 
     * @param member
     *                   SithClanMember member to display
     */
    private void displaySingleMember(SithClanMember member)
    {
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
     * @param rosterCollection
     *                             collection of clan members
     */
    private void displayAllMembers(Collection<SithClanMember> rosterCollection)
    {
        // fresh panel
        membersAreaPanel.removeAll();

        // sort roster by clan rank descending
        rosterList = new ArrayList<>(rosterCollection);
        rosterList.sort(Comparator.comparingInt(SithClanMember::getMemberRank).reversed()
                .thenComparing(SithClanMember::getMemberName));

        // reset pagination and load first page
        pageIndex = 0;
        loadNextPage();

    }

    /**
     * Loads the next page of member cards
     */
    private void loadNextPage()
    {
        int pageEnd = Math.min(pageIndex + PAGE_SIZE, rosterList.size());

        isLoading = true;
        // create member card and display page
        for (int i = pageIndex; i < pageEnd; i++)
        {
            membersAreaPanel.add(buildMemberCard(rosterList.get(i)));
        }
        pageIndex = pageEnd;
        membersAreaPanel.revalidate();
        membersAreaPanel.repaint();
        isLoading = false;
    }

    /**
     * Converts roster date into user local time
     * 
     * @param time
     *                 ZonedDateTime roster date
     */
    private void updateRosterDateLabel(ZonedDateTime time)
    {
        if (time == null)
        {
            rosterDateLabel.setVisible(false);
            return;
        }
        ZonedDateTime timeStamp = time
                .withZoneSameInstant(ZoneId.systemDefault());
        rosterDateLabel
                .setText(ROSTER_DATE_PREFIX + timeStamp.format(SithClanPluginConstants.DATE_FORMATTER));
        rosterDateLabel.setVisible(true);
    }
}

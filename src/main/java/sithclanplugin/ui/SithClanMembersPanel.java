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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import okhttp3.OkHttpClient;
import sithclanplugin.members.SithClanMember;
import sithclanplugin.members.SithClanMemberRoster;
import sithclanplugin.util.SithClanConstants;
import sithclanplugin.util.SithClanState;
import sithclanplugin.util.SithClanUtil;

// refactored june 16

@Slf4j
@Singleton
public class SithClanMembersPanel extends JPanel
{

    @Inject
    private OkHttpClient httpClient;

    @Inject
    private Gson gson;

    @Inject
    private ScheduledExecutorService executor;

    @Inject
    private SithClanState state;

    @Inject
    private SithClanMemberRoster memberRoster;

    private final Icon[] rankIcons;
    private final JLabel rosterDateLabel;
    private final JLabel statusLabel;
    private final JPanel statusPanel;
    private final JTextField membersSearchTextField;
    private final JButton membersSearchButton;
    private final JButton membersShowAllButton;
    private final JButton membersRefreshRosterButton;
    private final JButton membersEditAboutMeButton;
    private final JTextArea membersAboutMeTextArea;
    private final JLabel membersAboutMeCharCount;
    private final JPanel membersAboutMePanel;
    private final JPanel membersAreaPanel;
    private final JScrollPane membersAreaScrollPane;
    private final JLabel membersAreaLabel;

    private ArrayList<SithClanMember> rosterList;
    private int pageIndex;
    private boolean isLoading;
    private Map<String, String> aboutMeCache = null;

    private static final String MEMBERS_PANEL_TITLE = "Sith Member Info";
    private static final String MEMBERS_SEARCH_BUTTON = "Search Members";
    private static final String MEMBERS_SHOW_ALL_BUTTON = "Show All Members";
    private static final String MEMBERS_REFRESH_ROSTER_BUTTON = "Refresh Roster";
    private static final String ABOUT_ME_BUTTON = "Edit My About Me";
    private static final String MEMBERS_AREA_LABEL = "Member(s)";
    private static final String RATE_LIMITED_WARNING = "The roster has been retrieved too recently. Try again in a few minutes.";
    private static final String ROSTER_UNOBTAINABLE_WARNING = "Unable to obtain roster.";
    private static final String BLANK_SEARCH_VALUE = "Please input a value to search.";
    private static final String MEMBER_DOES_NOT_EXIST = "Member does not exist!";
    private static final String ROSTER_DATE_PREFIX = "Roster last updated on "; // trailing space intentional
    private static final String ABOUT_ME_INSTRUCTIONS = "<html><center>Edit your About Me (max 200 characters)</center></html>";
    private static final String ABOUT_ME_SAVE = "Save";
    private static final String ABOUT_ME_CANCEL = "Cancel";
    private static final String ABOUT_ME_FAILED = "Save failed.  Please try again.";
    private static final String ABOUT_ME_SUCCESSFUL = "About me saved successfully.";
    private static final String MEMBER_RANK = "<u>Rank</u>: "; // trailing space intentional
    private static final String MEMBER_CREDITS = " Imperial Credits"; // leading space intentional
    private static final String MEMBER_PROMOTED = "<u>Promoted On</u>: "; // trailing space intentional
    private static final String MEMBER_TO_PROMOTE = "<u>To Promote</u>: "; // trailing space intentional
    private static final String MEMBER_NONE_NEEDED = "None! Coming soon..";
    private static final String MEMBER_JOINED = "<u>Joined</u>: "; // trailing space intentional
    private static final String MEMBER_ALT = "<u>Alt</u>: "; // trailing space intentional
    private static final String MEMBER_UNKNOWN = "Unknown";
    private static final String SENATE_MEMBER = "Senate Member";
    private static final int AVATAR_SIZE = 64;
    private static final int PAGE_SIZE = 6;
    private static final int ABOUT_ME_LENGTH = 200;
    private static final int SCROLL_PANE_HEIGHT = 150;
    private static final int MEMBERS_AREA_SCROLL_PANE_HEIGHT = 600;

    SithClanMembersPanel()
    {
        // rank icons
        rankIcons = new Icon[SithClanConstants.RANK_ICON_PATHS.length];
        for (int i = 0; i < SithClanConstants.RANK_ICON_PATHS.length; i++)
        {
            rankIcons[i] = new ImageIcon(
                    ImageUtil.loadImageResource(getClass(), SithClanConstants.RANK_ICON_PATHS[i]));
        }

        this.setLayout(new BorderLayout());

        // main panel title
        JLabel membersPanelLabel = new JLabel(MEMBERS_PANEL_TITLE);
        membersPanelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // status label panel and label
        statusLabel = SithClanUtil.createStatusLabel();
        statusPanel = SithClanUtil.createStatusPanel(statusLabel);

        // current roster date
        rosterDateLabel = SithClanUtil.createStatusLabel();
        statusPanel.add(rosterDateLabel);
        statusPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        // search field
        membersSearchTextField = new JTextField();

        // highlights all text when box focused
        SithClanUtil.attachSelectAllOnFocus(membersSearchTextField);

        // panel for all buttons
        JPanel buttonContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        // search member button
        membersSearchButton = createButton(MEMBERS_SEARCH_BUTTON);

        // show all members button
        membersShowAllButton = createButton(MEMBERS_SHOW_ALL_BUTTON);

        // refresh roster button
        membersRefreshRosterButton = createButton(MEMBERS_REFRESH_ROSTER_BUTTON);

        // edit members about me button
        membersEditAboutMeButton = createButton(ABOUT_ME_BUTTON);
        membersEditAboutMeButton.setVisible(false);

        // edit about me ui panel
        membersAboutMeTextArea = new JTextArea();
        membersAboutMeCharCount = new JLabel("0/" + ABOUT_ME_LENGTH);
        membersAboutMePanel = buildEditAboutMePanel();
        membersAboutMePanel.setVisible(false);

        buttonPanel.add(membersSearchButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(membersShowAllButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(membersRefreshRosterButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(membersEditAboutMeButton);
        buttonContainer.add(buttonPanel);

        // contains search area and buttons
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(membersPanelLabel);
        topPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        topPanel.add(statusPanel);
        topPanel.add(membersSearchTextField);
        topPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        topPanel.add(buttonContainer);
        topPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        this.add(topPanel, BorderLayout.NORTH);

        // members panel title
        membersAreaLabel = new JLabel(MEMBERS_AREA_LABEL);
        membersAreaLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // members display panel
        membersAreaPanel = new JPanel();
        membersAreaPanel.setLayout(new BoxLayout(membersAreaPanel, BoxLayout.Y_AXIS));
        membersAreaPanel.setVisible(true);
        membersAreaPanel.setOpaque(true);
        membersAreaPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        membersAreaScrollPane = new JScrollPane(membersAreaPanel);
        membersAreaScrollPane.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH, MEMBERS_AREA_SCROLL_PANE_HEIGHT));
        membersAreaScrollPane.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, ColorScheme.BORDER_COLOR));
        membersAreaScrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        membersAreaScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // members label and scroll pane
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.add(membersAboutMePanel);
        bottomPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        bottomPanel.add(membersAreaLabel);
        bottomPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        bottomPanel.add(membersAreaScrollPane);

        this.add(bottomPanel, BorderLayout.CENTER);

        // get individual member action
        membersSearchButton.addActionListener(e ->
        {
            executor.submit(() ->
            {
                // get roster and about me if needed
                fetchRosterIfNeeded();
                fetchAboutMeCacheIfNeeded();

                String searchValue = membersSearchTextField.getText().trim();

                // if search is empty
                if (searchValue.isBlank())
                {
                    SwingUtilities.invokeLater(() ->
                    {
                        statusLabel.setForeground(ColorScheme.BRAND_ORANGE);
                        statusLabel.setText(BLANK_SEARCH_VALUE);
                        SithClanUtil.statusTimer(statusLabel);
                    });
                    return;
                }

                // get specific member
                ArrayList<SithClanMember> members = memberRoster.getMembersBySubstring(searchValue.toLowerCase());
                SwingUtilities.invokeLater(() ->
                {
                    // dismiss about me area if open
                    membersAboutMePanel.setVisible(false);
                    membersAreaLabel.setVisible(true);
                    membersAreaScrollPane.setVisible(true);

                    // if member does not exist
                    if (members.isEmpty())
                    {
                        statusLabel.setForeground(ColorScheme.BRAND_ORANGE);
                        statusLabel.setText(MEMBER_DOES_NOT_EXIST);
                        SithClanUtil.statusTimer(statusLabel);
                    } else if (members.size() == 1)
                    {
                        updateRosterDateLabel(memberRoster.getDateRosterPosted());
                        displaySingleMember(members.get(0));
                    } else
                    {
                        updateRosterDateLabel(memberRoster.getDateRosterPosted());
                        displayAllMembers(members);
                    }
                });
            });
        });

        // show all members action
        membersShowAllButton.addActionListener(e ->
        {
            executor.submit(() ->
            {
                fetchRosterIfNeeded();
                fetchAboutMeCacheIfNeeded();

                // get all members
                SwingUtilities.invokeLater(() ->
                {
                    // dismiss about me area if open
                    membersAboutMePanel.setVisible(false);
                    membersAreaLabel.setVisible(true);
                    membersAreaScrollPane.setVisible(true);

                    updateRosterDateLabel(memberRoster.getDateRosterPosted());
                    displayAllMembers(memberRoster.getRoster().values());
                });
            });
        });

        membersRefreshRosterButton.addActionListener(e ->
        {
            executor.submit(() ->
            {
                int status = memberRoster.parseRosterFromGet();

                if (status != SithClanConstants.STATUS_OK)
                {
                    SwingUtilities.invokeLater(() -> handleRosterStatus(status));
                    return;
                }

                aboutMeCache = null;
                fetchAboutMeCacheIfNeeded();

                SwingUtilities.invokeLater(() ->
                {
                    membersAboutMePanel.setVisible(false);
                    membersAreaLabel.setVisible(true);
                    membersAreaScrollPane.setVisible(true);

                    updateRosterDateLabel(memberRoster.getDateRosterPosted());
                    displayAllMembers(memberRoster.getRoster().values());
                });
            });
        });

        // edit about me button action
        membersEditAboutMeButton.addActionListener(e ->
        {
            // hide members area, show about me ui
            membersAreaLabel.setVisible(false);
            membersAreaScrollPane.setVisible(false);
            membersAboutMeTextArea.setText("");
            membersAboutMePanel.setVisible(true);

            // fetch existing about me
            executor.submit(() ->
            {
                String existingAboutMe = fetchAboutMe(state.getPlayerName());
                SwingUtilities.invokeLater(() ->
                {
                    String aboutMe = existingAboutMe == null ? "" : existingAboutMe;
                    membersAboutMeTextArea.setText(aboutMe);
                    membersAboutMeCharCount.setText(aboutMe.length() + "/" + ABOUT_ME_LENGTH);
                });
            });
        });

        // pagination for showing all members
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
     * DISPLAY FUNCTIONS
     */

    /**
     * Add single member card to panel
     * 
     * @param member
     *                   SithClanMember member to display
     */
    private void displaySingleMember(SithClanMember member)
    {
        // fresh panel
        membersAreaPanel.removeAll();
        rosterList = null;
        pageIndex = 0;

        // create member card
        JPanel singleMemberPanel = buildMemberCard(member);

        // add for display
        membersAreaPanel.add(singleMemberPanel);
        membersAreaPanel.revalidate();
        membersAreaPanel.repaint();
    }

    /**
     * Lazily add all member cards to panel
     * 
     * @param memberRoster
     *                         Collection list of all clan members
     */
    private void displayAllMembers(Collection<SithClanMember> memberRoster)
    {
        // fresh panel
        membersAreaPanel.removeAll();

        // sort roster by clan rank descending
        rosterList = new ArrayList<>(new LinkedHashSet<>(memberRoster));
        rosterList.sort(Comparator.comparingInt(SithClanMember::getMemberRank).reversed()
                .thenComparing(SithClanMember::getMemberName));

        // reset pagination and load first page
        pageIndex = 0;
        loadNextPage();
    }

    /**
     * Load next page in member roster
     */
    private void loadNextPage()
    {
        int pageEnd = Math.min(pageIndex + PAGE_SIZE, rosterList.size());

        isLoading = true;
        // create member cards and display page
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
     * CREATE FUNCTIONS
     */

    /**
     * Create panel to edit members about me
     * 
     * @return JPanel edit about me ui panel
     */
    private JPanel buildEditAboutMePanel()
    {
        // create panel
        JPanel editPanel = new JPanel();
        editPanel.setLayout(new BoxLayout(editPanel, BoxLayout.Y_AXIS));

        // default about me instructions
        JLabel instructions = new JLabel(ABOUT_ME_INSTRUCTIONS);
        instructions.setAlignmentX(Component.CENTER_ALIGNMENT);

        // about me input text area
        membersAboutMeTextArea.setLineWrap(true);
        membersAboutMeTextArea.setWrapStyleWord(true);
        membersAboutMeTextArea.setRows(7);

        // scroll pane for just in case
        JScrollPane aboutMeScrollPane = new JScrollPane(membersAboutMeTextArea);
        aboutMeScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        aboutMeScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        aboutMeScrollPane.setMaximumSize(new Dimension(Short.MAX_VALUE, SCROLL_PANE_HEIGHT));
        aboutMeScrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);

        // character count label
        JPanel charCountRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        charCountRow.setVisible(true);
        charCountRow.setOpaque(false);
        charCountRow
                .setMaximumSize(new Dimension(Short.MAX_VALUE, membersAboutMeCharCount.getPreferredSize().height));
        charCountRow.add(membersAboutMeCharCount);

        // update character count dynamically
        membersAboutMeTextArea.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                updateCount();
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                updateCount();
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                updateCount();
            }

            /**
             * Updates character count number
             */
            private void updateCount()
            {
                // get current length
                int len = membersAboutMeTextArea.getText().length();
                if (len > ABOUT_ME_LENGTH)
                {
                    SwingUtilities
                            .invokeLater(() -> membersAboutMeTextArea
                                    .setText(membersAboutMeTextArea.getText().substring(0, ABOUT_ME_LENGTH)));
                    len = ABOUT_ME_LENGTH;
                }
                final int finalLen = len;
                SwingUtilities.invokeLater(() -> membersAboutMeCharCount.setText(finalLen + "/" + ABOUT_ME_LENGTH));
            }
        });

        // save and cancel buttons
        JButton saveButton = new JButton(ABOUT_ME_SAVE);
        JButton cancelButton = new JButton(ABOUT_ME_CANCEL);

        // panel for button organization
        JPanel buttonRow = new JPanel();
        buttonRow.setLayout(new BoxLayout(buttonRow, BoxLayout.X_AXIS));
        buttonRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonRow.setOpaque(false);
        buttonRow.add(saveButton);
        buttonRow.add(Box.createRigidArea(new Dimension(5, 0)));
        buttonRow.add(cancelButton);

        // save button action
        saveButton.addActionListener(e ->
        {
            String text = membersAboutMeTextArea.getText().trim();
            executor.submit(() ->
            {
                boolean success = submitAboutMe(state.getPlayerName(), text);
                SwingUtilities.invokeLater(() ->
                {
                    if (success)
                    {
                        // fetches fresh data
                        aboutMeCache = null;

                        // return to normal
                        statusLabel.setText(ABOUT_ME_SUCCESSFUL);
                        SithClanUtil.statusTimer(statusLabel);

                        membersAboutMePanel.setVisible(false);
                        membersAreaLabel.setVisible(true);
                        membersAreaScrollPane.setVisible(true);

                    } else
                    {
                        statusLabel.setForeground(ColorScheme.BRAND_ORANGE);
                        statusLabel.setText(ABOUT_ME_FAILED);
                        SithClanUtil.statusTimer(statusLabel);
                    }
                });
            });
        });

        // cancel button action
        cancelButton.addActionListener(e ->
        {
            membersAboutMePanel.setVisible(false);
            membersAreaLabel.setVisible(true);
            membersAreaScrollPane.setVisible(true);
        });

        editPanel.add(instructions);
        editPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        editPanel.add(aboutMeScrollPane);
        editPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        editPanel.add(charCountRow);
        editPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        editPanel.add(buttonRow);

        editPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, editPanel.getPreferredSize().height));

        return editPanel;
    }

    /**
     * Create a single member card panel for display
     * 
     * @param member
     *                   SithClanMember member to create card for
     * @return JPanel completed member card panel
     */
    private JPanel buildMemberCard(SithClanMember member)
    {
        // container for all info
        JPanel singleMemberPanel = new JPanel();
        singleMemberPanel.setLayout(new BoxLayout(singleMemberPanel, BoxLayout.Y_AXIS));
        singleMemberPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, ColorScheme.BORDER_COLOR),
                BorderFactory.createEmptyBorder(15, 1, 4, 1)));
        singleMemberPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        singleMemberPanel.setOpaque(true);
        singleMemberPanel.setVisible(true);

        // container for avatar and member info
        JPanel memberInfoPanel = new JPanel();
        memberInfoPanel.setLayout(new BoxLayout(memberInfoPanel, BoxLayout.X_AXIS));
        memberInfoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        memberInfoPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        memberInfoPanel.setOpaque(true);
        memberInfoPanel.setVisible(true);

        // rank icon
        JPanel memberAvatar = new JPanel();
        String memberName = member.getMemberName();
        int memberRankInt = member.getMemberRank();
        int memberCreditsInt = member.getMemberCredits();
        String memberLastPromotionDate = member.getMemberDatePromoted();

        memberAvatar.setLayout(new BorderLayout());
        memberAvatar.setAlignmentY(Component.CENTER_ALIGNMENT);
        memberAvatar.setPreferredSize(new Dimension(AVATAR_SIZE, AVATAR_SIZE));
        memberAvatar.setMaximumSize(new Dimension(AVATAR_SIZE, AVATAR_SIZE));
        memberAvatar.setOpaque(false);

        JLabel avatar;
        // gold key rank
        if (memberRankInt == 15 && memberName.equalsIgnoreCase(SithClanConstants.CURRENT_GOLD_KEY))
        {
            avatar = new JLabel(rankIcons[15]);
        } else
        {
            avatar = new JLabel(rankIcons[memberRankInt - 1]);
        }
        memberAvatar.add(avatar, BorderLayout.CENTER);
        memberInfoPanel.add(memberAvatar);

        // member info container
        JPanel rightPanel = new JPanel();
        rightPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        rightPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

        // member name
        JLabel memberNameLabel = new JLabel(memberName);
        memberNameLabel.setFont(getFont().deriveFont(Font.BOLD, 16f));
        rightPanel.add(memberNameLabel);

        // senate member
        if (memberRankInt <= 15 && memberRankInt >= 13)
        {
            rightPanel.add(new JLabel("<html><b>" + SENATE_MEMBER + "</b></html>"));
        }

        // member rank
        rightPanel
                .add(new JLabel("<html>" + MEMBER_RANK + SithClanConstants.CLAN_RANKS[memberRankInt - 1] + "</html>"));

        // member credits
        rightPanel.add(new JLabel("<html>" + memberCreditsInt + MEMBER_CREDITS + "</html>"));

        // member promoted date
        JLabel memberPromoted = new JLabel("<html>" + MEMBER_PROMOTED
                + (memberLastPromotionDate == null ? MEMBER_UNKNOWN : memberLastPromotionDate) + "</html>");
        rightPanel.add(memberPromoted);

        // until next promotion
        // sith marauder and below
        if (memberRankInt <= 10)
        {
            // credits
            int creditsNeeded = SithClanConstants.CREDITS_TO_PROMOTE[memberRankInt] - memberCreditsInt;
            if (creditsNeeded > 0)
            {
                rightPanel.add(new JLabel("<html>" + MEMBER_TO_PROMOTE + creditsNeeded + " credits</html>"));
            } else
            {
                // between death trooper and sith marauder
                if (memberRankInt >= 5 && memberRankInt <= 10)
                {
                    JLabel daysUntilPromotion = null;
                    if (memberLastPromotionDate == null)
                    {
                        daysUntilPromotion = new JLabel("<html>" + MEMBER_TO_PROMOTE + MEMBER_UNKNOWN + "</html>");
                    } else
                    {
                        long daysInRank = ChronoUnit.DAYS.between(
                                LocalDate.parse(memberLastPromotionDate, SithClanConstants.SHORT_DATE_FORMATTER),
                                LocalDate.now());
                        long daysNeeded = SithClanConstants.DAYS_TO_PROMOTE[memberRankInt - 1] - daysInRank;
                        if (daysNeeded <= 0)
                        {
                            daysUntilPromotion = new JLabel(
                                    "<html>" + MEMBER_TO_PROMOTE + MEMBER_NONE_NEEDED + "</html>");
                        } else
                        {
                            daysUntilPromotion = new JLabel("<html>" + MEMBER_TO_PROMOTE + daysNeeded + " days</html>");
                        }
                    }
                    rightPanel.add(daysUntilPromotion);
                } else
                {
                    rightPanel.add(new JLabel("<html>" + MEMBER_TO_PROMOTE + MEMBER_NONE_NEEDED + "</html>"));
                }
            }
        }

        // member date joined
        rightPanel.add(new JLabel("<html>" + MEMBER_JOINED + member.getMemberDateJoined() + "</html>"));

        // member alt accounts
        String altName = member.getMemberAltName();
        if (altName != null && !altName.isBlank())
        {
            rightPanel.add(new JLabel("<html>" + MEMBER_ALT + altName + "</html>"));
        }

        // member about me section
        JPanel aboutMePanel = new JPanel(new BorderLayout());
        aboutMePanel.setOpaque(false);
        aboutMePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // about me text display area
        JTextArea aboutMeText = new JTextArea();
        aboutMeText.setLineWrap(true);
        aboutMeText.setWrapStyleWord(true);
        aboutMeText.setEditable(false);
        aboutMeText.setFocusable(false);
        aboutMeText.setOpaque(false);
        aboutMeText.setFont(getFont().deriveFont(Font.ITALIC));
        aboutMeText.setAlignmentX(Component.LEFT_ALIGNMENT);
        aboutMeText.setBorder(BorderFactory.createEmptyBorder(0, 4, 2, 4));
        aboutMeText.setSize(PluginPanel.PANEL_WIDTH, Short.MAX_VALUE);

        aboutMePanel.add(aboutMeText, BorderLayout.CENTER);

        // get members current about me
        final String nameForFetch = member.getMemberName();
        if (aboutMeCache != null)
        {
            String aboutMe = aboutMeCache.get(nameForFetch.toLowerCase());
            if (aboutMe != null && !aboutMe.isBlank())
            {
                aboutMeText.setText(aboutMe);
            }
        }

        memberInfoPanel.add(rightPanel);
        singleMemberPanel.add(memberInfoPanel);
        singleMemberPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        singleMemberPanel.add(aboutMePanel);
        singleMemberPanel
                .setMaximumSize(new Dimension(PluginPanel.PANEL_WIDTH, singleMemberPanel.getPreferredSize().height));

        return singleMemberPanel;
    }

    /**
     * MISC FUNCTIONS
     */

    /**
     * Helper method to create buttons
     * 
     * @param buttonText
     *                       String text to go on button
     * @return JButton created button
     */
    private JButton createButton(String buttonText)
    {
        JButton button = new JButton(buttonText);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(Short.MAX_VALUE, button.getPreferredSize().height));
        return button;
    }

    /**
     * Convert roster date into user local time
     * 
     * @param time
     *                 ZonedDateTime roster date
     */
    private void updateRosterDateLabel(ZonedDateTime time)
    {
        if (time == null)
        {
            rosterDateLabel.setText("");
            return;
        }
        ZonedDateTime timeStamp = time
                .withZoneSameInstant(ZoneId.systemDefault());
        rosterDateLabel
                .setText(ROSTER_DATE_PREFIX + timeStamp.format(SithClanConstants.DATE_FORMATTER));
    }

    /**
     * Search for member, directed from right-click menu
     * 
     * @param username
     *                     String player username
     */
    public void searchMemberFromMenu(String username)
    {
        executor.submit(() ->
        {
            // get roster if needed
            fetchRosterIfNeeded();
            fetchAboutMeCacheIfNeeded();

            // get specific member
            SithClanMember member = memberRoster.getMemberByName(username);
            SwingUtilities.invokeLater(() ->
            {
                // dismiss about me editor if open
                membersAboutMePanel.setVisible(false);
                membersAreaLabel.setVisible(true);
                membersAreaScrollPane.setVisible(true);

                // if member does not exist
                if (member == null)
                {
                    statusLabel.setForeground(ColorScheme.BRAND_ORANGE);
                    statusLabel.setText(MEMBER_DOES_NOT_EXIST);
                    SithClanUtil.statusTimer(statusLabel);
                } else
                {
                    membersSearchTextField.setText(username);
                    updateRosterDateLabel(memberRoster.getDateRosterPosted());
                    displaySingleMember(member);
                }
            });
        });
    }

    /**
     * Fetches roster if not currently saved to memory
     * Rate limiting 5 minutes
     */
    private void fetchRosterIfNeeded()
    {
        if (memberRoster.getRoster().isEmpty())
        {
            int status = memberRoster.parseRosterFromGet();

            if (status != SithClanConstants.STATUS_OK)
            {
                SwingUtilities.invokeLater(() -> handleRosterStatus(status));
                return;
            }
        }
    }

    /**
     * Set visibility of about me button
     * 
     */
    public void refreshAboutMeButton()
    {
        SwingUtilities.invokeLater(() -> membersEditAboutMeButton.setVisible(state.getPlayerName() != null));
    }

    /**
     * Fetch members about me from database
     * 
     * @param memberName
     *                       String member username
     * @return String members about me info
     */
    private String fetchAboutMe(String memberName)
    {
        // send get request
        String uri = SithClanConstants.MEMBER_SINGLE_ABOUT_ME_URI + memberName;
        String response = SithClanUtil.sendGetRequest(httpClient, uri);
        if (response == null)
        {
            return null;
        }

        // parse response
        JsonObject json = gson.fromJson(response, JsonObject.class);
        if (json == null || !json.has("aboutMe"))
        {
            return null;
        }

        // return about me
        String text = json.get("aboutMe").getAsString();
        return text.isBlank() ? null : text;
    }

    /**
     * Send member about me to database
     * 
     * @param memberName
     *                        String member username
     * @param aboutMeText
     *                        String member about me info
     * @return boolean if response submitted successfully
     */
    private boolean submitAboutMe(String memberName, String aboutMeText)
    {
        String uri = SithClanConstants.MEMBER_SINGLE_ABOUT_ME_URI + memberName;

        JsonObject body = new JsonObject();
        body.addProperty("aboutMe", aboutMeText);
        body.addProperty("submittedName", memberName);
        String jsonBody = body.toString();

        String response = SithClanUtil.sendPutRequest(httpClient, "", jsonBody, uri);
        return response != null;
    }

    /**
     * Handle the returned status of the roster
     * 
     * @param status
     *                   int status code
     */
    private void handleRosterStatus(int status)
    {
        switch (status)
        {
            case SithClanConstants.STATUS_RATE_LIMITED:
                log.warn("Roster fetch rate limited");
                statusLabel.setForeground(ColorScheme.BRAND_ORANGE);
                statusLabel.setText(RATE_LIMITED_WARNING);
                SithClanUtil.statusTimer(statusLabel);
                break;
            case SithClanConstants.STATUS_NOT_FOUND:
                log.error("Roster fetch failed with status: {}", status);
                statusLabel.setForeground(ColorScheme.BRAND_ORANGE);
                statusLabel.setText(ROSTER_UNOBTAINABLE_WARNING);
                SithClanUtil.statusTimer(statusLabel);
                break;
            default:
                break;
        }
    }

    /**
     * Fetches about me map from database containing all members
     */
    private void fetchAboutMeCacheIfNeeded()
    {
        // cache already loaded
        if (aboutMeCache != null)
        {
            return;
        }

        // send HTTP GET request
        String response = SithClanUtil.sendGetRequest(httpClient,
                SithClanConstants.MEMBER_ALL_ABOUT_ME_URI);
        if (response == null)
        {
            aboutMeCache = new HashMap<>();
            return;
        }

        // parse JSON into member name and about me map
        JsonObject json = gson.fromJson(response, JsonObject.class);
        aboutMeCache = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet())
        {
            aboutMeCache.put(entry.getKey(), entry.getValue().getAsString());
        }
    }
}

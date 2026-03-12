package sithclanplugin.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;
import sithclanplugin.eventschedule.SithClanEventSchedule;

@Singleton
public class SithClanSenatePanel extends JPanel {

    private final JLabel senatePanelLabel;
    private final JLabel senatePostScheduleLabel;
    private final JPanel senatePostSchedulePanel;
    private final JScrollPane senatePostScheduleScrollPane;
    private final JTextArea senatePostScheduleTextArea;
    private final JButton senatePostScheduleButton;

    private SithClanEventSchedule eventSchedule;

    private static final String SENATE_OPTIONS_LABEL = "Senate Options";
    private static final String UPDATE_SCHEDULE_LABEL = "Update Schedule";
    private static final String UPDATE_BUTTON = "Update";
    private static final String TEXT_AREA_DEFAULT = "Post Event Schedule Here";
    private static final String ARROW_RIGHT_PATH = "/arrow_right.png";
    private static final String ARROW_DOWN_PATH = "/arrow_down.png";

    @Inject
    SithClanSenatePanel(SithClanEventSchedule eventSchedule) {

        this.eventSchedule = eventSchedule;
        final Icon rightArrowIcon = new ImageIcon(ImageUtil.loadImageResource(getClass(), ARROW_RIGHT_PATH));
        final Icon downArrowIcon = new ImageIcon(ImageUtil.loadImageResource(getClass(), ARROW_DOWN_PATH));

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // title panel label at top
        senatePanelLabel = new JLabel(SENATE_OPTIONS_LABEL);
        senatePanelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.add(senatePanelLabel);
        this.add(Box.createRigidArea(new Dimension(0, 10)));

        // label for schedule panel
        senatePostScheduleLabel = new JLabel(UPDATE_SCHEDULE_LABEL);
        senatePostScheduleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        senatePostScheduleLabel.setOpaque(true);
        senatePostScheduleLabel.setIcon(rightArrowIcon);
        senatePostScheduleLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        senatePostScheduleLabel
                .setMaximumSize(new Dimension(Integer.MAX_VALUE, senatePostScheduleLabel.getPreferredSize().height));
        this.add(senatePostScheduleLabel);

        // panel to post the event schedule
        senatePostSchedulePanel = new JPanel();
        senatePostSchedulePanel.setLayout(new BoxLayout(senatePostSchedulePanel, BoxLayout.Y_AXIS));
        senatePostScheduleTextArea = new JTextArea();
        senatePostScheduleTextArea.setText(TEXT_AREA_DEFAULT);
        senatePostScheduleTextArea.setRows(20);
        senatePostScheduleTextArea.setLineWrap(false);
        senatePostScheduleScrollPane = new JScrollPane(senatePostScheduleTextArea);
        senatePostScheduleScrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        senatePostScheduleButton = new JButton(UPDATE_BUTTON);
        senatePostScheduleButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // expand/collapse event schedule options
        senatePostScheduleLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                boolean isVisible = !senatePostSchedulePanel.isVisible();
                senatePostSchedulePanel.setVisible(isVisible);
                if (isVisible)
                    senatePostScheduleLabel.setIcon(downArrowIcon);
                else
                    senatePostScheduleLabel.setIcon(rightArrowIcon);
                revalidate();
                repaint();
            }
        });

        // adding to main panel
        senatePostSchedulePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        senatePostSchedulePanel.add(senatePostScheduleScrollPane);

        senatePostSchedulePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        senatePostSchedulePanel.add(senatePostScheduleButton);

        senatePostSchedulePanel.setVisible(false);
        this.add(senatePostSchedulePanel);

        // post event schedule action
        senatePostScheduleButton.addActionListener(e -> {
            // TODO: validate data robustness
            eventSchedule.parseScheduleForPost(senatePostScheduleTextArea.getText());
            // if response good, popup good?
        });
    }
}

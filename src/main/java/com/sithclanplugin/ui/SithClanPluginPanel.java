package com.sithclanplugin.ui;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Panel;

import javax.inject.Inject;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.google.inject.Singleton;

import net.runelite.client.ui.PluginPanel;

@Singleton
public class SithClanPluginPanel extends PluginPanel {

    private final SithClanEventSchedulePanel schedulePanel;
    
    private final JPanel cardPanel;
    private final JPanel senatePanel;
    private final JPanel navPanel;
    private final JPanel buttonPanel;

    private final JLabel schedulePanelLabel;
    private final JLabel senatePanelLabel;

    private final JButton scheduleButton;
    private final JButton senateButton;

    private int currentCard = 1;
    private CardLayout cardLayout;

    @Inject
    SithClanPluginPanel() {
        super();
        getScrollPane().setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // create card panel and layout manager
        cardPanel = new JPanel();
        cardLayout = new CardLayout();
        cardPanel.setLayout(cardLayout);

        // create cards and button panel
        schedulePanel = new SithClanEventSchedulePanel();
        senatePanel = new JPanel();
        buttonPanel = new JPanel();
        navPanel = new JPanel();

        // configure panel layouts
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        navPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

        // create card labels
        schedulePanelLabel = new JLabel();
        senatePanelLabel = new JLabel();

        // create panel buttons
        scheduleButton = new JButton("Event Schedule");
        senateButton = new JButton("Senate Options");
        
        // add labels to cards
        schedulePanel.add(schedulePanelLabel);
        senatePanel.add(senatePanelLabel);

        // add cards to card panel
        cardPanel.add(schedulePanel);
        cardPanel.add(senatePanel);

        // adding buttons to button panel
        buttonPanel.add(scheduleButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0,5)));
        buttonPanel.add(senateButton);

        // add panels to main panel
        navPanel.add(buttonPanel);
        this.add(navPanel);
        this.add(cardPanel);
    }

}

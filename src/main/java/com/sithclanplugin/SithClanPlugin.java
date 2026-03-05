package com.sithclanplugin;

import java.awt.image.BufferedImage;

import javax.inject.Inject;

import com.google.inject.Provider;
import com.google.inject.Provides;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(name = "Sith Clan Plugin", description = "Enable the Sith Clan Plugin")
public class SithClanPlugin extends Plugin {
	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private SithClanPluginConfig config;

	@Inject
	private Provider<SithClanPluginPanel> uiPanel;

	private NavigationButton uiNavigationButton;

	@Override
	protected void startUp() throws Exception {
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");

		uiNavigationButton = NavigationButton.builder()
				.tooltip("Sith Clan Plugin")
				.icon(icon)
				.priority(6)
				.panel(uiPanel.get())
				.build();

		clientToolbar.addNavigation(uiNavigationButton);
	}

	@Override
	protected void shutDown() throws Exception {
		clientToolbar.removeNavigation(uiNavigationButton);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged) {
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
			// TODO: remove or update
		}
	}

	// allows config to be accessible from RL settings panel
	@Provides
	SithClanPluginConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(SithClanPluginConfig.class);
	}
}

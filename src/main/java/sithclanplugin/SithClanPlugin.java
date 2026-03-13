package sithclanplugin;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.inject.Inject;
import javax.swing.SwingUtilities;

import com.google.inject.Provider;
import com.google.inject.Provides;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import sithclanplugin.eventschedule.SithClanEventSchedule;
import sithclanplugin.eventschedule.SithClanNotificationManager;
import sithclanplugin.ui.SithClanPluginPanel;

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

	@Inject
	private SithClanEventSchedule eventSchedule;

	@Inject
	private SithClanNotificationManager notificationManager;

	private NavigationButton uiNavigationButton;

	private static final String PLUGIN_ICON_PATH = "/icon.png";
	private static final String PLUGIN_TOOLTIP = "Sith Clan Plugin";
	private static final String LOCAL_DIRECTORY_NAME = "sithclanplugin";
	private static final String STORED_SCHEDULE_NAME = "sithclaneventschedule.txt";

	@Override
	protected void startUp() throws Exception {

		// navigation bar plugin icon
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), PLUGIN_ICON_PATH);
		uiNavigationButton = NavigationButton.builder()
				.tooltip(PLUGIN_TOOLTIP)
				.icon(icon)
				.priority(6)
				.panel(uiPanel.get())
				.build();
		clientToolbar.addNavigation(uiNavigationButton);

		// create plugin folder if does not exist
		File localDirectory = new File(RuneLite.RUNELITE_DIR, LOCAL_DIRECTORY_NAME);
		if (!localDirectory.exists())
			// plugin directory
			localDirectory.mkdirs();

		// create saved schedule file if does not exist
		File storedSchedule = new File(localDirectory, STORED_SCHEDULE_NAME);
		if (!storedSchedule.exists())
			// schedule file
			storedSchedule.createNewFile();

		// load schedule if saved, else get new schedule
		// validate API key of Senate members
		boolean hasStoredSchedule = storedSchedule.length() > 0;
		new Thread(() -> {
			int status = hasStoredSchedule ? eventSchedule.parseScheduleFromFile()
					: eventSchedule.parseScheduleFromGet();
			boolean isSenateMember = eventSchedule.validateApiKey();
			SwingUtilities.invokeLater(() -> {
				if (status == SithClanPluginConstants.STATUS_OK) {
					uiPanel.get().getSchedulePanel().displaySchedule();
				}
				uiPanel.get().getSenateButton().setVisible(isSenateMember);
			});
		}).start();
	}

	@Override
	protected void shutDown() throws Exception {
		clientToolbar.removeNavigation(uiNavigationButton);
		notificationManager.shutDown();
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
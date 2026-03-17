package sithclanplugin;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.inject.Inject;
import javax.swing.SwingUtilities;

import com.google.inject.Provider;
import com.google.inject.Provides;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;
import sithclanplugin.eventschedule.SithClanEventSchedule;
import sithclanplugin.eventschedule.SithClanNotificationManager;
import sithclanplugin.ui.SithClanPluginPanel;

@PluginDescriptor(name = "Sith Clan Plugin", description = "Enable the Sith Clan Plugin")
public class SithClanPlugin extends Plugin {

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private SithClanPluginConfig config;

	@Inject
	private WorldService worldService;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private Provider<SithClanPluginPanel> uiPanel;

	@Inject
	private SithClanEventSchedule eventSchedule;

	@Inject
	private SithClanNotificationManager notificationManager;

	private NavigationButton uiNavigationButton;
	private net.runelite.api.World eventLocationWorld;
	private int displaySwitcherAttempts = 0;

	private static final String PLUGIN_ICON_PATH = "/icon.png";
	private static final String PLUGIN_TOOLTIP = "Sith Clan Plugin";
	private static final int DISPLAY_SWITCHER_MAX_ATTEMPTS = 3;

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
		File localDirectory = new File(RuneLite.RUNELITE_DIR, SithClanPluginConstants.LOCAL_DIRECTORY_NAME);
		if (!localDirectory.exists())
			// plugin directory
			localDirectory.mkdirs();

		// create saved schedule file if does not exist
		File storedSchedule = new File(localDirectory, SithClanPluginConstants.STORED_SCHEDULE_NAME);
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
	public void onGameTick(GameTick event) {
		if (eventLocationWorld == null)
			return;
		if (client.getWidget(InterfaceID.Worldswitcher.BUTTONS) == null) {
			client.openWorldHopper();
			if (++displaySwitcherAttempts >= DISPLAY_SWITCHER_MAX_ATTEMPTS) {
				eventLocationWorld = null;
				displaySwitcherAttempts = 0;
			}
		} else {
			client.hopToWorld(eventLocationWorld);
			eventLocationWorld = null;
			displaySwitcherAttempts = 0;
		}
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

	// CUSTOM FUNCTIONS

	/**
	 * Entry point for world hopping to run on client thread
	 * Transfer from EDT to client thread
	 * 
	 * @param worldId int id of world to hop to
	 */
	public void hopTo(int worldId) {
		clientThread.invoke(() -> hop(worldId));
	}

	/**
	 * Finds World from list and passes forward to hop
	 * 
	 * @param worldId int id of world to hop to
	 */
	private void hop(int worldId) {
		WorldResult worldResult = worldService.getWorlds();
		if (worldResult == null) {
			return;
		}
		World world = worldResult.findWorld(worldId);
		if (world == null) {
			return;
		}
		hop(world);
	}

	/**
	 * Hops to provided world on next gametick
	 * 
	 * @param world World world to hop to in game
	 */
	private void hop(World world) {
		assert client.isClientThread();
		final net.runelite.api.World rsWorld = client.createWorld();
		rsWorld.setActivity(world.getActivity());
		rsWorld.setAddress(world.getAddress());
		rsWorld.setId(world.getId());
		rsWorld.setPlayerCount(world.getPlayers());
		rsWorld.setLocation(world.getLocation());
		rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));

		if (client.getGameState() == GameState.LOGIN_SCREEN) {
			client.changeWorld(rsWorld);
			return;
		}
		String chatMessage = new ChatMessageBuilder()
				.append(ChatColorType.NORMAL)
				.append("Quick hopping to World ")
				.append(ChatColorType.HIGHLIGHT)
				.append(Integer.toString(world.getId()))
				.append("..")
				.build();

		chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage(chatMessage)
				.build());
		eventLocationWorld = rsWorld;
		displaySwitcherAttempts = 0;
	}
}
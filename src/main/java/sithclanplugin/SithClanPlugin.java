package sithclanplugin;

import java.awt.image.BufferedImage;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;
import javax.swing.SwingUtilities;

import com.google.gson.Gson;
import com.google.inject.Provider;
import com.google.inject.Provides;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.InterfaceID;
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
import okhttp3.OkHttpClient;
import sithclanplugin.announcements.SithClanAnnouncements;
import sithclanplugin.dto.StartupResponse;
import sithclanplugin.eventschedule.SithClanEventSchedule;
import sithclanplugin.managers.SithClanPluginFileManager;
import sithclanplugin.managers.SithClanPluginNotificationManager;
import sithclanplugin.members.SithClanMemberRoster;
import sithclanplugin.ui.SithClanPluginPanel;
import sithclanplugin.util.SithClanPluginConstants;
import sithclanplugin.util.SithClanPluginUtil;

@Slf4j
@PluginDescriptor(name = "Sith Clan Plugin", description = "Enable the Sith Clan Plugin")
public class SithClanPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private WorldService worldService;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private OkHttpClient httpClient;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private Gson gson;

	@Inject
	private SithClanPluginConfig config;

	@Inject
	private SithClanPluginFileManager fileManager;

	@Inject
	private SithClanPluginNotificationManager notificationManager;

	@Inject
	private SithClanEventSchedule eventSchedule;

	@Inject
	private SithClanAnnouncements announcements;

	@Inject
	private SithClanMemberRoster memberRoster;

	@Inject
	private Provider<SithClanPluginPanel> uiPanel;

	private NavigationButton uiNavigationButton;
	private boolean pendingClanCheck = false;
	private net.runelite.api.World quickHopTargetWorld;
	private int displaySwitcherAttempts = 0;

	private static final String PLUGIN_ICON_PATH = "/icon.png";
	private static final String PLUGIN_TOOLTIP = "Sith Clan Plugin";
	private static final int DISPLAY_SWITCHER_MAX_ATTEMPTS = 3;
	private static final String QUICK_HOP_MESSAGE = "Quick hopping to World "; // trailing space intentional

	/**
	 * RUNELITE FUNCTIONS
	 */

	/**
	 * Runs when plugin starts up
	 */
	@Override
	protected void startUp() throws Exception
	{
		// navigation bar icon
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), PLUGIN_ICON_PATH);
		uiNavigationButton = NavigationButton.builder()
				.tooltip(PLUGIN_TOOLTIP)
				.icon(icon)
				.priority(6)
				.panel(uiPanel.get())
				.build();

		clientToolbar.addNavigation(uiNavigationButton);

		// create plugin directory and config files
		fileManager.initializeFiles();

		// bypass check for testing
		if (SithClanPluginConstants.BYPASS_CLAN_CHECK)
		{
			pendingClanCheck = false;
			SwingUtilities.invokeLater(() -> uiPanel.get().showMainPanel());
		}

		// startup loading
		executor.submit(() ->
		{
			// get startup info and parse
			int status = parseStartupInfo();
			// if fails, load from local file
			if (status != SithClanPluginConstants.STATUS_OK)
			{
				eventSchedule.parseScheduleFromFile();
			}
			// validate API key of Senate members
			boolean isSenateMember = SithClanPluginUtil.validateApiKey(httpClient, config);
			eventSchedule.setSenateMember(isSenateMember);
			announcements.setSenateMember(isSenateMember);
			memberRoster.setSenateMember(isSenateMember);
			SwingUtilities.invokeLater(() ->
			{
				// display event schedule
				uiPanel.get().getSchedulePanel().displaySchedule();
				// display clan announcements
				uiPanel.get().getAnnouncementsPanel().displayAnnouncements();
				// display senate options button if senate
				uiPanel.get().getSenateButton().setVisible(isSenateMember);
			});
		});
	}

	/**
	 * Runs when plugin shuts down
	 */
	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(uiNavigationButton);
		notificationManager.shutDown();
		uiPanel.get().getSchedulePanel().shutDown();
	}

	/**
	 * Runs whenever game state has been changed
	 * 
	 * @param event
	 *                  game state event
	 */
	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			pendingClanCheck = true;
		}
	}

	/**
	 * Runs every game tick
	 * 
	 * @param event
	 *                  GameTick gametick event object
	 */
	@Subscribe
	public void onGameTick(GameTick event)
	{
		// clan check logic
		if (pendingClanCheck)
		{
			if (client.getClanSettings() != null)
			{
				pendingClanCheck = false;
				// if not in clan hide panels
				SwingUtilities.invokeLater(() ->
				{
					if (isInClan())
					{
						uiPanel.get().showMainPanel();
					} else
					{
						uiPanel.get().userNotInClan();
					}
				});
			}

		}

		// world hopping logic
		if (quickHopTargetWorld == null)
		{
			return;
		}
		// open worlds list
		if (client.getWidget(InterfaceID.Worldswitcher.BUTTONS) == null)
		{
			client.openWorldHopper();
			if (++displaySwitcherAttempts >= DISPLAY_SWITCHER_MAX_ATTEMPTS)
			{
				quickHopTargetWorld = null;
				displaySwitcherAttempts = 0;
			}
		} else
		{
			client.hopToWorld(quickHopTargetWorld);
			quickHopTargetWorld = null;
			displaySwitcherAttempts = 0;
		}
	}

	/**
	 * Run whenever players clan channel changes (joined, leaves, kicked)
	 * 
	 * @param event
	 *                  ClanChannelChanged event object
	 */
	@Subscribe
	public void onClanChannelChanged(ClanChannelChanged event)
	{
		SwingUtilities.invokeLater(() ->
		{
			if (isInClan())
			{
				uiPanel.get().showMainPanel();
			} else
			{
				uiPanel.get().userNotInClan();
			}
		});
	}

	/**
	 * Allow config to be accessible from RL settings panel
	 * 
	 * @param configManager
	 *                          ConfigManager configuration manager object
	 * @return ConfigManager plugin configuration
	 */
	@Provides
	SithClanPluginConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SithClanPluginConfig.class);
	}

	/**
	 * CUSTOM FUNCTIONS
	 */

	/**
	 * Entry point to transfer from EDT to client thread
	 * 
	 * @param worldId
	 *                    int id of world to hop to
	 */
	public void hopTo(int worldId)
	{
		clientThread.invoke(() -> hop(worldId));
	}

	/**
	 * Find World from list and passes forward to hop
	 * 
	 * @param worldId
	 *                    int id of world to hop to
	 */
	private void hop(int worldId)
	{
		WorldResult worldResult = worldService.getWorlds();
		if (worldResult == null)
		{
			return;
		}
		World world = worldResult.findWorld(worldId);
		if (world == null)
		{
			return;
		}
		hop(world);
	}

	/**
	 * Hop to provided world on next gametick
	 * 
	 * @param world
	 *                  World world to hop to in game
	 */
	private void hop(World world)
	{
		assert client.isClientThread(); // must be run on client thread

		// creating world object
		final net.runelite.api.World rsWorld = client.createWorld();
		rsWorld.setActivity(world.getActivity());
		rsWorld.setAddress(world.getAddress());
		rsWorld.setId(world.getId());
		rsWorld.setPlayerCount(world.getPlayers());
		rsWorld.setLocation(world.getLocation());
		rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));

		// if logged out can just swap worlds
		if (client.getGameState() == GameState.LOGIN_SCREEN)
		{
			client.changeWorld(rsWorld);
			return;
		}

		// crafting quick hop chat message
		String chatMessage = new ChatMessageBuilder()
				.append(ChatColorType.NORMAL)
				.append(QUICK_HOP_MESSAGE)
				.append(ChatColorType.HIGHLIGHT)
				.append(Integer.toString(world.getId()))
				.append("..")
				.build();

		// posting
		chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage(chatMessage)
				.build());
		quickHopTargetWorld = rsWorld;
		displaySwitcherAttempts = 0;
	}

	/**
	 * Check is player in clan
	 * 
	 * @return boolean if member in clan
	 */
	private boolean isInClan()
	{
		ClanSettings clanSettings = client.getClanSettings();

		// not in a clan
		if (clanSettings == null)
		{
			return false;
		}

		return clanSettings.getName().equalsIgnoreCase(SithClanPluginConstants.CLAN_NAME);
	}

	/**
	 * Parse schedule and announcements on plugin startup
	 * 
	 * @return int status code
	 */
	private int parseStartupInfo()
	{
		log.info("Fetching startup info from server..");
		String jsonStartupInfo = SithClanPluginUtil.sendGetRequest(httpClient, SithClanPluginConstants.STARTUP_URI);
		if (jsonStartupInfo == null)
		{
			log.error("Failed to fetch startup info -- server returned null");
			return SithClanPluginConstants.STATUS_NOT_FOUND;
		}
		log.info("Startup info retrieved successfully, deserializing..");
		StartupResponse startupResponse = gson.fromJson(jsonStartupInfo, StartupResponse.class);
		String scheduleJson = gson.toJson(startupResponse.getStartupSchedule());
		eventSchedule.loadStartupSchedule(startupResponse.getStartupSchedule(), scheduleJson);
		announcements.loadStartupAnnouncements(startupResponse.getStartupAnnouncements());
		log.info("Startup info loaded successfully.");
		return SithClanPluginConstants.STATUS_OK;
	}
}
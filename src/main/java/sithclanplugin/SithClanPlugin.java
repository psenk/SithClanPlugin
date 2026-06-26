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
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.WorldService;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;
import okhttp3.OkHttpClient;
import sithclanplugin.announcements.SithClanAnnouncements;
import sithclanplugin.dto.StartupResponse;
import sithclanplugin.eventschedule.SithClanEventSchedule;
import sithclanplugin.managers.SithClanFileManager;
import sithclanplugin.managers.SithClanNotificationManager;
import sithclanplugin.ui.SithClanMainPanel;
import sithclanplugin.util.SithClanConstants;
import sithclanplugin.util.SithClanState;
import sithclanplugin.util.SithClanUtil;

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
	private MenuManager menuManager;

	@Inject
	private WorldService worldService;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private OkHttpClient httpClient;

	@Inject
	private Gson gson;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private SithClanConfig config;

	@Inject
	private SithClanState state;

	@Inject
	private SithClanAnnouncements announcements;

	@Inject
	private SithClanEventSchedule eventSchedule;

	@Inject
	private SithClanFileManager fileManager;

	@Inject
	private SithClanNotificationManager notificationManager;

	@Inject
	private Provider<SithClanMainPanel> uiPanel;

	private NavigationButton uiNavigationButton;
	private boolean pendingClanCheck = false;
	private net.runelite.api.World quickHopTargetWorld;
	private int displaySwitcherAttempts = 0;

	private static final String PLUGIN_ICON_PATH = "/icon.png";
	private static final String PLUGIN_TOOLTIP = "Sith Clan Plugin";
	private static final String SITH_LOOKUP = "Sith Lookup";
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

		// add member lookup option to menu
		if (config.memberLookupMenu())
		{
			menuManager.addPlayerMenuItem(SITH_LOOKUP);
		}

		// create plugin directory and config files
		fileManager.initializeFiles();

		// bypass check for testing
		if (SithClanConstants.BYPASS_CLAN_CHECK)
		{
			SwingUtilities.invokeLater(() -> uiPanel.get().showMainPanel());
		}

		// startup loading
		executor.submit(() ->
		{
			// get startup info and parse
			int status = parseStartupInfo();

			// if fails, load from local file
			if (status != SithClanConstants.STATUS_OK)
			{
				eventSchedule.parseScheduleFromFile();
			}
			// validate API key of Senate members
			if (!config.senateApiKey().isBlank())
			{
				state.setSenateMember(SithClanUtil.validateSenateApiKey(httpClient, config));
			}
			SwingUtilities.invokeLater(() ->
			{
				// display event schedule
				uiPanel.get().getSchedulePanel().displaySchedule();
				// display clan announcements
				uiPanel.get().getAnnouncementsPanel().displayAnnouncements();
				// display senate options button if senate
				uiPanel.get().getSenateButton().setVisible(state.isSenateMember());
			});

			// allow plugin to work immediately on install
			clientThread.invokeLater(() ->
			{
				if (client.getGameState() == GameState.LOGGED_IN)
				{
					if (client.getClanSettings() != null)
					{
						boolean isInClan = isInClan();
						SwingUtilities.invokeLater(() ->
						{
							if (isInClan)
							{
								uiPanel.get().showMainPanel();
							} else
							{
								uiPanel.get().userNotInClan();
							}
						});
					} else
					{
						pendingClanCheck = true;
					}
				}
			});
		});

		// schedule next event refresh event
		uiPanel.get().getSchedulePanel().startNextEventRefresh();
	}

	/**
	 * Runs when plugin shuts down
	 */
	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(uiNavigationButton);
		notificationManager.shutDown();
		uiPanel.get().getSchedulePanel().stopNextEventRefresh();
		menuManager.removePlayerMenuItem(SITH_LOOKUP);
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

		if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
		{
			state.setPlayerName(null);
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
				boolean isInClan = isInClan();
				// if not in clan hide panels
				SwingUtilities.invokeLater(() ->
				{
					if (isInClan)
					{
						uiPanel.get().showMainPanel();
					} else
					{
						uiPanel.get().userNotInClan();
					}
				});
			}
		}

		// get player name logic
		if (state.getPlayerName() == null)
		{
			clientThread.invokeLater(() ->
			{
				state.setPlayerName(client.getLocalPlayer().getName());
				SwingUtilities.invokeLater(() -> uiPanel.get().getMembersPanel().refreshAboutMeButton());
			});
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
		boolean isInClan = isInClan();
		SwingUtilities.invokeLater(() ->
		{
			if (isInClan)
			{
				uiPanel.get().showMainPanel();
			} else
			{
				uiPanel.get().userNotInClan();
			}
		});
	}

	/**
	 * Called whenever a menu entry is added to a menu
	 * 
	 * @param event
	 *                  MenuEntryAdded event
	 */
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!config.memberLookupMenu())
		{
			return;
		}

		// get event metadata
		final String option = event.getOption();
		final int componentId = event.getActionParam1();
		final int groupId = WidgetUtil.componentToInterface(componentId);

		// checks to determine if menu being created in appropriate user areas (friends
		// list, clan chat, etc.)
		if (groupId == InterfaceID.FRIENDS && option.equals("Delete")
				|| groupId == InterfaceID.CHATCHANNEL_CURRENT
						&& (option.equals("Add ignore") || option.equals("Remove friend"))
				|| groupId == InterfaceID.CHATBOX && (option.equals("Add ignore") || option.equals("Message"))
				|| groupId == InterfaceID.IGNORE && option.equals("Delete")
				|| (componentId == InterfaceID.ClansSidepanel.PLAYERLIST
						|| componentId == InterfaceID.ClansGuestSidepanel.PLAYERLIST)
						&& (option.equals("Add ignore") || option.equals("Remove friend"))
				|| groupId == InterfaceID.PM_CHAT && (option.equals("Add ignore") || option.equals("Message"))
				|| groupId == InterfaceID.GIM_SIDEPANEL && (option.equals("Add friend")
						|| option.equals("Remove friend") || option.equals("Remove ignore")))
		{
			// create custom menu entry
			Menu menu = client.getMenu();
			MenuEntry menuEntry = menu.createMenuEntry(1);
			menuEntry.setOption(SITH_LOOKUP);
			menuEntry.setTarget(event.getTarget());
			menuEntry.setType(MenuAction.RUNELITE);
			menuEntry.setIdentifier(event.getIdentifier());
			menuEntry.onClick(e ->
			{
				String username = Text.removeTags(e.getTarget()).replace("\u00A0", " ");
				SwingUtilities.invokeLater(() ->
				{
					clientToolbar.openPanel(uiNavigationButton);
					uiPanel.get().navigateToMemberCard();
					uiPanel.get().getMembersPanel().searchMemberFromMenu(username);
				});
			});
		}

		if (event.getOption().equals(SITH_LOOKUP) && event.getType() == MenuAction.RUNELITE_PLAYER.getId())
		{
			event.getMenuEntry().onClick(e ->
			{
				String username = Text.removeTags(e.getTarget()).replace("\u00A0", " ");
				SwingUtilities.invokeLater(() ->
				{
					clientToolbar.openPanel(uiNavigationButton);
					uiPanel.get().navigateToMemberCard();
					uiPanel.get().getMembersPanel().searchMemberFromMenu(username);
				});
			});
		}
	}

	/**
	 * Called whenever a config option is changed
	 * 
	 * @param event
	 *                  ConfigChanged event
	 */
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		// return if not relevant to plugin
		if (!event.getGroup().equals("sithclanplugin"))
		{
			return;
		}

		// enable/disable event notification checkboxes
		if (event.getKey().equals("eventNotifications"))
		{
			SwingUtilities.invokeLater(
					() -> uiPanel.get().getSchedulePanel().setCheckboxesEnabled(config.eventNotifications()));
		}

		// enable/disable clan attendance import option
		if (event.getKey().equals("attendanceImport"))
		{
			SwingUtilities
					.invokeLater(() -> uiPanel.get().getEventLogPanel().setImportEnabled(config.attendanceImport()));
		}

		// enable/disable sith lookup menu option
		if (event.getKey().equals("memberLookupMenu"))
		{
			if (config.memberLookupMenu())
			{
				menuManager.addPlayerMenuItem(SITH_LOOKUP);
			} else
			{
				menuManager.removePlayerMenuItem(SITH_LOOKUP);
			}
		}

		// enable senate button
		if (event.getKey().equals("senateApiKey"))
		{
			executor.submit(() ->
			{
				state.setSenateMember(SithClanUtil.validateSenateApiKey(httpClient, config));
				SwingUtilities.invokeLater(() -> uiPanel.get().getSenateButton().setVisible(state.isSenateMember()));
			});
		}
	}

	/**
	 * Allow config to be accessible from RL settings panel
	 * 
	 * @param configManager
	 *                          ConfigManager configuration manager object
	 * @return ConfigManager plugin configuration
	 */
	@Provides
	SithClanConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SithClanConfig.class);
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

		return clanSettings.getName().equalsIgnoreCase(SithClanConstants.CLAN_NAME);
	}

	/**
	 * Parse schedule and announcements on plugin startup
	 * 
	 * @return int status code
	 */
	private int parseStartupInfo()
	{
		log.info("Fetching startup info from server..");
		String jsonStartupInfo = SithClanUtil.sendGetRequest(httpClient, SithClanConstants.STARTUP_URI);
		if (jsonStartupInfo == null)
		{
			log.error("Failed to fetch startup info -- server returned null");
			return SithClanConstants.STATUS_NOT_FOUND;
		}
		log.info("Startup info retrieved successfully, deserializing..");
		StartupResponse startupResponse = gson.fromJson(jsonStartupInfo, StartupResponse.class);
		String scheduleJson = gson.toJson(startupResponse.getResponseSchedule());
		eventSchedule.loadStartupSchedule(startupResponse.getResponseSchedule(), scheduleJson);
		announcements.loadStartupAnnouncements(startupResponse.getResponseAnnouncements());
		log.info("Startup info loaded successfully.");
		return SithClanConstants.STATUS_OK;
	}
}
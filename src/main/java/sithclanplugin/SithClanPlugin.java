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
import java.util.LinkedHashMap;
import java.util.Map;
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
import sithclanplugin.members.SithClanMember;
import sithclanplugin.members.SithClanMemberRoster;
import sithclanplugin.ui.SithClanMainPanel;
import sithclanplugin.util.SithClanConstants;
import sithclanplugin.util.SithClanState;
import sithclanplugin.util.SithClanUtil;

// refactored on july 28

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
	private SithClanMemberRoster memberRoster;

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
	private static final String QUICK_HOP_MESSAGE = "Quick hopping to World "; // trailing space intentional
	private static final int DISPLAY_SWITCHER_MAX_ATTEMPTS = 3;

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

		// sith lookup
		if (config.memberLookupMenu())
		{
			menuManager.addPlayerMenuItem(SITH_LOOKUP);
		}

		// create plugin directory and files
		fileManager.initializeFiles();

		// for testing
		if (SithClanConstants.BYPASS_CLAN_CHECK)
		{
			SwingUtilities.invokeLater(() -> uiPanel.get().showMainPanel());
		}

		// startup loading
		executor.submit(() ->
		{
			int status = parseStartupInfo();

			// if fails, load from local file
			if (status != SithClanConstants.STATUS_OK)
			{
				eventSchedule.parseScheduleFromFile();
			}

			// validate Senate API key
			if (!config.senateApiKey().isBlank())
			{
				state.setSenateMember(SithClanUtil.validateSenateApiKey(httpClient, config));
			}

			SwingUtilities.invokeLater(() ->
			{
				uiPanel.get().getSchedulePanel().displaySchedule();
				uiPanel.get().getAnnouncementsPanel().displayAnnouncements();
				uiPanel.get().getSenateButton().setVisible(state.isSenateMember());
				uiPanel.get().getEventLogPanel().setImportEnabled(config.attendanceImport());
			});

			// work immediately on plugin install
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

		// schedule next event refresh task
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
	 * Runs when game state changes
	 * 
	 * @param event
	 *                  GameStateChanged state change event
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
	 *                  GameTick gametick event
	 */
	@Subscribe
	public void onGameTick(GameTick event)
	{
		// clan check
		if (pendingClanCheck)
		{
			if (client.getClanSettings() != null)
			{
				pendingClanCheck = false;
				boolean isInClan = isInClan();

				if (isInClan)
				{
					SwingUtilities.invokeLater(() -> uiPanel.get().showMainPanel());
					executor.submit(this::checkAnniversaries);
				} else
				{
					SwingUtilities.invokeLater(() -> uiPanel.get().userNotInClan());
				}
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

		// world hopping
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
	 * Runs whenever clan channel changes
	 * 
	 * @param event
	 *                  ClanChannelChanged channel changed event
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
	 * Runs whenever a menu entry is added to a menu
	 * 
	 * @param event
	 *                  MenuEntryAdded menu added event
	 */
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!config.memberLookupMenu())
		{
			return;
		}

		// event metadata
		final String option = event.getOption();
		final int componentId = event.getActionParam1();
		final int groupId = WidgetUtil.componentToInterface(componentId);

		// determine if menu being created in desired user areas
		if (isValidMenuTarget(groupId, componentId, option))
		{
			// create menu entry
			Menu menu = client.getMenu();
			MenuEntry menuEntry = menu.createMenuEntry(1);
			menuEntry.setOption(SITH_LOOKUP);
			menuEntry.setTarget(event.getTarget());
			menuEntry.setType(MenuAction.RUNELITE);
			menuEntry.setIdentifier(event.getIdentifier());
			menuEntry.onClick(e -> performSithLookup(e.getTarget()));
		}

		// is menu created on player
		if (event.getOption().equals(SITH_LOOKUP) && event.getType() == MenuAction.RUNELITE_PLAYER.getId())
		{
			event.getMenuEntry().onClick(e -> performSithLookup(e.getTarget()));
		}
	}

	/**
	 * Runs whenever config changes
	 * 
	 * @param event
	 *                  ConfigChanged config change event
	 */
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		// event not relevant to plugin
		if (!event.getGroup().equals("sithclanplugin"))
		{
			return;
		}

		// event notification checkboxes
		if (event.getKey().equals("eventNotifications"))
		{
			SwingUtilities.invokeLater(
					() -> uiPanel.get().getSchedulePanel().setCheckboxesEnabled(config.eventNotifications()));
		}

		// clan attendance import options
		if (event.getKey().equals("attendanceImport"))
		{
			SwingUtilities
					.invokeLater(() -> uiPanel.get().getEventLogPanel().setImportEnabled(config.attendanceImport()));
		}

		// sith lookup menu option
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

		// senate options
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
	 * Access config from RL side panel
	 * 
	 * @param configManager
	 *                          ConfigManager config manager object
	 * @return SithClanConfig plugin config object
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
	 * Transfer from EDT to client thread
	 * 
	 * @param worldId
	 *                    int id of target hop world
	 */
	public void hopTo(int worldId)
	{
		clientThread.invoke(() -> hop(worldId));
	}

	/**
	 * Find World pass forward to hop
	 * 
	 * @param worldId
	 *                    int id of target hop world
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
	 *                  World target hop world
	 */
	private void hop(World world)
	{
		assert client.isClientThread(); // must be run on client thread

		// create world object
		final net.runelite.api.World rsWorld = client.createWorld();
		rsWorld.setActivity(world.getActivity());
		rsWorld.setAddress(world.getAddress());
		rsWorld.setId(world.getId());
		rsWorld.setPlayerCount(world.getPlayers());
		rsWorld.setLocation(world.getLocation());
		rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));

		// if logged out just swap worlds
		if (client.getGameState() == GameState.LOGIN_SCREEN)
		{
			client.changeWorld(rsWorld);
			return;
		}

		// quick hop chat message
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
	 * Check if player in clan
	 * 
	 * @return boolean is member in clan
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
	 * Checks roster for members with anniversary and posts messages
	 */
	private void checkAnniversaries()
	{
		memberRoster.parseRosterFromGet();

		LinkedHashMap<SithClanMember, Integer> anniversaryMembers = memberRoster.getMembersWithAnniversary();

		for (Map.Entry<SithClanMember, Integer> entry : anniversaryMembers.entrySet())
		{
			postAnniversaryMessage(entry.getKey().getMemberName(), entry.getValue());
		}
	}

	/**
	 * Posts anniversary message to chat
	 * 
	 * @param memberName
	 *                       String members name
	 * @param years
	 *                       int num years to celebrate
	 */
	private void postAnniversaryMessage(String memberName, int years)
	{
		String yearLabel = years == 1 ? " year" : " years";
		String chatMessage = new ChatMessageBuilder()
				.append(ChatColorType.HIGHLIGHT)
				.append(memberName)
				.append(ChatColorType.NORMAL)
				.append(" is celebrating their ")
				.append(ChatColorType.HIGHLIGHT)
				.append(years + yearLabel)
				.append(ChatColorType.NORMAL)
				.append(" anniversary in Sith!").build();

		chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage(chatMessage)
				.build());
	}

	/**
	 * Parse schedule and announcements on startup
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

	/**
	 * If entry added in valid player UI target for Sith Lookup
	 * 
	 * @param groupId
	 *                        int interface group of target component
	 * @param componentId
	 *                        int component id
	 * @param option
	 *                        String menu option string
	 * @return boolean if Sith Lookup can be injected to target
	 */
	private boolean isValidMenuTarget(int groupId, int componentId, String option)
	{
		return
		// friends list
		(groupId == InterfaceID.FRIENDS && option.equals("Delete"))
				// chat box
				|| (groupId == InterfaceID.CHATCHANNEL_CURRENT
						&& (option.equals("Add ignore") || option.equals("Remove friend")))
				// clan chat
				|| (groupId == InterfaceID.CHATBOX && (option.equals("Add ignore") || option.equals("Message")))
				// ignore list
				|| (groupId == InterfaceID.IGNORE && option.equals("Delete"))
				// clan member list
				|| ((componentId == InterfaceID.ClansSidepanel.PLAYERLIST
						|| componentId == InterfaceID.ClansGuestSidepanel.PLAYERLIST)
						&& (option.equals("Add ignore") || option.equals("Remove friend")))
				// private messages
				|| (groupId == InterfaceID.PM_CHAT && (option.equals("Add ignore") || option.equals("Message")))
				// gim chat
				|| (groupId == InterfaceID.GIM_SIDEPANEL
						&& (option.equals("Add friend") || option.equals("Remove friend")
								|| option.equals("Remove ignore")));
	}

	/**
	 * Search member in plugin member panel
	 * 
	 * @param target
	 *                   String raw target string
	 */
	private void performSithLookup(String target)
	{
		// stripping everything but name
		String username = Text.removeTags(target).replace("\u00A0", " ").replaceAll("\\s*\\(.*\\)$", "");
		SwingUtilities.invokeLater(() ->
		{
			clientToolbar.openPanel(uiNavigationButton);
			uiPanel.get().navigateToMemberCard();
			uiPanel.get().getMembersPanel().searchMemberFromMenu(username);
		});
	}
}
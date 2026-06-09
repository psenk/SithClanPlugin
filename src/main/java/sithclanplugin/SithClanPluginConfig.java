package sithclanplugin;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("sithclanplugin")
public interface SithClanPluginConfig extends Config
{
	@ConfigItem(keyName = "eventAlerts", name = "Event Alerts", description = "Enables or disables notifications for upcoming clan events.", position = 1)
	default boolean eventNotifications()
	{
		return false;
	}

	@Range(min = 1, max = 60)
	@ConfigItem(keyName = "eventAlertTimeBuffer", name = "Alert Time Buffer", description = "Sets amount of time in minutes before event notification occurs.", position = 2)
	default int notificationTimeBuffer()
	{
		return 15;
	}

	@ConfigItem(keyName = "memberLookupMenu", name = "Clan Member Lookup", description = "Enables option to lookup clan members in right-click menus.", position = 3)
	default boolean memberLookup()
	{
		return false;
	}

	@ConfigItem(keyName = "eventLogWebhook", name = "Discord Event Log Webhook URL", description = "Discord webhook URL to post event logs", position = 4)
	default String eventLogWebhook()
	{
		return "".trim();
	}

	@ConfigItem(keyName = "senateApiKey", name = "Senate API Key", description = "API Key for access to Senate member plugin options", position = 5)
	default String apiKey()
	{
		return "".trim();
	}
}

package sithclanplugin;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("sithclanplugin")
public interface SithClanPluginConfig extends Config
{
	@ConfigItem(keyName = "eventAlerts", name = "Event Alerts", description = "Enables or disables notifications for upcoming clan events", position = 1)
	default boolean eventNotifications()
	{
		return false;
	}

	@Range(min = 1, max = 60)
	@ConfigItem(keyName = "eventAlertTimeBuffer", name = "Alert Time Buffer", description = "Sets amount of time in minutes before event notification occurs", position = 2)
	default int notificationTimeBuffer()
	{
		return 15;
	}

	@ConfigItem(keyName = "senateapikey", name = "Senate API Key", description = "API Key for access to Senate member plugin options", position = 3)
	default String apiKey()
	{
		return "".trim();
	}
}

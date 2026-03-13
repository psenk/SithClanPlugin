package sithclanplugin;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

// TODO: reshuffle config items on panel (currently alphabetically per name value)

@ConfigGroup("sithclanplugin")
public interface SithClanPluginConfig extends Config {
	@ConfigItem(keyName = "senateapikey", name = "Senate API Key", description = "API Key for posting clan info to server")
	default String apiKey() {
		return "".trim();
	}

	@ConfigItem(keyName = "eventAlerts", name = "Event Alerts", description = "Enables or disables notifications for upcoming clan events")
	default boolean eventNotifications() {
		return false;
	}

	@Range(min = 1, max = 60)
	@ConfigItem(keyName = "eventAlertTimeBuffer", name = "Alert Time Buffer", description = "Sets amount of time in minutes before event notification occurs")
	default int notificationTimeBuffer() {
		return 15;
	}

}

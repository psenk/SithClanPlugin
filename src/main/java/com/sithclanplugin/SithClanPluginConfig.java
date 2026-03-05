package com.sithclanplugin;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

// TODO: reshuffle config items on panel (currently alphabetically per name value)

@ConfigGroup("sithclanplugin")
public interface SithClanPluginConfig extends Config {
	@ConfigItem(keyName = "senateapikey", name = "Senate API Key", description = "API Key for posting clan info to server")
	default String apiKey() {
		return "";
	}

	@ConfigItem(keyName = "eventAlerts", name = "Event Alerts", description = "Enables or disables notifications for upcoming clan events")
	default boolean eventNotifications() {
		return false;
	}

	@ConfigItem(keyName = "eventAlertTimeBuffer", name = "Alert Time Buffer", description = "Sets amount of time before start of event notification occurs")
	default int notificationTimeBuffer() {
		return 15;
	}

}

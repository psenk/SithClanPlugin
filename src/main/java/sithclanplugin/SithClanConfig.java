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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("sithclanplugin")
public interface SithClanConfig extends Config
{
	@ConfigSection(name = "Clan Events", description = "Clan event configuration options.", position = 0)
	String eventsSection = "eventsSection";

	@ConfigItem(keyName = "eventNotifications", name = "Event Alerts", description = "Enables or disables notifications for upcoming clan events.", section = "eventsSection", position = 0)
	default boolean eventNotifications()
	{
		return false;
	}

	@Range(min = 1, max = 60)
	@ConfigItem(keyName = "eventAlertTimeBuffer", name = "Alert Time Buffer", description = "Sets amount of time in minutes before event notification occurs.", section = "eventsSection", position = 1)
	default int eventAlertTimeBuffer()
	{
		return 15;
	}

	@ConfigItem(keyName = "attendanceImport", name = "Import Clan Attendance", description = "Enables the option to import event logs directly from the Clan Event Attendance plugin. "
			+ "WARNING: The 'File Save -> Save Locally' setting must be enabled on the Clan Event Attendance plugin for this to work.", section = "eventsSection", position = 2)
	default boolean attendanceImport()
	{
		return false;
	}

	@ConfigItem(keyName = "eventLogWebhook", name = "Discord Event Log Webhook URL", description = "Discord webhook URL to post event logs", section = "eventsSection", position = 3)
	default String eventLogWebhook()
	{
		return "".trim();
	}

	@ConfigSection(name = "Miscellaneous", description = "Miscellaneous configuration options.", position = 1)
	String miscSection = "miscSection";

	@ConfigItem(keyName = "memberLookupMenu", name = "Clan Member Lookup", description = "Enables option to lookup clan members in right-click menus.", section = "miscSection", position = 0)
	default boolean memberLookupMenu()
	{
		return false;
	}

	@ConfigItem(keyName = "senateApiKey", name = "Senate API Key", description = "API Key for access to Senate member plugin options", section = "miscSection", position = 999)
	default String senateApiKey()
	{
		return "".trim();
	}
}

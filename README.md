# Sith Clan Plugin v1.2.1

A [RuneLite](https://runelite.net/) plugin for members of the Sith clan in Old School Runescape.

| NOTE: This plugin will only work for members of the Sith clan in-game. |
| --- |


## Features

### Side Panel Interface

The plugin includes a dedicated side panel that connects users to each of the it's available features using navigation buttons and dropdown interfaces. The panel is accessible via a Sith clan navigation icon on RuneLite's right edge navigation panel.

![Sidepanel](./assets/side_panel.png "Side Panel")

### Clan Announcements

View announcements about ongoing clan events.  Announcements panel is collapsible by clicking on the Clan Announcements label.

![Clan Announcements](./assets/clan_announcements.png "Clan Announcements")

### Event Schedule

View the clans weekly event schedule and appropriate event information. Event lists can expand or collapse by clicking on the date label.  World links allow quick hopping to the events world. The checkboxes enable or disable an in-game notification for that event, configurable in the plugin settings.

![Event Schedule](./assets/event_schedule.png "Event Schedule")

### Member Info

View individual clan member information or entire clan roster.  Update users about me section allowing for individual flair.

![Member Info](./assets/member_info.png "Member Info")

### Post Event Log

Post event logs from the plugin that will post directly to the Sith Discord server #event-log channel. The format used should match the Discord Code Block option and default format for the [Clan Event Attendance](https://github.com/JoRouss/runelite-ClanEventAttendance) plugin.

![Post Event Log](./assets/event_log.png "Post Event Log")

### Senate (Clan Leadership) Options _(Senate members only)_

Features options that allow for managing the Sith clan plugin info. This section is viewable and usable by Senate members only via an assigned API key saved in the plugin settings.

![Senate Options](./assets/senate_options.png "Senate Options")

## Configuration

Open the RuneLite settings panel by clicking the wrench icon at the top of the right navigation bar and search **Sith Clan Plugin** to access the following options:

| Setting                       | Description                                                        | Default   |
| ----------------------------- | ------------------------------------------------------------------ | --------- |
| Event Alerts                  | Enables or disables notifications for upcoming clan events         | Off       |
| Alert Time Buffer             | Time to receive notification before an event (in minutes) (1–60)   | 15        |
| Clan Member Lookup            | Enables or disables option to lookup clan members via right click  | Off       |
| Discord Event Log Webhook URL | The Discord webhook URL used to post event logs                    | _(empty)_ |
| Senate API Key                | API key granting access to Senate member options                   | _(empty)_ |

| NOTE: The Discord Webhook URL will need to be obtained from a qualified Sith clan member. |
| --- |

| NOTE: Senate API keys will be handed out by the plugins author only. |
| --- |

![Configuration](./assets/plugin_config.png "Configuration")

## Installation

### RuneLite Plugin Hub

1. Open the RuneLite client.
2. Click on the wrench icon at the top of the right navigation bar. (Configuration)
3. Click on the Plugin Hub button. (cord plug icon)
4. Search for "Sith Clan".  Plugin author will be "Kyanize".
5. Click Install.

## Support

For bug reports, questions, and feature requests, please open an issue on the [GitHub Repository](https://github.com/psenk/SithClanPlugin).

## Development

Information from the plugin and RuneLite client is sent to a CloudFlare worker backend managed by the author.  The public repository for this can be found [here](https://github.com/psenk/SithClanPluginAPI).

## License

This project is licensed under the BSD 2-Clause License -- see the LICENSE file for details.

## Acknowledgements

Portions of this plugin were inspired by or derived from:

- [Clan Event Attendance](https://github.com/JoRouss/runelite-ClanEventAttendance) - Licensed under BSD 2-Clause License
- [World Hopper](https://github.com/runelite/runelite/tree/master/runelite-client/src/main/java/net/runelite/client/plugins/worldhopper) - Licensed under BSD 2-Clause License
- [Hiscore Plugin](https://github.com/runelite/runelite/tree/master/runelite-client/src/main/java/net/runelite/client/plugins/hiscore) - Licensed under BSD 2-Clause License

See the LICENSES directory for third-party details.

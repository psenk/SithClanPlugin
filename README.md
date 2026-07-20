# Sith Clan Plugin v1.4

A [RuneLite](https://runelite.net/) plugin for members of the Sith clan in Old School Runescape.

| NOTE: This plugin will only work for members of the Sith clan. |
| --- |

## Features

### Side Panel Interface

The plugin includes a dedicated side panel that connects users to each feature using navigation buttons and dropdown interfaces. The panel is accessible via the Sith clan icon on the RuneLite navigation bar.

![Sidepanel](./assets/side_panel.png "Side Panel")

### Clan Announcements

View clan announcements. Panel is collapsible by clicking on the Clan Announcements label.

![Clan Announcements](./assets/clan_announcements.png "Clan Announcements")

### Event Schedule

View the weekly event schedule and event information. The daily event lists can expand or collapse by clicking on the date label.  Click on world links to quick hop. The checkboxes enable or disable an in-game notification before the event starts, configurable in the plugin settings.

![Event Schedule](./assets/event_schedule.png "Event Schedule")

### Member Info

View individual clan member information or the entire clan roster.  Update your about me section for individual flair.

![Member Info](./assets/member_info.png "Member Info")

### Post Event Log

Post event attendance logs directly to the Sith Discord server. The format used should match the Discord Code Block option and default format for the [Clan Event Attendance](https://github.com/JoRouss/runelite-ClanEventAttendance) plugin.  With the config option enabled, import event logs directly from the Clan Event Attendance plugin.

| NOTE: For the import feature to work, you must have the [Clan Event Attendance](https://github.com/JoRouss/runelite-ClanEventAttendance) plugin installed on RuneLite, with that plugins 'File Save' -> 'Save Locally' setting enabled. |
| --- |

![Post Event Log](./assets/event_log.png "Post Event Log")

### Senate (Clan Leadership) Options

Only Senate members will have access to these features.  Allows for managing the Sith clan plugin info.

![Senate Options](./assets/senate_options.png "Senate Options")

## Configuration

Open the RuneLite settings panel by clicking the wrench icon at the top of the right navigation bar and search **Sith Clan Plugin** to access the following options:

| Setting                       | Description                                                        | Default   |
| :---                          | :---                                                               |      ---: |
| ***Clan Events Section***     | ---                                                                | ---       |
| Event Alerts                  | Enables or disables notifications for upcoming clan events         | Off       |
| Alert Time Buffer             | Time to receive notification before an event (in minutes) (1–60)   | 15        |
| Import Clan Attendance        | Enables or disables importing Clan Event Attendance plugin logs    | Off       |
| Discord Event Log Webhook URL | The Discord webhook URL used to post event logs                    | *(empty)* |
| ***Miscellaneous Section***   | ---                                                                | ---       |
| Clan Member Lookup            | Enables or disables option to lookup clan members via right click  | Off       |
| Senate API Key                | API key granting access to Senate member options                   | *(empty)* |

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

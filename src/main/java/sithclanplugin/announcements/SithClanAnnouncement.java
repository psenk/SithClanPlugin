package sithclanplugin.announcements;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Clan Announcement Object
 */

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SithClanAnnouncement
{
    private int announcementId;
    private String announcementDate;
    private String announcementText;
    private String lastEdited;
}

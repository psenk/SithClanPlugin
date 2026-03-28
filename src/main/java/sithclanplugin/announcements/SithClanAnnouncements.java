package sithclanplugin.announcements;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import com.google.inject.Singleton;

import lombok.Getter;

@Singleton
public class SithClanAnnouncements {

    @Getter
    private ArrayList<SithClanAnnouncement> announcements;

    private ZonedDateTime announcementsUpdated;

    public SithClanAnnouncements() {
        announcements = new ArrayList<>();
        announcementsUpdated = ZonedDateTime.now().withZoneSameInstant(ZoneId.systemDefault());
    }

    /**
     * Loads announcements received during plugin startup
     * 
     * @param announcement ArrayList<SithClanAnnouncements> announcements
     */
    public void loadStartupAnnouncements(ArrayList<SithClanAnnouncement> announcement) {
        this.announcements = announcement;
        this.announcementsUpdated = ZonedDateTime.now().withZoneSameInstant(ZoneId.systemDefault());
    }

    public int parseAnnouncementForPost(String text) {
        return 0;
    }

    /**
     * TODO: FUNCTIONALITY
     * TODO: JAVADOC
     * 
     * @param id
     * @param announcement
     * @return
     */
    public int parseAnnouncementForEdit(int id, String announcement) {
        return 0;
    }

    /**
     * TODO: FUNCTIONALITY
     * TODO: JAVADOC
     * 
     * @param id
     * @return
     */
    public int deleteAnnouncement(int id) {
        return 0;
    }
}

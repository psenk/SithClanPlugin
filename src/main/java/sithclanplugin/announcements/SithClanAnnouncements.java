package sithclanplugin.announcements;

import java.util.ArrayList;

import com.google.inject.Singleton;

@Singleton
public class SithClanAnnouncements {

    private ArrayList<SithClanAnnouncement> announcements;

    public SithClanAnnouncements() {
        announcements = new ArrayList<>();
    }

    /**
     * Loads announcements received during plugin startup
     * 
     * @param announcement ArrayList<SithClanAnnouncements> announcements
     */
    public void loadStartupAnnouncements(ArrayList<SithClanAnnouncement> announcement) {
        this.announcements = announcement;
    }

}

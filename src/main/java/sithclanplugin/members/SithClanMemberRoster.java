package sithclanplugin.members;

import java.util.HashMap;

public class SithClanMemberRoster {

    private HashMap<String, SithClanMember> roster;

    public SithClanMemberRoster() {
        roster = new HashMap<>();
    }

    /**
     * TODO: JAVADOC
     * TODO: FUNCTIONALITY
     * 
     * @return
     */
    private String getMemberRoster() {
        return "";
    }

    /**
     * TODO: JAVADOC
     * TODO: FUNCTIONALITY
     * 
     * @return
     */
    private String postRoster(String jsonData) {
        return "";
    }

    /**
     * TODO: JAVADOC
     * TODO: FUNCTIONALITY
     * 
     * @return
     */
    public int parseRosterFromGet() {
        return 0;
    }

    /**
     * TODO: JAVADOC
     * TODO: FUNCTIONALITY
     * 
     * @return
     */
    public int parseRosterForPost(String rosterInput) {
        return 0;
    }

    /**
     * Searches for member in roster by name
     * TODO: what if member does not exist
     * 
     * @param name String name of member to search for
     * @return SithClanMember member searched for
     */
    public SithClanMember getMemberByName(String memberName) {
        return roster.get(memberName.toLowerCase());
    }

    /**
     * Returns size of member roster
     * 
     * @return int size of member roster
     */
    public int getClanSize() {
        return roster.size();
    }
}

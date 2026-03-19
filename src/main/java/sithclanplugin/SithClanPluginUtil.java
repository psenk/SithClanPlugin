package sithclanplugin;

public class SithClanPluginUtil {

    /**
     * Helper method, removes Discord emojis from text
     * 
     * @param text String text in to search for emojis using regex
     * @return String output without emojis
     */
    public static String removeEmojis(String text) {
        return text.replaceAll(":[a-zA-Z0-9_]+:", "").trim();
    }
}

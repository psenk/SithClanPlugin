package sithclanplugin;

public class SithClanPluginUtil {

    /**
     * Removes Discord emojis from text
     * 
     * @param input String text to search for emojis using regex
     * @return String text without emojis
     */
    public static String removeEmojis(String input) {
        return input.replaceAll(":[a-zA-Z0-9_]+:", "").trim();
    }
}

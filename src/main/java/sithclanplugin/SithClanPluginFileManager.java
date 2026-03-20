package sithclanplugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Singleton;

import net.runelite.client.RuneLite;

@Singleton
public class SithClanPluginFileManager {

    private final File localDirectory;
    private final File storedScheduleFile;
    private final File storedSubscriptionsFile;

    public SithClanPluginFileManager() {
        this.localDirectory = new File(RuneLite.RUNELITE_DIR, SithClanPluginConstants.LOCAL_DIRECTORY_NAME);
        this.storedScheduleFile = new File(localDirectory, SithClanPluginConstants.STORED_SCHEDULE_NAME);
        this.storedSubscriptionsFile = new File(localDirectory, SithClanPluginConstants.STORED_SUBSCRIPTIONS_NAME);
    }

    /**
     * Creates plugin specific files and directories
     * 
     * @throws IOException
     */
    public void initializeFiles() throws IOException {
        // create main plugin directory
        if (!localDirectory.exists())
            localDirectory.mkdirs();

        // create saved schedule file
        if (!storedScheduleFile.exists())
            storedScheduleFile.createNewFile();

        // create event subscriptions file
        if (!storedSubscriptionsFile.exists())
            storedSubscriptionsFile.createNewFile();
    }

    /**
     * Checks if user has a saved schedule to load
     * 
     * @return boolean if schedule is saved
     */
    public boolean hasSavedSchedule() {
        return storedScheduleFile.length() > 0;
    }

    /**
     * Saves schedule to local file for cached loading
     * 
     * @param data String schedule as JSON string
     */
    public void saveScheduleLocally(String data) {
        try (FileWriter fileWriter = new FileWriter(storedScheduleFile)) {
            fileWriter.write(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets event schedule from local file for display
     * 
     * @return String JSON schedule as string
     */
    public String readScheduleFile() {
        try {
            String jsonSchedule = new String(Files.readAllBytes(storedScheduleFile.toPath()));
            return jsonSchedule.isBlank() ? null : jsonSchedule;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Saves list of event subscriptions
     * 
     * @param subscriptions ArrayList<String> list of event subscriptions to save
     */
    private void saveSubscriptions(ArrayList<String> subscriptions) {
        try (FileWriter fileWriter = new FileWriter(storedSubscriptionsFile)) {
            fileWriter.write(new Gson().toJson(subscriptions));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Obtains the list of saved event subscriptions
     * 
     * @return ArrayList<String> list of subscribed events
     */
    private ArrayList<String> loadSubscriptions() {
        try {
            String input = new String(Files.readAllBytes(storedSubscriptionsFile.toPath()));
            if (input.isBlank())
                return new ArrayList<>();
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<String>>() {
            }.getType();
            ArrayList<String> subscriptions = gson.fromJson(input, listType);
            return subscriptions != null ? subscriptions : new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Add new event subscription
     * 
     * @param eventTitle String name of event
     */
    public void addSubscription(String eventTitle) {
        ArrayList<String> subscriptions = loadSubscriptions();
        if (!subscriptions.contains(eventTitle)) {
            subscriptions.add(eventTitle);
            saveSubscriptions(subscriptions);
        }
    }

    /**
     * Remove event subscription
     * 
     * @param eventTitle String name of event
     */
    public void removeSubscription(String eventTitle) {
        ArrayList<String> subscriptions = loadSubscriptions();
        subscriptions.remove(eventTitle);
        saveSubscriptions(subscriptions);
    }

    /**
     * Finds out if user is subscribed to event or not
     * 
     * @param eventTitle String name of event
     * @return boolean whether event is subscribed or not
     */
    public boolean isSubscribed(String eventTitle) {
        ArrayList<String> subscriptions = loadSubscriptions();
        return subscriptions.contains(eventTitle);
    }
}

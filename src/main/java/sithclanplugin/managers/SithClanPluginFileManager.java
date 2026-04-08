package sithclanplugin.managers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import net.runelite.client.RuneLite;
import sithclanplugin.util.SithClanPluginConstants;

@Singleton
public class SithClanPluginFileManager
{
    @Inject
    private Gson gson;

    private final File localDirectory;
    private final File storedScheduleFile;
    private final File storedSubscriptionsFile;
    private ArrayList<String> cachedSubscriptions = null;

    public SithClanPluginFileManager()
    {
        this.localDirectory = new File(RuneLite.RUNELITE_DIR, SithClanPluginConstants.LOCAL_DIRECTORY_NAME);
        this.storedScheduleFile = new File(localDirectory, SithClanPluginConstants.STORED_SCHEDULE_NAME);
        this.storedSubscriptionsFile = new File(localDirectory, SithClanPluginConstants.STORED_SUBSCRIPTIONS_NAME);
    }

    /**
     * SCHEDULE FUNCTIONS
     */

    /**
     * Check if user has a saved event schedule file to load
     * 
     * @return boolean has local schedule file
     */
    public boolean hasSavedSchedule()
    {
        return storedScheduleFile.length() > 0;
    }

    /**
     * Save event schedule to local file
     * 
     * @param data
     *                 String JSON schedule
     */
    public void saveScheduleLocally(String data)
    {
        try (FileWriter fileWriter = new FileWriter(storedScheduleFile))
        {
            // write data to file
            fileWriter.write(data);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Get event schedule from local file
     * 
     * @return String JSON schedule
     */
    public String readScheduleFile()
    {
        try
        {
            // read schedule from file
            String jsonSchedule = Files.readString(storedScheduleFile.toPath(), StandardCharsets.UTF_8);
            return jsonSchedule.isBlank() ? null : jsonSchedule;
        } catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Save list of event subscriptions
     * 
     * @param subscriptions
     *                          ArrayList<String> list of event subscriptions to be
     *                          save
     */
    private void saveSubscriptions(ArrayList<String> subscriptions)
    {
        try (FileWriter fileWriter = new FileWriter(storedSubscriptionsFile))
        {
            // write subscriptions to file
            fileWriter.write(gson.toJson(subscriptions));
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Get list of saved or cached event subscriptions
     * 
     * @return ArrayList<String> list of subscribed events
     */
    private ArrayList<String> loadSubscriptions()
    {
        // return cached subscriptions
        if (cachedSubscriptions != null)
        {
            return cachedSubscriptions;
        }
        try
        {
            // load subscriptions from file
            String input = Files.readString(storedSubscriptionsFile.toPath(), StandardCharsets.UTF_8);

            if (input.isBlank())
            {
                cachedSubscriptions = new ArrayList<>();
                return cachedSubscriptions;
            }
            Type listType = new TypeToken<ArrayList<String>>()
            {
            }.getType();
            cachedSubscriptions = gson.fromJson(input, listType);
            return cachedSubscriptions;
        } catch (Exception e)
        {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Add new event subscription
     * 
     * @param eventTitle
     *                       String title of event
     */
    public void addSubscription(String eventTitle)
    {
        ArrayList<String> subscriptions = loadSubscriptions();
        if (!subscriptions.contains(eventTitle))
        {
            subscriptions.add(eventTitle);
            saveSubscriptions(subscriptions);
        }
    }

    /**
     * Remove event subscription
     * 
     * @param eventTitle
     *                       String title of event
     */
    public void removeSubscription(String eventTitle)
    {
        ArrayList<String> subscriptions = loadSubscriptions();
        subscriptions.remove(eventTitle);
        saveSubscriptions(subscriptions);
    }

    /**
     * Check if user is subscribed to event
     * 
     * @param eventTitle
     *                       String title of event
     * @return boolean whether event is subscribed
     */
    public boolean isSubscribed(String eventTitle)
    {
        ArrayList<String> subscriptions = loadSubscriptions();
        return subscriptions.contains(eventTitle);
    }

    /**
     * MISC FUNCTIONS
     */

    /**
     * Create plugin specific files and directories
     * 
     * @throws IOException
     */
    public void initializeFiles() throws IOException
    {
        // main plugin directory
        if (!localDirectory.exists())
        {
            localDirectory.mkdirs();
        }

        // saved event schedule file
        if (!storedScheduleFile.exists())
        {
            storedScheduleFile.createNewFile();
        }

        // event subscriptions file
        if (!storedSubscriptionsFile.exists())
        {
            storedSubscriptionsFile.createNewFile();
        }
    }
}

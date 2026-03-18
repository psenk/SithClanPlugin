package sithclanplugin;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import net.runelite.client.RuneLite;
import sithclanplugin.eventschedule.SithClanDaySchedule;

// TODO: save event subscriptions
// TODO: load event subscriptions
// SithClanEventSchedule object instead of ArrayList<SithClanDaySchedule>?

@Singleton
public class SithClanPluginFileManager {

    @Inject
    private SithClanNotificationManager notificationManager;

    private ArrayList<SithClanDaySchedule> eventSchedule;
    private final File localDirectory;
    private final File storedScheduleFile;

    @Inject
    public SithClanPluginFileManager(ArrayList<SithClanDaySchedule> eventSchedule) {
        this.eventSchedule = eventSchedule;
        this.localDirectory = new File(RuneLite.RUNELITE_DIR, SithClanPluginConstants.LOCAL_DIRECTORY_NAME);
        this.storedScheduleFile = new File(localDirectory, SithClanPluginConstants.STORED_SCHEDULE_NAME);
    }

    /**
     * Saves schedule to local file for cached loading
     * 
     * @param data String Schedule as JSON string
     */
    public void saveScheduleLocally(String data) {
        try (FileWriter fileWriter = new FileWriter(storedScheduleFile)) {
            fileWriter.write(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets event schedule from local file for display on panel
     * 
     * @return int SithClanPluginConstants status code value
     */
    public int parseScheduleFromFile() {
        try {
            String jsonSchedule = new String(Files.readAllBytes(storedScheduleFile.toPath()));
            if (jsonSchedule.isBlank())
                return SithClanPluginConstants.STATUS_BAD_INPUT;
            Gson gson = new Gson();
            Type scheduleType = new TypeToken<ArrayList<SithClanDaySchedule>>() {
            }.getType();
            this.eventSchedule = gson.fromJson(jsonSchedule, scheduleType);
            notificationManager.scheduleNotifications(eventSchedule);
            return SithClanPluginConstants.STATUS_OK;
        } catch (Exception e) {
            e.printStackTrace();
            return SithClanPluginConstants.STATUS_NOT_FOUND;
        }
    }
}

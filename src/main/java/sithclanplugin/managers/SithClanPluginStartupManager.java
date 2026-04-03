package sithclanplugin.managers;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import okhttp3.OkHttpClient;
import sithclanplugin.announcements.SithClanAnnouncements;
import sithclanplugin.dto.StartupResponse;
import sithclanplugin.eventschedule.SithClanEventSchedule;
import sithclanplugin.util.SithClanPluginConstants;
import sithclanplugin.util.SithClanPluginUtil;

@Singleton
public class SithClanPluginStartupManager
{
    @Inject
    private OkHttpClient httpClient;

    @Inject
    private Gson gson;

    @Inject
    private SithClanEventSchedule eventSchedule;

    @Inject
    private SithClanAnnouncements announcements;

    /**
     * Create and send an HTTP GET request to obtain the plugin startup info
     * 
     * @return String startup info in an HTTP response body
     */
    private String getStartupInfo()
    {
        return SithClanPluginUtil.sendGetRequest(httpClient, SithClanPluginConstants.STARTUP_URI);
    }

    /**
     * Get startup info
     * 
     * @return int SithClanPluginConstants status code value
     */
    public int parseStartupInfoFromGet()
    {
        // get fresh startup info
        String jsonStartupInfo = getStartupInfo();
        if (jsonStartupInfo == null)
        {
            return SithClanPluginConstants.STATUS_NOT_FOUND;
        }
        // convert startup info to JSON
        deserializeStartupInfo(jsonStartupInfo);
        return SithClanPluginConstants.STATUS_OK;
    }

    /**
     * Deserialize startup info from JSON string
     * 
     * @param jsonStartupInfo
     *                            String JSON startup data
     */
    private void deserializeStartupInfo(String jsonStartupInfo)
    {
        // convert startup info to JSON
        StartupResponse startupResponse = gson.fromJson(jsonStartupInfo, StartupResponse.class);
        String scheduleJson = gson.toJson(startupResponse.getStartupSchedule());
        eventSchedule.loadStartupSchedule(startupResponse.getStartupSchedule(), scheduleJson);
        announcements.loadStartupAnnouncements(startupResponse.getStartupAnnouncements());
    }
}

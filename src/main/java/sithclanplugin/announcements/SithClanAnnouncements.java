package sithclanplugin.announcements;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.Getter;
import lombok.Setter;
import okhttp3.OkHttpClient;
import sithclanplugin.SithClanPluginConfig;
import sithclanplugin.util.SithClanPluginConstants;
import sithclanplugin.util.SithClanPluginUtil;

@Getter
@Singleton
public class SithClanAnnouncements
{
    @Inject
    private SithClanPluginConfig config;

    @Inject
    private OkHttpClient httpClient;

    @Inject
    private Gson gson;

    @Setter
    private ZonedDateTime announcementsUpdated;

    @Setter
    private boolean isSenateMember = false;

    private ArrayList<SithClanAnnouncement> announcementsList;
    private LocalDateTime lastTimeAnnouncementsFetched;

    private static final int ANNOUNCEMENTS_FETCH_COOLDOWN_MINUTES = 5;

    public SithClanAnnouncements()
    {
        announcementsList = new ArrayList<>();
        announcementsUpdated = ZonedDateTime.now().withZoneSameInstant(ZoneId.systemDefault());
    }

    /**
     * Create and send an HTTP GET request to obtain announcements
     * 
     * @return String announcements in an HTTP response body
     */
    private String getAnnouncements()
    {
        return SithClanPluginUtil.sendGetRequest(httpClient, SithClanPluginConstants.ANNOUNCEMENTS_URI);
    }

    /**
     * Create and send HTTP POST request to post new announcement
     * 
     * @param jsonData
     *                     String JSON announcement in string format
     * @return String HTTP Response body with status code
     */
    private String postAnnouncement(String jsonData)
    {
        return SithClanPluginUtil.sendPostRequest(httpClient, config.apiKey(), jsonData,
                SithClanPluginConstants.ANNOUNCEMENTS_URI);
    }

    /**
     * Create and send HTTP PUT request to edit announcement
     * 
     * @param jsonData
     *                           String JSON announcement in string format
     * @param announcementId
     *                           int id of announcement
     * @return String HTTP Response body with status code
     */
    private String putAnnouncement(String jsonData, int announcementId)
    {
        String uri = SithClanPluginConstants.ANNOUNCEMENTS_URI + "/" + announcementId;
        return SithClanPluginUtil.sendPutRequest(httpClient, config.apiKey(), jsonData,
                uri);
    }

    /**
     * Create and send HTTP DELETE request to delete announcement
     * 
     * @param announcementId
     *                           int id of announcement
     * @return String HTTP Response body with status code
     */
    private String deleteAnnouncement(int announcementId)
    {
        String uri = SithClanPluginConstants.ANNOUNCEMENTS_URI + "/" + announcementId;
        return SithClanPluginUtil.sendDeleteRequest(httpClient, config.apiKey(), uri);
    }

    /**
     * Get clan announcements
     * Include 5 min rate limiting
     * 
     * @return int SithClanPluginConstants status code value
     */
    public int parseAnnouncementsFromGet()
    {
        // rate limiting
        if (SithClanPluginUtil.isRateLimited(lastTimeAnnouncementsFetched, ANNOUNCEMENTS_FETCH_COOLDOWN_MINUTES,
                isSenateMember))
        {
            return SithClanPluginConstants.STATUS_RATE_LIMITED;
        }

        // get announcements
        String jsonAnnouncements = getAnnouncements();
        if (jsonAnnouncements == null)
        {
            return SithClanPluginConstants.STATUS_NOT_FOUND;
        }
        // convert announcements to JSON
        this.announcementsList = deserializeAnnouncements(jsonAnnouncements);
        // refresh rate limiting timestamp
        this.lastTimeAnnouncementsFetched = LocalDateTime.now();
        return SithClanPluginConstants.STATUS_OK;
    }

    /**
     * Take String input and convert to JSON for posting
     * 
     * @param announcementInput
     *                              String announcement from plugin text box
     * @return int SithClanPluginConstants status code value
     */
    public int parseAnnouncementForPost(String announcementInput)
    {
        if (announcementInput.isBlank())
        {
            return SithClanPluginConstants.STATUS_BAD_INPUT;
        }

        // turn string into announcement object
        SithClanAnnouncement announcement = convertAnnouncement(announcementInput);
        if (announcement == null)
        {
            return SithClanPluginConstants.STATUS_BAD_INPUT;
        }

        // store announcement as JSON object
        String data = gson.toJson(announcement);

        // post announcement
        String response = postAnnouncement(data);
        if (response == null)
        {
            return SithClanPluginConstants.STATUS_NOT_FOUND;
        }
        // save to cache
        String newAnnouncements = getAnnouncements();
        if (newAnnouncements != null)
        {
            this.announcementsList = deserializeAnnouncements(newAnnouncements);
        }
        return SithClanPluginConstants.STATUS_RESOURCE_CREATED;
    }

    /**
     * Take a string input and convert to JSON for editing
     * 
     * @param id
     *                              int id number of announcement
     * @param announcementInput
     *                              String announcement from plugin text box
     * @return int SithClanPluginConstants status code value
     */
    public int parseAnnouncementForEdit(int id, String announcementInput)
    {
        if (announcementInput.isBlank() || id <= 0)
        {
            return SithClanPluginConstants.STATUS_BAD_INPUT;
        }

        // turn string into announcement object
        SithClanAnnouncement announcement = convertAnnouncement(announcementInput, id);
        if (announcement == null)
        {
            return SithClanPluginConstants.STATUS_BAD_INPUT;
        }

        // store announcement as JSON object
        String data = gson.toJson(announcement);

        // put announcement
        String response = putAnnouncement(data, id);
        if (response == null)
        {
            return SithClanPluginConstants.STATUS_NOT_FOUND;
        }
        // replace announcement in cache
        announcementsList.remove(getAnnouncementById(id));
        announcementsList.add(announcement);
        return SithClanPluginConstants.STATUS_OK;
    }

    /**
     * Delete announcement
     * 
     * @param id
     *               int id number of announcement
     * @return SithClanPluginConstants status code value
     */
    public int parseAnnouncementForDelete(int id)
    {
        if (id <= 0)
        {
            return SithClanPluginConstants.STATUS_BAD_INPUT;
        }

        String response = deleteAnnouncement(id);
        if (response == null)
        {
            return SithClanPluginConstants.STATUS_NOT_FOUND;
        }
        // remove from cache
        try
        {
            announcementsList.remove(getAnnouncementById(id));
        } catch (NoSuchElementException e)
        {
            e.printStackTrace();
        }
        return SithClanPluginConstants.STATUS_OK;
    }

    /**
     * Load announcements received during plugin startup
     * 
     * @param announcements
     *                          ArrayList<SithClanAnnouncements> announcements
     */
    public void loadStartupAnnouncements(ArrayList<SithClanAnnouncement> announcements)
    {
        this.announcementsList = announcements;
        setAnnouncementsUpdated(ZonedDateTime.now().withZoneSameInstant(ZoneId.systemDefault()));
        this.lastTimeAnnouncementsFetched = LocalDateTime.now();
    }

    /**
     * Convert string into custom object
     * 
     * @param announcementInput
     *                              String announcement input as string
     * @return SithClanAnnouncement newly created object
     */
    private SithClanAnnouncement convertAnnouncement(String announcementInput)
    {
        SithClanAnnouncement newAnnouncement = new SithClanAnnouncement();
        newAnnouncement.setAnnouncementText(announcementInput.trim());

        return newAnnouncement;
    }

    /**
     * Convert string into custom object with custom id
     * 
     * @param announcementInput
     *                              String announcement input as string
     * @param id
     *                              int announcement id number
     * @return SithClanAnnouncement newly created object
     */

    private SithClanAnnouncement convertAnnouncement(String announcementInput, int id)
    {
        SithClanAnnouncement newAnnouncement = new SithClanAnnouncement();
        newAnnouncement.setAnnouncementText(announcementInput.trim());
        newAnnouncement.setAnnouncementId(id);

        return newAnnouncement;
    }

    /**
     * Deserialize JSON string to announcement object
     * 
     * @param jsonAnnouncements
     *                              JSON string of announcements
     * @return ArrayList<SithClanAnnouncement> deserialized clan announcements
     */
    private ArrayList<SithClanAnnouncement> deserializeAnnouncements(String jsonAnnouncements)
    {
        // convert roster
        Type announcementType = new TypeToken<ArrayList<SithClanAnnouncement>>()
        {
        }.getType();
        return gson.fromJson(jsonAnnouncements,
                announcementType);
    }

    /**
     * Return specific announcement by id
     * 
     * @param id
     *               int announcement id number
     * @return SithClanAnnouncement specific announcement
     */
    private SithClanAnnouncement getAnnouncementById(int id)
    {
        return announcementsList.stream()
                .filter(announcement -> announcement.getAnnouncementId() == id)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Announcement not found with ID: " + id));
    }
}
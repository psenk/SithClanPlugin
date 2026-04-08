package sithclanplugin.members;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import sithclanplugin.SithClanPluginConfig;
import sithclanplugin.dto.RosterResponse;
import sithclanplugin.util.SithClanPluginConstants;
import sithclanplugin.util.SithClanPluginUtil;

/**
 * Member Roster Object
 */

@Slf4j
@Getter
@Singleton
public class SithClanMemberRoster
{
    @Inject
    private OkHttpClient httpClient;

    @Inject
    private Gson gson;

    @Inject
    private SithClanPluginConfig config;

    @Setter
    private boolean isSenateMember;

    private HashMap<String, SithClanMember> roster;
    private ZonedDateTime dateRosterPosted;
    private LocalDateTime lastTimeRosterFetched;

    private static final int ROSTER_FETCH_COOLDOWN_MINUTES = 30;

    public SithClanMemberRoster()
    {
        roster = new HashMap<>();
        dateRosterPosted = null;
    }

    /**
     * HTTP FUNCTIONS
     */

    /**
     * Create and send an HTTP GET request to obtain the event roster
     * 
     * @return String member roster in an HTTP response body
     */
    private String getMemberRoster()
    {
        return SithClanPluginUtil.sendGetRequest(httpClient, SithClanPluginConstants.MEMBER_ROSTER_URI);
    }

    /**
     * Create and send HTTP POST request to post new member roster
     * 
     * @param jsonData
     *                     String JSON member roster in string format
     * @return String HTTP Response body with status code
     */
    private String postMemberRoster(String jsonData)
    {
        return SithClanPluginUtil.sendPostRequest(httpClient, config.apiKey(), jsonData,
                SithClanPluginConstants.MEMBER_ROSTER_URI);
    }

    /**
     * PARSING FUNCTIONS
     */

    /**
     * Get member roster
     * Includes rate limiting of 30 mins
     * 
     * @return int SithClanPluginConstants status code value
     */
    public int parseRosterFromGet()
    {
        // rate limiting
        if (SithClanPluginUtil.isRateLimited(lastTimeRosterFetched, ROSTER_FETCH_COOLDOWN_MINUTES, isSenateMember))
        {
            log.debug("Roster fetch skipped: rate limited");
            return SithClanPluginConstants.STATUS_RATE_LIMITED;
        }

        log.info("Fetching roster from server..");
        // get fresh member roster
        String jsonRoster = getMemberRoster();
        if (jsonRoster == null)
        {
            log.warn("Roster fetch returned null");
            return SithClanPluginConstants.STATUS_NOT_FOUND;
        }
        // convert roster to JSON
        this.roster = deserializeRoster(jsonRoster);
        log.info("Roster loaded successfully");
        return SithClanPluginConstants.STATUS_OK;
    }

    /**
     * Take String input and converts to JSON format for posting
     * 
     * @param rosterInput
     *                        String member roster from plugin text box
     * @return int SithClanPluginConstants status code value
     */
    public int parseRosterForPost(String rosterInput)
    {
        if (rosterInput.isBlank())
        {
            log.warn("Roster post failed: no input");
            return SithClanPluginConstants.STATUS_BAD_INPUT;
        }
        log.info("Posting member roster to server..");
        // split input into list of strings
        String[] rosterInputList = rosterInput.split("\\r?\\n");
        // turn list into member roster
        HashMap<String, SithClanMember> newRoster = convertRoster(rosterInputList);
        if (newRoster == null || newRoster.isEmpty())
        {
            log.warn("Roster conversion failed.");
            return SithClanPluginConstants.STATUS_BAD_INPUT;
        }

        // convert roster to array for worker
        Collection<SithClanMember> rosterOutput = newRoster.values();

        // store roster as JSON object
        String data = gson.toJson(rosterOutput);

        // post roster
        String response = postMemberRoster(data);
        if (response == null)
        {
            return SithClanPluginConstants.STATUS_NOT_FOUND;
        }
        // save roster
        this.roster = newRoster;
        this.dateRosterPosted = ZonedDateTime.now();
        log.info("Member roster posted successfully.");
        return SithClanPluginConstants.STATUS_OK;
    }

    /**
     * MISC FUNCTIONS
     */

    /**
     * Convert member roster String list into custom object list
     * 
     * @param rosterInput
     *                        member roster in String[] list
     * @return HashMap<String, SithClanMember> converted member roster
     */
    private HashMap<String, SithClanMember> convertRoster(String[] rosterInput)
    {

        HashMap<String, SithClanMember> newRoster = new HashMap<>();

        // iterate through roster list
        for (String member : rosterInput)
        {
            member = member.trim();
            if (member.isBlank() || member.startsWith("name,"))
            {
                continue;
            }
            // parse member info
            String[] memberInfo = member.split(",", -1);
            SithClanMember newMember = new SithClanMember();
            newMember.setMemberName(memberInfo[0]);
            newMember.setMemberRank(Integer.parseInt(memberInfo[1]));
            newMember.setMemberCredits(Integer.parseInt(memberInfo[2]));
            // memberInfo[3] is users Discord ID which we are not storing
            newMember.setMemberDateJoined(memberInfo[4]);
            newMember.setMemberAltName(memberInfo[5].isBlank() ? null : memberInfo[5]);
            newMember.setMemberDatePromoted(memberInfo[6].isBlank() ? null
                    : memberInfo[6]);

            // add member to roster
            newRoster.put(newMember.getMemberName().toLowerCase(), newMember);
            if (newMember.getMemberAltName() != null && !newMember.getMemberAltName().isBlank())
            {
                newRoster.put(newMember.getMemberAltName().toLowerCase(), newMember);
            }

        }
        return newRoster;
    }

    /**
     * Deserialize JSON string to HashMap roster
     * 
     * @param jsonRoster
     *                       JSON String of member roster
     * @return HashMap<String, SithClanMember> deserialized member roster
     */
    private HashMap<String, SithClanMember> deserializeRoster(String jsonRoster)
    {
        // convert roster to JSON
        RosterResponse rosterResponse = gson.fromJson(jsonRoster, RosterResponse.class);
        ZonedDateTime utcTime = ZonedDateTime.parse(rosterResponse.getDate());
        this.dateRosterPosted = utcTime.withZoneSameInstant(ZoneId.systemDefault());
        HashMap<String, SithClanMember> roster = new HashMap<>();
        // create member map
        for (SithClanMember member : rosterResponse.getRoster())
        {
            roster.put(member.getMemberName().toLowerCase(), member);
            if (member.getMemberAltName() != null && !member.getMemberAltName().isBlank())
            {
                roster.put(member.getMemberAltName().toLowerCase(), member);
            }
        }
        return roster;
    }

    /**
     * Search for member in roster by name
     * 
     * @param name
     *                 String name of member to search for
     * @return SithClanMember member searched for or null
     */
    public SithClanMember getMemberByName(String memberName)
    {
        return roster.get(memberName.toLowerCase());
    }
}

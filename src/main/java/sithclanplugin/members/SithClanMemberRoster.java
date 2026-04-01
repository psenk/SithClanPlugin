package sithclanplugin.members;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.Getter;
import okhttp3.OkHttpClient;
import sithclanplugin.SithClanPluginConfig;
import sithclanplugin.dto.RosterResponse;
import sithclanplugin.util.SithClanPluginConstants;
import sithclanplugin.util.SithClanPluginUtil;

/**
 * Member Roster Object
 */

@Getter
@Singleton
public class SithClanMemberRoster
{
    @Inject
    private SithClanPluginConfig config;

    @Inject
    private OkHttpClient httpClient;

    @Inject
    private Gson gson;

    private HashMap<String, SithClanMember> roster;
    private ZonedDateTime dateRosterPosted;

    public SithClanMemberRoster()
    {
        roster = new HashMap<>();
        dateRosterPosted = null;
    }

    /**
     * Creates and sends an HTTP GET request to obtain the event roster
     * 
     * @return String member roster in an HTTP response body
     */
    private String getMemberRoster()
    {
        return SithClanPluginUtil.sendGetRequest(httpClient, SithClanPluginConstants.MEMBER_ROSTER_URI);
    }

    /**
     * Creates and sends HTTP POST request to post new member roster
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
     * Gets member roster
     * 
     * TODO: rate limiting?
     * 
     * @return int SithClanPluginConstants status code value
     */
    public int parseRosterFromGet()
    {
        // get fresh member roster
        String jsonRoster = getMemberRoster();
        if (jsonRoster == null)
        {
            return SithClanPluginConstants.STATUS_NOT_FOUND;
        }
        // convert roster to JSON
        this.roster = deserializeRoster(jsonRoster);
        return SithClanPluginConstants.STATUS_OK;
    }

    /**
     * Takes String input and converts to JSON format for posting
     * 
     * @param rosterInput
     *                        String member roster from plugin text box
     * @return int SithClanPluginConstants status code value
     */
    public int parseRosterForPost(String rosterInput)
    {
        if (rosterInput.isBlank())
        {
            return SithClanPluginConstants.STATUS_BAD_INPUT;
        }

        // split input into list of strings
        String[] rosterInputList = rosterInput.split("\\r?\\n");
        // turn list into member roster
        HashMap<String, SithClanMember> newRoster = convertRoster(rosterInputList);
        if (newRoster == null || newRoster.isEmpty())
        {
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
        return SithClanPluginConstants.STATUS_OK;
    }

    /**
     * Converts member roster String list into custom object list
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

        }
        return newRoster;
    }

    /**
     * Deserializes JSON string to HashMap roster
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
        for (SithClanMember member : rosterResponse.getRoster())
        {
            roster.put(member.getMemberName().toLowerCase(), member);
        }
        return roster;
    }

    /**
     * Searches for member in roster by name
     * 
     * @param name
     *                 String name of member to search for
     * @return SithClanMember member searched for
     */
    public SithClanMember getMemberByName(String memberName)
    {
        return roster.get(memberName.toLowerCase());
    }

    /**
     * Returns size of member roster
     * 
     * @return int size of member roster
     */
    public int getClanSize()
    {
        return roster.size();
    }
}

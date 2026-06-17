/*
 * Copyright (c) 2026, Kyanize
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package sithclanplugin.members;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import sithclanplugin.SithClanConfig;
import sithclanplugin.dto.RosterResponse;
import sithclanplugin.util.SithClanConstants;
import sithclanplugin.util.SithClanState;
import sithclanplugin.util.SithClanUtil;

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
    private SithClanConfig config;

    @Inject
    private SithClanState state;

    private HashMap<String, SithClanMember> roster;
    private ZonedDateTime dateRosterPosted;
    private ZonedDateTime lastTimeRosterFetched;

    private static final int ROSTER_FETCH_COOLDOWN_MINUTES = 5;

    public SithClanMemberRoster()
    {
        roster = new HashMap<>();
        dateRosterPosted = null;
        lastTimeRosterFetched = null;
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
        return SithClanUtil.sendGetRequest(httpClient, SithClanConstants.MEMBER_ROSTER_URI);
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
        return SithClanUtil.sendPostRequest(httpClient, config.senateApiKey(), jsonData,
                SithClanConstants.MEMBER_ROSTER_URI);
    }

    /**
     * PARSING FUNCTIONS
     */

    /**
     * Get member roster
     * Includes rate limiting of 5 mins
     * 
     * @return int SithClanPluginConstants status code value
     */
    public int parseRosterFromGet()
    {
        // rate limiting
        if (SithClanUtil.isRateLimited(lastTimeRosterFetched, ROSTER_FETCH_COOLDOWN_MINUTES, state.isSenateMember()))
        {
            log.debug("Roster fetch skipped: rate limited");
            return SithClanConstants.STATUS_RATE_LIMITED;
        }

        log.info("Fetching roster from server..");
        // get fresh member roster
        String jsonRoster = getMemberRoster();
        if (jsonRoster == null)
        {
            return SithClanConstants.STATUS_NOT_FOUND;
        }
        // convert roster to JSON
        this.roster = deserializeRoster(jsonRoster);
        this.lastTimeRosterFetched = ZonedDateTime.now();
        log.info("Roster loaded successfully");
        return SithClanConstants.STATUS_OK;
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
            return SithClanConstants.STATUS_BAD_INPUT;
        }
        log.info("Posting member roster to server..");
        // split input into list of strings
        String[] rosterInputList = rosterInput.split("\\r?\\n");
        // turn list into member roster
        HashMap<String, SithClanMember> newRoster = new HashMap<>();
        try
        {
            newRoster = convertRoster(rosterInputList);
        } catch (Exception e)
        {
            log.warn("Roster conversion failed.");
            return SithClanConstants.STATUS_BAD_INPUT;
        }

        if (newRoster == null || newRoster.isEmpty())
        {
            log.warn("Roster conversion failed.");
            return SithClanConstants.STATUS_BAD_INPUT;
        }

        // convert roster to array for worker
        Collection<SithClanMember> rosterOutput = newRoster.values();

        // store roster as JSON object
        String data = gson.toJson(rosterOutput);

        // post roster
        String response = postMemberRoster(data);
        if (response == null)
        {
            return SithClanConstants.STATUS_NOT_FOUND;
        }
        // save roster
        this.roster = newRoster;
        this.dateRosterPosted = ZonedDateTime.now();
        log.info("Member roster posted successfully.");
        return SithClanConstants.STATUS_OK;
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
        ZonedDateTime utcTime = ZonedDateTime.parse(rosterResponse.getResponseDate());
        this.dateRosterPosted = utcTime.withZoneSameInstant(ZoneId.systemDefault());
        HashMap<String, SithClanMember> roster = new HashMap<>();
        // create member map
        for (SithClanMember member : rosterResponse.getResponseRoster())
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

    /**
     * Returns list of members based on given substring
     * 
     * @param substring
     *                      String substring to search for
     * @return ArrayList<SithClanMember> list of members that contain substring
     */
    public ArrayList<SithClanMember> getMembersBySubstring(String substring)
    {
        LinkedHashSet<SithClanMember> seen = new LinkedHashSet<>();

        for (SithClanMember member : roster.values())
        {
            if (member.getMemberName().toLowerCase().contains(substring)
                    || (member.getMemberAltName() != null
                            && member.getMemberAltName().toLowerCase().contains(substring)))
            {
                seen.add(member);
            }
        }
        return new ArrayList<>(seen);
    }
}

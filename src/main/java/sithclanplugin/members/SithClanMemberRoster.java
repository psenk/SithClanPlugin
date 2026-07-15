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

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

// refactored july 14

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
    private ZonedDateTime lastFetchTime;

    private static final int ROSTER_FETCH_COOLDOWN_MINUTES = 5;

    public SithClanMemberRoster()
    {
        roster = new HashMap<>();
        dateRosterPosted = null;
        lastFetchTime = null;
    }

    /**
     * HTTP FUNCTIONS
     */

    /**
     * Create and send HTTP GET request for member roster
     * 
     * @return String HTTP response with member roster
     */
    private String getMemberRoster()
    {
        return SithClanUtil.sendGetRequest(httpClient, SithClanConstants.MEMBER_ROSTER_URI);
    }

    /**
     * Create and send HTTP POST request to post member roster
     * 
     * @param jsonData
     *                     String JSON member roster as string
     * @return String HTTP Response with status code
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
     * @return int SithClanPluginConstants status code
     */
    public int parseRosterFromGet()
    {
        // rate limiting
        if (SithClanUtil.isRateLimited(lastFetchTime, ROSTER_FETCH_COOLDOWN_MINUTES, state.isSenateMember()))
        {
            log.debug("Roster fetch skipped: rate limited");
            return SithClanConstants.STATUS_RATE_LIMITED;
        }

        log.info("Fetching roster..");
        String jsonRoster = getMemberRoster();
        if (jsonRoster == null)
        {
            log.error("Error obtaining member roster");
            return SithClanConstants.STATUS_NOT_FOUND;
        }
        // convert roster to JSON
        this.roster = deserializeRoster(jsonRoster);
        this.lastFetchTime = ZonedDateTime.now();
        log.info("Roster loaded successfully");
        return SithClanConstants.STATUS_OK;
    }

    /**
     * Convert String input to JSON format for posting
     * 
     * @param rosterInput
     *                        String member roster from Senate panel
     * @return int status code
     */
    public int parseRosterForPost(String rosterInput)
    {
        if (rosterInput.isBlank())
        {
            log.warn("Roster post failed: no input");
            return SithClanConstants.STATUS_BAD_INPUT;
        }
        log.info("Posting member roster..");

        String[] rosterInputList = rosterInput.split("\\r?\\n");
        // convert into member roster
        HashMap<String, SithClanMember> memberRoster = new HashMap<>();
        try
        {
            memberRoster = convertRoster(rosterInputList);
        } catch (Exception e)
        {
            log.warn("Roster conversion failed");
            return SithClanConstants.STATUS_BAD_INPUT;
        }

        if (memberRoster == null || memberRoster.isEmpty())
        {
            log.warn("Roster conversion failed");
            return SithClanConstants.STATUS_BAD_INPUT;
        }

        // convert to array for CloudFlare worker
        Collection<SithClanMember> rosterOutput = memberRoster.values();
        // store as JSON object
        String data = gson.toJson(rosterOutput);

        String response = postMemberRoster(data);
        if (response == null)
        {
            log.error("Roster post failed");
            return SithClanConstants.STATUS_NOT_FOUND;
        }
        // save roster
        this.roster = memberRoster;
        this.dateRosterPosted = ZonedDateTime.now();
        log.info("Member roster posted successfully");
        return SithClanConstants.STATUS_OK;
    }

    /**
     * MISC FUNCTIONS
     */

    /**
     * Convert member roster input list to SithClanMember HashMap
     * 
     * @param rosterInput
     *                        String[] member roster list
     * @return HashMap<String, SithClanMember> member roster map
     */
    private HashMap<String, SithClanMember> convertRoster(String[] rosterInput)
    {
        HashMap<String, SithClanMember> memberRoster = new HashMap<>();

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
            memberRoster.put(newMember.getMemberName().toLowerCase(), newMember);
            if (newMember.getMemberAltName() != null && !newMember.getMemberAltName().isBlank())
            {
                memberRoster.put(newMember.getMemberAltName().toLowerCase(), newMember);
            }
        }
        return memberRoster;
    }

    /**
     * Deserialize JSON string to HashMap roster
     * 
     * @param jsonRoster
     *                       String member roster JSON
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
     * Search for member by name
     * 
     * @param name
     *                 String name of member to search for
     * @return SithClanMember member or null
     */
    public SithClanMember getMemberByName(String memberName)
    {
        return roster.get(memberName.toLowerCase());
    }

    /**
     * Returns list of members based onsubstring
     * 
     * @param substring
     *                      String substring to search for
     * @return ArrayList<SithClanMember> list of members
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

    /**
     * Creates map of all members with anniversaries today
     * 
     * @return LinkedHashMap map of members and their # year anniversary
     */
    public LinkedHashMap<SithClanMember, Integer> getMembersWithAnniversary()
    {
        LinkedHashMap<SithClanMember, Integer> anniversaryMembers = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        // removing alts from member list
        for (SithClanMember member : new LinkedHashSet<>(roster.values()))
        {
            try
            {
                // calc date
                LocalDate joinDate = LocalDate.parse(member.getMemberDateJoined(), SithClanConstants.DATE_FORMATTER);
                int yearsSinceJoined = Period.between(joinDate, today).getYears();

                // if joined date is today
                if (joinDate.getMonth() == today.getMonth() && joinDate.getDayOfMonth() == today.getDayOfMonth()
                        && yearsSinceJoined >= 1)
                {
                    anniversaryMembers.put(member, yearsSinceJoined);
                }
            } catch (Exception e)
            {
                log.warn("Failed to parse join date for member: {}", member.getMemberName());
            }
        }
        return anniversaryMembers;
    }
}

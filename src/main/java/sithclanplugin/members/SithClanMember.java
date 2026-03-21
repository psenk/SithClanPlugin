package sithclanplugin.members;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Clan Member Object
 */

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SithClanMember {

    private String username;
    private int clanRank;
    private int clanCredits;
    private long discordUserId;
    private LocalDate dateJoinedClan;
    private String altUsername = null;
    private LocalDate datePromoted = null;
}

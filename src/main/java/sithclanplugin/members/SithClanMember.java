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

    private String memberName;
    private int memberRank;
    private int memberCredits;
    private long memberDiscordId;
    private LocalDate memberDateJoined;
    private String memberAltName = null;
    private LocalDate memberDatePromoted = null;
}

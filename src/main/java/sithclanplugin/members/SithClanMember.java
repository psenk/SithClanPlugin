package sithclanplugin.members;

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
    private String memberDateJoined;
    private String memberAltName = null;
    private String memberDatePromoted = null;
}

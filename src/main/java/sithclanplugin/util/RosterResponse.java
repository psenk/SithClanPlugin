package sithclanplugin.util;

import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sithclanplugin.members.SithClanMember;

/**
 * Roster response DTO
 */

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RosterResponse {

    private ArrayList<SithClanMember> roster;
    private String date;
}

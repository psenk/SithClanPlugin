package sithclanplugin.members;

import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

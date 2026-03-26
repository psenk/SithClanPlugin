package sithclanplugin.util;

import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sithclanplugin.members.SithClanMember;

/**
 * Custom class for roster posting response
 */

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RosterResponse {

    private ArrayList<SithClanMember> roster;
    private String date;
}

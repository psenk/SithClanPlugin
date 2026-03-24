package sithclanplugin.members;

import java.util.ArrayList;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RosterResponse {

    private ArrayList<SithClanMember> roster;
    private String date;
}

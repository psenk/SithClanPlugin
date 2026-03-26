package sithclanplugin.dto;

import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sithclanplugin.announcements.SithClanAnnouncement;
import sithclanplugin.eventschedule.SithClanDaySchedule;

/**
 * Startup Response DTO
 */

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StartupResponse {

    private ArrayList<SithClanDaySchedule> startupSchedule;
    private ArrayList<SithClanAnnouncement> startupAnnouncements;
}

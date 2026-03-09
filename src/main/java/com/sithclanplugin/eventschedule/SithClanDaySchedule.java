package com.sithclanplugin.eventschedule;

import java.util.ArrayList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Object for each days schedule
 */

@Getter
@Setter
@NoArgsConstructor
public class SithClanDaySchedule {

    String date;
    ArrayList<SithClanEvent> events;
}

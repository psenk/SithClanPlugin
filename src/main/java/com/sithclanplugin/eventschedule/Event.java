package com.sithclanplugin.eventschedule;

import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Event Object
 */

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    String eventTitle;
    String eventTime;
    String eventHost = "";
    ArrayList<String> eventMiscInfo = new ArrayList<>();
    String eventLocation = "";
    boolean eventRepeated = false;
}

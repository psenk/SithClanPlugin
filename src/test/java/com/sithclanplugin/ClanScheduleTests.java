package com.sithclanplugin;

import com.sithclanplugin.eventschedule.*;

import org.junit.Test;
import static org.junit.Assert.*;

public class ClanScheduleTests {
    
    @Test
    public void testClassExists() {
        EventSchedule clanSchedule = new EventSchedule();
        assertNotNull(clanSchedule);
    }
}

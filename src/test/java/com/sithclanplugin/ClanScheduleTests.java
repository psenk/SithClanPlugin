package com.sithclanplugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sithclanplugin.eventschedule.SithClanEvent;
import com.sithclanplugin.eventschedule.SithClanEventSchedule;

public class ClanScheduleTests {

    private static SithClanEventSchedule eventSchedule;
    private static ArrayList<String> testSchedule;
    private static SithClanEvent testEvent;
    private static final String testSchedulePath = "src\\test\\resources\\testschedule.txt";

    @BeforeClass
    public static void setup() {
        eventSchedule = new SithClanEventSchedule();
        testSchedule = eventSchedule.readScheduleFromFile(testSchedulePath);
        testEvent = new SithClanEvent("Mahogany Homes", "12:00 PM", "Sasa254", new ArrayList<>(), ":earth_americas: W491",
                true);

    }

    @AfterClass
    public static void teardown() {

    }

    @Test
    public void testScheduleObjectCreated() {
        assertNotNull(eventSchedule);
    }

    @Test
    public void testScheduleContainerCreated() {
        assertNotNull(eventSchedule);
        assertNotNull(eventSchedule.getSchedule());
        assertTrue(eventSchedule.getSchedule().isEmpty());
    }

    @Test
    public void testScheduleReadInputFileSuccessful() {
        assertNotNull(testSchedule);
        assertFalse(testSchedule.isEmpty());
    }

    @Test
    public void testPrintRawSchedule() {
        eventSchedule.printRawSchedule();
    }

    @Test
    public void testConvertScheduleToEvents() {
        String testDateOne = "1/1/2026";
        String testDateTwo = "1/3/2026";
        String testDateThree = "1/7/2026";

        assertTrue(eventSchedule.getSchedule().size() == 7);
        assertTrue(eventSchedule.getSchedule().get(testDateOne).size() == 1);
        assertTrue(eventSchedule.getSchedule().get(testDateTwo).size() == 0);
        assertTrue(eventSchedule.getSchedule().get(testDateThree).size() == 4);
    }

    @Test
    public void testEventsConvertedProperly() {
        String testDate = "1/1/2026";
        eventSchedule.inputSchedule(testSchedule);
        assertEquals(eventSchedule.getSchedule().get(testDate).get(0).getEventTitle(), testEvent.getEventTitle());
        assertEquals(eventSchedule.getSchedule().get(testDate).get(0).getEventTime(), testEvent.getEventTime());
        assertEquals(eventSchedule.getSchedule().get(testDate).get(0).getEventHost(), testEvent.getEventHost());
        assertEquals(eventSchedule.getSchedule().get(testDate).get(0).getEventLocation(), testEvent.getEventLocation());
        assertEquals(eventSchedule.getSchedule().get(testDate).get(0).getEventMiscInfo(), testEvent.getEventMiscInfo());
        assertEquals(eventSchedule.getSchedule().get(testDate).get(0).isEventRepeated(), testEvent.isEventRepeated());
    }

    @Test
    public void testPrintConvertedSchedule() {
        eventSchedule.printConvertedSchedule();
    }
}

package com.sithclanplugin.eventschedule;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * Event Schedule Object
 */

@Setter
@Getter
public class SithClanEventSchedule {

    private Map<String, ArrayList<SithClanEvent>> schedule;

    public SithClanEventSchedule() {
        schedule = new LinkedHashMap<>();
    }

    /**
     * Takes the event schedule list, converts to list of event objects,
     * then adds list to schedule map with date as the key.
     * 
     * @param scheduleList event schedule as arraylist of strings
     */
    // TODO: refactor this, too long
    public void inputSchedule(ArrayList<String> scheduleList) {
        String eventDate = "";

        for (int i = 0; i < scheduleList.size(); i++) {
            String line = scheduleList.get(i);
            if (line.startsWith("--")) {
                eventDate = line.substring(2).trim();
                ArrayList<SithClanEvent> eventsForToday = new ArrayList<>();
                this.schedule.put(eventDate, eventsForToday);
                continue;
            }
            if (line.startsWith("-")) {
                SithClanEvent newEvent = new SithClanEvent();
                ArrayList<String> eventMiscInfo = new ArrayList<>();

                newEvent.setEventTitle(line.substring(1).trim());
                i++;
                newEvent.setEventTime(scheduleList.get(i).trim());
                i++;
                if (scheduleList.get(i).startsWith("Hosted by:")) {
                    newEvent.setEventHost(scheduleList.get(i).substring(11).trim());
                    i++;
                }
                while (!scheduleList.get(i).startsWith(":")) {
                    eventMiscInfo.add(scheduleList.get(i).trim());
                    i++;
                }
                newEvent.setEventLocation(scheduleList.get(i).trim());
                i++;
                if (scheduleList.get(i).startsWith("**")) {
                    newEvent.setEventRepeated(true);
                } else {
                    i--;
                }
                newEvent.setEventMiscInfo(eventMiscInfo);
                this.schedule.get(eventDate).add(newEvent);
            }
        }
    }

    /**
     * Reads the schedule text file and puts it into a list for later manipulation.
     * 
     * @param path string path of .txt file
     * @return arraylist containing contents of .txt file as strings
     */
    public ArrayList<String> readScheduleFromFile(String path) {
        ArrayList<String> newSchedule = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    newSchedule.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading file.");
        }

        return newSchedule;
    }

    public void printRawSchedule() {
        // TODO: remove later
        ArrayList<String> schedule = readScheduleFromFile("src\\test\\resources\\testschedule.txt");
        for (String line : schedule) {
            System.out.println(line);
        }
    }

    public void printConvertedSchedule() {
        // TODO: remove later
        ArrayList<String> schedule = readScheduleFromFile("src\\test\\resources\\testschedule.txt");
        this.inputSchedule(schedule);
        for (Map.Entry<String, ArrayList<SithClanEvent>> entry : this.schedule.entrySet()) {
            System.out.println("Date: " + entry.getKey());
            System.out.println("Events: ");
            for (SithClanEvent clanEvent : entry.getValue()) {
                System.out.println(" - Event Title: " + clanEvent.eventTitle);
                System.out.println(" - Event Time: " + clanEvent.eventTime);
                System.out.println(" - Event Host: " + clanEvent.eventHost);
                System.out.println(" - Misc Info: " + clanEvent.eventMiscInfo);
                System.out.println(" - Event Location: " + clanEvent.eventLocation);
                System.out.println(" - Repeated Weekly: " + clanEvent.eventRepeated);
                System.out.println();
            }
            System.out.println();
        }
    }

}

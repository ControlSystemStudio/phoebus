package org.phoebus.pv.alarm;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AlarmPVInfoTest {

    String onlyRoot = "root";
    String oneNode = "root/firstNode";
    String twoNode = "root/firstNode/secondNode";
    String leaf = "root/firstNode/secondNode/alarmPV";

    @Test
    public void parsePath()
    {
        AlarmPVInfo alarmPVInfo = AlarmPVInfo.of(onlyRoot);
        assertEquals(
                alarmPVInfo.getRoot(), "root",
                "Failed to parse the root info for pv: " + onlyRoot);
        assertEquals(
                alarmPVInfo.getPath(), Optional.empty(),
                "Failed to parse the path info for pv: " + onlyRoot);

        alarmPVInfo = AlarmPVInfo.of(oneNode);
        assertEquals(
                alarmPVInfo.getRoot(), "root",
                "Failed to parse the root info for pv: " + oneNode);
        assertEquals(
                alarmPVInfo.getPath(), Optional.of("/firstNode"),
                "Failed to parse the path info for pv: " + oneNode);

        alarmPVInfo = AlarmPVInfo.of(twoNode);
        assertEquals(
                alarmPVInfo.getRoot(), "root",
                "Failed to parse the root info for pv: " + twoNode);
        assertEquals(
                alarmPVInfo.getPath(), Optional.of("/firstNode/secondNode"),
                "Failed to parse the path info for pv: " + twoNode);

        alarmPVInfo = AlarmPVInfo.of(leaf);
        assertEquals(
                alarmPVInfo.getRoot(), "root",
                "Failed to parse the root info for pv: " + leaf);
        assertEquals(
                alarmPVInfo.getPath(), Optional.of("/firstNode/secondNode/alarmPV"),
                "Failed to parse the path info for pv: " + leaf);

    }

    /**
     * Test the mapping of the pv paths to the complete paths as encoded in the messages
     * coming from the alarm server
     */
    @Test
    public void parseCompletePath()
    {
        AlarmPVInfo alarmPVInfo = AlarmPVInfo.of(onlyRoot);
        assertEquals(
                "/root",
                          alarmPVInfo.getCompletePath(),
                "Failed to parse the complete path info for pv: " + onlyRoot);

        alarmPVInfo = AlarmPVInfo.of(oneNode);
        assertEquals(
                "/root/firstNode",
                alarmPVInfo.getCompletePath(),
                "Failed to parse the complete path info for pv: " + onlyRoot);

        alarmPVInfo = AlarmPVInfo.of(twoNode);
        assertEquals(
                "/root/firstNode/secondNode",
                alarmPVInfo.getCompletePath(),
                "Failed to parse the complete path info for pv: " + onlyRoot);

        alarmPVInfo = AlarmPVInfo.of(leaf);
        assertEquals(
                "/root/firstNode/secondNode/alarmPV",
                alarmPVInfo.getCompletePath(),
                "Failed to parse the complete path info for pv: " + onlyRoot);
    }

    String activeField = "active";
    String stateField = "state";
    String enableField ="enabled";
    String durationField ="duration";

    /**
     * Test if the special fields of alarm pv names are correctly parsed
     */
    @Test
    public void parseFields()
    {
        AlarmPVInfo alarmPVInfo = AlarmPVInfo.of(onlyRoot+"."+activeField);
        assertEquals(
                alarmPVInfo.getRoot(), "root",
                "Failed to parse the root info for pv: " + onlyRoot);
        assertEquals(
                alarmPVInfo.getPath(), Optional.empty(),
                "Failed to parse the path info for pv: " + onlyRoot);
        assertEquals(
                "/root",
                alarmPVInfo.getCompletePath(),
                "Failed to parse the complete path info for pv: " + onlyRoot);
        assertEquals(
                Optional.of(activeField),
                alarmPVInfo.getField(),
                "Failed to parse the field path info for pv: " + onlyRoot);

        alarmPVInfo = AlarmPVInfo.of(onlyRoot+"."+stateField);
        assertEquals(
                alarmPVInfo.getRoot(), "root",
                "Failed to parse the root info for pv: " + onlyRoot);
        assertEquals(
                alarmPVInfo.getPath(), Optional.empty(),
                "Failed to parse the path info for pv: " + onlyRoot);
        assertEquals(
                "/root",
                alarmPVInfo.getCompletePath(),
                "Failed to parse the complete path info for pv: " + onlyRoot);
        assertEquals(
                Optional.of(stateField),
                alarmPVInfo.getField(),
                "Failed to parse the field path info for pv: " + onlyRoot);

        alarmPVInfo = AlarmPVInfo.of(onlyRoot+"."+enableField);
        assertEquals(
                alarmPVInfo.getRoot(), "root",
                "Failed to parse the root info for pv: " + onlyRoot);
        assertEquals(
                alarmPVInfo.getPath(), Optional.empty(),
                "Failed to parse the path info for pv: " + onlyRoot);
        assertEquals(
                "/root",
                alarmPVInfo.getCompletePath(),
                "Failed to parse the complete path info for pv: " + onlyRoot);
        assertEquals(
                Optional.of(enableField),
                alarmPVInfo.getField(),
                "Failed to parse the field path info for pv: " + onlyRoot);

        alarmPVInfo = AlarmPVInfo.of(onlyRoot+"."+durationField);
        assertEquals(
                alarmPVInfo.getRoot(), "root",
                "Failed to parse the root info for pv: " + onlyRoot);
        assertEquals(
                alarmPVInfo.getPath(), Optional.empty(),
                "Failed to parse the path info for pv: " + onlyRoot);
        assertEquals(
                "/root",
                alarmPVInfo.getCompletePath(),
                "Failed to parse the complete path info for pv: " + onlyRoot);
        assertEquals(
                Optional.of(durationField),
                alarmPVInfo.getField(),
                "Failed to parse the field path info for pv: " + onlyRoot);
    }
}
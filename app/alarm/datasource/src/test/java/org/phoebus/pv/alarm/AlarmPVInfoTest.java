package org.phoebus.pv.alarm;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class AlarmPVInfoTest {

    String onlyRoot = "root";
    String oneNode = "root/firstNode";
    String twoNode = "root/firstNode/secondNode";
    String leaf = "root/firstNode/secondNode/alarmPV";

    @Test
    public void parsePath()
    {
        AlarmPVInfo alarmPVInfo = AlarmPVInfo.of(onlyRoot);
        assertEquals("Failed to parse the root info for pv: " + onlyRoot,
                alarmPVInfo.getRoot(), "root");
        assertEquals("Failed to parse the path info for pv: " + onlyRoot,
                alarmPVInfo.getPath(), Optional.empty());

        alarmPVInfo = AlarmPVInfo.of(oneNode);
        assertEquals("Failed to parse the root info for pv: " + oneNode,
                alarmPVInfo.getRoot(), "root");
        assertEquals("Failed to parse the path info for pv: " + oneNode,
                alarmPVInfo.getPath(), Optional.of("/firstNode"));

        alarmPVInfo = AlarmPVInfo.of(twoNode);
        assertEquals("Failed to parse the root info for pv: " + twoNode,
                alarmPVInfo.getRoot(), "root");
        assertEquals("Failed to parse the path info for pv: " + twoNode,
                alarmPVInfo.getPath(), Optional.of("/firstNode/secondNode"));

        alarmPVInfo = AlarmPVInfo.of(leaf);
        assertEquals("Failed to parse the root info for pv: " + leaf,
                alarmPVInfo.getRoot(), "root");
        assertEquals("Failed to parse the path info for pv: " + leaf,
                alarmPVInfo.getPath(), Optional.of("/firstNode/secondNode/alarmPV"));

    }

    /**
     * Test the mapping of the pv paths to the complete paths as encoded in the messages
     * coming from the alarm server
     */
    @Test
    public void parseCompletePath()
    {
        AlarmPVInfo alarmPVInfo = AlarmPVInfo.of(onlyRoot);
        assertEquals("Failed to parse the complete path info for pv: " + onlyRoot,
                "/root",
                          alarmPVInfo.getCompletePath());

        alarmPVInfo = AlarmPVInfo.of(oneNode);
        assertEquals("Failed to parse the complete path info for pv: " + onlyRoot,
                "/root/firstNode",
                alarmPVInfo.getCompletePath());

        alarmPVInfo = AlarmPVInfo.of(twoNode);
        assertEquals("Failed to parse the complete path info for pv: " + onlyRoot,
                "/root/firstNode/secondNode",
                alarmPVInfo.getCompletePath());

        alarmPVInfo = AlarmPVInfo.of(leaf);
        assertEquals("Failed to parse the complete path info for pv: " + onlyRoot,
                "/root/firstNode/secondNode/alarmPV",
                alarmPVInfo.getCompletePath());
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
        assertEquals("Failed to parse the root info for pv: " + onlyRoot,
                alarmPVInfo.getRoot(), "root");
        assertEquals("Failed to parse the path info for pv: " + onlyRoot,
                alarmPVInfo.getPath(), Optional.empty());
        assertEquals("Failed to parse the complete path info for pv: " + onlyRoot,
                "/root",
                alarmPVInfo.getCompletePath());
        assertEquals("Failed to parse the field path info for pv: " + onlyRoot,
                Optional.of(activeField),
                alarmPVInfo.getField());

        alarmPVInfo = AlarmPVInfo.of(onlyRoot+"."+stateField);
        assertEquals("Failed to parse the root info for pv: " + onlyRoot,
                alarmPVInfo.getRoot(), "root");
        assertEquals("Failed to parse the path info for pv: " + onlyRoot,
                alarmPVInfo.getPath(), Optional.empty());
        assertEquals("Failed to parse the complete path info for pv: " + onlyRoot,
                "/root",
                alarmPVInfo.getCompletePath());
        assertEquals("Failed to parse the field path info for pv: " + onlyRoot,
                Optional.of(stateField),
                alarmPVInfo.getField());

        alarmPVInfo = AlarmPVInfo.of(onlyRoot+"."+enableField);
        assertEquals("Failed to parse the root info for pv: " + onlyRoot,
                alarmPVInfo.getRoot(), "root");
        assertEquals("Failed to parse the path info for pv: " + onlyRoot,
                alarmPVInfo.getPath(), Optional.empty());
        assertEquals("Failed to parse the complete path info for pv: " + onlyRoot,
                "/root",
                alarmPVInfo.getCompletePath());
        assertEquals("Failed to parse the field path info for pv: " + onlyRoot,
                Optional.of(enableField),
                alarmPVInfo.getField());

        alarmPVInfo = AlarmPVInfo.of(onlyRoot+"."+durationField);
        assertEquals("Failed to parse the root info for pv: " + onlyRoot,
                alarmPVInfo.getRoot(), "root");
        assertEquals("Failed to parse the path info for pv: " + onlyRoot,
                alarmPVInfo.getPath(), Optional.empty());
        assertEquals("Failed to parse the complete path info for pv: " + onlyRoot,
                "/root",
                alarmPVInfo.getCompletePath());
        assertEquals("Failed to parse the field path info for pv: " + onlyRoot,
                Optional.of(durationField),
                alarmPVInfo.getField());
    }
}
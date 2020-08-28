package org.phoebus.pv.alarm;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class AlarmPVInfoTest {

    @Test
    public void parsePath()
    {
        String onlyRoot = "root";
        String oneNode = "root/firstNode";
        String twoNode = "root/firstNode/secondNode";
        String leaf = "root/firstNode/secondNode/alarmPV";

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

}
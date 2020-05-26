/*******************************************************************************
 * Copyright (c) 2010-2020 Oak Ridge National Laboratory.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.alarm.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Ignore;
import org.junit.Test;
import org.phoebus.applications.alarm.Messages;
import org.phoebus.applications.alarm.model.AlarmState;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.util.time.TimeDuration;

/** JUnit test of AlarmLogic
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmLogicUnitTest
{
    private static final String OK = "OK";

    /** Alarm logic listener that remembers update/annunc. actions */
    static class AlarmLogicDemo implements AlarmLogicListener
    {
        final private AlarmLogic logic;
        private boolean fired_enablement = false;
        private boolean fired_update = false;
        private boolean annunciated = false;
        final private AtomicInteger global_updates = new AtomicInteger();
        private AtomicReference<AlarmState> global_alarm = new AtomicReference<>();

        AlarmLogicDemo(final boolean latching, final boolean annunciating)
        {
            this(latching, annunciating, 0, 0, 0);
        }

        AlarmLogicDemo(final boolean latching, final boolean annunciating,
                       final int delay)
        {
            this(latching, annunciating, delay, 0, 0);
        }

        AlarmLogicDemo(final boolean latching, final boolean annunciating,
                       final int delay, final int count)
        {
            this(latching, annunciating, delay, count, 0);
        }

        AlarmLogicDemo(final boolean latching, final boolean annunciating,
                final int delay, final int count, final int global_delay)
        {
            logic = new AlarmLogic(this, latching, annunciating, delay, count,
                    AlarmState.createClearState(""),
                    AlarmState.createClearState(""),
                    global_delay);
        }

        // AlarmLogicListener
        @Override
        public void alarmEnablementChanged(final boolean is_enabled)
        {
            System.out.println(is_enabled ? "enabled" : "disabled");
            fired_enablement = true;
        }

        // AlarmLogicListener
        @Override
        public void alarmStateChanged(final AlarmState current, final AlarmState alarm)
        {
            fired_update = true;
        }

        // AlarmLogicListener
        @Override
        public void annunciateAlarm(final SeverityLevel level)
        {
            annunciated = true;
        }

        // AlarmLogicListener
        @Override
        public void globalStateChanged(final AlarmState alarm)
        {
            System.out.println(new Date() + ": Global alarm state: " + alarm);
            global_alarm.set(alarm);
            global_updates.incrementAndGet();
        }

        /** Check logic
         *  @param update Did we expect an update?
         *  @param annunciate Did we expect an annunciation?
         *  @param current_sevr Expected 'current' severity
         *  @param current_msg .. and message.
         *  @param sevr Expected 'alarm' severity
         *  @param msg .. and message.
         */
        void check(final boolean update, final boolean annunciate,
                final SeverityLevel current_sevr, final String current_msg,
                final SeverityLevel sevr, final String msg)
        {
            System.out.println(
                (fired_update ? "new, " : "old, ") +
                (annunciated ? "annunciate : " : "silent     : ") +
                logic.toString());
            assertEquals("Update", update, fired_update);
            assertEquals("Annunciation", annunciate, annunciated);
            assertEquals("Current severity", current_sevr, logic.getCurrentState().getSeverity());
            assertEquals("Current message", current_msg, logic.getCurrentState().getMessage());
            assertEquals("Alarm severity", sevr, logic.getAlarmState().getSeverity());
            assertEquals("Alarm message", msg, logic.getAlarmState().getMessage());
            // Reset
            fired_update = false;
            annunciated = false;
        }

        void checkEnablementChange()
        {
            assertTrue("Enablement changed", fired_enablement);
            System.out.println("Logic is " + (logic.isEnabled() ? "enabled" : "disabled"));
            fired_enablement = false;
        }

        void checkGlobalUpdates(final int expected)
        {
            assertEquals(expected, global_updates.get());
        }

        AlarmState getGlobalAlarm()
        {
            return global_alarm.get();
        }

        public void computeNewState(final String value, final SeverityLevel sevr,
                final String msg)
        {
            logic.computeNewState(new AlarmState(sevr, msg, value, Instant.now()));
        }

        public AlarmState getAlarmState()
        {
            return logic.getAlarmState();
        }

        public int getGlobalUpdates()
        {
            return global_updates.get();
        }

        public void acknowledge(final boolean acknowledge)
        {
            logic.acknowledge(acknowledge);
        }

        public void computeNewState(final AlarmState received_state)
        {
            logic.computeNewState(received_state);
        }

        public void setCount(final int count)
        {
            logic.setCount(count);
        }

        public boolean isEnabled()
        {
            return logic.isEnabled();
        }

        public void setEnabled(final boolean enable)
        {
            logic.setEnabled(enable);
        }

        public void setPriority()
        {
            logic.setPriority(true);
        }
    }

    @Test
    public void testLatchedAnnunciatedAlarmAckOK()
    {
        System.out.println("* Latched, annunciated: Minor, Minor, Major, Major, Ack, Minor, OK");
        final AlarmLogicDemo logic = new AlarmLogicDemo(true, true);
        logic.check(false, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);
        assertEquals("", logic.getAlarmState().getValue());

        // Follow into MINOR alarm
        logic.computeNewState("a", SeverityLevel.MINOR, "high");
        logic.check(true, true, SeverityLevel.MINOR, "high", SeverityLevel.MINOR, "high");
        assertEquals("a", logic.getAlarmState().getValue());

        // No updates when state stays
        logic.computeNewState("b", SeverityLevel.MINOR, "high");
        logic.check(false, false, SeverityLevel.MINOR, "high", SeverityLevel.MINOR, "high");
        assertEquals("a", logic.getAlarmState().getValue());

        // Follow into MAJOR alarm
        logic.computeNewState("c", SeverityLevel.MAJOR, "very high");
        logic.check(true, true, SeverityLevel.MAJOR, "very high", SeverityLevel.MAJOR, "very high");
        assertEquals("c", logic.getAlarmState().getValue());

        // Same severity, so no annuncuation, but alarm message still triggers update
        logic.computeNewState("d", SeverityLevel.MAJOR, "ignored");
        logic.check(true, false, SeverityLevel.MAJOR, "ignored", SeverityLevel.MAJOR, "very high");
        assertEquals("c", logic.getAlarmState().getValue());

        // Ack'
        logic.acknowledge(true);
        logic.check(true, false, SeverityLevel.MAJOR, "ignored", SeverityLevel.MAJOR_ACK, "very high");
        assertEquals("c", logic.getAlarmState().getValue());

        // MINOR, but latch MAJOR alarm (not annunciated)
        logic.computeNewState("e", SeverityLevel.MINOR, "just high");
        logic.check(true, false, SeverityLevel.MINOR, "just high", SeverityLevel.MAJOR_ACK, "very high");
        assertEquals("c", logic.getAlarmState().getValue());

        // All back to OK
        logic.computeNewState("f", SeverityLevel.OK, OK);
        logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);
        assertEquals("f", logic.getAlarmState().getValue());
    }

    @Test
    public void testLatchedAnnunciatedAlarmOKAck()
    {
        System.out.println("* Latched, annunciated: Minor, Major, Minor, OK, Ack");
        final AlarmLogicDemo logic = new AlarmLogicDemo(true, true);
        logic.check(false, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);

        // Follow into MINOR alarm
        logic.computeNewState("a", SeverityLevel.MINOR, "high");
        logic.check(true, true, SeverityLevel.MINOR, "high", SeverityLevel.MINOR, "high");

        // Follow into MAJOR alarm
        logic.computeNewState("b", SeverityLevel.MAJOR, "very high");
        logic.check(true, true, SeverityLevel.MAJOR, "very high", SeverityLevel.MAJOR, "very high");

        // MINOR, but latch MAJOR alarm (not annunciated)
        logic.computeNewState("c", SeverityLevel.MINOR, "just high");
        logic.check(true, false, SeverityLevel.MINOR, "just high", SeverityLevel.MAJOR, "very high");

        // OK, but latch MAJOR alarm (not annunciated)
        logic.computeNewState("d", SeverityLevel.OK, OK);
        logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.MAJOR, "very high");

        // Ack'
        logic.acknowledge(true);
        logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);
    }

    @Test
    public void testLatchedAnnunciatedAlarmUndefined()
    {
        System.out.println("* Latched, annunciated: Major, Ack, Undefined, Ack, Major, OK");
        final AlarmLogicDemo logic = new AlarmLogicDemo(true, true);
        logic.check(false, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);

        // Follow into MAJOR alarm
        logic.computeNewState("a", SeverityLevel.MAJOR, "very high");
        logic.check(true, true, SeverityLevel.MAJOR, "very high", SeverityLevel.MAJOR, "very high");

        // Ack'
        logic.acknowledge(true);
        logic.check(true, false, SeverityLevel.MAJOR, "very high", SeverityLevel.MAJOR_ACK, "very high");

        // Follow into INVALID alarm
        logic.computeNewState("b", SeverityLevel.INVALID, "LINK");
        logic.check(true, true, SeverityLevel.INVALID, "LINK", SeverityLevel.INVALID, "LINK");

        // MAJOR is less severe, alarm severity stays at INVALID
        logic.computeNewState("c", SeverityLevel.MAJOR, "very high");
        logic.check(true, false, SeverityLevel.MAJOR, "very high", SeverityLevel.INVALID, "LINK");

        // Acknowledge what's there right now, i.e. the MAJOR alarm, not the one that was latched
        logic.acknowledge(true);
        logic.check(true, false, SeverityLevel.MAJOR, "very high", SeverityLevel.MAJOR_ACK, "very high");

        // Follow into UNDEFINED alarm
        logic.computeNewState("d", SeverityLevel.UNDEFINED, "Disconnected");
        logic.check(true, true, SeverityLevel.UNDEFINED, "Disconnected", SeverityLevel.UNDEFINED, "Disconnected");

        // Ack'
        logic.acknowledge(true);
        logic.check(true, false, SeverityLevel.UNDEFINED, "Disconnected", SeverityLevel.UNDEFINED_ACK, "Disconnected");

        // MAJOR is less severe, stays UNDEFINED_ACK
        logic.computeNewState("e", SeverityLevel.MAJOR, "very high");
        logic.check(true, false, SeverityLevel.MAJOR, "very high", SeverityLevel.UNDEFINED_ACK, "Disconnected");

        // OK
        logic.computeNewState("f", SeverityLevel.OK, "OK");
        logic.check(true, false, SeverityLevel.OK, "OK", SeverityLevel.OK, "OK");
    }

    @Test
    public void testLatchedAnnunciatedMajMinMajAckMinOK()
    {
        System.out.println("* Latched, annunciated: Major, Minor, Major, Ack, Minor, OK.");
        final AlarmLogicDemo logic = new AlarmLogicDemo(true, true);
        logic.check(false, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);

        // Follow into MAJOR alarm
        logic.computeNewState("a", SeverityLevel.MAJOR, "very high");
        logic.check(true, true, SeverityLevel.MAJOR, "very high", SeverityLevel.MAJOR, "very high");

        // MINOR, but latch MAJOR alarm (not annunciated)
        logic.computeNewState("b", SeverityLevel.MINOR, "just high");
        logic.check(true, false, SeverityLevel.MINOR, "just high", SeverityLevel.MAJOR, "very high");

        // Back into MAJOR alarm: Not annunciated
        logic.computeNewState("c", SeverityLevel.MAJOR, "very high");
        logic.check(true, false, SeverityLevel.MAJOR, "very high", SeverityLevel.MAJOR, "very high");

        // Ack'
        logic.acknowledge(true);
        logic.check(true, false, SeverityLevel.MAJOR, "very high", SeverityLevel.MAJOR_ACK, "very high");

        // MINOR
        logic.computeNewState("d", SeverityLevel.MINOR, "just high");
        logic.check(true, false, SeverityLevel.MINOR, "just high", SeverityLevel.MAJOR_ACK, "very high");

        // OK
        logic.computeNewState("e", SeverityLevel.OK, OK);
        logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);
    }

    @Test
    public void testLatchedUnAckToLowerSeverity()
    {
        // This test was added for the Phoebus/Kafka version.
        // Behavior was not tested in the previous version.
        System.out.println("* Latched, annunciated: Major, Undefined, Ack, Reconnect, UnAck, Ack, OK");
        final AlarmLogicDemo logic = new AlarmLogicDemo(true, true);
        logic.check(false, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);

        // Follow into MAJOR alarm
        logic.computeNewState("a", SeverityLevel.MAJOR, "very high");
        logic.check(true, true, SeverityLevel.MAJOR, "very high", SeverityLevel.MAJOR, "very high");

        // PV gets disconnected
        logic.computeNewState("a", SeverityLevel.UNDEFINED, "Disconnected");
        logic.check(true, true, SeverityLevel.UNDEFINED, "Disconnected", SeverityLevel.UNDEFINED, "Disconnected");

        // Ack': Naturally becomes UNDEFINED_ACK, since UNDEFINED was acknowledged
        logic.acknowledge(true);
        logic.check(true, false, SeverityLevel.UNDEFINED, "Disconnected", SeverityLevel.UNDEFINED_ACK, "Disconnected");

        // PV reconnects, again found in MAJOR alarm, but still has UNDEFINED_ACK.
        // Makes sense because an undefined has been acked,
        // but looks a little odd if it remains like this for a long time with no way to change:
        // The PV is mostly in MAJOR alarm, and that has been acked.
        // Yet alarm shows UNDEFINED_ACK just because PV was briefly disconnected..
        // In original implementation, that was the end of the story.
        logic.computeNewState("a", SeverityLevel.MAJOR, "very high");
        logic.check(true, false, SeverityLevel.MAJOR, "very high", SeverityLevel.UNDEFINED_ACK, "Disconnected");

        // Un-ack: Now shows MAJOR alarm
        // Previous implementation stayed with alarm severity UNDEFINED.
        // New implementation un-acks based on the current severity,
        // so now alarm is closer to the current reality
        logic.acknowledge(false);
        logic.check(true, false, SeverityLevel.MAJOR, "very high", SeverityLevel.MAJOR, "very high");

        // Ack the MAJOR: Naturally becomes MAJOR_ACK.
        // --> After a flurry of brief disconnects, a manual ack & unack allows a "reset"
        //     of the alarms to their current state.
        // This is a change from previous behavior, but no change to previously _tested_ behavior,
        // and more in line with natural expectation:
        // I've acked severity X, some time ago. Now PV has lesser severity Y, so un-ack should show alarm severity == Y.
        logic.acknowledge(true);
        logic.check(true, false, SeverityLevel.MAJOR, "very high", SeverityLevel.MAJOR_ACK, "very high");

        // OK
        logic.computeNewState("b", SeverityLevel.OK, OK);
        logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);
    }

    @Test
    public void testLatchedAnnunciatedAckToLowerSeverity()
    {
        System.out.println("* Latched, annunciated: Major, Minor, Ack, Major, Ack, Minor, OK.");
        final AlarmLogicDemo logic = new AlarmLogicDemo(true, true);
        logic.check(false, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);

        // Follow into MAJOR alarm
        logic.computeNewState("a", SeverityLevel.MAJOR, "very high");
        logic.check(true, true, SeverityLevel.MAJOR, "very high", SeverityLevel.MAJOR, "very high");

        // MINOR, but latched to MAJOR alarm (not annunciated)
        logic.computeNewState("b", SeverityLevel.MINOR, "just high");
        logic.check(true, false, SeverityLevel.MINOR, "just high", SeverityLevel.MAJOR, "very high");

        // Ack': Forget the MAJOR alarm, ack that it's now MINOR
        logic.acknowledge(true);
        logic.check(true, false, SeverityLevel.MINOR, "just high", SeverityLevel.MINOR_ACK, "just high");
        assertEquals("b", logic.getAlarmState().getValue());

        // Back into MAJOR alarm: Annunciated, since we ack'ed MINOR
        logic.computeNewState("c", SeverityLevel.MAJOR, "very high");
        logic.check(true, true, SeverityLevel.MAJOR, "very high", SeverityLevel.MAJOR, "very high");
        assertEquals("c", logic.getAlarmState().getValue());

        // MINOR: Still latched to MAJOR alarm
        logic.computeNewState("d", SeverityLevel.MINOR, "just high");
        logic.check(true, false, SeverityLevel.MINOR, "just high", SeverityLevel.MAJOR, "very high");
        assertEquals("c", logic.getAlarmState().getValue());

        // Ack': Forget the MAJOR alarm, ack that it's now MINOR
        logic.acknowledge(true);
        logic.check(true, false, SeverityLevel.MINOR, "just high", SeverityLevel.MINOR_ACK, "just high");
        assertEquals("d", logic.getAlarmState().getValue());

        // OK
        logic.computeNewState("e", SeverityLevel.OK, OK);
        logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);
    }

    @Test
    public void testUnlatchedAnnunciatedMajMinMajAckMinOK()
    {
        System.out.println("* Unlatched, annunciated: Major, Minor, Major, Ack, Minor, OK.");
        final AlarmLogicDemo logic = new AlarmLogicDemo(false, true);
        logic.check(false, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);

        // Follow into MAJOR alarm
        logic.computeNewState("a", SeverityLevel.MAJOR, "very high");
        logic.check(true, true, SeverityLevel.MAJOR, "very high", SeverityLevel.MAJOR, "very high");

        // Follow into MINOR (not annunc)
        logic.computeNewState("b", SeverityLevel.MINOR, "high");
        logic.check(true, false, SeverityLevel.MINOR, "high", SeverityLevel.MINOR, "high");

        // Back into MAJOR alarm (annunc)
        logic.computeNewState("c", SeverityLevel.MAJOR, "very high");
        logic.check(true, true, SeverityLevel.MAJOR, "very high", SeverityLevel.MAJOR, "very high");

        // Ack'.
        logic.acknowledge(true);
        logic.check(true, false, SeverityLevel.MAJOR, "very high", SeverityLevel.MAJOR_ACK, "very high");

        // MINOR, but remember that MAJOR was ack'ed
        logic.computeNewState("d", SeverityLevel.MINOR, "just high");
        logic.check(true, false, SeverityLevel.MINOR, "just high", SeverityLevel.MAJOR_ACK, "very high");

        // OK
        logic.computeNewState("e", SeverityLevel.OK, OK);
        logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);
    }

    @Test
    public void testUnlatchedAnnunciatedMajMinAckMajAckMinOK()
    {
        System.out.println("* Unlatched, annunciated: Major, Minor, Ack, Major, Ack, Minor, OK.");
        final AlarmLogicDemo logic = new AlarmLogicDemo(false, true);
        logic.check(false, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);
        assertEquals("", logic.getAlarmState().getValue());

        // Follow into MAJOR alarm
        logic.computeNewState("a", SeverityLevel.MAJOR, "very high");
        logic.check(true, true, SeverityLevel.MAJOR, "very high", SeverityLevel.MAJOR, "very high");
        assertEquals("a", logic.getAlarmState().getValue());

        // Follow into MINOR (not annunc)
        logic.computeNewState("b", SeverityLevel.MINOR, "high");
        logic.check(true, false, SeverityLevel.MINOR, "high", SeverityLevel.MINOR, "high");
        assertEquals("b", logic.getAlarmState().getValue());

        // Ack'.
        logic.acknowledge(true);
        logic.check(true, false, SeverityLevel.MINOR, "high", SeverityLevel.MINOR_ACK, "high");

        // Back into MAJOR alarm (annunc)
        logic.computeNewState("c", SeverityLevel.MAJOR, "very high");
        logic.check(true, true, SeverityLevel.MAJOR, "very high", SeverityLevel.MAJOR, "very high");

        // Ack'.
        logic.acknowledge(true);
        logic.check(true, false, SeverityLevel.MAJOR, "very high", SeverityLevel.MAJOR_ACK, "very high");

        // MINOR, but remember that MAJOR was ack'ed
        logic.computeNewState("d", SeverityLevel.MINOR, "just high");
        logic.check(true, false, SeverityLevel.MINOR, "just high", SeverityLevel.MAJOR_ACK, "very high");

        // OK
        logic.computeNewState("e", SeverityLevel.OK, OK);
        logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);
    }

    @Test
    public void testDelayedButShort() throws Exception
    {
        System.out.println("* Latched, annunciated, delayed: Major, clear");
        final int delay = 2;
        final AlarmLogicDemo logic = new AlarmLogicDemo(true, true, delay);
        logic.check(false, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);
        assertEquals("", logic.getAlarmState().getValue());

        // MAJOR alarm has no immediate effect
        logic.computeNewState("a", SeverityLevel.MAJOR, "very high");
        logic.check(true, false, SeverityLevel.MAJOR, "very high", SeverityLevel.OK, OK);
        assertEquals("", logic.getAlarmState().getValue());

        // .. if it clears in time (1/2 the delay time)
        Thread.sleep(delay * 500);
        logic.computeNewState("b", SeverityLevel.OK, OK);
        logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);
        assertEquals("b", logic.getAlarmState().getValue());

        // Assert that it stays that way
        System.out.println("wait...");
        Thread.sleep(delay * 1500);
        logic.check(false, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);
        assertEquals("b", logic.getAlarmState().getValue());
    }

    @Test
    public void testLatchedAnnunciatedDelayed() throws Exception
    {
        System.out.println("* Latched, annunciated, delayed: Major, persists, clear, ack; MINOR, MAJOR, MINOR, persist");
        final int delay = 2;
        final AlarmLogicDemo logic = new AlarmLogicDemo(true, true, delay);
        logic.check(false, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);
        assertEquals("", logic.getAlarmState().getValue());

        // MAJOR alarm has no immediate effect
        logic.computeNewState("a", SeverityLevel.MAJOR, "very high");
        logic.check(true, false, SeverityLevel.MAJOR, "very high", SeverityLevel.OK, OK);
        assertEquals("", logic.getAlarmState().getValue());

        // ... until after some delay
        System.out.println("wait...");
        Thread.sleep(delay * 1500);
        logic.check(true, true, SeverityLevel.MAJOR, "very high", SeverityLevel.MAJOR, "very high");
        assertEquals("a", logic.getAlarmState().getValue());

        // Clear PV, but alarm still latched
        logic.computeNewState("b", SeverityLevel.OK, OK);
        logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.MAJOR, "very high");
        assertEquals("a", logic.getAlarmState().getValue());
        // Ack to clear alarm
        logic.acknowledge(true);
        logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);
        assertEquals("b", logic.getAlarmState().getValue());

        // -----

        // MINOR alarm has no immediate effect
        logic.computeNewState("c", SeverityLevel.MINOR, "high");
        Thread.sleep(500);
        logic.check(true, false, SeverityLevel.MINOR, "high", SeverityLevel.OK, OK);
        assertEquals("b", logic.getAlarmState().getValue());

        // Neither has MAJOR
        final Instant now = Instant.now();
        logic.computeNewState(new AlarmState(SeverityLevel.MAJOR, "too high", "d", now));
        logic.check(true, false, SeverityLevel.MAJOR, "too high", SeverityLevel.OK, OK);
        Thread.sleep(delay * 100);
        assertEquals("b", logic.getAlarmState().getValue());

        // Back to MINOR
        logic.computeNewState("e", SeverityLevel.MINOR, "high");
        logic.check(true, false, SeverityLevel.MINOR, "high", SeverityLevel.OK, OK);
        assertEquals("b", logic.getAlarmState().getValue());

        // ... until latched MAJOR (!) appears after some delay
        System.out.println("wait...");
        Thread.sleep(delay * 1500);
        logic.check(true, true, SeverityLevel.MINOR, "high", SeverityLevel.MAJOR, "too high");
        assertEquals("d", logic.getAlarmState().getValue());
        // Time should match the time of MAJOR event
        assertEquals(now, logic.getAlarmState().getTime());
     }

    @Test
    public void testUnlatchedAnnunciatedDelayed() throws Exception
    {
        System.out.println("* Unlatched, annunciated, delayed: Major, persists, clear, ack; MAJOR, MINOR, persist");
        final int delay = 2;
        final AlarmLogicDemo logic = new AlarmLogicDemo(false, true, delay);
        logic.check(false, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);

        // MAJOR alarm has no immediate effect
        logic.computeNewState("a", SeverityLevel.MAJOR, "very high");
        logic.check(true, false, SeverityLevel.MAJOR, "very high", SeverityLevel.OK, OK);

        // ... until after some delay
        System.out.println("wait...");
        Thread.sleep(delay * 1500);
        logic.check(true, true, SeverityLevel.MAJOR, "very high", SeverityLevel.MAJOR, "very high");

        // Clearing the alarm
        logic.computeNewState("b", SeverityLevel.OK, OK);
        logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);

        // -----

        // MAJOR alarm has no immediate effect
        logic.computeNewState("c", SeverityLevel.MAJOR, "too high");
        logic.check(true, false, SeverityLevel.MAJOR, "too high", SeverityLevel.OK, OK);

        // Back to MINOR
        logic.computeNewState("d", SeverityLevel.MINOR, "high");
        logic.check(true, false, SeverityLevel.MINOR, "high", SeverityLevel.OK, OK);

        // ... until alarm persists, using the last alarm (MINOR) because not latched
        System.out.println("wait...");
        Thread.sleep(delay * 1500);
        logic.check(true, true, SeverityLevel.MINOR, "high", SeverityLevel.MINOR, "high");
    }

    @Test
    public void testDelayedButShortBurst() throws Exception
    {
        System.out.println("* Latched, annunciated, delayed: { Major, clear} several times within delay, no alarm");
        final int delay = 2;
        final AlarmLogicDemo logic = new AlarmLogicDemo(true, true, delay);
        logic.check(false, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);
        assertEquals("", logic.getAlarmState().getValue());

        String last_value = "";
        for (int i=0; i<10; ++i)
        {
            // MAJOR alarm has no immediate effect
            logic.computeNewState("a"+i, SeverityLevel.MAJOR, "very high");
            logic.check(true, false, SeverityLevel.MAJOR, "very high", SeverityLevel.OK, OK);
            assertEquals(last_value, logic.getAlarmState().getValue());

            // .. if it clears in time (1/2 the delay time)
            Thread.sleep(delay * 500);
            last_value = "b" + i;
            logic.computeNewState(last_value, SeverityLevel.OK, OK);
            logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);
            assertEquals(last_value, logic.getAlarmState().getValue());

            // The 'delay' timer resets when the PV returns to OK
        }

        // Assert that it stays that way
        System.out.println("wait...");
        Thread.sleep(delay * 1500);
        logic.check(false, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);
        assertEquals(last_value, logic.getAlarmState().getValue());
    }

    @Test
    public void testLatchedAnnunciatedCount() throws Exception
    {
        System.out.println("* Latched, annunciated, count: minor, ok, minor, ok");
        final int delay = 200;
        int count = 3;
        final AlarmLogicDemo logic = new AlarmLogicDemo(true, true, delay, count);
        logic.check(false, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);

        // (count-1) brief MINOR alarms have no effect
        System.out.println((count-1) + " ignored alarms....");
        for (int i=0; i<count-1; ++i)
        {
            logic.computeNewState("a", SeverityLevel.MINOR, "high");
            logic.check(true, false, SeverityLevel.MINOR, "high", SeverityLevel.OK, OK);
            logic.computeNewState("b", SeverityLevel.OK, OK);
            logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);
        }

        // But when they reach the count, it matters
        System.out.println("Final alarm to get count of " + count);
        logic.computeNewState("c", SeverityLevel.MINOR, "high");
        logic.check(true, true, SeverityLevel.MINOR, "high", SeverityLevel.MINOR, "high");
        // Clear alarm
        logic.computeNewState("d", SeverityLevel.OK, OK);
        logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.MINOR, "high");
        // Ack.
        logic.acknowledge(true);
        logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);

        // Change the count
        count = 10;
        logic.setCount(count);
        logic.check(false, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);

        // (count-1) brief MINOR alarms have no effect
        System.out.println((count-1) + " ignored alarms....");
        for (int i=0; i<count-1; ++i)
        {
            logic.computeNewState("e", SeverityLevel.MINOR, "high");
            logic.check(true, false, SeverityLevel.MINOR, "high", SeverityLevel.OK, OK);
            logic.computeNewState("f", SeverityLevel.OK, OK);
            logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);
        }

        // But when they reach the count, it matters
        System.out.println("Final alarm to get count of " + count);
        logic.computeNewState("g", SeverityLevel.MINOR, "high");
        logic.check(true, true, SeverityLevel.MINOR, "high", SeverityLevel.MINOR, "high");
    }

    @Test
    public void testShadesOfInvalid() throws Exception
    {
        System.out.println("* Invalid/disconnected, Invalid/Timeout");
        final AlarmLogicDemo logic = new AlarmLogicDemo(true, true);
        logic.check(false, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);

        logic.computeNewState("a", SeverityLevel.INVALID, "Disconnected");
        logic.check(true, true, SeverityLevel.INVALID, "Disconnected", SeverityLevel.INVALID, "Disconnected");

        // Different message
        logic.computeNewState("b", SeverityLevel.INVALID, "Timeout");
        logic.check(true, false, SeverityLevel.INVALID, "Timeout", SeverityLevel.INVALID, "Disconnected");

        // Same message
        logic.computeNewState("c", SeverityLevel.INVALID, "Timeout");
        logic.check(false, false, SeverityLevel.INVALID, "Timeout", SeverityLevel.INVALID, "Disconnected");
    }

    @Test
    public void testDisabledLatchedAnnunciatedAlarmAckOK()
    {
        System.out.println("* Disabled, latched, annunciated: Minor");
        final AlarmLogicDemo logic = new AlarmLogicDemo(true, true);
        logic.check(false, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);
        assertEquals("", logic.getAlarmState().getValue());
        assertTrue(logic.isEnabled());

        // Disabling results in one update that fakes an all OK
        // with message "Disabled"
        logic.setEnabled(false);
        logic.checkEnablementChange();
        assertFalse(logic.isEnabled());
        logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.OK, Messages.Disabled);

        // Should now ignore received MINOR alarm
        logic.computeNewState("a", SeverityLevel.MINOR, "high");
        logic.check(false, false, SeverityLevel.OK, OK, SeverityLevel.OK, Messages.Disabled);

        // Re-enable
        logic.setEnabled(true);
        logic.checkEnablementChange();
        assertTrue(logic.isEnabled());
        logic.check(true, true, SeverityLevel.MINOR, "high", SeverityLevel.MINOR, "high");
        assertEquals("a", logic.getAlarmState().getValue());

        // Another Minor doesn't matter
        logic.computeNewState("a2", SeverityLevel.MINOR, "high");
        logic.check(false, false, SeverityLevel.MINOR, "high", SeverityLevel.MINOR, "high");
        assertEquals("a", logic.getAlarmState().getValue());

        // Follow into Major
        logic.computeNewState("b", SeverityLevel.MAJOR, "hihi");
        logic.check(true, true, SeverityLevel.MAJOR, "hihi", SeverityLevel.MAJOR, "hihi");
        assertEquals("b", logic.getAlarmState().getValue());

        // Disable again
        logic.setEnabled(false);
        logic.checkEnablementChange();
        assertFalse(logic.isEnabled());
        logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.OK, Messages.Disabled);

        // Re-enable, and MAJOR alarm resurfaces since nothing else was received
        logic.setEnabled(true);
        logic.checkEnablementChange();
        assertTrue(logic.isEnabled());
        logic.check(true, true, SeverityLevel.MAJOR, "hihi", SeverityLevel.MAJOR, "hihi");
        assertEquals("b", logic.getAlarmState().getValue());
    }

    /** There used to be an error in the logic:
     *  After getting 'count' alarms within 'delay',
     *  it would immediately react to the next one
     *  instead of waiting for another 'count'.
     *  This checks for that problem
     */
    @Test
    public void testUnlatchedAnnunciatedCount() throws Exception
    {
        System.out.println("* NonLatched, annunciated, count: minor, ok, minor, ok");
        final int delay = 10;
        int count = 5;
        final AlarmLogicDemo logic = new AlarmLogicDemo(false, true, delay, count);
        logic.check(false, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);

        // (count-1) brief MINOR alarms have no effect
        System.out.println((count-1) + " ignored alarms....");
        for (int i=0; i<count-1; ++i)
        {
            logic.computeNewState("a", SeverityLevel.MINOR, "high");
            logic.check(true, false, SeverityLevel.MINOR, "high", SeverityLevel.OK, OK);
            logic.computeNewState("b", SeverityLevel.OK, OK);
            logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);
        }

        // But when they reach the count, it matters
        System.out.println("Final alarm to get count of " + count);
        logic.computeNewState("c", SeverityLevel.MINOR, "high");
        logic.check(true, true, SeverityLevel.MINOR, "high", SeverityLevel.MINOR, "high");
        // Clear alarm
        logic.computeNewState("d", SeverityLevel.OK, OK);
        logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);

        // Start over: another (count-1) brief MINOR alarms have no effect
        System.out.println((count-1) + " ignored alarms....");
        for (int i=0; i<count-1; ++i)
        {
            logic.computeNewState("a2", SeverityLevel.MINOR, "high");
            logic.check(true, false, SeverityLevel.MINOR, "high", SeverityLevel.OK, OK);
            logic.computeNewState("b2", SeverityLevel.OK, OK);
            logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);
        }
        // But when they reach the count, it matters
        System.out.println("Final alarm to get count of " + count);
        logic.computeNewState("c2", SeverityLevel.MINOR, "high");
        logic.check(true, true, SeverityLevel.MINOR, "high", SeverityLevel.MINOR, "high");
        // Clear alarm
        logic.computeNewState("d", SeverityLevel.OK, OK);
        logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);
    }

    @Test
    public void testMaintenanceMode() throws Exception
    {
        System.out.println("* testMaintenanceMode");
        AlarmLogicDemo logic = new AlarmLogicDemo(false, true);
        logic.check(false, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);

        AlarmLogic.setMaintenanceMode(true);

        // Normal alarm
        logic.computeNewState("a", SeverityLevel.MINOR, "high");
        logic.check(true, true, SeverityLevel.MINOR, "high", SeverityLevel.MINOR, "high");

        // INVALID is ack'ed automatically, no annunciation
        logic.computeNewState("b", SeverityLevel.INVALID, "Disconnected");
        logic.check(true, false, SeverityLevel.INVALID, "Disconnected", SeverityLevel.INVALID_ACK, "Disconnected");

        // Another non-INVALID alarm comes through
        logic.computeNewState("c", SeverityLevel.MINOR, "high");
        logic.check(true, true, SeverityLevel.MINOR, "high", SeverityLevel.MINOR, "high");

        // -- Similar, but with 'priority' alarm --
        logic = new AlarmLogicDemo(false, true);
        logic.setPriority();
        logic.check(false, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);

        // Normal alarm
        logic.computeNewState("a", SeverityLevel.MINOR, "high");
        logic.check(true, true, SeverityLevel.MINOR, "high", SeverityLevel.MINOR, "high");

        // INVALID is _not_ ack'ed, there _is_ annunciation:
        // Priority alarm ignores the maintenance mode
        logic.computeNewState("b", SeverityLevel.INVALID, "Disconnected");
        logic.check(true, true, SeverityLevel.INVALID, "Disconnected", SeverityLevel.INVALID, "Disconnected");

        // Another non-INVALID alarm comes through
        logic.computeNewState("c", SeverityLevel.MINOR, "high");
        logic.check(true, true, SeverityLevel.MINOR, "high", SeverityLevel.MINOR, "high");
    }

    @Ignore
    @Test(timeout=60000)
    public void testGlobalNotifications() throws Exception
    {
        System.out.println("* testGlobalNotifications");
        // Latch, annunciate, no local delay & count, global notification after 4s
        final int global_delay = 4;
        int expected_count = 0;
        final AlarmLogicDemo logic = new AlarmLogicDemo(true, true, 0, 0, global_delay);
        logic.check(false, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);

        // Alarm that clears and is ack'ed in time, NO global alarm
        logic.computeNewState("a", SeverityLevel.MINOR, "high");
        logic.check(true, true, SeverityLevel.MINOR, "high", SeverityLevel.MINOR, "high");

        // Return to OK
        logic.computeNewState("b", SeverityLevel.OK, OK);
        logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.MINOR, "high");

        // Acknowledged in time, all clear
        Thread.sleep(global_delay * 500);
        logic.acknowledge(true);
        logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);

        // There should have been no global alarm
        logic.checkGlobalUpdates(expected_count);

        // Even after delay, there should be no global update
        Thread.sleep(global_delay * 1300);
        logic.checkGlobalUpdates(expected_count);

        // ------

        // Alarm that doesn't clear
        System.out.println("Trigger of 'global' alarm:");
        logic.computeNewState("a", SeverityLevel.MINOR, "high");
        logic.check(true, true, SeverityLevel.MINOR, "high", SeverityLevel.MINOR, "high");

        // Nothing happens right away
        logic.checkGlobalUpdates(expected_count);

        // There should be a global alarm after the delay
        Thread.sleep(global_delay * 1300);
        logic.checkGlobalUpdates(++expected_count);
        assertEquals(SeverityLevel.MINOR, logic.getGlobalAlarm().getSeverity());

        // Acknowledged but still in alarm, the global state stays
        logic.acknowledge(true);
        logic.check(true, false, SeverityLevel.MINOR, "high", SeverityLevel.MINOR_ACK, "high");
        Thread.sleep(global_delay * 500);
        logic.checkGlobalUpdates(expected_count);
        assertEquals(SeverityLevel.MINOR, logic.getGlobalAlarm().getSeverity());

        // Alarm clears, was already ack'ed: All clear, global update
        logic.computeNewState("b", SeverityLevel.OK, OK);
        logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);
        Thread.sleep(global_delay * 500);
        logic.checkGlobalUpdates(++expected_count);
        assertEquals(SeverityLevel.OK, logic.getGlobalAlarm().getSeverity());

        // ------

        // Alarm that clears, but is not acknowledged
        System.out.println("Trigger of 'global' alarm:");
        logic.computeNewState("a", SeverityLevel.MINOR, "high");
        logic.check(true, true, SeverityLevel.MINOR, "high", SeverityLevel.MINOR, "high");
        Thread.sleep(global_delay * 500);
        logic.computeNewState("b", SeverityLevel.OK, OK);
        logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.MINOR, "high");

        // Nothing happens right away
        logic.checkGlobalUpdates(expected_count);
        assertEquals(SeverityLevel.OK, logic.getGlobalAlarm().getSeverity());

        // There should be a global alarm after the delay
        Thread.sleep(global_delay * 1300);
        logic.checkGlobalUpdates(++expected_count);
        assertEquals(SeverityLevel.MINOR, logic.getGlobalAlarm().getSeverity());

        // Once acknowledged, the global state should also clear
        logic.acknowledge(true);
        logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);
        logic.checkGlobalUpdates(++expected_count);
        assertEquals(SeverityLevel.OK, logic.getGlobalAlarm().getSeverity());


        // ------

        // Channel goes back into alarm
        System.out.println("Trigger of 'global' alarm:");
        logic.computeNewState("a", SeverityLevel.MINOR, "high");
        logic.check(true, true, SeverityLevel.MINOR, "high", SeverityLevel.MINOR, "high");
        logic.computeNewState("b", SeverityLevel.OK, OK);
        logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.MINOR, "high");

        // Nothing happens right away
        logic.checkGlobalUpdates(expected_count);
        assertEquals(SeverityLevel.OK, logic.getGlobalAlarm().getSeverity());

        // There should be a global alarm after the delay
        Thread.sleep(global_delay * 1300);
        logic.checkGlobalUpdates(++expected_count);
        assertEquals(SeverityLevel.MINOR, logic.getGlobalAlarm().getSeverity());

        // One could ack' the alarm now. Instead, the channel returns into alarm for some reason
        logic.computeNewState("c", SeverityLevel.MINOR, "high");
        // Update but no annunciation
        logic.check(true, false, SeverityLevel.MINOR, "high", SeverityLevel.MINOR, "high");
        // No new global alarm
        Thread.sleep(global_delay * 1300);
        logic.checkGlobalUpdates(expected_count);
        assertEquals(SeverityLevel.MINOR, logic.getGlobalAlarm().getSeverity());

        // Once ack'ed, the global alarm stays because the channel is still in alarm
        logic.acknowledge(true);
        // 'Local' alarm display updates to ack'ed
        logic.check(true, false, SeverityLevel.MINOR, "high", SeverityLevel.MINOR_ACK, "high");
        // .. but no change in global alarm
        Thread.sleep(global_delay * 1300);
        logic.checkGlobalUpdates(expected_count);
        assertEquals(SeverityLevel.MINOR, logic.getGlobalAlarm().getSeverity());

        // Channel clears, which also clears the global alarm
        logic.computeNewState("d", SeverityLevel.OK, OK);
        logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);
        logic.checkGlobalUpdates(++expected_count);
        assertEquals(SeverityLevel.OK, logic.getGlobalAlarm().getSeverity());
    }

    @Ignore
    @Test
    public void testGlobalEscalation() throws Exception
    {
        System.out.println("* testGlobalEscalation");
        // Latch, annunciate, no local delay & count, global notification after 4s
        final int global_delay = 4;
        final AlarmLogicDemo logic = new AlarmLogicDemo(true, true, 0, 0, global_delay);
        logic.check(false, false, SeverityLevel.OK, OK, SeverityLevel.OK, OK);

        // Initial, Minor alarm
        logic.computeNewState("a", SeverityLevel.MINOR, "high");
        logic.check(true, true, SeverityLevel.MINOR, "high", SeverityLevel.MINOR, "high");
        final Instant initial_alarm_time = logic.getAlarmState().getTime();

        // Within the 'global' delay, escalates to Major
        Thread.sleep(global_delay * 500);
        logic.computeNewState("b", SeverityLevel.MAJOR, "higher");
        logic.check(true, true, SeverityLevel.MAJOR, "higher", SeverityLevel.MAJOR, "higher");

        // Return to OK
        logic.computeNewState("b", SeverityLevel.OK, OK);
        logic.check(true, false, SeverityLevel.OK, OK, SeverityLevel.MAJOR, "higher");

        // No global alarm, yet
        logic.checkGlobalUpdates(0);

        // After the 'global' delay from the _initial_ (!) alarm, there should be a global update
        for (int i=0; logic.getGlobalUpdates() < 1  &&  i < global_delay * 10; ++i)
            Thread.sleep(100);
        final Instant now = Instant.now();
        logic.checkGlobalUpdates(1);

        System.out.println("Initial alarm      : " + initial_alarm_time);
        System.out.println("Global notification: " + now);
        // Should use global_delay from the initial alarm...
        assertEquals(global_delay, TimeDuration.toSecondsDouble(Duration.between(initial_alarm_time, now)), global_delay/3);

        // .. but reflect the most severe alarm in the notification.
        // Not really checking what was in the notification,
        // assuming that it matches the current alarm state:
        assertEquals(SeverityLevel.MAJOR, logic.getAlarmState().getSeverity());
    }
}

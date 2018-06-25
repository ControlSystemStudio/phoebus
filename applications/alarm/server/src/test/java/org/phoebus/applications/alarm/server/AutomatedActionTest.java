/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.alarm.server;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.logging.LogManager;

import org.junit.BeforeClass;
import org.junit.Test;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.model.TitleDetail;

/** JUnit test of the AutomatedActions
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AutomatedActionTest
{
    // TODO Anything in here that uses TitleDetail should use
    // a new, to-be-created class that adds a Delay, e.g. TitleDetailDelay

    /** Time-based tests are fragile.
     *  Pick a large enough test delay.
     */
    private static final long DELAY_MS = TimeUnit.SECONDS.toMillis(1);

    final BlockingQueue<String> action_performed = new ArrayBlockingQueue<>(1);

    final BiConsumer<AlarmTreeItem<?>, TitleDetail> perform_action = (item, action) ->
    {
        System.out.println("Invoked " + action + " on " + item);
        action_performed.offer(action.title);
    };

    @BeforeClass
    public static void setup() throws Exception
    {
        final String config =
            "handlers = java.util.logging.ConsoleHandler\n" +
            ".level = FINE\n" +
            "java.util.logging.SimpleFormatter.format=%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$s [%3$s] %5$s%6$s%n\n";
        final ByteArrayInputStream stream = new ByteArrayInputStream(config.getBytes());
        LogManager.getLogManager().readConfiguration(stream);
    }

    @Test
    public void testBasicAutomatedAction() throws Exception
    {
        // TODO Use TitleDetailDelay with delay of DELAY_MS
        final TitleDetail email = new TitleDetail("Send Email", "mailto:fred@mail.com");
        final AlarmClientNode test_item = new AlarmClientNode(null, "test");
        test_item.setActions(List.of(email));

        final AutomatedActions auto_action = new AutomatedActions(test_item, perform_action);

        // Action has not been triggered, so should not be invoked
        assertThat(action_performed.poll(2*DELAY_MS, TimeUnit.MILLISECONDS), nullValue());

        // Trigger alarm
        final long start = System.currentTimeMillis();
        auto_action.handleSeverityUpdate(SeverityLevel.MAJOR);
        // Should now happen after the delay
        assertThat(action_performed.poll(2*DELAY_MS, TimeUnit.MILLISECONDS), equalTo("Send Email"));
        final long passed = System.currentTimeMillis() - start;
        System.out.println("Action performed after " + passed + " ms");
        // Actual delay should be within 20% of the expected delay
        assertTrue(Math.abs(DELAY_MS - passed) < DELAY_MS / 5);

        // When no longer needed, close to stop timers etc.
        auto_action.cancel();
    }

    @Test
    public void testMinorMajor() throws Exception
    {
        // TODO Use TitleDetailDelay with delay of DELAY_MS
        final TitleDetail email = new TitleDetail("Send Email", "mailto:fred@mail.com");
        final AlarmClientNode test_item = new AlarmClientNode(null, "test");
        test_item.setActions(List.of(email));

        final AutomatedActions auto_action = new AutomatedActions(test_item, perform_action);

        // Trigger alarm via MINOR alarm
        final long start = System.currentTimeMillis();
        auto_action.handleSeverityUpdate(SeverityLevel.MINOR);

        // Half way through the delay, update to MAJOR alarm
        TimeUnit.MILLISECONDS.sleep(DELAY_MS/2);
        auto_action.handleSeverityUpdate(SeverityLevel.MAJOR);

        // This should _not_ change the start time for scheduling the action
        assertThat(action_performed.poll(DELAY_MS, TimeUnit.MILLISECONDS), equalTo("Send Email"));
        final long passed = System.currentTimeMillis() - start;
        System.out.println("Action performed after " + passed + " ms");
        // Actual delay should be within 20% of the expected delay
        assertTrue(Math.abs(DELAY_MS - passed) < DELAY_MS / 5);

        // When no longer needed, close to stop timers etc.
        auto_action.cancel();
    }

    @Test
    public void testAutomatedActionReset() throws Exception
    {
        // TODO Use TitleDetailDelay with delay of DELAY_MS
        final TitleDetail email = new TitleDetail("Send Email", "mailto:fred@mail.com");
        final AlarmClientNode test_item = new AlarmClientNode(null, "test");
        test_item.setActions(List.of(email));

        final AutomatedActions auto_action = new AutomatedActions(test_item, perform_action);

        // Trigger Action..
        auto_action.handleSeverityUpdate(SeverityLevel.MAJOR);
        // .. but acknowledge alarm within the DELAY
        TimeUnit.MILLISECONDS.sleep(DELAY_MS/2);
        auto_action.handleSeverityUpdate(SeverityLevel.MAJOR_ACK);
        // .. so nothing should happen
        assertThat(action_performed.poll(2*DELAY_MS, TimeUnit.MILLISECONDS), nullValue());

        // Trigger alarm and expect the action to be performed after DELAY_MS
        auto_action.handleSeverityUpdate(SeverityLevel.MAJOR);
        final long start = System.currentTimeMillis();
        assertThat(action_performed.poll(2*DELAY_MS, TimeUnit.MILLISECONDS), equalTo("Send Email"));
        final long passed = System.currentTimeMillis() - start;
        System.out.println("Action performed after " + passed + " ms");
        assertTrue(Math.abs(DELAY_MS - passed) < DELAY_MS / 5);

        auto_action.cancel();
    }

    @Test
    public void testAutomatedActionClear() throws Exception
    {
        // TODO Use TitleDetailDelay with delay of DELAY_MS
        final TitleDetail email = new TitleDetail("Send Email", "mailto:fred@mail.com");
        final AlarmClientNode test_item = new AlarmClientNode(null, "test");
        test_item.setActions(List.of(email));

        final AutomatedActions auto_action = new AutomatedActions(test_item, perform_action);

        // Trigger Action..
        for (int i=0; i<5; ++i)
        {
            auto_action.handleSeverityUpdate(SeverityLevel.MAJOR);
            // .. but acknowledge alarm within the DELAY
            TimeUnit.MILLISECONDS.sleep(DELAY_MS/2);
            auto_action.handleSeverityUpdate(SeverityLevel.MAJOR_ACK);
            // .. so nothing should happen
            assertThat(action_performed.poll(2*DELAY_MS, TimeUnit.MILLISECONDS), nullValue());
        }

        auto_action.handleSeverityUpdate(SeverityLevel.OK);
        assertThat(action_performed.poll(2*DELAY_MS, TimeUnit.MILLISECONDS), nullValue());

        auto_action.cancel();
    }
}

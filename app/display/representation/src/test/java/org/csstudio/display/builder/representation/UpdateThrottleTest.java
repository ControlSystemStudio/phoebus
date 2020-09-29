/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.csstudio.display.builder.model.Widget;
import org.junit.Test;

/** JUnit test of {@link UpdateThrottleTest}
 *
 *  <p>More of a demo because there is limited control
 *  over the timing of threads
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class UpdateThrottleTest
{
    private final RepresentationUpdateThrottle throttle = new RepresentationUpdateThrottle(Executors.newSingleThreadExecutor());

    private class TestWidgetRepresentation extends WidgetRepresentation<Object, Object, Widget>
    {
        private final String name;
        private final AtomicInteger updates;
        public volatile boolean trigger_on_update = false;

        public TestWidgetRepresentation(final String name, final AtomicInteger updates)
        {
            this.name = name;
            this.updates = updates;
            model_widget = new Widget("Demo");
        }

        @Override
        public Object createComponents(Object parent) throws Exception
        {
            return null;
        }

        @Override
        public void updateChanges()
        {
            final String now = Instant.now().toString();
            System.out.println(now + ": Widget " + name + " updates: " + updates.incrementAndGet());

            if (trigger_on_update)
            {   // Cause a burst of triggers right at the update
                trigger_on_update = false;
                throttle.scheduleUpdate(this);
                throttle.scheduleUpdate(this);
                throttle.scheduleUpdate(this);
                System.out.println(now + ": Widget " + name + " scheduled more updates");
            }
        }

        @Override
        public void dispose()
        {
            // NOP
        }

        @Override
        public void updateOrder()
        {
            // NOP
        }
    }

    @Test
    public void demonstrateUpdateThrottle() throws Throwable
    {
        final AtomicInteger updates_a = new AtomicInteger();
        final AtomicInteger updates_b = new AtomicInteger();
        final TestWidgetRepresentation widget_a = new TestWidgetRepresentation("A", updates_a);
        final TestWidgetRepresentation widget_b = new TestWidgetRepresentation("B", updates_b);

        // Nothing happened, yet
        assertThat(updates_a.get(), equalTo(0));
        assertThat(updates_b.get(), equalTo(0));

        // Widget A changes rapidly, Widget B changes once
        throttle.scheduleUpdate(widget_a);
        throttle.scheduleUpdate(widget_a);
        throttle.scheduleUpdate(widget_b);
        throttle.scheduleUpdate(widget_a);

        // There are no immediate updates because of UpdateThrottle update_accumulation_time
        assertThat(updates_a.get(), equalTo(0));
        assertThat(updates_b.get(), equalTo(0));

        // A little later, each widget was asked to update _once_
        TimeUnit.SECONDS.sleep(1);
        assertThat(updates_a.get(), equalTo(1));
        assertThat(updates_b.get(), equalTo(1));

        // Then no more updates, since no trigger
        TimeUnit.SECONDS.sleep(1);
        assertThat(updates_a.get(), equalTo(1));
        assertThat(updates_b.get(), equalTo(1));

        // One more update request per widget,
        // where widget_a will self-trigger more updates right away
        widget_a.trigger_on_update = true;
        throttle.scheduleUpdate(widget_a);
        throttle.scheduleUpdate(widget_b);

        // The printout should show that the self-trigger of widget A (3 times)
        // resulted in another update of Widget A (1 time) about 0.120 secs
        // after the update that causes the self-triggers
        TimeUnit.SECONDS.sleep(1);
        assertThat(updates_a.get(), equalTo(3));
        assertThat(updates_b.get(), equalTo(2));

        throttle.shutdown();
    }
}

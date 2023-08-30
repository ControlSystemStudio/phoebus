/*******************************************************************************
 * Copyright (c) 2018-2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv;

import io.reactivex.rxjava3.disposables.Disposable;
import org.epics.vtype.VDouble;
import org.epics.vtype.VType;
import org.junit.jupiter.api.Test;
import org.phoebus.pv.loc.LocalPVFactory;
import org.phoebus.pv.loc.ValueHelper;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class LocalPVTest {
    @Test
    public void testParser() throws Exception {
        String[] ntv = ValueHelper.parseName("x(42)");
        assertThat(ntv[0], equalTo("x"));
        assertThat(ntv[1], nullValue());
        assertThat(ntv[2], equalTo("42"));

        ntv = ValueHelper.parseName("x<VLong>(42)");
        assertThat(ntv[0], equalTo("x"));
        assertThat(ntv[1], equalTo("VLong"));
        assertThat(ntv[2], equalTo("42"));

        ntv = ValueHelper.parseName("text(\"Hello, \\\"Dolly\\\"\")");
        assertThat(ntv[0], equalTo("text"));
        assertThat(ntv[1], nullValue());
        assertThat(ntv[2], equalTo("\"Hello, \\\"Dolly\\\"\""));

        ntv = ValueHelper.parseName("text(\"Line1\nLine2\nLine3\")");
        assertThat(ntv[0], equalTo("text"));
        assertThat(ntv[1], nullValue());
        assertThat(ntv[2], equalTo("\"Line1\nLine2\nLine3\""));

        ntv = ValueHelper.parseName("text(\"Check if 5 < 7\")");
        assertThat(ntv[0], equalTo("text"));
        assertThat(ntv[1], nullValue());
        assertThat(ntv[2], equalTo("\"Check if 5 < 7\""));

        ntv = ValueHelper.parseName("Valid_PV-name");
        assertThat(ntv[0], equalTo("Valid_PV-name"));
        assertThat(ntv[1], nullValue());
        assertThat(ntv[2], nullValue());

        ntv = ValueHelper.parseName("Valid1.VAL");
        assertThat(ntv[0], equalTo("Valid1.VAL"));
        assertThat(ntv[1], nullValue());
        assertThat(ntv[2], nullValue());
    }

    @Test
    public void testParserErrors() {
        try {
            ValueHelper.parseName("");
            fail("missing name");
        } catch (Exception ex) {
            assertThat(ex.getMessage(), containsString("name"));
        }

        try {
            ValueHelper.parseName("number<VLong(32)");
            fail("missing type");
        } catch (Exception ex) {
            assertThat(ex.getMessage(), containsString("type"));
        }

        try {
            ValueHelper.parseName("number(32");
            fail("missing value");
        } catch (Exception ex) {
            assertThat(ex.getMessage(), containsString("value"));
        }
    }

    @Test
    public void testLocalPV() throws Exception {
        final LocalPVFactory factory = new LocalPVFactory();
        PV pv1 = factory.createPV("loc://x(42)", "x(42)");
        assertThat(pv1.getName(), equalTo("loc://x"));

        PV pv2 = factory.createPV("loc://x(47)", "x(47)");
        assertThat(pv2, sameInstance(pv1));

        final VType value = pv1.read();
        System.out.println(value);
        assertThat(value, instanceOf(VDouble.class));
        assertThat(((VDouble) value).getValue(), equalTo(42.0));

        pv1.close();
    }

    /**
     * For local PVs, assert that notifications happen right away
     * inside the calling thread.
     * <p>
     * Some other tests as well as the 'ArrayPVDispatcher' used in the display runtime
     * depend on 'write' to a local PV having an immediate effect,
     * not decoupled to another thread.
     */
    @Test
    public void testLocalPVNotifications() throws Exception {
        Thread.currentThread().setName("TestThread");
        final PV pv = PVPool.getPV("loc://event_demo(1)");

        System.out.println("Running local PV test on " + Thread.currentThread());

        // Count received values and track on which thread we receive them
        final AtomicReference<String> update_thread_name = new AtomicReference<>("none");
        final AtomicInteger updates = new AtomicInteger();
        final Disposable sub = pv.onValueEvent().subscribe(value ->
        {
            System.out.println(pv + " = " + value + " on " + Thread.currentThread());
            updates.incrementAndGet();
            update_thread_name.set(Thread.currentThread().getName());
        });

        // Expect initial value update right away on calling thread
        assertThat(updates.get(), equalTo(1));
        assertThat(update_thread_name.get(), equalTo("TestThread"));

        pv.write(42);
        // Expect update for newly written value right away on calling thread
        assertThat(updates.get(), equalTo(2));
        assertThat(update_thread_name.get(), equalTo("TestThread"));
        final VType value = pv.read();
        assertThat(value, instanceOf(VDouble.class));
        assertThat(((VDouble) value).getValue(), equalTo(42.0));

        sub.dispose();
        PVPool.releasePV(pv);
    }

}

/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.runtime.pv.ArrayPVDispatcher;
import org.csstudio.display.builder.runtime.pv.ArrayPVDispatcher.Listener;
import org.csstudio.display.builder.runtime.pv.PVFactory;
import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.epics.util.array.ListNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VStringArray;
import org.junit.Test;

/** JUnit demo of the {@link ArrayPVDispatcher}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ArrayPVDispatcherTest
{
    /** Test double-typed array PV */
    @Test
    public void testArrayPVDispatcher() throws Exception
    {
        // Array PV. It's elements are to be dispatched into separate PVs
        final RuntimePV array_pv = PVFactory.getPV("loc://an_array(1.0, 2.0, 3, 4)");

        // The per-element PVs that will be bound to the array
        final AtomicReference<List<RuntimePV>> element_pvs = new AtomicReference<>();

        final CountDownLatch got_element_pvs = new CountDownLatch(1);

        // Listener to the ArrayPVDispatcher
        final Listener dispatch_listener = new Listener()
        {
            @Override
            public void arrayChanged(final List<RuntimePV> pvs)
            {
                System.out.println("Per-element PVs: ");
                element_pvs.set(pvs);
                dump(pvs);
                got_element_pvs.countDown();
            }
        };

        final ArrayPVDispatcher dispatcher = new ArrayPVDispatcher(array_pv, "elementA247FE_", dispatch_listener);

        // Await initial set of per-element PVs
        got_element_pvs.await();
        assertThat(VTypeUtil.getValueNumber(element_pvs.get().get(0).read()).doubleValue(), equalTo(1.0));
        assertThat(VTypeUtil.getValueNumber(element_pvs.get().get(1).read()).doubleValue(), equalTo(2.0));
        assertThat(VTypeUtil.getValueNumber(element_pvs.get().get(2).read()).doubleValue(), equalTo(3.0));
        assertThat(VTypeUtil.getValueNumber(element_pvs.get().get(3).read()).doubleValue(), equalTo(4.0));

        // Change array -> Observe update of per-element PV
        System.out.println("Updating array");
        array_pv.write(new double[] { 1.0, 22.5, 3, 4 } );
        dump(element_pvs.get());
        // On one hand, this changed only one array element.
        // On the other hand, it's a new array value with a new time stamp.
        // Unclear if the array dispatcher should detect this and only update
        // the per-element PVs that really have a new value, or all.
        // Currently it updates all, but the test is satisfied with just one update:
        assertThat(VTypeUtil.getValueNumber(element_pvs.get().get(1).read()).doubleValue(), equalTo(22.5));

        // Change per-element PV -> Observe update of array
        System.out.println("Updating per-element PV for element [2]");
        element_pvs.get().get(2).write(30.7);

        // Test assumes a local array PV which immediately reflects the change.
        // A "real" PV can have a delay between writing a new value and receiving the update.

        final ListNumber array_value = ( (VNumberArray) array_pv.read() ).getData();
        System.out.println("Array: " +  array_value );
        assertThat(array_value.getDouble(2), equalTo(30.7));

        // Close dispatcher
        dispatcher.close();

        // Close the array PV
        PVFactory.releasePV(array_pv);
    }

    /** Test double-typed array PV */
    @Test
    public void testStringArray() throws Exception
    {
        final RuntimePV array_pv = PVFactory.getPV("loc://an_array(\"One\", \"Two\", \"Three\")");

        final AtomicReference<List<RuntimePV>> element_pvs = new AtomicReference<>();

        final CountDownLatch got_element_pvs = new CountDownLatch(1);

        // Listener to the ArrayPVDispatcher
        final Listener dispatch_listener = new Listener()
        {
            @Override
            public void arrayChanged(final List<RuntimePV> pvs)
            {
                element_pvs.set(pvs);
                got_element_pvs.countDown();
            }
        };

        final ArrayPVDispatcher dispatcher = new ArrayPVDispatcher(array_pv, "elementA247FE_", dispatch_listener);

        // Await initial set of per-element PVs
        got_element_pvs.await();
        assertThat(VTypeUtil.getValueString(element_pvs.get().get(0).read(), false), equalTo("One"));
        assertThat(VTypeUtil.getValueString(element_pvs.get().get(1).read(), false), equalTo("Two"));
        assertThat(VTypeUtil.getValueString(element_pvs.get().get(2).read(), false), equalTo("Three"));

        // Change array -> Observe update of per-element PV
        System.out.println("Updating array");
        array_pv.write(new String[] { "Uno", "Due", "Another", "Vier" } );
        dump(element_pvs.get());
        assertThat(VTypeUtil.getValueString(element_pvs.get().get(0).read(), false), equalTo("Uno"));
        assertThat(VTypeUtil.getValueString(element_pvs.get().get(3).read(), false), equalTo("Vier"));

        // Change per-element PV -> Observe update of array
        System.out.println("Updating per-element PV for element [2]");
        element_pvs.get().get(2).write("Hello");

        final List<String> array_value = ( (VStringArray) array_pv.read() ).getData();
        System.out.println("Array: " +  array_value );
        assertThat(array_value.get(2), equalTo("Hello"));

        // Close dispatcher
        dispatcher.close();

        // Close the array PV
        PVFactory.releasePV(array_pv);
    }

    /** Test double-typed scalar PV */
    @Test
    public void testScalarPVDispatcher() throws Exception
    {
        // Not really an array..
        final RuntimePV array_pv = PVFactory.getPV("loc://no_array(3.14)");

        // The per-element PVs that will be bound to the array
        final AtomicReference<List<RuntimePV>> element_pvs = new AtomicReference<>();

        final CountDownLatch got_element_pvs = new CountDownLatch(1);

        // Listener to the ArrayPVDispatcher
        final Listener dispatch_listener = new Listener()
        {
            @Override
            public void arrayChanged(final List<RuntimePV> pvs)
            {
                System.out.println("Per-element PVs: ");
                element_pvs.set(pvs);
                dump(pvs);
                got_element_pvs.countDown();
            }
        };

        final ArrayPVDispatcher dispatcher = new ArrayPVDispatcher(array_pv, "element123456_", dispatch_listener);

        // Await initial set of per-element PVs
        got_element_pvs.await();
        assertThat(element_pvs.get().size(), equalTo(1));
        assertThat(VTypeUtil.getValueNumber(element_pvs.get().get(0).read()).doubleValue(), equalTo(3.14));

        // Change array -> Observe update of per-element PV
        System.out.println("Updating 'array'");
        array_pv.write(47.11);
        dump(element_pvs.get());
        assertThat(VTypeUtil.getValueNumber(element_pvs.get().get(0).read()).doubleValue(), equalTo(47.11));

        // Change per-element PV -> Observe update of 'array'
        System.out.println("Updating per-element PV for element [2]");
        element_pvs.get().get(0).write(11.47);

        final Number value = VTypeUtil.getValueNumber(array_pv.read());
        System.out.println("'Array': " +  value );
        assertThat(value, equalTo(11.47));

        // Close dispatcher
        dispatcher.close();

        // Close the array PV
        PVFactory.releasePV(array_pv);
    }

    private void dump(final List<RuntimePV> pvs)
    {
        for (RuntimePV pv : pvs)
            System.out.println(pv.getName() + " = " + pv.read());
    }
}

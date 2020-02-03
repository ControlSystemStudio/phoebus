/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.data;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.BitSet;

import org.junit.Test;

@SuppressWarnings("nls")
public class StructureTest
{
    @Test
    public void testStructure() throws Exception
    {
        // Create some structure, a few fields already set
        final PVAStructure time = new PVAStructure("timeStamp", "time_t",
                                                   new PVALong("secondsPastEpoch"),
                                                   new PVAInt("nanoseconds", 42),
                                                   new PVAInt("userTag"));
        // Set value of other field in struct
        final PVALong sec = time.get("secondsPastEpoch");
        sec.set(41);

        // Create some other struct
        final PVAStructure alarm = new PVAStructure("alarm", "alarm_t",
                                                    new PVAInt("severity"),
                                                    new PVAInt("status"),
                                                    new PVAString("message", "NO_ALARM"));

        // Create top-level data
        final PVAStructure data = new PVAStructure("demo", "NTBogus",
                                                   new PVADouble("value", 3.14),
                                                   time,
                                                   alarm,
                                                   new PVAInt("extra", 42));

        System.out.println("Type Description:");
        System.out.println(data.formatType());

        System.out.println("\nValue:");
        System.out.println(data);

        System.out.println("\nElements:");
        final PVADouble value = data.get("value");
        System.out.println(value);
        assertThat(value, not(nullValue()));
        assertThat(value.get(), equalTo(3.14));

        int idx = data.getIndex(value);
        System.out.println("Index: " + idx);
        assertThat(idx, equalTo(1));

        final PVAInt extra = data.get("extra");
        System.out.println(extra);
        assertThat(extra.get(), equalTo(42));
        idx = data.getIndex(extra);
        System.out.println("Index: " + idx);
        assertThat(idx, equalTo(10));

        System.out.println("\nIndexed access:");
        int i = 0;
        PVAData sub = data.get(i);
        System.out.println("Index " + i + ": " + sub.getName());
        assertThat(sub, equalTo(data));
        idx = data.getIndex(sub);
        System.out.println("Index lookup: " + idx);
        assertThat(idx, equalTo(i));

        i = 1;
        sub = data.get(i);
        System.out.println("Index " + i + ": " + sub.getName());
        assertThat(sub.getName(), equalTo("value"));
        idx = data.getIndex(sub);
        System.out.println("Index lookup: " + idx);
        assertThat(idx, equalTo(i));

        i = 2;
        sub = data.get(i);
        System.out.println("Index " + i + ": " + sub.getName());
        assertThat(sub.getName(), equalTo("timeStamp"));
        idx = data.getIndex(sub);
        System.out.println("Index lookup: " + idx);
        assertThat(idx, equalTo(i));

        i = 3;
        sub = data.get(i);
        System.out.println("Index " + i + ": " + sub.getName());
        assertThat(sub.getName(), equalTo("secondsPastEpoch"));
        idx = data.getIndex(sub);
        System.out.println("Index lookup: " + idx);
        assertThat(idx, equalTo(i));

        i = 10;
        sub = data.get(i);
        System.out.println("Index " + i + ": " + sub.getName());
        assertThat(sub.getName(), equalTo("extra"));
        idx = data.getIndex(sub);
        System.out.println("Index lookup: " + idx);
        assertThat(idx, equalTo(i));

        i = 11;
        sub = data.get(i);
        System.out.println("Index " + i + ": " + sub);
        assertThat(sub, nullValue());
    }

    /** Check structure errors */
    @Test
    public void testError() throws Exception
    {
        // OK for structure to be empty, not named
        new PVAStructure("", "");

        // But _elements_ of the structure need names to address them
        try
        {
            new PVAStructure("", "", new PVADouble(""));
            fail("Structure elements must be named");
        }
        catch (Exception ex)
        {
            System.out.println("Expected: " + ex.getMessage());
        }
    }

    /** Assign structure elements from a few different types */
    @Test
    public void testAssign() throws Exception
    {
        final PVAStructure time = new PVAStructure("timeStamp", "time_t",
                                                   new PVALong("secondsPastEpoch"),
                                                   new PVAInt("nanoseconds"),
                                                   new PVAInt("userTag"));
        final PVAStructure data = new PVAStructure("demo", "NTBogus",
                                                   new PVADouble("value", 3.14),
                                                   time,
                                                   new PVAString("extra"),
                                                   new PVABool("flag"));

        PVAData new_value = new PVAFloat("", 42.0f);
        PVAData new_extra = new PVAString("", "tag!");
        data.get(1).setValue(new_value);
        data.get(6).setValue(new_extra);
        data.get(7).setValue(1);
        System.out.println(data);

        data.get(1).setValue(24.0);
        data.get("extra").setValue(new_value);
        data.get(7).setValue(0);
        System.out.println(data);
    }

    /** Structure updates and change determination */
    @Test
    public void testUpdate() throws Exception
    {
        final PVAStructure time = new PVAStructure("timeStamp", "time_t",
                                                   new PVALong("secondsPastEpoch"),
                                                   new PVAInt("nanoseconds"),
                                                   new PVAInt("userTag"));
        final PVAStructure data = new PVAStructure("demo", "NTBogus",
                                                   new PVADouble("value", 3.14),
                                                   time,
                                                   new PVAString("extra"),
                                                   new PVABool("flag"),
                                                   new PVALongArray("array", false)
                                                   );
        final PVAStructure snapshot = data.cloneData();
        System.out.println(snapshot);

        // Update 1: value
        final PVADouble value = data.get("value");
        value.set(value.get() + 1);

        BitSet changes = snapshot.update(data);
        System.out.println("Changes: " + changes);
        System.out.println(snapshot);

        final BitSet expected = new BitSet();
        expected.set(1);
        assertThat(changes, equalTo(expected));

        // 1: Value, 2: time
        value.set(value.get() + 1);
        // secondsPastEpoch, nano, tag
        data.get(3).setValue(47);
        data.get(4).setValue(48);
        data.get(5).setValue(49);

        changes = snapshot.update(data);
        System.out.println("Changes: " + changes);
        System.out.println(snapshot);

        expected.clear();
        expected.set(1);
        expected.set(2);
        assertThat(changes, equalTo(expected));

        // 1: Value, 6: extra, 7: flag
        value.set(value.get() + 1);
        data.get(6).setValue("Kram");
        data.get(7).setValue(true);

        changes = snapshot.update(data);
        System.out.println("Changes: " + changes);
        System.out.println(snapshot);

        expected.clear();
        expected.set(1);
        expected.set(6);
        expected.set(7);
        assertThat(changes, equalTo(expected));

        // All
        value.set(value.get() + 1);
        data.get(3).setValue(48);
        data.get(4).setValue(49);
        data.get(5).setValue(50);
        data.get(6).setValue("Kram2");
        data.get(7).setValue(false);
        data.get(8).setValue(new long[] { 1, 2 });

        changes = snapshot.update(data);
        System.out.println("Changes: " + changes);
        System.out.println(snapshot);

        expected.clear();
        expected.set(0);
        assertThat(changes, equalTo(expected));

        // Array set to same value, i.e. no change
        data.get(8).setValue(new long[] { 1, 2 });
        changes = snapshot.update(data);
        System.out.println("Changes: " + changes);
        System.out.println(snapshot);

        expected.clear();
        assertThat(changes, equalTo(expected));

        // Array set to new value
        data.get(8).setValue(new long[] { 3, 4 });
        changes = snapshot.update(data);
        System.out.println("Changes: " + changes);
        System.out.println(snapshot);

        expected.clear();
        expected.set(8);
        assertThat(changes, equalTo(expected));
    }

    /** Test 'locate(path)' */
    @Test
    public void testLocateElement() throws Exception
    {
        final PVAInt value = new PVAInt("value", 42);
        final PVAStructure inner = new PVAStructure("a", "A", value);
        final PVAStructure outer = new PVAStructure("b", "B", inner);
        final PVAStructure data = new PVAStructure("demo", "NTBogus", outer);
        System.out.println(data);

        // 'get' only performs direct lookup
        assertThat(data.get("b"), sameInstance(outer));
        assertThat(data.get("b.a"), nullValue());

        // 'locate' finds sub-elements along a dot-path
        assertThat(data.locate("b"), sameInstance(outer));
        assertThat(data.locate("b.a"), sameInstance(inner));
        assertThat(data.locate("b.a.value"), sameInstance(value));

        assertThat(data.get("b.x"), nullValue());
        try
        {
            data.locate("b.a.value.bogus");
            fail("There is nothing below b.a.value");
        }
        catch (Exception ex)
        {
            assertThat(ex.getMessage(), containsString("not a structure"));
        }

        try
        {
            data.locate("b.x.value.bogus");
            fail("There is no b.x.*");
        }
        catch (Exception ex)
        {
            assertThat(ex.getMessage(), containsString("Cannot locate 'x'"));
        }
    }
}
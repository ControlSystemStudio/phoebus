/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.data;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

@SuppressWarnings("nls")
public class PVAnyTest
{
    @Test
    public void testAny() throws Exception
    {
        final PVAny any = new PVAny("any");
        System.out.println(any);

        any.setValue(new PVAInt("number", 42));
        System.out.println(any);
        PVAInt i = any.get();
        assertThat(i.get(), equalTo(42));

        PVAny copy = any.cloneData();
        i = any.get();
        assertThat(i.get(), equalTo(42));

        // Two 'any' types are equal if they contain the same value
        final PVAny any2 = new PVAny("any", new PVAInt("number", 42));
        assertThat(any, equalTo(any2));

        // An 'any' is NOT equal to a different type which happens to have the same value
        final PVAData number = new PVAInt("number", 42);
        assertThat(any.get(), equalTo(number));
        assertThat(any, not(equalTo(number)));

        any.setValue(null);
        System.out.println(any);
        assertThat(any.get(), nullValue());

        copy = any.cloneData();
        assertThat(copy.get(), nullValue());

        assertThat(any, equalTo(copy));

        any.setValue(new PVADouble("number", 3.13));
        System.out.println(any);
        PVADouble d = any.get();
        assertThat(d.get(), equalTo(3.13));
    }
}
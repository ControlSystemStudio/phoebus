/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import org.epics.vtype.VDouble;
import org.epics.vtype.VType;
import org.junit.Test;
import org.phoebus.pv.loc.LocalPVFactory;

/** @author Kay Kasemir */
@SuppressWarnings("nls")
public class LocalPVTest
{
    @Test
    public void testLocalPV() throws Exception
    {
        final LocalPVFactory factory = new LocalPVFactory();
        PV pv1 = factory.createPV("loc://x(42)", "x(42)");
        assertThat(pv1.getName(), equalTo("loc://x"));

        PV pv2 = factory.createPV("loc://x(47)", "x(47)");
        assertThat(pv2, sameInstance(pv1));

        final VType value = pv1.read();
        System.out.println(value);
        assertThat(value, instanceOf(VDouble.class));
        assertThat(((VDouble)value).getValue(), equalTo(42.0));

        pv1.close();
    }
}

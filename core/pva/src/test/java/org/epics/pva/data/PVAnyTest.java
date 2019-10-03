/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.data;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

@SuppressWarnings("nls")
public class PVAnyTest
{
    @Test
    public void testAny() throws Exception
    {
        final PVAny data = new PVAny("any");
        System.out.println(data);

        data.setValue(new PVAInt("number", 42));
        PVAInt i = data.get();
        assertThat(i.get(), equalTo(42));

        data.setValue(null);
        assertThat(data.get(), nullValue());

        data.setValue(new PVADouble("number", 3.13));
        PVADouble d = data.get();
        assertThat(d.get(), equalTo(3.13));

    }
}
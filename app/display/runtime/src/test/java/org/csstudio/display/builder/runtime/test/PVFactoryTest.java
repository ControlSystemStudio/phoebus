/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.test;

import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.runtime.pv.PVFactory;
import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.csstudio.display.builder.runtime.pv.RuntimePVListener;
import org.epics.vtype.VType;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;


/** JUnit demo of the {@link PVFactory}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVFactoryTest
{
    @Test
    public void testPVFactory() throws Exception
    {
        final RuntimePV pv = PVFactory.getPV("loc://test(3.14)");
        try
        {   // PV uses the base name, without initializer
            assertThat(pv.getName(), equalTo("loc://test"));

            final CountDownLatch updates = new CountDownLatch(1);
            final AtomicReference<Number> number = new AtomicReference<>();
            RuntimePVListener listener = (pv1, value) -> {
                System.out.println(pv1.getName() + " = " + value);
                number.set(VTypeUtil.getValueNumber(value));
                updates.countDown();
            };
            pv.addListener(listener);
            updates.await();
            assertThat(number.get(), equalTo(3.14));
        }
        finally
        {
            PVFactory.releasePV(pv);
        }
    }
}

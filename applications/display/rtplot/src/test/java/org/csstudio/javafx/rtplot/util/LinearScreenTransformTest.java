/*******************************************************************************
 * Copyright (c) 2010-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.csstudio.javafx.rtplot.internal.util.LinearScreenTransform;
import org.csstudio.javafx.rtplot.internal.util.ScreenTransform;
import org.junit.Test;

/** JUnit test of {@link LinearScreenTransform}.
 *  @author Kay Kasemir
 */
public class LinearScreenTransformTest
{
    @Test
    public void testTransform()
    {
        ScreenTransform<Double> t = new LinearScreenTransform();

        // Default 1:1
        assertThat(t.transform(0.0), equalTo(0.0));
        assertThat(t.transform(1.0), equalTo(1.0));

        t.config(0.0, 10.0, -1.0, 0.0);
        assertThat(t.transform(0.0), equalTo(-1.0));
        assertThat(t.transform(10.0), equalTo(0.0));
        assertThat(t.transform(20.0), equalTo(1.0));
    }
}

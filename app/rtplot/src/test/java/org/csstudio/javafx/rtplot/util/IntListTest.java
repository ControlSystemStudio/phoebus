/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.csstudio.javafx.rtplot.internal.util.IntList;
import org.junit.Test;

/** JUnit test of {@link IntList}
 *  @author Kay Kasemir
 */
public class IntListTest
{
    @Test
    public void testIntList()
    {
        IntList list = new IntList(2);
        list.add(1);
        list.add(2);
        assertThat(list.size(), equalTo(2));
        list.add(3);
        list.add(4);
        list.add(5);
        assertThat(list.size(), equalTo(5));
        assertThat(list.toArray(), equalTo(new int[] { 1, 2, 3, 4, 5 }));
    }
}

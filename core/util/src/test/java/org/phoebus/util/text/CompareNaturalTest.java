/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.util.text;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

/** JUnit Test of {@link CompareNatural}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class CompareNaturalTest
{
    @Test
    public void testComparison()
    {
        final List<String> items = Arrays.asList("Sys1:V", "Sys1:A", "Sys10:V", "Sys10:A");

        // ASCII-type sort places "Sys10.." before "Sys1:" because '0' < ':'
        items.sort(Comparator.naturalOrder());
        System.out.println(items);
        assertThat(items, equalTo(List.of("Sys10:A", "Sys10:V", "Sys1:A", "Sys1:V")));

        // Natural Sort
        items.sort(CompareNatural.INSTANCE);
        System.out.println(items);
        assertThat(items, equalTo(List.of("Sys1:A", "Sys1:V", "Sys10:A", "Sys10:V")));
    }
}

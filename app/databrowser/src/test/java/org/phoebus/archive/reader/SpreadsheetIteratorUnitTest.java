/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.epics.vtype.VType;
import org.junit.Test;
import org.phoebus.archive.vtype.DefaultVTypeFormat;
import org.phoebus.archive.vtype.VTypeFormat;
import org.phoebus.archive.vtype.VTypeHelper;

/** JUnit test of the {@link SpreadsheetIterator}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SpreadsheetIteratorUnitTest
{
    /** Show two DemoDataIterators in a spreadsheet.
     *  Time stamps align perfectly
     */
    @Test
    public void testSpreadsheetIterator() throws Exception
    {
        final String result = runSheetTest(0);
        assertThat(result, equalTo("A 1,B 1,A 2,B 2,A 3,B 3,A 4,B 4,A 5,B 5,A 6,B 6,A 7,B 7,A 8,B 8,A 9,B 9,A 10,B 10"));
    }

    /** Show two DemoDataIterators in a spreadsheet.
     *  Second iter lags first one in time.
     */
    @Test
    public void testSpreadsheetIterator2() throws Exception
    {
        final String result = runSheetTest(5);
        assertThat(result, equalTo("A 1,null,A 2,null,A 3,null,A 4,null,A 5,null,A 6,B 1,A 7,B 2,A 8,B 3,A 9,B 4,A 10,B 5,A 10,B 6,A 10,B 7,A 10,B 8,A 10,B 9,A 10,B 10"));
    }

    private String runSheetTest(final int time_lag) throws Exception
    {
        System.out.println("SpreadsheetIterator");
        final DemoDataIterator iter1 = DemoDataIterator.forStrings("A");
        final DemoDataIterator iter2 = DemoDataIterator.forStrings("B", time_lag);
        final SpreadsheetIterator sheet = new SpreadsheetIterator(
                new ValueIterator[] { iter1, iter2 });
        final StringBuilder result = new StringBuilder();
        int count = 0;
        final VTypeFormat format = new DefaultVTypeFormat();
        while (sheet.hasNext())
        {
            System.out.print(sheet.getTime());
            final VType[] values = sheet.next();
            for (VType value : values)
            {
                System.out.print("\t" + (value == null ? "-" : VTypeHelper.toString(value)));
                if (result.length() > 0)
                    result.append(",");
                format.format(value, result);
            }
            System.out.println();
            ++count;
        }
        assertThat(count, equalTo(10 + time_lag));
        assertThat(iter1.isOpen(), equalTo(true));
        assertThat(iter2.isOpen(), equalTo(true));
        sheet.close();
        assertThat(iter1.isOpen(), equalTo(false));
        assertThat(iter2.isOpen(), equalTo(false));
        return result.toString();
    }
}

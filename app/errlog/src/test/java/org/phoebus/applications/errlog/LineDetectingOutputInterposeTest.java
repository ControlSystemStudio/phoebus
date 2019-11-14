/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.errlog;

import static org.junit.Assert.assertEquals;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/** Test of {@link LineDetectingOutputInterpose}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class LineDetectingOutputInterposeTest
{
    @Test
    public void testInterpose()
    {
        final List<String> captured = new ArrayList<>();

        final LineDetectingOutputInterpose interpose =
                new LineDetectingOutputInterpose(System.out,
                                                 line ->
        {
            System.out.println(" -- LINE >>" + line + "<<");
            captured.add(line);
        });

        final PrintStream print = new PrintStream(interpose);
        print.print("Hello, ");
        assertEquals(0, captured.size());
        print.println("Dolly!");
        assertEquals(1, captured.size());
        print.println("Bye.");
        assertEquals(2, captured.size());
        print.close();

        // Original stream is not closed
        System.out.println("Done.");
        assertEquals(2, captured.size());
    }
}

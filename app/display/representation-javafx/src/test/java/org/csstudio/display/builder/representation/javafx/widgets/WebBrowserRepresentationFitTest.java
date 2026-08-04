/*******************************************************************************
 * Copyright (c) 2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** JUnit tests for {@link WebBrowserRepresentation#computeFitSize}.
 *
 *  @author Gianluca Martino
 */
@SuppressWarnings("nls")
public class WebBrowserRepresentationFitTest
{
    private static final double EPS = 0.0001;

    /** At 100% zoom and origin (0,0) the browser fills the whole viewport */
    @Test
    public void testFillsViewport()
    {
        final double[] size = WebBrowserRepresentation.computeFitSize(1000, 800, 1.0, 0, 0, 10, 10);
        assertEquals(1000, size[0], EPS);
        assertEquals(800, size[1], EPS);
    }

    /** The design (x,y) is subtracted so the right/bottom edges track the viewport */
    @Test
    public void testAnchorOffset()
    {
        final double[] size = WebBrowserRepresentation.computeFitSize(1000, 800, 1.0, 50, 30, 10, 10);
        assertEquals(950, size[0], EPS);
        assertEquals(770, size[1], EPS);
    }

    /** Zoom divides the viewport into widget coordinates */
    @Test
    public void testZoomScales()
    {
        final double[] size = WebBrowserRepresentation.computeFitSize(1000, 800, 2.0, 0, 0, 10, 10);
        assertEquals(500, size[0], EPS);
        assertEquals(400, size[1], EPS);
    }

    /** Result never drops below the supplied minimum */
    @Test
    public void testMinimumFloor()
    {
        final double[] size = WebBrowserRepresentation.computeFitSize(40, 40, 1.0, 100, 100, 25, 15);
        assertEquals(25, size[0], EPS);
        assertEquals(15, size[1], EPS);
    }

    /** A non-positive zoom is treated as 100% to avoid divide-by-zero */
    @Test
    public void testNonPositiveZoomGuard()
    {
        final double[] size = WebBrowserRepresentation.computeFitSize(1000, 800, 0.0, 0, 0, 10, 10);
        assertEquals(1000, size[0], EPS);
        assertEquals(800, size[1], EPS);
    }

    /** A negative zoom is also treated as 100% (full non-positive range guarded) */
    @Test
    public void testNegativeZoomGuard()
    {
        final double[] size = WebBrowserRepresentation.computeFitSize(1000, 800, -1.0, 0, 0, 10, 10);
        assertEquals(1000, size[0], EPS);
        assertEquals(800, size[1], EPS);
    }
}

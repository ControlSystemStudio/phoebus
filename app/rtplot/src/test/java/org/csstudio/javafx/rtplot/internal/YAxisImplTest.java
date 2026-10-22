/*******************************************************************************
 * Copyright (c) 2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

/** JUnit test for {@link YAxisImpl} */
public class YAxisImplTest
{
    /** Hiding the tick labels must not move the ticks: the axis keeps the
     *  same room at both ends, so a ticks-only scale lines up with a
     *  labelled one */
    @Test
    public void testTicksOnlyKeepsPixelGaps()
    {
        final PlotPartListener listener = new PlotPartListener()
        {
            @Override
            public void layoutPlotPart(final PlotPart plotPart)
            {
            }

            @Override
            public void refreshPlotPart(final PlotPart plotPart)
            {
            }
        };
        final YAxisImpl<Double> axis = new YAxisImpl<>("", listener);
        axis.setValueRange(0.0, 100.0);
        axis.setBounds(0, 0, 40, 300);
        final Graphics2D gc = new BufferedImage(40, 300, BufferedImage.TYPE_INT_ARGB).createGraphics();
        axis.computeTicks(gc);
        for (boolean perpendicular : new boolean[] { false, true })
        {
            axis.setPerpendicularTickLabels(perpendicular);
            axis.setScaleLabelsVisible(true);
            final int[] labelled = axis.getPixelGaps(gc);
            assertTrue(labelled[0] > 0  &&  labelled[1] > 0);
            axis.setScaleLabelsVisible(false);
            assertArrayEquals(labelled, axis.getPixelGaps(gc));
        }
        gc.dispose();
    }
}

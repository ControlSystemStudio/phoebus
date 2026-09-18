/*******************************************************************************
 * Copyright (c) 2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;

import org.junit.jupiter.api.Test;

/** JUnit test for the label format of a {@link YAxisImpl} */
@SuppressWarnings("nls")
public class YAxisLabelFormatTest
{
    /** Setting a label format must relabel ticks that were already computed */
    @Test
    public void testLabelFormatRelabelsTicks()
    {
        final PlotPartListener listener = new PlotPartListener()
        {
            @Override
            public void layoutPlotPart(final PlotPart plotPart)
            {
                // The test computes the ticks itself, no plot to lay out
            }

            @Override
            public void refreshPlotPart(final PlotPart plotPart)
            {
                // Nothing is drawn in this test
            }
        };
        final YAxisImpl<Double> axis = new YAxisImpl<>("", listener);
        axis.setValueRange(0.0, 1.0);
        axis.setBounds(0, 0, 60, 300);
        final Graphics2D gc = new BufferedImage(60, 300, BufferedImage.TYPE_INT_ARGB).createGraphics();
        axis.computeTicks(gc);
        assertThat(axis.getTicks().getMajorTicks().get(0).getLabel(), equalTo("0.0"));

        axis.setLabelFormat(new DecimalFormat("0.000"));
        axis.computeTicks(gc);
        assertThat(axis.getTicks().getMajorTicks().get(0).getLabel(), equalTo("0.000"));
        gc.dispose();
    }
}

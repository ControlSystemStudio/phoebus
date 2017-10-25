/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

/** Listener to a {@link PlotPart}
 *  @author Kay Kasemir
 */
public interface PlotPartListener
{
    /** Called when plot has changed its size */
    void layoutPlotPart(PlotPart plotPart);

    /** Called when plot part needs to be re-drawn in place */
    void refreshPlotPart(PlotPart plotPart);
}

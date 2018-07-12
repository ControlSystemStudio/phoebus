/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

/** Real-time plot using numbers on the 'X' axis
 *  @author Kay Kasemir
 */
public class RTValuePlot extends RTPlot<Double>
{
    /** Constructor
     *  @param active Active mode where plot reacts to mouse/keyboard?
     */
    public RTValuePlot(final boolean active)
    {
        super(Double.class, active);
    }
}

/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.data;

import org.csstudio.javafx.rtplot.AxisRange;

/** Range for 'value' (Y) axes
 *  @author Kay Kasemir
 */
public class ValueRange extends AxisRange<Double>
{
    public ValueRange(final double low, final double high)
    {
        super(low, high);
    }
}

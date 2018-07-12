/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.test;

import static java.lang.Math.PI;
import static java.lang.Math.round;
import static java.lang.Math.sin;
import static java.lang.System.currentTimeMillis;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propWidth;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propX;

import org.csstudio.display.builder.model.Widget;

/** Helper for generating dummy widget updates
 *  @author Kay Kasemir
 */
class DummyPositionUpdater implements Runnable
{
    private final Widget widget;
    private final int x;
    private final int width;

    public DummyPositionUpdater(final Widget widget)
    {
        this.widget = widget;
        this.x = widget.getProperty(propX).getValue();
        this.width = widget.getProperty(propWidth).getValue();
    }

    @Override
    public void run()
    {
        final int current_width = (int)round(width + 10.0*sin(2.0*PI * currentTimeMillis()/1000.0));
        // Change width while keeping right edge of widget in same location
        widget.getProperty(propX).setValue(x + width - current_width);
        widget.getProperty(propWidth).setValue(current_width);
    }
}
/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.test;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propText;

import org.csstudio.display.builder.model.Widget;

/** Helper for generating dummy widget updates
 *  @author Kay Kasemir
 */
class DummyTextUpdater implements Runnable
{
    private final Widget widget;

    public DummyTextUpdater(final Widget widget)
    {
        this.widget = widget;
    }

    @Override
    public void run()
    {
        // Count 0..99 at 10 Hz
        final String text = Long.toString(
                (System.currentTimeMillis() / 100) % 100
                );
        widget.setPropertyValue(propText, text);
    }
}
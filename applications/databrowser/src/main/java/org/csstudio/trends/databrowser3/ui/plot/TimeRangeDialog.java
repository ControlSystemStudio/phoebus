/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.plot;

import org.csstudio.trends.databrowser3.Messages;
import org.phoebus.ui.time.TemporalAmountPane;
import org.phoebus.ui.time.TimeRelativeIntervalPane;
import org.phoebus.util.time.TimeRelativeInterval;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

/** Dialog for entering time range
 *  @author Kay Kasemir
 */
public class TimeRangeDialog extends Dialog<TimeRelativeInterval>
{
    private final TimeRelativeIntervalPane times = new TimeRelativeIntervalPane(TemporalAmountPane.Type.ONLY_NOW);

    public TimeRangeDialog(final TimeRelativeInterval range)
    {
        times.setInterval(range);
        setTitle(Messages.TimeColumn);
        getDialogPane().setContent(times);
        getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        setResultConverter(button ->
        {
            if (button == ButtonType.OK)
                return times.getInterval();
            return null;
        });
    }
}

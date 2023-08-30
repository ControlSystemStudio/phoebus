/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui;

import javafx.scene.Node;
import javafx.scene.layout.Region;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.ui.properties.ChangeTimerangeCommand;
import org.phoebus.ui.dialog.PopOver;
import org.phoebus.ui.time.TimeRelativeIntervalPane;
import org.phoebus.ui.undo.UndoableActionManager;
import org.phoebus.util.time.TimeInterval;
import org.phoebus.util.time.TimeRelativeInterval;

import java.util.function.BiConsumer;

/** Helper for handling start/end time changes
 *  @author Kay Kasemir
 */
public class ChangeTimerangeAction
{
    /** Prompt user for new start/end time
     *  @param model Model to change
     *  @param node Node for positioning the time range dialog
     *  @param undo Undo/Redo operations manager
     */
    public static void run(final Model model, final Node node, final UndoableActionManager undo)
    {

        BiConsumer<TimeRelativeIntervalPane, PopOver> closeCallback = (timePane, popOver) -> {
            popOver.hide();
        };

        BiConsumer<TimeRelativeIntervalPane, PopOver> applyCallback = (timePane, popOver) -> {
            TimeRelativeInterval range = timePane.getInterval();
            if (range.isStartAbsolute()  ||  range.isEndAbsolute())
            {
                final TimeInterval abs = range.toAbsoluteInterval();
                new ChangeTimerangeCommand(model, undo, TimeRelativeInterval.of(abs.getStart(), abs.getEnd()));
            }
            else {
                new ChangeTimerangeCommand(model, undo, range);
            }
            popOver.hide();
        };

        final TimeRangePopover popover = TimeRangePopover.withDefaultTimePane(model, closeCallback, applyCallback);

        popover.show((Region) node);
    }
}

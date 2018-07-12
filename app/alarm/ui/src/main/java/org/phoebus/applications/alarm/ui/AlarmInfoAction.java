/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui;

import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.ui.dialog.MultiLineInputDialog;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.util.time.TimestampFormats;

import javafx.scene.Node;
import javafx.scene.control.MenuItem;

/** Action that displays info about duration of an alarm
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmInfoAction extends MenuItem
{
    /** @param node Node to position dialog
     *  @param item Alarm item
     */
    public AlarmInfoAction(final Node node, final AlarmClientLeaf item)
    {
        super(item.getState().getDuration(), ImageCache.getImageView(AlarmUI.class, "/icons/clock.png"));
        setOnAction(event ->
        {
            final StringBuilder info = new StringBuilder();
            info.append(item.getPathName()).append("\n\n")
                .append("Description: ").append(item.getDescription()).append("\n\n")
                .append("In alarm since ")
                .append(TimestampFormats.MILLI_FORMAT.format(item.getState().getTime()))
                .append(", that is ").append(item.getState().getDuration()).append(" HH:MM:SS");

            final MultiLineInputDialog dialog = new MultiLineInputDialog(node, info.toString(), false);
            dialog.setTextWidth(800);
            dialog.showAndWait();
        });
    }
}

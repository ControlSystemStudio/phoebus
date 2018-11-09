/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtree.ui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.epics.vtype.AlarmSeverity;
import org.phoebus.applications.pvtree.model.TreeModelItem;
import org.phoebus.ui.pv.SeverityColors;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.TreeCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

/** Cell for JFX tree that represents TreeModelItem
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class TreeModelItemCell extends TreeCell<TreeModelItem>
{
    /** @param color Color of icon
     *  @return Icon
     */
    private static Image createAlarmIcon(final java.awt.Color color)
    {
        final BufferedImage buf = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D gc = buf.createGraphics();
        gc.setColor(new java.awt.Color(0, 0, 0, 0));
        gc.fillRect(0, 0, 16, 16);
        gc.setColor(color);
        gc.fillOval(0, 0, 16, 16);
        gc.dispose();
        return SwingFXUtils.toFXImage(buf, null);
    }

    /** Icon for null alarm */
    private static final Image NO_ICON = createAlarmIcon(new java.awt.Color(0, 200, 0, 50));

    /** Icons for alarm severity by ordinal */
    private static final Image[] ALARM_ICONS = new Image[]
    {
        createAlarmIcon(java.awt.Color.GREEN),
        createAlarmIcon(java.awt.Color.YELLOW),
        createAlarmIcon(java.awt.Color.RED),
        createAlarmIcon(java.awt.Color.MAGENTA),
        createAlarmIcon(new java.awt.Color(139, 0, 139)) // DARKMAGENTA
    };

    static
    {
        // This code depends on the number and order of AlarmSeverity values
        if (ALARM_ICONS.length != AlarmSeverity.values().length)
            throw new IllegalStateException("Number of alarm severities has changed");
    }

    @Override
    protected void updateItem(final TreeModelItem item, final boolean empty)
    {
        super.updateItem(item, empty);
        if (empty  ||  item == null)
        {
            setText(null);
            setGraphic(null);
        }
        else
        {
            setText(item.toString());
            final AlarmSeverity severity = item.getSeverity();
            if (severity == null)
            {
                setGraphic(new ImageView(NO_ICON));
                setTextFill(Color.BLACK);
            }
            else
            {
                final int ordinal = severity.ordinal();
                setGraphic(new ImageView(ALARM_ICONS[ordinal]));
                setTextFill(SeverityColors.getTextColor(severity));
            }
        }
    }
}

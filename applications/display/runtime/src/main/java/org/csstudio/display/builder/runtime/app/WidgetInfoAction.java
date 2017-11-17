/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import java.util.ArrayList;
import java.util.List;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.representation.javafx.WidgetInfoDialog;
import org.csstudio.display.builder.representation.javafx.widgets.JFXBaseRepresentation;
import org.csstudio.display.builder.runtime.WidgetRuntime;
import org.csstudio.display.builder.runtime.pv.RuntimePV;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** Action that displays information about a widget
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WidgetInfoAction extends MenuItem
{
    private static final Image icon = new Image(WidgetInfoAction.class.getResource("/icons/information.png").toExternalForm());

    public WidgetInfoAction(final Widget widget)
    {
        super("'" + widget.getName() + "' Information", new ImageView(icon));

        setOnAction(event ->
        {
            final WidgetRuntime<?> runtime = WidgetRuntime.ofWidget(widget);
            final List<WidgetInfoDialog.NameStateValue> pvs = new ArrayList<>();
            for (RuntimePV pv : runtime.getPVs())
                pvs.add(new WidgetInfoDialog.NameStateValue(pv.getName(), pv.isReadonly() ? "read-only" : "writable", pv.read()));
            final WidgetInfoDialog dialog = new WidgetInfoDialog(widget, pvs);

            final Node node = JFXBaseRepresentation.getJFXNode(widget);
            final Bounds bounds = node.localToScreen(node.getBoundsInLocal());
            dialog.setX(bounds.getMinX());
            dialog.setY(bounds.getMinY());
            dialog.show();
        });
    }
}

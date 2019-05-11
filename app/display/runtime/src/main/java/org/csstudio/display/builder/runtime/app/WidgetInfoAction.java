/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.representation.javafx.WidgetInfoDialog;
import org.csstudio.display.builder.representation.javafx.widgets.JFXBaseRepresentation;
import org.csstudio.display.builder.runtime.Messages;
import org.csstudio.display.builder.runtime.WidgetRuntime;
import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.ImageCache;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** Action that displays information about a widget
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WidgetInfoAction extends WeakRefWidgetAction
{
    private static final Image icon = ImageCache.getImage(WidgetInfoAction.class, "/icons/information.png");

    public WidgetInfoAction(final Widget the_widget)
    {
        //super("'" + the_widget.getName() + "' Information", new ImageView(icon), the_widget);
        super(MessageFormat.format(Messages.WidgetInformationHdr, the_widget.getName()), new ImageView(icon), the_widget);

        setOnAction(event ->
        {
            final Widget widget = getWidget();
            final WidgetRuntime<?> runtime = WidgetRuntime.ofWidget(widget);
            final List<WidgetInfoDialog.NameStateValue> pvs = new ArrayList<>();
            for (RuntimePV pv : runtime.getPVs())
                pvs.add(new WidgetInfoDialog.NameStateValue(pv.getName(), pv.isReadonly() ? Messages.WidgetInformationRo : Messages.WidgetInformationWr, pv.read()));
            final WidgetInfoDialog dialog = new WidgetInfoDialog(widget, pvs);

            final Node node = JFXBaseRepresentation.getJFXNode(widget);
            final Bounds pos = node.localToScreen(node.getBoundsInLocal());
            DialogHelper.positionDialog(dialog, node, (int)-pos.getWidth()/2, (int)-pos.getHeight()/2);
            dialog.show();
        });
    }
}

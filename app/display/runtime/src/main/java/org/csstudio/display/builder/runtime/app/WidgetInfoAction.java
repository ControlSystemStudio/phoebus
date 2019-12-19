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
import java.util.Optional;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.widgets.EmbeddedDisplayWidget;
import org.csstudio.display.builder.model.widgets.NavigationTabsWidget;
import org.csstudio.display.builder.model.widgets.TabsWidget;
import org.csstudio.display.builder.model.widgets.TabsWidget.TabItemProperty;
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
            final List<WidgetInfoDialog.NameStateValue> pvs = new ArrayList<>();
            getChildrenPvs(widget, pvs, widget.getName());
            final WidgetInfoDialog dialog = new WidgetInfoDialog(widget, pvs);

            final Node node = JFXBaseRepresentation.getJFXNode(widget);
            final Bounds pos = node.localToScreen(node.getBoundsInLocal());
            DialogHelper.positionDialog(dialog, node, (int)-pos.getWidth()/2, (int)-pos.getHeight()/2);
            dialog.show();
        });
    }

    private void getChildrenPvs(Widget widget, List<WidgetInfoDialog.NameStateValue> pvs, String path)
    {
        final WidgetRuntime<?> runtime = WidgetRuntime.ofWidget(widget);
        for (RuntimePV pv : runtime.getPVs())
            pvs.add(new WidgetInfoDialog.NameStateValue(pv.getName(), pv.isReadonly() ? Messages.WidgetInformationRo : Messages.WidgetInformationWr, pv.read(), path));

        if (widget instanceof EmbeddedDisplayWidget || widget instanceof NavigationTabsWidget)
        {
            final Optional<WidgetProperty<DisplayModel>> optPropModel = widget.checkProperty("embedded_model");
            if (optPropModel.isPresent())
            {
                final DisplayModel emb_model = optPropModel.get().getValue();
                if (emb_model != null)
                    exploreChildren(emb_model, pvs, path);
            }
        }
        else if (widget instanceof TabsWidget)
        {
            final List<TabItemProperty> tabs = ((TabsWidget)widget).propTabs().getValue();
            for (TabItemProperty tab : tabs)
                for (Widget child : tab.children().getValue())
                    getChildrenPvs(child, pvs, path + ":" + tab.name().getValue() + "." + child.getName());
        }
        else
            exploreChildren(widget, pvs, path);
    }

    private void exploreChildren(Widget widget, List<WidgetInfoDialog.NameStateValue> pvs, String path) {
        Optional<WidgetProperty<List<Widget>>> children = widget.checkProperty(ChildrenProperty.DESCRIPTOR);
        if (children.isPresent())
            for (Widget child : children.get().getValue())
                getChildrenPvs(child, pvs, path + "." + child.getName());
    }

}

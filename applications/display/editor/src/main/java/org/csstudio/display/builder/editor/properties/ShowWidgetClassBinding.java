/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.properties;

import org.csstudio.display.builder.editor.DisplayEditor;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.phoebus.ui.javafx.ImageCache;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** Indicate if property's value is based on class or not
 *
 *  <p>Binds to the widget's "class" property.
 *  As the widget class is changed, it updates the enablement
 *  and is-using-class-setting indicator of the property.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ShowWidgetClassBinding extends WidgetPropertyBinding<Node, WidgetProperty<?>>
{
    private static final Image class_icon = getClassIcon();

    private final UntypedWidgetPropertyListener model_listener = (p, o, n) -> updateFromModel();

    private final Label indicator;

    public ShowWidgetClassBinding(final Node field, final WidgetProperty<?> property, final Label indicator)
    {
        super(null, field, property, null);
        this.indicator = indicator;
    }

    private static Image getClassIcon()
    {
        return ImageCache.getImage(DisplayEditor.class, "/icons/class_property.png");
    }

    @Override
    public void bind()
    {
        updateFromModel();
        widget_property.getWidget().propClass().addUntypedPropertyListener(model_listener);
    }

    @Override
    public void unbind()
    {
        widget_property.getWidget().propClass().removePropertyListener(model_listener);
    }

    private void updateFromModel()
    {
        Platform.runLater(() ->
        {
            jfx_node.setDisable(widget_property.isUsingWidgetClass());
            if (widget_property.isUsingWidgetClass())
                indicator.setGraphic(new ImageView(class_icon));
            else
                indicator.setGraphic(null);
        });
    }
}

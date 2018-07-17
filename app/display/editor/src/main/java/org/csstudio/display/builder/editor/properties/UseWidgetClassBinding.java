/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.properties;

import java.util.List;

import org.csstudio.display.builder.editor.undo.UseClassAction;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;

/** Bind checkbox and field for property's value to 'use_class' attribute
 *
 *  <p>Updates the proeprty's 'use_class' attribute,
 *  and disables the property's field editor.
 *
 *  @author Kay Kasemir
 */
public class UseWidgetClassBinding extends WidgetPropertyBinding<CheckBox, WidgetProperty<?>>
{
    private final Node property_field;

    private final UntypedWidgetPropertyListener model_listener = (p, o, n) ->
    {
        if (! updating)
            updateFromModel();
    };

    public UseWidgetClassBinding(final UndoableActionManager undo, final CheckBox node,
                                 final Node property_field,
                                 final WidgetProperty<?> widget_property, final List<Widget> other)
    {
        super(undo, node, widget_property, other);
        this.property_field = property_field;
    }

    @Override
    public void bind()
    {
        updateFromModel();
        jfx_node.setOnAction(event ->
        {
            updating = true;
            property_field.setDisable(! jfx_node.isSelected());
            undo.execute(new UseClassAction(widget_property, jfx_node.isSelected()));
            for (Widget w : other)
            {
                final WidgetProperty<?> other_prop = w.getProperty(widget_property.getName());
                undo.execute(new UseClassAction(other_prop, jfx_node.isSelected()));
            }
            updating = false;
        });
        widget_property.addUntypedPropertyListener(model_listener);
    }

    @Override
    public void unbind()
    {
        widget_property.removePropertyListener(model_listener);
        jfx_node.setOnAction(null);
    }

    private void updateFromModel()
    {
        jfx_node.setSelected(widget_property.isUsingWidgetClass());
        property_field.setDisable(! widget_property.isUsingWidgetClass());
    }
}

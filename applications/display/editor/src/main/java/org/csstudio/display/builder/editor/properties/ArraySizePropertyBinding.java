/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.properties;

import java.util.List;

import org.csstudio.display.builder.editor.undo.AddArrayElementAction;
import org.csstudio.display.builder.editor.undo.RemoveArrayElementAction;
import org.csstudio.display.builder.model.ArrayWidgetProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.phoebus.ui.javafx.FocusUtil;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.beans.value.ChangeListener;
import javafx.scene.control.Spinner;

/** Bidirectional binding between an ArrayWidgetProperty and Java FX Spinner for number of array elements
 *
 *  <p>In comparison to most {@link WidgetPropertyBinding}s this binding
 *  will not only control a property (the number of array elements)
 *  but also update (possibly a sub-panel of) the property panel to show the current
 *  list of array elements.
 *
 *  @author Kay Kasemir
 */
public class ArraySizePropertyBinding extends WidgetPropertyBinding<Spinner<Integer>, ArrayWidgetProperty<WidgetProperty<?>>>
{
    private PropertyPanelSection panel_section;

    /** Add/remove elements from array property in response to property UI */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private final ChangeListener<? super Integer> ui_listener = (prop, old, value) ->
    {
        // The spinner has the focus right now,
        // user just clicked it to add/remove array elements.
        // This will trigger a complete refresh of the property panel.
        // Since this removes the focused node,
        // the new property view will scroll back to the topmost element,
        // and user needs to scroll back down to find the array that was just resized.
        // By removing the focus from the spinner, the property panel seems to "stay put".
        FocusUtil.removeFocus(jfx_node);

        final int desired = jfx_node.getValue();

        // Grow/shrink array via undo-able actions
        final String path = widget_property.getPath();
        while (widget_property.size() < desired)
        {
            undo.add(new AddArrayElementAction<>(widget_property, widget_property.addElement()));
            for (Widget w : other)
            {
                final ArrayWidgetProperty other_prop = (ArrayWidgetProperty) w.getProperty(path);
                undo.add(new AddArrayElementAction<>(other_prop, other_prop.addElement()));
            }
        }
        while (widget_property.size() > desired)
        {
            undo.execute(new RemoveArrayElementAction<>(widget_property));
            for (Widget w : other)
            {
                final ArrayWidgetProperty other_prop = (ArrayWidgetProperty) w.getProperty(path);
                undo.add(new RemoveArrayElementAction<>(other_prop));
            }
        }
    };

    /** Update property sub-panel as array elements are added/removed */
    private WidgetPropertyListener<List<WidgetProperty<?>>> prop_listener = (prop, removed, added) ->
    {
        // Re-populate the complete property panel.
        // Combined with the un-focus call above when changing the array size, this "works":
        // User can change the array, and property panel doesn't scroll much.
        // But it might be a hack, eventually requiring careful update of just the array
        // element section of the property panel
        panel_section.clear();
        panel_section.fill(undo, widget_property.getWidget().getProperties(), other);
    };

    /** @param panel_section Panel section for array elements
     *  @param undo Undo support
     *  @param node JFX node for array element count
     *  @param widget_property {@link ArrayWidgetProperty}
     *  @param other Widgets that also have this array property
     */
    public ArraySizePropertyBinding(final PropertyPanelSection panel_section,
            final UndoableActionManager undo,
            final Spinner<Integer> node,
            final ArrayWidgetProperty<WidgetProperty<?>> widget_property,
            final List<Widget> other)
    {
        super(undo, node, widget_property, other);
        this.panel_section = panel_section;
    }

    @Override
    public void bind()
    {
        jfx_node.valueProperty().addListener(ui_listener);
        jfx_node.getValueFactory().setValue(widget_property.size());

        widget_property.addPropertyListener(prop_listener);
    }

    @Override
    public void unbind()
    {
        widget_property.removePropertyListener(prop_listener);
        jfx_node.valueProperty().removeListener(ui_listener);
    }
}

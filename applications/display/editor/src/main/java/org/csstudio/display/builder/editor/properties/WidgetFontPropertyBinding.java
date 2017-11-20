/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.properties;

import java.util.List;
import java.util.Optional;

import org.csstudio.display.builder.editor.undo.SetWidgetPropertyAction;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.properties.FontWidgetProperty;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.csstudio.display.builder.representation.javafx.ModalityHack;
import org.csstudio.display.builder.representation.javafx.WidgetFontDialog;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;


/** Bidirectional binding between a color property in model and Java FX Node in the property panel
 *  @author Kay Kasemir
 */
public class WidgetFontPropertyBinding
       extends WidgetPropertyBinding<Button, FontWidgetProperty>
{
    /** Update property panel field as model changes */
    private final WidgetPropertyListener<WidgetFont> model_listener = (p, o, n) ->
    {
        jfx_node.setText(widget_property.getValue().toString());
    };

    /** Update model from user input */
    private EventHandler<ActionEvent> action_handler = event ->
    {
        final WidgetFontDialog dialog = new WidgetFontDialog(widget_property.getValue());
        DialogHelper.positionDialog(dialog, DialogHelper.getContainer(jfx_node), -200, -200);
        ModalityHack.forDialog(dialog);
        final Optional<WidgetFont> result = dialog.showAndWait();
        if (result.isPresent())
        {
            undo.execute(new SetWidgetPropertyAction<WidgetFont>(widget_property, result.get()));
            final String path = widget_property.getPath();
            for (Widget w : other)
            {
                final FontWidgetProperty other_prop = (FontWidgetProperty) w.getProperty(path);
                undo.execute(new SetWidgetPropertyAction<WidgetFont>(other_prop, result.get()));
            }
        }
    };

    public WidgetFontPropertyBinding(final UndoableActionManager undo,
                                     final Button field,
                                     final FontWidgetProperty widget_property,
                                     final List<Widget> other)
    {
        super(undo, field, widget_property, other);
    }

    @Override
    public void bind()
    {
        widget_property.addPropertyListener(model_listener);
        jfx_node.setOnAction(action_handler);
        jfx_node.setText(widget_property.getValue().toString());
    }

    @Override
    public void unbind()
    {
        jfx_node.setOnAction(null);
        widget_property.removePropertyListener(model_listener);
    }
}

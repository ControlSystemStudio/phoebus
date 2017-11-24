/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.properties;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.editor.undo.SetWidgetPropertyAction;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.properties.ScriptInfo;
import org.csstudio.display.builder.model.properties.ScriptsWidgetProperty;
import org.csstudio.display.builder.representation.javafx.AutocompleteMenu;
import org.csstudio.display.builder.representation.javafx.ModalityHack;
import org.csstudio.display.builder.representation.javafx.ScriptsDialog;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;

/** Bidirectional binding between a scripts property in model and Java FX Node in the property panel
 *  @author Kay Kasemir
 */
public class ScriptsPropertyBinding
       extends WidgetPropertyBinding<Button, ScriptsWidgetProperty>
{
    private AutocompleteMenu menu = null;

    /** Update property panel field as model changes */
    private final WidgetPropertyListener<List<ScriptInfo>> model_listener = (p, o, n) ->
    {
        jfx_node.setText(MessageFormat.format(Messages.ScriptCountFMT, widget_property.getValue().size()));
    };


    /** Update model from user input */
    private EventHandler<ActionEvent> action_handler = event ->
    {
        final ScriptsDialog dialog = new ScriptsDialog(widget_property.getWidget(), widget_property.getValue(), menu);
        DialogHelper.positionDialog(dialog, DialogHelper.getContainer(jfx_node), -200, -200);
        ModalityHack.forDialog(dialog);
        final Optional<List<ScriptInfo>> result = dialog.showAndWait();
        if (result.isPresent())
        {
            undo.execute(new SetWidgetPropertyAction<List<ScriptInfo>>(widget_property, result.get()));
            final String path = widget_property.getPath();
            for (Widget w : other)
            {
                final ScriptsWidgetProperty other_prop = (ScriptsWidgetProperty) w.getProperty(path);
                undo.execute(new SetWidgetPropertyAction<List<ScriptInfo>>(other_prop, result.get()));
            }
        }
    };

    public ScriptsPropertyBinding(final UndoableActionManager undo,
                                  final Button field,
                                  final ScriptsWidgetProperty widget_property,
                                  final List<Widget> other,
                                  final AutocompleteMenu menu)
    {
        super(undo, field, widget_property, other);
        this.menu = menu;
    }

    @Override
    public void bind()
    {
        widget_property.addPropertyListener(model_listener);
        jfx_node.setOnAction(action_handler);
        model_listener.propertyChanged(widget_property, null, null);
    }

    @Override
    public void unbind()
    {
        jfx_node.setOnAction(null);
        widget_property.removePropertyListener(model_listener);
    }
}

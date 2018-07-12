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
import org.csstudio.display.builder.model.properties.RulesWidgetProperty;
import org.csstudio.display.builder.model.rules.RuleInfo;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;

/** Bidirectional binding between a rules property in model and Java FX Node in the property panel
 *  @author Megan Grodowitz
 */
public class RulesPropertyBinding
extends WidgetPropertyBinding<Button, RulesWidgetProperty>
{
    /** Update property panel field as model changes */
    private final WidgetPropertyListener<List<RuleInfo>> model_listener = (p, o, n) ->
    {
        jfx_node.setText(MessageFormat.format(Messages.RuleCountFMT, widget_property.getValue().size()));
    };

    /** Update model from user input */
    private EventHandler<ActionEvent> action_handler = event ->
    {
        final RulesDialog dialog = new RulesDialog(undo, widget_property.getValue(), widget_property.getWidget(), jfx_node);
        //ScenicView.show(dialog.getDialogPane());
        final Optional<List<RuleInfo>> result = dialog.showAndWait();

        if (result.isPresent())
        {
            undo.execute(new SetWidgetPropertyAction<>(widget_property, result.get()));
            if (! other.isEmpty())
            {
                final String path = widget_property.getPath();
                for (Widget w : other)
                {
                    final RulesWidgetProperty other_prop = (RulesWidgetProperty) w.getProperty(path);
                    undo.execute(new SetWidgetPropertyAction<>(other_prop, result.get()));
                }
            }
        }
    };

    public RulesPropertyBinding(final UndoableActionManager undo,
            final Button field,
            final RulesWidgetProperty widget_property,
            final List<Widget> other)
    {
        super(undo, field, widget_property, other);
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

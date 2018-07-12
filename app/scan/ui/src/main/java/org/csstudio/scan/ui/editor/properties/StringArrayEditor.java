/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor.properties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.VBox;

/** Editor for array of strings
 *
 *  <p>Presents as bunch of text fields,
 *  with one extra line for adding a new value.
 *  Empty values are removed.
 *
 *  <p>Compared to list view, text fields can quickly
 *  be edited, no double-click to create text field for
 *  table cell. "Enter" moves to next row.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class StringArrayEditor extends VBox
{
    private final List<String> values = new ArrayList<>();
    private Consumer<List<String>> handler = list -> {};

    public StringArrayEditor()
    {
        updateFields();
    }

    public void setValues(final Collection<String> values)
    {
        this.values.clear();
        this.values.addAll(values);
        this.values.removeIf(String::isEmpty);
        updateFields();
    }

    public void setValueHandler(final Consumer<List<String>> handler)
    {
        this.handler = handler;
    }

    private void updateFields()
    {
        final List<Node> fields = getChildren();

        // Create or update field for each value
        int i = 0;
        for (String value : values)
        {
            if (fields.size() > i)
            {
                final TextField field = (TextField) fields.get(i);
                field.setText(value);
                field.setPromptText("");
            }
            else
            {
                final EnterTextField field = new EnterTextField(value);
                field.setOnEnter(text -> handleEnter(field));
                configureTextField(field);
                fields.add(field);
            }
            ++i;
        }

        // Remove extra fields
        for (i = fields.size()-1;  i > values.size();  --i)
            fields.remove(i);

        // .. except for a last one that allows adding a new value
        final EnterTextField field;
        if (fields.size() <= values.size())
        {
            field = new EnterTextField();
            field.setOnEnter(text -> handleEnter(field));
            configureTextField(field);
            fields.add(field);
        }
        else
            field = (EnterTextField) fields.get(values.size());
        field.setText("");
        field.setPromptText("<new value>");
    }

    /** Derived class can attach autocompletion menu
     *  to the text field
     *  @param text_field
     */
    protected void configureTextField(final TextInputControl text_field)
    {
    }

    private void handleEnter(final TextField field)
    {
        final String text = field.getText();
        final List<Node> fields = getChildren();
        final int i = fields.indexOf(field);

        if (i >= values.size())
            values.add(text);
        values.set(i, text);

        values.removeIf(String::isEmpty);

        updateFields();

        final Node next = fields.get(Math.min(i+1, fields.size()-1));
        Platform.runLater(() -> next.requestFocus());

        handler.accept(values);
    }
}

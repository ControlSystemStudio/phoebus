/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor.properties;

import java.util.Objects;
import java.util.function.Consumer;

import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.command.ScanCommandProperty;
import org.csstudio.scan.command.UnknownScanCommandPropertyException;

import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

/** Text field that requires 'enter'
 *
 *  <p>Resets to original value when focus is lost
 *  or escape is pressed.
 *
 *  @author Kay Kasemir
 */
class PropertyTextField extends TextField
{
    protected final ScanCommand command;
    protected final ScanCommandProperty property;

    PropertyTextField(final ScanCommand command,
                      final ScanCommandProperty property,
                      final Consumer<String> update)
    {
        this.command = command;
        this.property = property;

        setOnAction(event ->
        {
            update.accept(getText());
            // Assert that current value of property is shown,
            // which may not be what we tried to set
            reset();
        });

        setOnKeyPressed(event ->
        {
            if (event.getCode() == KeyCode.ESCAPE)
                reset();
        });

        focusedProperty().addListener((f, old, focus) ->
        {
            if (! focus)
                reset();
        });

        reset();
    }

    /** Convert property's value to text
     *  Derived class can override
     *  @param value
     *  @return
     */
    protected String value2text(final Object value)
    {
        return Objects.toString(value);
    }

    /** Reset text field to current value of property */
    private void reset()
    {
        try
        {
            setText(value2text(command.getProperty(property)));
        }
        catch (UnknownScanCommandPropertyException ex)
        {
            // Not expected, property should be known
        }
    }
}

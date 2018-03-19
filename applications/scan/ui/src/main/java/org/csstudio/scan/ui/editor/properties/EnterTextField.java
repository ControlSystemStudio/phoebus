/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor.properties;

import java.util.function.Consumer;

import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

/** Text field that requires 'enter'
*
*  <p>Resets to original value when focus is lost
*  or escape is pressed.
*
*  @author Kay Kasemir
*/
@SuppressWarnings("nls")
public class EnterTextField extends TextField
{
    private String original_value = "";
    private Consumer<String> on_enter = text -> {};

    public EnterTextField()
    {
        this("");
    }

    public EnterTextField(final String initial)
    {
        super(initial);
        original_value = initial;

        setOnKeyPressed(event ->
        {
            if (event.getCode() == KeyCode.ESCAPE)
                reset();
        });

        setOnAction(event ->
        {
            original_value = getText();
            on_enter.accept(original_value);
        });

        focusedProperty().addListener((f, old, focus) ->
        {
            if (focus)
                original_value = getText();
            else
                reset();
        });
    }

    /** Similar to <code>setOnAction</code>,
     *  which is already used internally.
     *
     *  @param on_enter Called when value was entered
     */
    public void setOnEnter(final Consumer<String> on_enter)
    {
        this.on_enter = on_enter;
    }

    private void reset()
    {
        setText(original_value);
    }
}

/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor.properties;

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
            else if (event.getCode() == KeyCode.ENTER)
                original_value = getText();
        });

        focusedProperty().addListener((f, old, focus) ->
        {
            if (focus)
                original_value = getText();
            else
                reset();
        });
    }

    private void reset()
    {
        setText(original_value);
    }
}

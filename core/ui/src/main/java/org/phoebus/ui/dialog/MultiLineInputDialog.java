/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.dialog;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

/** Dialog for entering multi-line text
 *  @author Kay Kasemir
 */
public class MultiLineInputDialog extends Dialog<String>
{
    private final TextArea text;

    /** @param initial_text Initial text */
    public MultiLineInputDialog(final String initial_text)
    {
        text = new TextArea(initial_text);

        getDialogPane().setContent(new BorderPane(text));
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        setResizable(true);

        setResultConverter(button ->
        {
            return button == ButtonType.OK ? text.getText() : null;
        });
    }

    /** @param parent Parent node, dialog will be positioned relative to it
     *  @param initial_text Initial text
     */
    public MultiLineInputDialog(final Node parent, final String initial_text)
    {
        this(initial_text);
        initOwner(parent.getScene().getWindow());
        final Bounds bounds = parent.localToScreen(parent.getBoundsInLocal());
        setX(bounds.getMinX());
        setY(bounds.getMinY());
    }

    /** @param pixels Suggested height of text in pixels */
    public void setTextHeight(final double pixels)
    {
        text.setPrefHeight(pixels);
    }

    // TODO Catch/consume 'escape'
    // If ESC key is pressed while editing the text,
    // the dialog closes, not returning a value.
    // Fine.
    // But the ESC passes on to whoever called the dialog..
}

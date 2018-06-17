/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.dialog;

import org.phoebus.framework.preferences.PhoebusPreferenceService;

import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

/** Dialog for entering multi-line text
 *
 *  <p>Can also be used to just display text,
 *  allowing to 'copy' the text, but no changes.
 *
 *  @author Kay Kasemir
 */
public class MultiLineInputDialog extends Dialog<String>
{
    private final TextArea text;

    /** @param initial_text Initial text */
    public MultiLineInputDialog(final String initial_text)
    {
        this(null, initial_text, true);
    }

    /** @param parent Parent node, dialog will be positioned relative to it
     *  @param initial_text Initial text
     */
    public MultiLineInputDialog(final Node parent, final String initial_text)
    {
        this(parent, initial_text, true);
    }

    /** @param parent Parent node, dialog will be positioned relative to it
     *  @param initial_text Initial text
     *  @param editable Allow editing?
     *  */
    public MultiLineInputDialog(final Node parent, final String initial_text, final boolean editable)
    {
        text = new TextArea(initial_text);
        text.setEditable(editable);

        getDialogPane().setContent(new BorderPane(text));
        if (editable)
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        else
            getDialogPane().getButtonTypes().addAll(ButtonType.OK);
        setResizable(true);

        setResultConverter(button ->
        {
            return button == ButtonType.OK ? text.getText() : null;
        });

        DialogHelper.positionAndSize(this, parent,
                                     PhoebusPreferenceService.userNodeForClass(MultiLineInputDialog.class),
                                     600, 300);
    }

    /** @param pixels Suggested height of text in pixels */
    public void setTextHeight(final double pixels)
    {
        text.setPrefHeight(pixels);
    }

    /** @param pixels Suggested width of text in pixels */
    public void setTextWidth(final double pixels)
    {
        text.setPrefWidth(pixels);
    }

    // TODO Catch/consume 'escape'
    // If ESC key is pressed while editing the text,
    // the dialog closes, not returning a value.
    // Fine.
    // But the ESC passes on to whoever called the dialog..
}

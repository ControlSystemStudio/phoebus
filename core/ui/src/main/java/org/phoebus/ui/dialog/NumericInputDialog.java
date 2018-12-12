/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.dialog;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.phoebus.ui.Messages;

import javafx.event.ActionEvent;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;

/** Dialog that allows entering a number
 *  @author Kay Kasemir
 */
public class NumericInputDialog extends TextInputDialog
{
    /** Construct dialog
     *  @param title Title
     *  @param message Message
     *  @param initial_value Initial value to present
     *  @param check_value Function that will be called with value. Return <code>null</code> if OK, otherwise return error message
     */
    public NumericInputDialog(final String title, final String message, final double initial_value, final Function<Double, String> check_value)
    {
        super(Double.toString(initial_value));
        setTitle(title);
        setHeaderText(message);

        getDialogPane().lookupButton(ButtonType.OK)
                       .addEventFilter(ActionEvent.ACTION, event ->
        {
            final String text = getEditor().getText().trim();
            double value = Double.NaN;
            try
            {
                value = Double.parseDouble(text);
            }
            catch (NumberFormatException ex)
            {
                setHeaderText(Messages.NumberInputHdr);
                event.consume();
                return;
            }
            final String error = check_value.apply(value);
            if (error != null)
            {
                setHeaderText(error);
                event.consume();
            }
        });
    }

    /** Execute dialog
     *  @return Valid number entered by user or {@link Double#NaN} if dialog was cancelled
     */
    public double prompt()
    {
        final Optional<String> result = showAndWait();
        if (result.isPresent())
            return Double.parseDouble(result.get());
        return Double.NaN;
    }

    /** Execute dialog
     *  @param on_success Will be called with valid number if user presses 'OK'
     */
    public void promptAndHandle(final Consumer<Double> on_success)
    {
        final double value = prompt();
        if (! Double.isNaN(value))
            on_success.accept(value);
    }
}

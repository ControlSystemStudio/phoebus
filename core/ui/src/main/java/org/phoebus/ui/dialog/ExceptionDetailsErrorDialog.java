/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.dialog;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextArea;

/** Dialog that shows error message with exception detail
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ExceptionDetailsErrorDialog
{
    /** Open dialog that shows detail of error
     *
     *  <p>May be called from non-UI thread
     *
     *  @param title Title
     *  @param message Message, may have multiple lines
     *  @param exception Exception
     */
    public static void openError(final String title, final String message, final Exception exception)
    {
        Platform.runLater(() -> doOpenError(title, message, exception));
    }

    private static void doOpenError(final String title, final String message, final Exception exception)
    {
        final Alert dialog = new Alert(AlertType.ERROR);
        dialog.setHeaderText(message);

        if (exception != null)
            dialog.setContentText("Exception: " + exception.getMessage());

        // Show exception stack trace in expandable section of dialog
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        exception.printStackTrace(new PrintStream(buf));

        // User can copy trace out of read-only text area
        final TextArea trace = new TextArea(buf.toString());
        trace.setEditable(false);
        dialog.getDialogPane().setExpandableContent(trace);

        dialog.setResizable(true);
        dialog.getDialogPane().setPrefWidth(800);

        dialog.showAndWait();
    }
}

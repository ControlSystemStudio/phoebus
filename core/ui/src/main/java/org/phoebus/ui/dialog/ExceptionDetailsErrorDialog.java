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
import javafx.scene.control.ButtonType;

/** Dialog that shows error message with exception detail
 *  @author Kay Kasemir
 */
public class ExceptionDetailsErrorDialog
{
    /** Open dialog that shows detail of errot
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
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        exception.printStackTrace(new PrintStream(buf));
        final Alert dialog = new Alert(AlertType.ERROR, buf.toString(), ButtonType.OK);
        dialog.setHeaderText(message);
        dialog.setResizable(true);
        dialog.getDialogPane().setPrefWidth(900);

        // TODO Custom dialog pane that allows showing/hiding the exception detail.
        //      Show in read-only TextField that allows copying the error out.

        dialog.showAndWait();
    }
}

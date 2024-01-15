/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.dialog;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Dialog that shows error message with exception detail
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ExceptionDetailsErrorDialog
{
    private static final String LINE_SEPARATOR = System.lineSeparator();

    /** Open dialog that shows detail of error
     *
     *  <p>May be called from non-UI thread
     *
     *  @param node Node relative to which the dialog will be positioned
     *  @param title Title
     *  @param message Message, may have multiple lines
     *  @param exception Exception
     */
    public static void openError(final Node node, final String title, final String message, final Exception exception)
    {
        Platform.runLater(() -> doOpenError(node, title, message, exception, true));
    }

    /** Open dialog that shows detail of error
     *
     *  <p>May be called from non-UI thread
     *
     *  @param node Node relative to which the dialog will be positioned
     *  @param title Title
     *  @param message Message, may have multiple lines
     *  @param exception Exception
     *  @param appendStacktraceMsgs Append a summary for all the exceptions in the stacktrace
     */
    public static void openError(final Node node, final String title, final String message, final Exception exception, final boolean appendStacktraceMsgs)
    {
        Platform.runLater(() -> doOpenError(node, title, message, exception, appendStacktraceMsgs));
    }

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
        openError(null, title, message, exception, true);
    }

    /** Open dialog that shows detail of error, the message/header text is constructed using the messages from the
     * exception's stacktrace
     *
     *  <p>May be called from non-UI thread
     *
     *  @param title Title
     *  @param exception Exception
     */
    public static void openError(final String title, final Exception exception)
    {
        openError(null, title, "", exception, true);
    }

    private static void doOpenError(final Node node, final String title, final String message, final Exception exception, final boolean append_stacktrace_msgs)
    {
        StringBuilder messageBuilder = new StringBuilder(message).append(LINE_SEPARATOR);
        if(append_stacktrace_msgs)
        {
            messageBuilder.append(exception.getMessage() != null ? exception.getMessage() : exception.getClass()).append(LINE_SEPARATOR).append("Cause:").append(LINE_SEPARATOR);
            Throwable cause = exception.getCause();
            int exceptionIndex = 1;
            // maintain a list of 'causes' to handle CIRCULAR REFERENCE s
            final List<Throwable> throwableCausesList = new ArrayList<>();
            while (cause != null && !throwableCausesList.contains(cause)) {
                throwableCausesList.add(cause);
                messageBuilder.append("[" + exceptionIndex + "] ").append(cause.getMessage() != null ? cause.getMessage() : cause.getClass().getName()).append(LINE_SEPARATOR);
                exceptionIndex++;
                cause = cause.getCause();
            }
        }

        final Alert dialog = new Alert(AlertType.ERROR);
        dialog.setTitle(title);
        dialog.setHeaderText(messageBuilder.toString());

        // A custom button which copies the message to clipboard
        final ButtonType copyMessage = new ButtonType("Copy Msg & Close", ButtonBar.ButtonData.RIGHT);
        dialog.getButtonTypes().add(copyMessage);

        if (exception != null)
        {
            // Show the simple exception name,
            // e.g. "Exception" or "FileAlreadyExistsException" without package name,
            // followed by message
            dialog.setContentText(exception.getClass().getSimpleName() + ": " + exception.getMessage());

            // Show exception stack trace in expandable section of dialog
            final ByteArrayOutputStream buf = new ByteArrayOutputStream();
            exception.printStackTrace(new PrintStream(buf));

            // User can copy trace out of read-only text area
            final TextArea trace = new TextArea(buf.toString());
            trace.setEditable(false);
            dialog.getDialogPane().setExpandableContent(trace);
        }

        dialog.setResizable(true);
        dialog.getDialogPane().setPrefWidth(800);

        if (node != null)
            DialogHelper.positionDialog(dialog, node, -400, -200);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == copyMessage) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(message);
            clipboard.setContent(content);
        }
    }


}

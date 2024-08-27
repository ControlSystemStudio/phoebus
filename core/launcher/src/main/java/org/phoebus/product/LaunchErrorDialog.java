/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.product;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Minimal Swing dialog showing an error message as extracted from
 * an {@link Exception}.
 */
public class LaunchErrorDialog extends JFrame {

    /**
     * Access method.
     * @param exception The {@link Exception} from which to
     *                  extract text shown in the dialog.
     */
    public static void show(Exception exception){
        new LaunchErrorDialog(exception).setVisible(true);
    }

    private LaunchErrorDialog(Exception exception){
        setSize(600, 300);
        setTitle(Messages.launchErrorTitle);
        String message = exception.getMessage();
        // If there is no exception message, use stack track instead
        if(message == null) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            exception.printStackTrace(printWriter);
            message = stringWriter.toString();
        }
        JTextArea jTextArea = new JTextArea(message);
        jTextArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        jTextArea.setLineWrap(true);
        add(jTextArea, BorderLayout.CENTER);
        // Exit when the JFrame is closed
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Center on screen
        setLocationRelativeTo(null);
        setVisible(true);
    }
}

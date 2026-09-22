/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.csstudio.display.builder.model.properties.CryptedPassword;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/** Dialog that prompts for password
 *
 *  <p>When a 'correct' password is provided,
 *  the dialog keeps prompting until entered
 *  password is correct, or user cancels the dialog.
 *
 *  <p>When called without 'correct' password,
 *  dialog simply prompts once and returns
 *  what the user entered.
 *
 *  <p>Returns the entered (and optionally verified)
 *  password, or <code>null</code>.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PasswordDialog extends Dialog<String>
{
    private final String correct_password;
    private final CryptedPassword crypted_password;
    private final PasswordField pass_entry = new PasswordField();
    private final Label pass_caption = new Label(Messages.Password_Caption);

    public static String hash(String value, String algorithm) throws Exception {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] hash = md.digest(value.getBytes(StandardCharsets.UTF_8));

        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }

        return hex.toString();
    }

    /** @param title Title, message
     *  @param correct_password Password to check
     */
    public PasswordDialog(final String title, final String correct_password, final CryptedPassword crypted_password)
    {
        this.correct_password = correct_password;
        this.crypted_password = crypted_password;
        final DialogPane pane = getDialogPane();

        pass_entry.setPromptText(Messages.Password_Prompt);
        pass_entry.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(pass_entry, Priority.ALWAYS);

        pane.setContent(new HBox(6, pass_caption, pass_entry));

        pane.setMinSize(300, 150);
        setResizable(true);

        setTitle(Messages.Password);
        setHeaderText(title);
        pane.getStyleClass().add("text-input-dialog");
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Check password in dialog?
        if (correct_password != null  &&  correct_password.length() > 0)
        {
            final Button okButton = (Button) pane.lookupButton(ButtonType.OK);
            okButton.addEventFilter(ActionEvent.ACTION, event ->
            {
                try {
                    if (! checkPassword())
                        event.consume();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        setResultConverter((button) ->
        {
            return button.getButtonData() == ButtonData.OK_DONE ? pass_entry.getText() : null;
        });

        Platform.runLater(() -> pass_entry.requestFocus());
    }

    private boolean checkPassword() throws Exception {
        final String password = pass_entry.getText();
        boolean check;
        if ("None".equals(crypted_password.toString())) {
            check = correct_password.equals(password);
        } else {
            check = correct_password.equals(hash(password, crypted_password.toString()));
        }
        if (check)
            return true;

        setHeaderText(Messages.Password_Error);
        Platform.runLater(() ->
        {
            pass_entry.requestFocus();
            pass_entry.selectAll();
        });
        return false;
    }
}
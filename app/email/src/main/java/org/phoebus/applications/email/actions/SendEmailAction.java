/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.email.actions;

import static org.phoebus.ui.application.PhoebusApplication.logger;

import java.io.IOException;
import java.util.logging.Level;

import org.phoebus.applications.email.EmailApp;
import org.phoebus.ui.javafx.ImageCache;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

/**
 * Context menu item to send an email.
 * @author Evan Smith
 */
public class SendEmailAction extends MenuItem
{
    public SendEmailAction(final Node parent)
    {
        setText("Send Email...");
        setGraphic(ImageCache.getImageView(ImageCache.class, "/icons/mail-send-16.png"));
        setOnAction(event ->
        {
            try {
                Parent root = FXMLLoader.load(EmailApp.class.getResource("ui/SimpleCreate.fxml"));
                Scene scene = new Scene(root, 600, 800);
                Stage stage = new Stage();
                stage.setTitle("Send EMail");
                stage.setScene(scene);
                stage.setX(parent.getScene().getWindow().getX() + 100);
                stage.setY(parent.getScene().getWindow().getY() + 50);
                stage.show();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to create email dialog", e);
            }
        });
    }
}

/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.email.actions;

import static org.phoebus.applications.email.EmailApp.logger;

import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;

import org.phoebus.applications.email.EmailApp;
import org.phoebus.applications.email.ui.EmailDialogController;
import org.phoebus.email.EmailPreferences;
import org.phoebus.ui.javafx.ImageCache;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/** Context menu item to send an email.
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class SendEmailAction extends MenuItem
{
    /** @param parent Parent node used to position dialog
     *  @param title Initial title or <code>null</code>
     *  @param get_body Supplied for initial body text or <code>null</code>
     *  @param get_image Supplier for image to attach, or <code>null</code>
     */
    public SendEmailAction(final Node parent, final String title, final Supplier<String> get_body, final Supplier<Image> get_image)
    {
        setText("Send Email...");
        setGraphic(ImageCache.getImageView(ImageCache.class, "/icons/mail-send-16.png"));

        if (EmailPreferences.isEmailSupported())
            setOnAction(event ->
            {
                try
                {
                    final FXMLLoader loader = new FXMLLoader();
                    loader.setLocation(EmailApp.class.getResource("ui/EmailDialog.fxml"));
                    Parent root = loader.load();
                    final EmailDialogController controller = loader.getController();

                    if (title != null)
                        controller.setTitle(title);
                    if (get_body != null)
                        controller.setBody(get_body.get());
                    if (get_image != null)
                        controller.setImages(List.of(get_image.get()));

                    final Stage stage = new Stage();
                    stage.setTitle("Send Email");
                    final Scene scene = new Scene(root, 600, 800);
                    stage.setScene(scene);

                    if (parent != null)
                    {
                        controller.setSnapshotNode(parent.getScene().getRoot());
                        stage.setX(parent.getScene().getWindow().getX() + 100);
                        stage.setY(parent.getScene().getWindow().getY() + 50);
                    }
                    stage.show();
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Email dialog failed", ex);
                }
            });
        else
            setDisable(true);
    }

    /** @param parent Parent node used to position dialog
     *  @param title Initial title or <code>null</code>
     *  @param body Initial body text or <code>null</code>
     *  @param get_image Supplier for image to attach, or <code>null</code>
     */
    public SendEmailAction(final Node parent, final String title, final String body, final Supplier<Image> get_image)
    {
        this(parent, title, body == null ? null : () -> body, get_image);
    }
}

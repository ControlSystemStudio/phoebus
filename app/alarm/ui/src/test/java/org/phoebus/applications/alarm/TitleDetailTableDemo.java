/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.phoebus.applications.alarm.model.TitleDetail;
import org.phoebus.applications.alarm.ui.Messages;
import org.phoebus.applications.alarm.ui.config.ItemConfigDialog;
import org.phoebus.applications.alarm.ui.config.TitleDetailTableController;
import org.phoebus.applications.alarm.ui.config.TitleDetailToolbarController;
import org.phoebus.framework.nls.NLS;
import org.phoebus.ui.javafx.ApplicationWrapper;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Demo of {@link TitleDetailTableController}'s UI.
 *
 * @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TitleDetailTableDemo extends ApplicationWrapper {
    @Override
    public void start(final Stage stage) throws Exception {
        final FXMLLoader fxmlLoader = new FXMLLoader();
        try {
            fxmlLoader.setResources(NLS.getMessages(Messages.class));
            fxmlLoader.setLocation(this.getClass().getResource("/org/phoebus/applications/alarm/ui/config/TitleDetailTable.fxml"));
            fxmlLoader.setControllerFactory(clazz -> {
                try {
                    if (clazz.isAssignableFrom(TitleDetailTableController.class)) {
                        return new TitleDetailTableController();
                    } else if (clazz.isAssignableFrom(TitleDetailToolbarController.class)) {
                        return new TitleDetailToolbarController();
                    }
                } catch (Exception e) {
                    Logger.getLogger(ItemConfigDialog.class.getName()).log(Level.SEVERE, "Failed to construct controller for " + clazz.getName(), e);
                }
                return null;
            });

            final List<TitleDetail> items = List.of(
                    new TitleDetail("Example 1", "Some detail"),
                    new TitleDetail("Example 2", "Much\nmore\ndetail")
            );

            Node root = fxmlLoader.load();
            TitleDetailTableController titleDetailTableController = fxmlLoader.getController();
            titleDetailTableController.setItems(items);

            final Button dump = new Button("Dump Items");
            dump.setOnAction(event ->
            {
                System.out.println("Items:");
                titleDetailTableController.setItems(List.of());
            });

            BorderPane borderPane = new BorderPane();
            borderPane.setCenter(root);
            borderPane.setBottom(dump);

            final Scene scene = new Scene(borderPane, 600, 800);
            stage.setTitle("TitleDetailTable Demo");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(final String[] args) {
        launch(TitleDetailTableDemo.class, args);
    }
}

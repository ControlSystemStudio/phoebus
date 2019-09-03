/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.dialog;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/** {@link PopOver} demo
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PopOverDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage)
    {
        final BorderPane content = new BorderPane(new TextField("Center"), new Label("Top"), new Label("Right"), new Label("Bottom"), new Label("Left"));
        final PopOver popover = new PopOver(content);

        final Button toggle_popup = new Button("Popup");
        toggle_popup.setOnAction(event ->
        {
            if (popover.isShowing())
                popover.hide();
            else
                popover.show(toggle_popup);

        });

        final BorderPane layout = new BorderPane(toggle_popup);
        stage.setScene(new Scene(layout, 400, 300));
        stage.show();
    }

    public static void main(String[] args)
    {
        launch(PopOverDemo.class, args);
    }
}

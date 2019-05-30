/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.sandbox;

import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.representation.javafx.JFXRepresentation;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Scene;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/** Demo of MenuButton
 *
 *  <p>Menu button's drop-down menu was invisible
 *  for dark button background
 *  because drop-down text uses 'ladder(..)' to turn
 *  while for dark background - of the button,
 *  while the drop-down pane was still white
 *   -> white on white text.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MenuButtonDemo extends ApplicationWrapper
{
    public static void main(final String[] args)
    {
        launch(MenuButtonDemo.class, args);
    }

    @Override
    public void start(final Stage stage)
    {
        final MenuButton button1 = new MenuButton("Plain Button", null, new MenuItem("Item 1"), new MenuItem("Item 2"));
        button1.getStyleClass().add("action_button");

        final MenuItem item = new MenuItem("Action Button Item");
        item.getStyleClass().add("action_button_item");
        final MenuButton button2 = new MenuButton("Dark Button", null, item, new MenuItem("Plain Item"));

        button2.setStyle(JFXUtil.shadedStyle(new WidgetColor(100, 0, 0)));
        button2.setTextFill(Color.YELLOW);
        button2.getStyleClass().add("action_button");

        final HBox box = new HBox(button1, button2);

        final Scene scene = new Scene(box, 800, 700);
        // XXX Enable scenic view to debug styles
        // ScenicView.show(scene);
        JFXRepresentation.setSceneStyle(scene);
        stage.setScene(scene);
        stage.setTitle("MenuButtons");

        stage.show();
    }
}

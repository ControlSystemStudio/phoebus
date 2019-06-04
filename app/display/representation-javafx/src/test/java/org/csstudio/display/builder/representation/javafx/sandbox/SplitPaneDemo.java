/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.sandbox;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/** SplitPane demo
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SplitPaneDemo extends ApplicationWrapper
{
    final String DEBUG_STYLE = "-fx-background-color: rgb(255, 100, 0, 0.2)";

    public static void main(final String[] args)
    {
        launch(SplitPaneDemo.class, args);
    }

    @Override
    public void start(final Stage stage)
    {
        final SplitPane split = new SplitPane();

        Label label = new Label("Left");
        label.setMaxWidth(Double.MAX_VALUE);
        label.setStyle(DEBUG_STYLE);
        final StackPane left = new StackPane(label);

        label = new Label("Some long text in the right panel");
        label.setStyle(DEBUG_STYLE);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setPrefWidth(Double.MAX_VALUE);
        final ScrollPane scroll = new ScrollPane(label);
        scroll.setHbarPolicy(ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollBarPolicy.ALWAYS);
        final StackPane right = new StackPane(scroll);

        split.getItems().addAll(left, right);
        split.setDividerPositions(0.5);

        final Scene scene = new Scene(split, 800, 700);
        stage.setScene(scene);
        stage.show();
    }
}

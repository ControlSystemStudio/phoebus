/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.sandbox;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.beans.InvalidationListener;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

/** Demo of zoomed area with scrollbars
 *
 *  Goal:
 *  Bunch of widgets, inside a scrollable and zoomable view.
 *
 *  Basic idea:
 *  ScrollPane[ Pane[ widgets .. ] ]
 *
 *  ScrollPane - Obviously needed for scrolling
 *  Pane - Holds all the widgets and supports zooming
 *
 *  Problem:
 *  ScrollPane shows scroll bars based on the original size of the widgets,
 *  not based on the actual size.
 *  https://pixelduke.wordpress.com/2012/09/16/zooming-inside-a-scrollpane
 *  explains how to solve that by adding another nested Group:
 *
 *  ScrollPane[ Group[ Pane[ widgets .. ] ] ]
 *
 *  ScrollPane - Obviously needed for scrolling
 *  outer Group - Automatically gets the layout bounds of the zoomed
 *                widgets in the inner group, so ScrollPane
 *                can correctly configure the scroll bars
 *  inner Pane - Holds all the widgets and supports zooming
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings({ "nls", "unused" })
public class ZoomPan extends ApplicationWrapper
{
    private static boolean zoom_in = false;

    private static void handleViewportChanges(final ScrollPane scroll)
    {
        final double width = scroll.getWidth(), height = scroll.getHeight();
        System.out.println("Resized to " + width + " x " + height);
    }

    public static Scene createScene()
    {
        // Stuff to show as 'widgets'
        final Node[] stuff = new Node[3];
        for (int i=0; i<stuff.length; ++i)
        {
            final Rectangle rect = new Rectangle(50+i*100, 100, 10+i*50, 20+i*40);
            rect.setFill(Color.BLUE);
            stuff[i] = rect;
        }

        // With 'Group', stuff would start in top-left (0, 0) corner,
        // not at (50, 100) based on its coordinates
        final Pane widgets = new Pane(stuff);
        widgets.setStyle("-fx-background-color: coral;");

        final ScrollPane scroll;

        if (true)
        {   // Additional Group to help ScrollPane get correct bounds
            final Group scroll_content = new Group(widgets);
            scroll = new ScrollPane(scroll_content);
        }
        else
            scroll = new ScrollPane(widgets);

        final InvalidationListener resized = prop -> handleViewportChanges(scroll);
        scroll.widthProperty().addListener(resized);
        scroll.heightProperty().addListener(resized);

        System.out.println("Press 'space' to change zoom");
        final Scene scene = new Scene(scroll);

        scene.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) ->
        {
            if (event.getCode() == KeyCode.SPACE)
            {
                zoom_in = !zoom_in;
                if (zoom_in)
                    widgets.getTransforms().setAll(new Scale(2.5, 2.5));
                else
                    widgets.getTransforms().setAll(new Scale(0.5, 0.5));
            }
        });

        return scene;
    }

    @Override
    public void start(final Stage stage)
    {
        stage.setTitle("Zoom Pan Demo");
        stage.setScene(createScene());
        stage.show();
    }

    public static void main(String[] args)
    {
        launch(ZoomPan.class, args);
    }
}
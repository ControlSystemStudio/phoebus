/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.sandbox;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/** Tab demo
 * 
 *  Goal:
 *  Bunch of widgets, including a Tab pane,
 *  inside a scrollable and zoomable view.
 *  
 *  Basic idea:
 *  ScrollPane[ Group[ widgets .. including TabPane ] ]
 * 
 *  ScrollPane - Obviously needed for scrolling
 *  Group - Holds all the widgets and supports zooming
 *  
 *  Problem: 
 *  ScrollPane shows scroll bars based on the original size of the widgets,
 *  not based on the actual size.
 *  https://pixelduke.wordpress.com/2012/09/16/zooming-inside-a-scrollpane
 *  explains how to solve that by adding another nested Group:
 * 
 *  ScrollPane[ Group[ Group[ widgets .. including TabPane ] ] ]
 * 
 *  ScrollPane - Obviously needed for scrolling
 *  outer Group - Automatically gets the layout bounds of the zoomed
 *                widgets in the inner group, so ScrollPane
 *                can correctly configure the scroll bars
 *  inner Group - Holds all the widgets and supports zooming
 *  
 *  .. but now a TabPane will fail to properly draw itself
 *  unless it's somehow forced to refresh, for example by toggling
 *  the 'side' property.
 *  
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TabDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage)
    {
        // TabPane with some tabs
        final TabPane tabs = new TabPane();
        tabs.setStyle("-fx-background-color: red;");
        for (int i=0; i<3; ++i)
        {
            final Rectangle rect = new Rectangle(i*100, 100, 10+i*100, 20+i*80);
            rect.setFill(Color.BLUE);
            final Pane content = new Pane(rect);
            final Tab tab = new Tab("Tab " + (i+1), content);
            tab.setClosable(false);
            tabs.getTabs().add(tab);
        }
        tabs.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        tabs.setPrefSize(400, 300);

        final Group widgets = new Group(tabs);
        widgets.setScaleX(0.5);
        widgets.setScaleY(0.5);
        final Group scroll_content = new Group(widgets);
        final ScrollPane scroll = new ScrollPane(scroll_content);
        final Scene scene = new Scene(scroll);
        stage.setTitle("Tab Demo");
        stage.setScene(scene);
        stage.show();

        // Unfortunately, the setup of ScrollPane -> Group -> Group -> TabPane
        // breaks the rendering of the TabPane.
        // While the red background shows the area occupied by TabPane,
        // the actual Tabs are missing..
        System.out.println("See anything?");
        scene.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) ->
		{
			if (event.getCode() == KeyCode.SPACE)
	        {   // .. until 'side' or 'tabMinWidth' or .. are twiddled to force a refresh
	            tabs.setSide(Side.BOTTOM);
	            tabs.setSide(Side.TOP);
	            System.out.println("See it now?");
	        }
		});
    }

    public static void main(String[] args)
    {
        launch(TabDemo.class, args);
    }
}
/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.app.viewer3d;

import org.phoebus.applications.viewer3d.Viewer3dPane;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Demo class for the {@link Viewer3dPane} class.
 * @author Evan Smith
 */
public class Demo3dViewer extends ApplicationWrapper
{
    @Override
    public void start(Stage primaryStage) throws Exception
    {
        Viewer3dPane viewerPane = new Viewer3dPane(null, null);
        
        Scene scene = new Scene(viewerPane, 1000, 1000);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    public static void main(String[] args)
    {
        launch(Demo3dViewer.class, args);
    }
}

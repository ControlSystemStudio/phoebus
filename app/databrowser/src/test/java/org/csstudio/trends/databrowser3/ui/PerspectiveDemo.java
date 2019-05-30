/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Scene;
import javafx.stage.Stage;

/** Demo of the {@link Perspective}
 *  @author Kay Kasemir
 */
public class PerspectiveDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        final Perspective perspective = new Perspective(false);
        final Scene scene = new Scene(perspective, 1000, 900);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(final String[] args)
    {
        launch(PerspectiveDemo.class, args);
    }
}

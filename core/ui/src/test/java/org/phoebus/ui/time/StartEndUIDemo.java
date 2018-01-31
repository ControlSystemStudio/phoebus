/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.time;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Demo of {@link StartEndUI}
 *  @author Kay Kasemir
 */
public class StartEndUIDemo extends Application
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        final StartEndUI ui = new StartEndUI();
        ui.setStart(Duration.of(2, ChronoUnit.DAYS));
        ui.setEnd(Duration.ZERO);

        stage.setScene(new Scene(ui, 600, 500));
        stage.show();
    }

    public static void main(final String[] args)
    {
        Application.launch(args);
    }
}

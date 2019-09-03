/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.pace;
import org.csstudio.display.pace.gui.GUI;
import org.csstudio.display.pace.model.Model;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Scene;
import javafx.stage.Stage;

/** GUI Demo
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class GUIDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        final GUI gui = new GUI(dirty -> System.out.println("Table is dirty: " + dirty));
        stage.setScene(new Scene(gui, 800, 600));
        stage.show();

        JobManager.schedule("Load...", monitor ->
        {
            Thread.sleep(2000);
            final Model model = new Model(Model.class.getResourceAsStream("/pace_examples/localtest.pace"));
            gui.setModel(model);
            model.start();
        });
    }

    public static void main(final String[] args)
    {
        launch(GUIDemo.class, args);
    }
}

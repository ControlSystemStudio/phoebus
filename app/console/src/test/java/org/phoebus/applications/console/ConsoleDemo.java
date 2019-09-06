/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.console;

import java.io.File;

import org.phoebus.applications.console.Console.LineType;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Scene;
import javafx.stage.Stage;

/** Console Demo
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ConsoleDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        final String script = ConsoleDemo.class.getResource("/demo.py").getFile();

        final File python = new File("/usr/bin/python");
        if (! python.canExecute())
        {
            System.out.println("Cannot locate " + python);
            return;
        }

        final Console console = new Console();

        final Scene scene = new Scene(console.getNode(), 800, 600);

        String cmd = python.toString() + " -i " + script;
        // cmd = "/bin/bash";
        final ProcessWrapper process = new ProcessWrapper(cmd, new File("."),
                output -> console.addLine(output, LineType.OUTPUT),
                error  -> console.addLine(error, LineType.ERROR),
                ()     -> console.disable());

        console.setOnInput(process::sendInput);

        stage.setScene(scene);
        stage.setTitle("Console Demo");
        stage.show();

        process.start();
    }

    public static void main(final String[] args)
    {
        launch(ConsoleDemo.class, args);
    }
}

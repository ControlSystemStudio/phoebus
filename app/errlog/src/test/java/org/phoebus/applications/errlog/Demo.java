/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.errlog;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Scene;
import javafx.stage.Stage;

/** Demo of {@link LineView} and {@link ErrLog}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Demo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        ErrLog.prepare();

        final Logger logger = Logger.getLogger("");
        logger.log(Level.SEVERE, "Log before installing handler");
        System.out.println("stdout before installing handler");
        System.err.println("stderr before installing handler");

        // Create UI
        final LineView line_view = new LineView();
        final Scene scene = new Scene(line_view.getControl(), 800, 400);
        stage.setScene(scene);
        stage.show();

        // Install ErrLog, publishing to UI
        final ErrLog errlog = new ErrLog(out -> line_view.addLine(out,  false),
                                         err -> line_view.addLine(err, true));
        logger.log(Level.SEVERE, "Log with active handler");
        System.out.println("stdout with active handler");
        System.err.println("stderr with active handler");

        // Remove ErrLog
        errlog.close();
        logger.log(Level.SEVERE, "Log after handler removed");
        System.out.println("stdout after handler removed");
        System.err.println("stderr after handler removed");
    }

    public static void main(final String[] args)
    {
        launch(Demo.class, args);
    }
}

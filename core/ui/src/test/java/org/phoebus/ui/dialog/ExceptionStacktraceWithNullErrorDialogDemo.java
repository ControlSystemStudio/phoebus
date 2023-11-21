/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.dialog;

import javafx.stage.Stage;
import org.phoebus.ui.javafx.ApplicationWrapper;

import java.io.FileNotFoundException;
import java.io.IOException;

/** Demo of the error dialog, with a summary of the stack trace containing exceptions with null messages and a cyclic
 *  chain
 *  @author Kay Kasemir, Kunal Shroff
 */
@SuppressWarnings("nls")
public class ExceptionStacktraceWithNullErrorDialogDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage)
    {
        Exception rootCause = new FileNotFoundException();
        Exception cause = new IOException();
        cause.initCause(rootCause);
        rootCause.initCause(cause);
        Exception ex = new Exception("This is a test", cause);
        ExceptionDetailsErrorDialog.openError(null,"Test", "", ex, true);
    }

    public static void main(String[] args)
    {
        launch(ExceptionStacktraceWithNullErrorDialogDemo.class, args);
    }
}

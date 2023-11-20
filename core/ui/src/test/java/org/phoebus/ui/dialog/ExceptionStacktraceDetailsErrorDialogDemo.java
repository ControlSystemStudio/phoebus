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

/** Demo of the error dialog with a summary of the stack trace
 *  @author Kay Kasemir, Kunal Shroff
 */
@SuppressWarnings("nls")
public class ExceptionStacktraceDetailsErrorDialogDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage)
    {
        Exception rootCause = new Exception("The ROOT cause of the test exception");
        Exception cause = new Exception("The cause of the test exception", rootCause);
        Exception ex = new Exception("This is a test", cause);
        ExceptionDetailsErrorDialog.openError("Test", ex);
    }

    public static void main(String[] args)
    {
        launch(ExceptionStacktraceDetailsErrorDialogDemo.class, args);
    }
}

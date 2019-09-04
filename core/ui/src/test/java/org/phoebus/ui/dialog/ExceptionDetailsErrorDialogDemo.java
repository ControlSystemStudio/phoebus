/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.dialog;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.stage.Stage;

/** Demo of the error dialog
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ExceptionDetailsErrorDialogDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage)
    {
        ExceptionDetailsErrorDialog.openError("Test", "This is a test\nAnother line", new Exception("This is a test"));
    }

    public static void main(String[] args)
    {
        launch(ExceptionDetailsErrorDialogDemo.class, args);
    }
}

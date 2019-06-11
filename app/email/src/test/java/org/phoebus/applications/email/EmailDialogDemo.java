/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.email;

import org.phoebus.applications.email.actions.SendEmailAction;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.stage.Stage;

public class EmailDialogDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        new SendEmailAction(null, "Demo", "This is\na demonstration...", null).fire();
    }

    public static void main(final String[] args)
    {
        launch(EmailDialogDemo.class, args);
    }
}
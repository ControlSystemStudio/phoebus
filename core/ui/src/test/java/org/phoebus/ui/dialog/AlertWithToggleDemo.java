/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.dialog;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

/** Demo of {@link AlertWithToggle}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlertWithToggleDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage)
    {
        AlertWithToggle dialog = new AlertWithToggle(AlertType.CONFIRMATION, "Test",
                "This is a test\nAnother line");
        System.out.println("Selected: " + dialog.showAndWait());
        System.out.println("Hide from now on: " + dialog.isHideSelected());

        dialog = new AlertWithToggle(AlertType.WARNING,
                "Just using\nheader text",
                "");
        System.out.println("Selected: " + dialog.showAndWait());
        System.out.println("Hide from now on: " + dialog.isHideSelected());

    }

    public static void main(String[] args)
    {
        launch(AlertWithToggleDemo.class, args);
    }
}

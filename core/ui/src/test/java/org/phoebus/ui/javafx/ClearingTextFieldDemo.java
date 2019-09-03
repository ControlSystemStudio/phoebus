/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.javafx;

import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/** ClearingTextField demo
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ClearingTextFieldDemo extends ApplicationWrapper
{
    public static void main(String[] args)
    {
        launch(ClearingTextFieldDemo.class, args);
    }

    @Override
    public void start(final Stage stage)
    {
        final TextField text = new ClearingTextField();
        text.setMaxWidth(Double.MAX_VALUE);
        text.setOnAction(event -> System.out.println("Text is '" + text.getText() + "'"));
        final BorderPane layout = new BorderPane(text);
        stage.setScene(new Scene(layout));
        stage.show();
    }
}

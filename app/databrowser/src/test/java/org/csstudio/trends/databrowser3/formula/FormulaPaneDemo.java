/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.formula;

import java.util.List;

import org.csstudio.apputil.formula.ui.FormulaPane;
import org.csstudio.apputil.formula.ui.InputItem;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

/** Demo of the {@link FormulaPane}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FormulaPaneDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        final GridPane layout = new GridPane();

        final List<InputItem> inputs = List.of(
            new InputItem("fred", "x"),
            new InputItem("freddy", "y"),
            new InputItem("jane", "z"),
            new InputItem("janet", "jj"));

        final FormulaPane form_pane = new FormulaPane("2*x + y", inputs);
        layout.add(form_pane, 0, 0, 2, 1);

        layout.add(new Separator(), 0, 1, 2, 1);

        layout.add(new Label("Formula OK: "), 0, 2);
        final Label status = new Label();
        layout.add(status, 1, 2);
        status.textProperty().bind(form_pane.okProperty().asString());

        stage.setScene(new Scene(layout, 700, 400));
        stage.show();
    }

    public static void main(final String[] args)
    {
        launch(FormulaPaneDemo.class, args);
    }
}

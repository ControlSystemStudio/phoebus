/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.javafx;

import java.util.List;

import javafx.beans.Observable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

/** Demo of {@link MultiCheckboxCombo}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MultiCheckboxComboDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        final MultiCheckboxCombo<String> combo = new MultiCheckboxCombo<>("Select one or more...");
        combo.setOptions(List.of("Apples", "Oranges", "Figs", "Knoppers"));
        combo.selectOptions(List.of("Oranges", "Knoppers"));
        combo.selectedOptions().addListener((Observable o) ->
        {
            System.out.println("Selected: " + combo.getSelectedOptions());
        });

        final Button dump = new Button("Dump");
        dump.setOnAction(event -> System.out.println(combo.getSelectedOptions()));

        combo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(combo, Priority.ALWAYS);
        final HBox layout = new HBox(5, combo, dump);

        final Scene scene = new Scene(layout, 400, 200);
        stage.setTitle("MultiCheckboxCombo Demo");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(final String[] args)
    {
        launch(MultiCheckboxComboDemo.class, args);
    }
}

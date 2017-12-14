/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import org.csstudio.display.builder.model.WidgetProperty;
import org.phoebus.ui.dialog.PopOver;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;

/** Popover for configuring a scrollbar or slider's "increment"
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class SliderConfigPopOver extends PopOver
{
    private final WidgetProperty<Double> increment_property;
    private final TextField config_increment = new TextField();

    SliderConfigPopOver(final WidgetProperty<Double> increment_property)
    {
        this.increment_property = increment_property;
        final GridPane content = new GridPane();
        // content.setGridLinesVisible(true); // Debug layout
        content.setHgap(5);
        content.setVgap(5);
        content.add(new Label("Configure"), 0, 0, 2, 1);
        content.add(new Label("Increment:"), 0, 1);
        content.add(config_increment, 1, 1);

        final ButtonBar buttons = new ButtonBar();
        final Button ok = new Button(ButtonType.OK.getText());
        ButtonBar.setButtonData(ok, ButtonType.OK.getButtonData());
        ok.setOnAction(event ->
        {
            try
            {
                increment_property.setValue(Double.parseDouble(config_increment.getText().trim()));
                hide();
            }
            catch (NumberFormatException ex)
            {
                // Update text, don't close
                config_increment.setText(Double.toString(increment_property.getValue()));
            }
        });

        final Button cancel = new Button(ButtonType.CANCEL.getText());
        ButtonBar.setButtonData(cancel, ButtonType.CANCEL.getButtonData());
        cancel.setOnAction(event -> hide());

        buttons.getButtons().addAll(ok, cancel);
        content.add(buttons, 0, 2, 2, 1);

        // OK button is the 'default' button
        content.addEventFilter(KeyEvent.KEY_PRESSED, event ->
        {
            if (event.getCode() == KeyCode.ENTER)
                ok.getOnAction().handle(null);
        });

        setContent(content);
    }

    @Override
    public void show(final Region owner)
    {
        config_increment.setText(Double.toString(increment_property.getValue()));
        super.show(owner);
        Platform.runLater(() -> config_increment.requestFocus());
    }
}
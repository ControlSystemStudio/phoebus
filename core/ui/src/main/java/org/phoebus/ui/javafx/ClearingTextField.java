/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.javafx;

import javafx.scene.Group;
import javafx.scene.control.Skin;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.TextFieldSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

/** TextField with 'clear' button
 *
 *  <p>As soon as text is entered, a 'clear' button
 *  appears at the right edge.
 *  Clicking it, or pressing 'escape', will clear the text.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ClearingTextField extends TextField
{
    private class CustomSkin extends TextFieldSkin
    {
        public CustomSkin(TextField text)
        {
            super(text);

            // Circle with central 'x' as clear button
            final Circle circle = new Circle();
            circle.setFill(Color.GRAY);
            circle.radiusProperty().bind(text.heightProperty().multiply(0.325));
            circle.setFocusTraversable(false);

            final Line line1 = new Line();
            line1.setStroke(Color.WHITE);
            line1.setStrokeWidth(1.7);
            line1.startXProperty().bind(text.heightProperty().multiply(-0.1));
            line1.startYProperty().bind(text.heightProperty().multiply(-0.1));
            line1.endXProperty().bind(text.heightProperty().multiply(0.1));
            line1.endYProperty().bind(text.heightProperty().multiply(0.1));

            final Line line2 = new Line();
            line2.setStroke(Color.WHITE);
            line2.setStrokeWidth(1.7);
            line2.startXProperty().bind(text.heightProperty().multiply(0.1));
            line2.startYProperty().bind(text.heightProperty().multiply(-0.1));
            line2.endXProperty().bind(text.heightProperty().multiply(-0.1));
            line2.endYProperty().bind(text.heightProperty().multiply(0.1));

            final Group clear_button = new Group(circle, line1, line2);
            // Move clear button from center of text field to right edge
            clear_button.translateXProperty().bind(text.widthProperty().subtract(text.heightProperty()).divide(2.0));

            // Appear as soon as text is entered
            clear_button.visibleProperty().bind(text.textProperty().greaterThan(""));
            getChildren().add(clear_button);

            // Clicking the button clears text
            clear_button.setOnMouseClicked(event -> text.setText(""));
        }
    };

    public ClearingTextField()
    {
        // Pressing 'escape' clears text
        setOnKeyPressed(event ->
        {
            if (event.getCode() == KeyCode.ESCAPE)
            {
                setText("");
                event.consume();
            }
        });
    }

    @Override
    protected Skin<?> createDefaultSkin()
    {
        return new CustomSkin(this);
    }
}
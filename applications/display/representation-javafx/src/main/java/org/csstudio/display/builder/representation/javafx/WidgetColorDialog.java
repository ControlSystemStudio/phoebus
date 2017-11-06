/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import java.util.Collection;

import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.properties.NamedWidgetColor;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.util.ModelThreadPool;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

/** Dialog for selecting a {@link WidgetColor}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WidgetColorDialog extends Dialog<WidgetColor>
{
    private WidgetColor color;

    private final ListView<NamedWidgetColor> color_names = new ListView<>();

    private final ColorPicker picker = new ColorPicker();

    private final Slider red_slider = new Slider(0, 255, 50);
    private final Slider green_slider = new Slider(0, 255, 50);
    private final Slider blue_slider = new Slider(0, 255, 50);
    private final Slider alpha_slider = new Slider(0, 255, 50);

    private final TextField red_text = new TextField();
    private final TextField green_text = new TextField();
    private final TextField blue_text = new TextField();
    private final TextField alpha_text = new TextField();

    /** Prevent circular updates */
    private boolean updating = false;

    /** List cell for a NamedWidgetColor: Color 'blob' and color's name */
    private static class NamedWidgetColorCell extends ListCell<NamedWidgetColor>
    {
        private final static int SIZE = 16;
        private final Canvas blob = new Canvas(SIZE, SIZE);

        public NamedWidgetColorCell()
        {
            setGraphic(blob);
        }

        @Override
        protected void updateItem(final NamedWidgetColor color, final boolean empty)
        {
            super.updateItem(color, empty);
            if (color == null)
                return; // Content won't change from non-null to null, so no need to clear
            setText(color.getName());
            final GraphicsContext gc = blob.getGraphicsContext2D();
            gc.setFill(JFXUtil.convert(color));
            gc.fillRect(0, 0, SIZE, SIZE);
        }
    };

    /** Create dialog
     *  @param initial_color Initial {@link WidgetColor}
     */
    public WidgetColorDialog(final WidgetColor initial_color)
    {
        setTitle(Messages.ColorDialog_Title);
        setHeaderText(Messages.ColorDialog_Info);

        /* Predefined Colors   Custom Color
         * [               ]   Picker
         * [               ]   Red   ---*----  [100]
         * [     List      ]   Green ------*-  [250]
         * [               ]   Blue  *-------  [  0]
         * [               ]   Alpha -------*  [255]
         * [               ]   /..    dummy      ../
         */

        final GridPane content = new GridPane();
        // content.setGridLinesVisible(true); // For debugging
        content.setHgap(10);
        content.setVgap(10);
        content.setPadding(new Insets(10));

        content.add(new Label(Messages.ColorDialog_Predefined), 0, 0);

        // Represent NamedWidgetColor with custom ListCell
        color_names.setCellFactory((view) -> new NamedWidgetColorCell());
        // Get colors on background thread
        ModelThreadPool.getExecutor().execute(() ->
        {
            final NamedWidgetColors colors = WidgetColorService.getColors();
            final Collection<NamedWidgetColor> values = colors.getColors();
            Platform.runLater(() ->
            {
                color_names.getItems().addAll(values);

                if (initial_color instanceof NamedWidgetColor)
                {
                    color_names.getSelectionModel().select((NamedWidgetColor) initial_color);
                    color_names.scrollTo(color_names.getSelectionModel().getSelectedIndex());
                }
            });
        });
        content.add(color_names, 0, 1, 1, 6);

        content.add(new Label(Messages.ColorDialog_Custom), 1, 0, 3, 1);
        content.add(picker, 1, 1, 3, 1);

        content.add(new Label(Messages.Red), 1, 2);
        content.add(new Label(Messages.Green), 1, 3);
        content.add(new Label(Messages.Blue), 1, 4);
        content.add(new Label(Messages.Alpha), 1, 5);

        content.add(red_slider, 2, 2);
        content.add(green_slider, 2, 3);
        content.add(blue_slider, 2, 4);
        content.add(alpha_slider, 2, 5);
        red_slider.setBlockIncrement(1);

        red_text.setPrefColumnCount(3);
        green_text.setPrefColumnCount(3);
        blue_text.setPrefColumnCount(3);
        alpha_text.setPrefColumnCount(3);
        content.add(red_text, 3, 2);
        content.add(green_text, 3, 3);
        content.add(blue_text, 3, 4);
        content.add(alpha_text, 3, 5);

        // Placeholder that fills the lower right corner
        final Label dummy = new Label();
        content.add(dummy, 1, 6, 3, 1);

        getDialogPane().setContent(content);

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // User selects named color -> Update picker, sliders, texts
        color_names.getSelectionModel().selectedItemProperty().addListener((l, old, value) ->
        {
            setColor(value);
        });

        // Double-click confirms/closes
        color_names.setOnMouseClicked((event) ->
        {
            if (event.getButton() == MouseButton.PRIMARY  &&
                event.getClickCount() >= 2)
            {
                final Button ok = (Button) getDialogPane().lookupButton(ButtonType.OK);
                ok.fire();
            }
        });

        // User changes slider or text -> Update picker
        bind(red_slider,   red_text);
        bind(green_slider, green_text);
        bind(blue_slider,  blue_text);
        bind(alpha_slider, alpha_text);

        // User configures color in picker -> Update sliders, texts
        picker.setOnAction(event ->
        {
            if (updating)
                return;
            updating = true;

            final Color value = picker.getValue();
            final int r = (int) (value.getRed() * 255);
            final int g = (int) (value.getGreen() * 255);
            final int b = (int) (value.getBlue() * 255);
            final int a = (int) (value.getOpacity() * 255);
            red_slider.setValue(r);
            green_slider.setValue(g);
            blue_slider.setValue(b);
            alpha_slider.setValue(a);
            red_text.setText(Integer.toString(r));
            green_text.setText(Integer.toString(g));
            blue_text.setText(Integer.toString(b));
            alpha_text.setText(Integer.toString(a));

            color = new WidgetColor(r, g, b, a);
            updating = false;
        });

        // From http://code.makery.ch/blog/javafx-dialogs-official/,
        // attempts to focus on a field.
        // Will only work if the dialog is opened "soon".
        Platform.runLater(() -> color_names.requestFocus());

        setResultConverter(button ->
        {
            if (button == ButtonType.OK)
                return color;
            return null;
        });

        setColor(initial_color);
    }

    /** Bidirectionally bind slider and text, also update picker and color
     *  @param slider {@link Slider}
     *  @param text {@link TextField}
     */
    private void bind(final Slider slider, final TextField text)
    {
        slider.valueProperty().addListener((s, old, value) ->
        {
            if (updating)
                return;
            updating = true;
            text.setText(Integer.toString(value.intValue()));
            final Color jfx_col = getSliderColor();
            picker.setValue(jfx_col);
            color = JFXUtil.convert(jfx_col);
            updating = false;
        });

        text.textProperty().addListener((t, old, value) ->
        {
            if (updating)
                return;
            updating = true;
            try
            {
                int num = Integer.parseInt(value);
                if (num > 255)
                {
                    num = 255;
                    text.setText("255");
                }
                if (num < 0)
                {
                    num = 0;
                    text.setText("0");
                }
                slider.setValue(num);
                final Color jfx_col = getSliderColor();
                picker.setValue(jfx_col);
                color = JFXUtil.convert(jfx_col);
            }
            catch (Throwable ex)
            {
                text.setText(Integer.toString((int)slider.getValue()));
            }
            finally
            {
                updating = false;
            }
        });
    }

    /** @return Color currently configured in sliders */
    private Color getSliderColor()
    {
        return Color.rgb((int) red_slider.getValue(),
                         (int) green_slider.getValue(),
                         (int) blue_slider.getValue(),
                         alpha_slider.getValue() / 255.0);
    }

    /** Set all display elements to color
     *  @param color WidgetColor
     */
    private void setColor(final WidgetColor color)
    {
        picker.setValue(JFXUtil.convert(color));
        red_slider.setValue(color.getRed());
        green_slider.setValue(color.getGreen());
        blue_slider.setValue(color.getBlue());
        alpha_slider.setValue(color.getAlpha());
        red_text.setText(Integer.toString(color.getRed()));
        green_text.setText(Integer.toString(color.getGreen()));
        blue_text.setText(Integer.toString(color.getBlue()));
        alpha_text.setText(Integer.toString(color.getAlpha()));
        this.color = color;
    }
}

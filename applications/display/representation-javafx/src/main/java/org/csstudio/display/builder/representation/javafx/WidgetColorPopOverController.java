/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * Copyright (C) 2016 European Spallation Source ERIC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.csstudio.display.builder.representation.javafx;


import java.net.URL;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.properties.NamedWidgetColor;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.util.ModelThreadPool;
import org.phoebus.ui.dialog.PopOver;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;


/**
 * @author claudiorosati, European Spallation Source ERIC
 * @version 1.0.0 29 Nov 2017
 */
public class WidgetColorPopOverController implements Initializable {

    @FXML private GridPane root;

    @FXML private Label infoLabel;

    @FXML private ListView<NamedWidgetColor> colorNames;

    @FXML private ColorPicker picker;

    @FXML private Slider redSlider;
    @FXML private Slider greenSlider;
    @FXML private Slider blueSlider;
    @FXML private Slider alphaSlider;

    @FXML private Spinner<Integer> redSpinner;
    @FXML private Spinner<Integer> greenSpinner;
    @FXML private Spinner<Integer> blueSpinner;
    @FXML private Spinner<Integer> alphaSpinner;

    @FXML private Circle currentColorCircle;
    @FXML private Circle defaultColorCircle;
    @FXML private Circle originalColorCircle;

    @FXML private ButtonBar buttonBar;
    @FXML private Button defaultButton;
    @FXML private Button cancelButton;
    @FXML private Button okButton;

    private Consumer<WidgetColor>              colorChangeConsumer;
    private PopOver                            popOver;
    private final AtomicBoolean                namesLoaded   = new AtomicBoolean(false);
    private final Map<Color, NamedWidgetColor> namedColors   = Collections.synchronizedMap(new HashMap<>());
    private WidgetColor                        defaultColor  = null;
    private WidgetColor                        originalColor = null;
    private boolean                            updating      = false;

    /*
     * ---- color property -----------------------------------------------------
     */
    private final ObjectProperty<WidgetColor> color = new SimpleObjectProperty<WidgetColor>(this, "color", WidgetColorService.getColor(NamedWidgetColors.BACKGROUND)) {
        @Override
        protected void invalidated() {

            WidgetColor col = get();

            if ( col == null ) {
                set(WidgetColorService.getColor(NamedWidgetColors.BACKGROUND));
            }

        }
    };

    ObjectProperty<WidgetColor> colorProperty() {
        return color;
    }

    WidgetColor getColor() {
        return color.get();
    }

    void setColor( WidgetColor color ) {
        this.color.set(color);
    }

    /*
     * -------------------------------------------------------------------------
     */
    @Override
    public void initialize( URL location, ResourceBundle resources ) {

        updateButton(okButton, ButtonType.OK);
        updateButton(cancelButton, ButtonType.CANCEL);
        updateButton(defaultButton, new ButtonType(Messages.WidgetColorPopOver_DefaultButton, ButtonData.LEFT));

        okButton.setText(ButtonType.OK.getText());
        ButtonBar.setButtonData(okButton, ButtonType.OK.getButtonData());
        root.addEventFilter(KeyEvent.KEY_PRESSED, event ->
        {
            if (event.getCode() == KeyCode.ENTER)
                okPressed(null);
        });

        picker.valueProperty().addListener(( observable, oldColor, newColor ) -> {
            if ( !updating ) {
                setColor(JFXUtil.convert(newColor));
            }
        });
        defaultButton.disableProperty().bind(Bindings.createBooleanBinding(() -> getColor().equals(defaultColor), colorProperty()));
        okButton.disableProperty().bind(Bindings.createBooleanBinding(() -> getColor().equals(originalColor), colorProperty()));

        redSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 255));
        redSpinner.valueProperty().addListener(this::updateFromSpinner);
        greenSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 255));
        greenSpinner.valueProperty().addListener(this::updateFromSpinner);
        blueSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 255));
        blueSpinner.valueProperty().addListener(this::updateFromSpinner);
        alphaSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 255));
        alphaSpinner.valueProperty().addListener(this::updateFromSpinner);

        redSlider.valueProperty().addListener(this::updateFromSlider);
        greenSlider.valueProperty().addListener(this::updateFromSlider);
        blueSlider.valueProperty().addListener(this::updateFromSlider);
        alphaSlider.valueProperty().addListener(this::updateFromSlider);

        colorProperty().addListener(( observable, oldColor, newColor ) -> {

            updating = true;

            Color jfxColor = JFXUtil.convert(getColor());

            picker.setValue(jfxColor);
            currentColorCircle.setFill(jfxColor);

            redSlider.setValue(getRed());
            redSpinner.getValueFactory().setValue(getRed());
            greenSlider.setValue(getGreen());
            greenSpinner.getValueFactory().setValue(getGreen());
            blueSlider.setValue(getBlue());
            blueSpinner.getValueFactory().setValue(getBlue());
            alphaSlider.setValue(getAlpha());
            alphaSpinner.getValueFactory().setValue(getAlpha());

            updating = false;

        });

        colorNames.setCellFactory(view -> new NamedWidgetColorCell());
        colorNames.getSelectionModel().selectedItemProperty().addListener(( observable, oldValue, newValue ) -> {
            if ( newValue != null ) {
                setColor(newValue);
            }
        });

        // Get colors on background thread
        ModelThreadPool.getExecutor().execute( ( ) -> {

            final NamedWidgetColors colors = WidgetColorService.getColors();
            final Collection<NamedWidgetColor> values = colors.getColors();

            values.parallelStream().forEach(nc -> namedColors.put(JFXUtil.convert(nc), nc));

            Platform.runLater(() -> {
                values.stream().forEach(nc -> {
                    colorNames.getItems().addAll(nc);
                });
                namesLoaded.set(true);
            });

        });

        //  Add the 24 Visually Unique colors to the user's palette.
        //  They are useful to create plot tracks.
        //  See: http://phrogz.net/tmp/24colors.html
        picker.getCustomColors().add(Color.rgb(255, 0, 0));
        picker.getCustomColors().add(Color.rgb(255, 255, 0));
        picker.getCustomColors().add(Color.rgb(0, 234, 255));
        picker.getCustomColors().add(Color.rgb(170, 0, 255));
        picker.getCustomColors().add(Color.rgb(255, 127, 0));
        picker.getCustomColors().add(Color.rgb(191, 255, 0));
        picker.getCustomColors().add(Color.rgb(0, 149, 255));
        picker.getCustomColors().add(Color.rgb(255, 0, 170));
        picker.getCustomColors().add(Color.rgb(255, 212, 0));
        picker.getCustomColors().add(Color.rgb(106, 255, 0));
        picker.getCustomColors().add(Color.rgb(0, 64, 255));
        picker.getCustomColors().add(Color.rgb(237, 185, 185));
        picker.getCustomColors().add(Color.rgb(185, 215, 237));
        picker.getCustomColors().add(Color.rgb(231, 233, 185));
        picker.getCustomColors().add(Color.rgb(220, 185, 237));
        picker.getCustomColors().add(Color.rgb(185, 237, 224));
        picker.getCustomColors().add(Color.rgb(143, 35, 35));
        picker.getCustomColors().add(Color.rgb(35, 98, 143));
        picker.getCustomColors().add(Color.rgb(143, 106, 35));
        picker.getCustomColors().add(Color.rgb(107, 35, 143));
        picker.getCustomColors().add(Color.rgb(79, 143, 35));
        picker.getCustomColors().add(Color.rgb(0, 0, 0));
        picker.getCustomColors().add(Color.rgb(115, 115, 115));
        picker.getCustomColors().add(Color.rgb(204, 204, 204));

    }

    @FXML
    void cancelPressed ( ActionEvent event ) {
        if ( popOver != null ) {
            popOver.hide();
        }
    }


    @FXML
    void defaultColorClicked ( MouseEvent event ) {
        setColor(defaultColor);
    }

    @FXML
    void defaultPressed(ActionEvent event) {

        if ( colorChangeConsumer != null ) {
            colorChangeConsumer.accept(defaultColor);
        }

        cancelPressed(event);

    }

    @FXML
    void okPressed ( ActionEvent event ) {

        if ( colorChangeConsumer != null ) {
            colorChangeConsumer.accept(getColor());
        }

        cancelPressed(event);

    }
    @FXML
    void originalColorClicked ( MouseEvent event ) {
        setColor(originalColor);
    }

    @FXML
    void originalPressed(ActionEvent event) {
        setColor(originalColor);
    }

    void setInitialConditions (
            final PopOver popOver,
            WidgetColor originalWidgetColor,
            final WidgetColor defaultWidgetColor,
            final String propertyName,
            final Consumer<WidgetColor> colorChangeConsumer ) {

        this.colorChangeConsumer = colorChangeConsumer;
        this.popOver = popOver;
        this.originalColor = originalWidgetColor;
        this.defaultColor = defaultWidgetColor;

        infoLabel.setText(MessageFormat.format(Messages.WidgetColorPopOver_Info, propertyName));

        originalColorCircle.setFill(JFXUtil.convert(originalColor));
        defaultColorCircle.setFill(JFXUtil.convert(defaultColor));
        setColor(originalColor);

        ModelThreadPool.getExecutor().execute( ( ) -> {

            while ( !namesLoaded.get() ) {
                Thread.yield();
            }

            Platform.runLater(() -> {
                if ( originalWidgetColor instanceof NamedWidgetColor ) {
                    colorNames.getSelectionModel().select((NamedWidgetColor) originalWidgetColor);
                    colorNames.scrollTo(colorNames.getSelectionModel().getSelectedIndex());
                }
            });

        });

    }

    private int getAlpha() {
        return getColor().getAlpha();
    }

    private int getBlue() {
        return getColor().getBlue();
    }

    private int getGreen() {
        return getColor().getGreen();
    }

    private int getRed() {
        return getColor().getRed();
    }

    private WidgetColor getSliderColor ( ) {
        return JFXUtil.convert(Color.rgb(
            (int) redSlider.getValue(),
            (int) greenSlider.getValue(),
            (int) blueSlider.getValue(),
            (int) alphaSlider.getValue() / 255.0
        ));
    }

    private WidgetColor getSpinnerColor ( ) {
        return JFXUtil.convert(Color.rgb(
            Math.max(0, Math.min(255, redSpinner.getValue())),
            Math.max(0, Math.min(255, greenSpinner.getValue())),
            Math.max(0, Math.min(255, blueSpinner.getValue())),
            Math.max(0, Math.min(255, alphaSpinner.getValue() / 255.0))
        ));
    }

    private void updateButton ( final Button button, final ButtonType buttonType ) {
        button.setText(buttonType.getText());
        ButtonBar.setButtonData(button, buttonType.getButtonData());
        button.setDefaultButton(buttonType.getButtonData().isDefaultButton());
        button.setCancelButton(buttonType.getButtonData().isCancelButton());
    }

    private void updateFromSlider ( ObservableValue<? extends Number> observable, Number oldValue, Number newValue ) {
        if ( !updating ) {
            setColor(getSliderColor());
        }
    }

    private void updateFromSpinner ( ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue ) {
        if ( !updating ) {
            setColor(getSpinnerColor());
        }
    }

    /**
     * List cell for a NamedWidgetColor: Color 'blob' and color's name.
     */
    private static class NamedWidgetColorCell extends ListCell<NamedWidgetColor> {

        private final static int SIZE = 16;
        private final Canvas     blob = new Canvas(SIZE, SIZE);

        NamedWidgetColorCell ( ) {
            setGraphic(blob);
        }

        @Override
        protected void updateItem ( final NamedWidgetColor color, final boolean empty ) {

            super.updateItem(color, empty);

            if ( color == null ) {
                // Content won't change from non-null to null, so no need to clear.
                return;
            }

            setText(color.getName());

            final GraphicsContext gc = blob.getGraphicsContext2D();

            gc.setFill(JFXUtil.convert(color));
            gc.fillRect(0, 0, SIZE, SIZE);

        }
    };

}

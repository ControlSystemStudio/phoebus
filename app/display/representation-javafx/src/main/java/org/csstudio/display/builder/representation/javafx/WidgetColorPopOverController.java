/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * Copyright (C) 2016 European Spallation Source ERIC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.csstudio.display.builder.representation.javafx;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.net.URL;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.properties.NamedWidgetColor;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.util.ModelThreadPool;
import org.phoebus.ui.dialog.PopOver;
import org.phoebus.ui.javafx.ClearingTextField;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
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
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
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

    @FXML private ClearingTextField searchField;

    private Consumer<WidgetColor>              colorChangeConsumer;
    private PopOver                            popOver;
    private final CountDownLatch               namesLoaded   = new CountDownLatch(1);
    private WidgetColor                        defaultColor  = null;
    private WidgetColor                        originalColor = null;
    private boolean                            updating      = false;
    private final ObservableList<NamedWidgetColor> namedColorsList   = FXCollections.observableArrayList();
    private final FilteredList<NamedWidgetColor>   filteredNamedColorsList = new FilteredList<>(new SortedList<>(
                      namedColorsList,
                      (nc1, nc2) -> String.CASE_INSENSITIVE_ORDER.compare(nc1.getName(), nc2.getName())
                      ));


    /*
     * ---- color property -----------------------------------------------------
     */
    private final ObjectProperty<WidgetColor> color = new SimpleObjectProperty<>(this, "color", WidgetColorService.getColor(NamedWidgetColors.BACKGROUND)) {
        @Override
        protected void invalidated() {

            final WidgetColor col = get();

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
        updateButton(defaultButton, new ButtonType(Messages.WidgetColorPopOver_DefaultButton, ButtonData.BACK_PREVIOUS));

        okButton.setText(ButtonType.OK.getText());
        ButtonBar.setButtonData(okButton, ButtonType.OK.getButtonData());
        root.addEventFilter(KeyEvent.KEY_PRESSED, event ->
        {
            if (event.getCode() == KeyCode.ENTER)
                okPressed(null);
                event.consume();
        });

        picker.valueProperty().addListener(( observable, oldColor, newColor ) -> {
            if ( !updating ) {
                setColor(JFXUtil.convert(newColor));
            }
        });
        defaultButton.disableProperty().bind(Bindings.createBooleanBinding(() -> getColor().equals(defaultColor), colorProperty()));
        okButton.disableProperty().bind(Bindings.createBooleanBinding(() -> getColor().equals(originalColor), colorProperty()));

        final SpinnerValueFactory<Integer> redSpinnerValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 255);
        final TextFormatter<Integer> redSpinnerFormatter = new TextFormatter<>(redSpinnerValueFactory.getConverter(), redSpinnerValueFactory.getValue());

        redSpinnerValueFactory.valueProperty().bindBidirectional(redSpinnerFormatter.valueProperty());
        redSpinner.getEditor().setTextFormatter(redSpinnerFormatter);
        redSpinner.setValueFactory(redSpinnerValueFactory);
        redSpinner.valueProperty().addListener(this::updateFromSpinner);
        redSpinner.focusedProperty().addListener( ( s, ov, nv ) -> {
            if ( nv ) {
                Platform.runLater(() -> redSpinner.getEditor().selectAll());
            }
        });

        final SpinnerValueFactory<Integer> greenSpinnerValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 255);
        final TextFormatter<Integer> greenSpinnerFormatter = new TextFormatter<>(greenSpinnerValueFactory.getConverter(), greenSpinnerValueFactory.getValue());

        greenSpinnerValueFactory.valueProperty().bindBidirectional(greenSpinnerFormatter.valueProperty());
        greenSpinner.getEditor().setTextFormatter(greenSpinnerFormatter);
        greenSpinner.setValueFactory(greenSpinnerValueFactory);
        greenSpinner.valueProperty().addListener(this::updateFromSpinner);
        greenSpinner.focusedProperty().addListener( ( s, ov, nv ) -> {
            if ( nv ) {
                Platform.runLater(() -> greenSpinner.getEditor().selectAll());
            }
        });

        final SpinnerValueFactory<Integer> blueSpinnerValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 255);
        final TextFormatter<Integer> blueSpinnerFormatter = new TextFormatter<>(blueSpinnerValueFactory.getConverter(), blueSpinnerValueFactory.getValue());

        blueSpinnerValueFactory.valueProperty().bindBidirectional(blueSpinnerFormatter.valueProperty());
        blueSpinner.getEditor().setTextFormatter(blueSpinnerFormatter);
        blueSpinner.setValueFactory(blueSpinnerValueFactory);
        blueSpinner.valueProperty().addListener(this::updateFromSpinner);
        blueSpinner.focusedProperty().addListener( ( s, ov, nv ) -> {
            if ( nv ) {
                Platform.runLater(() -> blueSpinner.getEditor().selectAll());
            }
        });

        final SpinnerValueFactory<Integer> alphaSpinnerValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 255);
        final TextFormatter<Integer> alphaSpinnerFormatter = new TextFormatter<>(alphaSpinnerValueFactory.getConverter(), alphaSpinnerValueFactory.getValue());

        alphaSpinnerValueFactory.valueProperty().bindBidirectional(alphaSpinnerFormatter.valueProperty());
        alphaSpinner.getEditor().setTextFormatter(alphaSpinnerFormatter);
        alphaSpinner.setValueFactory(alphaSpinnerValueFactory);
        alphaSpinner.valueProperty().addListener(this::updateFromSpinner);
        alphaSpinner.focusedProperty().addListener( ( s, ov, nv ) -> {
            if ( nv ) {
                Platform.runLater(() -> alphaSpinner.getEditor().selectAll());
            }
        });

        redSlider.valueProperty().addListener(this::updateFromSlider);
        greenSlider.valueProperty().addListener(this::updateFromSlider);
        blueSlider.valueProperty().addListener(this::updateFromSlider);
        alphaSlider.valueProperty().addListener(this::updateFromSlider);

        colorProperty().addListener(( observable, oldColor, newColor ) -> {

            updating = true;

            final Color jfxColor = JFXUtil.convert(getColor());

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

        colorNames.setPlaceholder(new Label(Messages.WidgetColorPopOver_PredefinedColors));
        colorNames.setItems(filteredNamedColorsList);
        colorNames.setCellFactory(view -> new NamedWidgetColorCell());
        colorNames.getSelectionModel().selectedItemProperty().addListener(( observable, oldValue, newValue ) -> {
            if ( newValue != null ) {
                setColor(newValue);
            }
        });

        // Get colors on background thread
        ModelThreadPool.getExecutor().execute( ( ) -> {

            final Collection<NamedWidgetColor> namedColors = WidgetColorService.getColors().getColors();
            Platform.runLater(() -> {
                namedColorsList.addAll(namedColors);
                namesLoaded.countDown();
            });

        });

        //  Add the 24 Visually Unique colors to the user's palette.
        //  They are useful to create plot tracks.
        //  See: http://phrogz.net/tmp/24colors.html
        for (WidgetColor c : NamedWidgetColors.PALETTE)
            picker.getCustomColors().add(JFXUtil.convert(c));

        //  Search field
        //searchField.setPromptText(Messages.WidgetColorPopOver_SearchField);
        searchField.setTooltip(new Tooltip(Messages.WidgetColorPopOver_SearchFieldTT));
        searchField.setPrefColumnCount(9);
        searchField.textProperty().addListener(o -> {

            final String filter = searchField.getText();

            if ( filter == null || filter.isEmpty() ) {
                filteredNamedColorsList.setPredicate(null);
            } else {

                final String lcFilter = filter.toLowerCase();

                filteredNamedColorsList.setPredicate(s -> s.getName().toLowerCase().contains(lcFilter));
                Platform.runLater(() ->
                {
                    colorNames.refresh();
                    colorNames.scrollTo(0);
                });

            }

        });

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
            final Consumer<WidgetColor> colorChangeConsumer )
    {

        this.colorChangeConsumer = colorChangeConsumer;
        this.popOver = popOver;
        this.originalColor = originalWidgetColor;
        this.defaultColor = defaultWidgetColor;

        infoLabel.setText(MessageFormat.format(Messages.WidgetColorPopOver_Info, propertyName));

        originalColorCircle.setFill(JFXUtil.convert(originalColor));
        defaultColorCircle.setFill(JFXUtil.convert(defaultColor));
        setColor(originalColor);

        ModelThreadPool.getExecutor().execute( ( ) -> {

            try
            {
                namesLoaded.await();
            }
            catch ( final InterruptedException iex )
            {
                logger.throwing(WidgetColorPopOverController.class.getName(), "setInitialConditions[executor]", iex);
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

            if ( color == null || empty ) {
                setText(null);
                setGraphic(null);
            } else {

                setText(color.getName());
                setGraphic(blob);

                final GraphicsContext gc = blob.getGraphicsContext2D();

                gc.setFill(JFXUtil.convert(color));
                gc.fillRect(0, 0, SIZE, SIZE);

            }

        }
    };

}

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
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.csstudio.display.builder.model.persist.NamedWidgetFonts;
import org.csstudio.display.builder.model.persist.WidgetFontService;
import org.csstudio.display.builder.model.properties.NamedWidgetFont;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.csstudio.display.builder.model.properties.WidgetFontStyle;
import org.csstudio.display.builder.model.util.ModelThreadPool;
import org.phoebus.ui.dialog.PopOver;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.util.converter.FormatStringConverter;

/**
 * @author claudiorosati, European Spallation Source ERIC
 * @version 1.0.0 23 May 2018
 */
public class WidgetFontPopOverController implements Initializable {

    private final static Collection<Double> DEFAULT_SIZES = Arrays.asList(
        8.0, 9.0, 10.0, 11.0, 12.0, 14.0, 16.0, 18.0, 20.0, 24.0, 28.0, 32.0, 36.0, 48.0, 60.0, 72.0
    );

    private static <T> T defaultIfNull ( T value, T defaultValue ) {
        return ( value != null ) ? value : defaultValue;
    }

    @FXML private GridPane root;

    @FXML private Label infoLabel;

    @FXML private TextField searchField;

    @FXML private ListView<NamedWidgetFont> fontNames;
    @FXML private ListView<String> families;

    @FXML private ComboBox<WidgetFontStyle> styles;
    @FXML private ComboBox<Double> sizes;

    @FXML private TextField preview;

    @FXML private Button cancelButton;
    @FXML private Button defaultButton;
    @FXML private Button okButton;

    private WidgetFont                            defaultFont            = null;
    private final ObservableList<String>          familiesList         = FXCollections.observableArrayList();
    private Consumer<WidgetFont>                  fontChangeConsumer;
    private final ObservableList<NamedWidgetFont> namedFontsList         = FXCollections.observableArrayList();
    private final CountDownLatch                  namesLoaded            = new CountDownLatch(2);
    private WidgetFont                            originalFont           = null;
    private PopOver                               popOver;

    private final Updater<String> familiesUpdater = new Updater<>(newValue -> {
        if ( newValue != null ) {
            setFont(new WidgetFont(
                newValue,
                defaultIfNull(styles.getSelectionModel().getSelectedItem(), WidgetFontStyle.REGULAR),
                Math.max(1.0, defaultIfNull((Number) sizes.getSelectionModel().getSelectedItem(), 10.0).doubleValue())
            ));
        }
    });
    private final FilteredList<String> filteredFamiliesList = new FilteredList<>(new SortedList<>(
            familiesList,
            String.CASE_INSENSITIVE_ORDER
        ));
    private final FilteredList<NamedWidgetFont> filteredNamedFontsList = new FilteredList<>(new SortedList<>(
        namedFontsList,
        (nc1, nc2) -> String.CASE_INSENSITIVE_ORDER.compare(nc1.getName(), nc2.getName())
    ));
    private final Updater<WidgetFont> fontNamesUpdater = new Updater<>(newValue -> {
        if ( newValue != null ) {
            setFont(newValue);
        }
    });
    private final Updater<Double> sizesUpdater = new Updater<>(newValue -> {
        if ( newValue != null ) {
            setFont(new WidgetFont(
                defaultIfNull(families.getSelectionModel().getSelectedItem(), Font.font(10.0).getFamily()),
                defaultIfNull(styles.getSelectionModel().getSelectedItem(), WidgetFontStyle.REGULAR),
                Math.max(1.0, newValue)
            ));
        }
    });
    private final Updater<WidgetFontStyle> stylesUpdater = new Updater<>(newValue -> {
        if ( newValue != null ) {
            setFont(new WidgetFont(
                defaultIfNull(families.getSelectionModel().getSelectedItem(), Font.font(10.0).getFamily()),
                newValue,
                Math.max(1.0, defaultIfNull((Number) sizes.getSelectionModel().getSelectedItem(), 10.0).doubleValue())
            ));
        }
    });

    /*
     * ---- font property -----------------------------------------------------
     */
    private final ObjectProperty<WidgetFont> font = new SimpleObjectProperty<>(this, "font", new WidgetFont("", WidgetFontStyle.REGULAR, Double.NaN)) {
        @Override
        protected void invalidated() {

            final WidgetFont fnt = get();

            if ( fnt == null ) {
                set(WidgetFontService.get(NamedWidgetFonts.DEFAULT));
            }

        }
    };

    ObjectProperty<WidgetFont> fontProperty() {
        return font;
    }

    WidgetFont getFont() {
        return font.get();
    }

    void setFont( WidgetFont font ) {
        this.font.set(font);
    }

    /*
     * -------------------------------------------------------------------------
     */
    @Override
    public void initialize ( URL location, ResourceBundle resources ) {

        //  Listeners to font change.
        fontProperty().addListener(( observable, oldValue, newValue ) -> {

            if ( newValue instanceof NamedWidgetFont && !fontNamesUpdater.isUpdating() ) {
                fontNames.getSelectionModel().select((NamedWidgetFont) newValue);
                fontNames.scrollTo(fontNames.getSelectionModel().getSelectedItem());
            }

            if ( !familiesUpdater.isUpdating() ) {
                families.getSelectionModel().select(newValue.getFamily());
                families.scrollTo(families.getSelectionModel().getSelectedItem());
            }

            if ( !stylesUpdater.isUpdating() ) {
                styles.getSelectionModel().select(newValue.getStyle());
            }

            sizes.getSelectionModel().select(newValue.getSize());

            preview.setFont(JFXUtil.convert(newValue));

        });

        //  Buttons
        updateButton(okButton, ButtonType.OK);
        updateButton(cancelButton, ButtonType.CANCEL);
        updateButton(defaultButton, new ButtonType(Messages.WidgetColorPopOver_DefaultButton, ButtonData.BACK_PREVIOUS));

        okButton.setText(ButtonType.OK.getText());
        ButtonBar.setButtonData(okButton, ButtonType.OK.getButtonData());
        root.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if ( event.getCode() == KeyCode.ENTER && okButton.isDefaultButton()) {
                okPressed(null);
                event.consume();
            }
        });

        defaultButton.disableProperty().bind(Bindings.createBooleanBinding(() -> getFont().equals(defaultFont), fontProperty()));
        okButton.disableProperty().bind(Bindings.createBooleanBinding(() -> getFont().equals(originalFont), fontProperty()));

        //  Lists and combo boxes
        fontNames.setPlaceholder(new Label(Messages.WidgetFontPopOver_PredefinedFonts));
        fontNames.setItems(filteredNamedFontsList);
        fontNames.getSelectionModel().selectedItemProperty().addListener(( obserobsvable, oldValue, newValue ) -> fontNamesUpdater.accept(newValue));

        families.setPlaceholder(new Label(Messages.WidgetFontPopOver_FontsFamilies));
        families.setItems(filteredFamiliesList);
        families.getSelectionModel().selectedItemProperty().addListener(( observable, oldValue, newValue ) -> familiesUpdater.accept(newValue));

        styles.setPlaceholder(new Label(Messages.WidgetFontPopOver_Styles));
        styles.setItems(new SortedList<>(
            FXCollections.observableArrayList(WidgetFontStyle.values()),
            (s1, s2) -> String.CASE_INSENSITIVE_ORDER.compare(s1.name(), s2.name())
        ));
        styles.valueProperty().addListener(( observable, oldValue, newValue ) -> stylesUpdater.accept(newValue));

        sizes.setPlaceholder(new Label(Messages.WidgetFontPopOver_Sizes));
        sizes.setConverter(new FormatStringConverter<Double>(new DecimalFormat("##0.0#")) {
            @Override
            public Double fromString ( String value ) {
                return Double.valueOf(((Number) super.fromString(value)).doubleValue());
            }
        });
        sizes.getItems().addAll(DEFAULT_SIZES);
        sizes.getEditor().focusedProperty().addListener(( observable, oldValue, newValue ) -> {
            if ( newValue && okButton.isDefaultButton() ) {
                okButton.setDefaultButton(false);
            } else if ( !newValue && !okButton.isDefaultButton() ) {
                okButton.setDefaultButton(true);
            }
        });
        sizes.valueProperty().addListener(( observable, oldValue, newValue ) -> sizesUpdater.accept(newValue));

        // Get fonts on background thread
        ModelThreadPool.getExecutor().execute( ( ) -> {

            final List<String> fontFamilies = Font.getFamilies();

            Platform.runLater(() -> {
                familiesList.addAll(fontFamilies);
                namesLoaded.countDown();
            });

            final Collection<NamedWidgetFont> namedFonts = WidgetFontService.getFonts().getFonts();

            Platform.runLater(() -> {
                namedFontsList.addAll(namedFonts);
                namesLoaded.countDown();
            });

        });


        //  Search field
        searchField.setTooltip(new Tooltip(Messages.WidgetFontPopOver_SearchPromptTT));
        searchField.setPrefColumnCount(9);
        searchField.textProperty().addListener(o -> {

            final String filter = searchField.getText();

            if ( filter == null || filter.isEmpty() ) {
                filteredNamedFontsList.setPredicate(null);
                filteredFamiliesList.setPredicate(null);
            } else {

                final String lcFilter = filter.toLowerCase();

                filteredNamedFontsList.setPredicate(s -> s.getName().toLowerCase().contains(lcFilter));
                filteredFamiliesList.setPredicate(s -> s.toLowerCase().contains(lcFilter));

                Platform.runLater(() -> {
                    fontNames.refresh();
                    fontNames.scrollTo(0);
                    families.refresh();
                    families.scrollTo(0);
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
    void defaultPressed(ActionEvent event) {

        if ( fontChangeConsumer != null ) {
            fontChangeConsumer.accept(defaultFont);
        }

        cancelPressed(event);

    }

    @FXML
    void okPressed ( ActionEvent event ) {

        if ( fontChangeConsumer != null ) {
            fontChangeConsumer.accept(getFont());
        }

        cancelPressed(event);

    }

    void setInitialConditions (
        WidgetFontPopOver popOver,
        WidgetFont originalWidgetFont,
        WidgetFont defaultWidgetFont,
        String propertyName,
        Consumer<WidgetFont> fontChangeConsumer
    ) {

        this.fontChangeConsumer = fontChangeConsumer;
        this.popOver = popOver;
        this.originalFont = originalWidgetFont;
        this.defaultFont = defaultWidgetFont;

        infoLabel.setText(MessageFormat.format(Messages.WidgetFontPopOver_Info, propertyName));

        ModelThreadPool.getExecutor().execute( ( ) -> {

            try {
                namesLoaded.await();
            } catch ( final InterruptedException iex ) {
                logger.throwing(WidgetFontPopOverController.class.getName(), "setInitialConditions[executor]", iex);
            }

            Platform.runLater(() -> setFont(originalWidgetFont));

        });

    }

    private void updateButton ( final Button button, final ButtonType buttonType ) {
        button.setText(buttonType.getText());
        ButtonBar.setButtonData(button, buttonType.getButtonData());
        button.setDefaultButton(buttonType.getButtonData().isDefaultButton());
        button.setCancelButton(buttonType.getButtonData().isCancelButton());
    }

    private class Updater<T> implements Consumer<T> {

        private final Consumer<T> consumer;
        private final AtomicBoolean updating = new AtomicBoolean(false);

        Updater( Consumer<T> consumer ) {
            this.consumer = consumer;
        }

        @Override
        public void accept ( T parameter ) {
            if ( updating.compareAndSet(false, true) ) {
                try {
                    consumer.accept(parameter);
                } finally {
                    updating.set(false);
                }
            }
        }

        boolean isUpdating() {
            return updating.get();
        }

    }

}
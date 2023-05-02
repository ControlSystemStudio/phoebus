/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * Copyright (C) 2017 by European Spallation Source ERIC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.csstudio.display.widget;


import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static java.lang.Double.MAX_VALUE;
import static javafx.scene.layout.Priority.SOMETIMES;


/**
 * @author Claudio Rosati, European Spallation Source ERIC
 * @version 1.0.0 5 Dec 2017
 */
@SuppressWarnings( "ClassWithoutLogger" )
public class ThumbWheel extends GridPane {

    public ThumbWheel(double widgetWidth,
                      double widgetHeight,
                      boolean hasNegativeSign,
                      Consumer<Number> writeValueToPV) {
        this.widgetWidth = widgetWidth;
        this.widgetHeight = widgetHeight;
        this.hasNegativeSign = hasNegativeSign;
        this.writeValueToPV = writeValueToPV;
        initialize();
    }

    private double widgetWidth;
    private double widgetHeight;
    private Consumer<Number> writeValueToPV;
    private static final Color DEFAULT_DECREMENT_BUTTON_COLOR = Color.web("#d7d7ec");
    private static final Font DEFAULT_FONT = new Label().getFont();
    private static final double DEFAULT_MARGIN = 2.0;
    private static final Color DEFAULT_INCREMENT_BUTTON_COLOR = Color.web("#ecd7d7");
    private static final String INVALID_MARK = "\u00D7";
    private static final Logger LOGGER = Logger.getLogger(ThumbWheel.class.getName());
    private static final char SIGN_MARK = '\u2013';
    private static final char SIGN_SPACE = '\u2002';
    private static final Consumer<? super Button> STYLE_CLASS_REMOVER = button -> {
        button.getStyleClass().remove("thumb-wheel-increment-spinner-button");
        button.getStyleClass().remove("thumb-wheel-decrement-spinner-button");
    };

    private String decimalRepresentation = "00";
    private final List<Button> decimalDecrementButtons = new ArrayList<>(3);
    private final List<Button> decimalIncrementButtons = new ArrayList<>(3);
    private final List<Label> decimalLabels = new ArrayList<>(2);
    private boolean hasNegativeSign;

    public void setHasNegativeSign(boolean hasNegativeSign) {
        this.hasNegativeSign = hasNegativeSign;
    }

    private boolean hasDotSeparator = true;
    private String integerRepresentation = "0";
    private final List<Button> integerDecrementButtons = new ArrayList<>(3);
    private final List<Button> integerIncrementButtons = new ArrayList<>(3);
    private final List<Label> integerLabels = new ArrayList<>(3);
    private Label separatorLabel = null;
    private Label signLabel = null;
    private DecimalFormat valueFormat = new DecimalFormat("000.00");

    /*
     * ---- handlers -----------------------------------------------------------
     */
    private final EventHandler<ActionEvent> buttonPressedHandler = event -> {
        try {
            var new_value = valueFormat.parse(valueFormat.format(getValue() + (double) ((Button) event.getSource()).getUserData())).doubleValue();
            writeValueToPV.accept(new_value);
        } catch ( ParseException ex ) {
            LOGGER.throwing(ThumbWheel.class.getSimpleName(), "buttonPressedHandler", ex);
        }
    };

    protected void setWidgetWidth(Integer new_value) {
        widgetWidth = new_value;
        setMargins();
        updateGraphics();
    }

    protected void setWidgetHeight(Integer new_value) {
        widgetHeight = new_value;
        setMargins();
        updateGraphics();
    }

    /*
     * ---- backgroundColor ----------------------------------------------------
     */
    private final ObjectProperty<Color> backgroundColor = new SimpleObjectProperty<Color>(this, "backgroundColor", Color.LIGHTGRAY) {
        @Override
        protected void invalidated() {
            if ( get() == null ) {
                set(Color.LIGHTGRAY);
            } else {
                setStyle(MessageFormat.format(
                    "-se-thumbwheel-inner-background: rgb({0,number,###}, {1,number,###}, {2,number,###});",
                    (int) ( 255 * getBackgroundColor().getRed() ),
                    (int) ( 255 * getBackgroundColor().getGreen() ),
                    (int) ( 255 * getBackgroundColor().getBlue() )
                ));
            }
        }
    };

    public Color getBackgroundColor() {
        return backgroundColor.get();
    }

    public void setBackgroundColor( Color backgroundColor ) {
        this.backgroundColor.set(backgroundColor);
    }

    /*
     * ---- decrementButtonsColor ----------------------------------------------
     */
    private final ObjectProperty<Color> decrementButtonsColor = new SimpleObjectProperty<Color>(this, "decrementButtonsColor", DEFAULT_DECREMENT_BUTTON_COLOR) {
        @Override
        protected void invalidated() {
            if ( get() == null ) {
                set(DEFAULT_DECREMENT_BUTTON_COLOR);
            } else {

                String style = createColorStyle("-fx-base", get());

                integerDecrementButtons.stream().forEach(button -> button.setStyle(style));
                decimalDecrementButtons.stream().forEach(button -> button.setStyle(style));

            }
        }
    };

    public Color getDecrementButtonsColor() {
        return decrementButtonsColor.get();
    }

    public void setDecrementButtonsColor( Color decrementButtonsColor ) {
        this.decrementButtonsColor.set(decrementButtonsColor);
    }

    /*
     * ---- decimalDigits ------------------------------------------------------
     */
    private final IntegerProperty decimalDigits = new SimpleIntegerProperty(this, "decimalDigits", 2) {
        @Override
        protected void invalidated() {
            update(true);
        }
    };

    public int getDecimalDigits() {
        return decimalDigits.get();
    }

    public void setDecimalDigits( int decimalDigits ) {
        this.decimalDigits.set(decimalDigits);
    }

    /*
     * ---- foregroundColor ----------------------------------------------------
     */
    private final ObjectProperty<Color> foregroundColor = new SimpleObjectProperty<Color>(this, "foregroundColor", Color.BLACK) {
        @Override
        protected void invalidated() {
            if ( get() == null ) {
                set(Color.BLACK);
            } else if ( !isInvalid() ) {
                changeLabelsColor(get());
            }
        }
    };

    public Color getForegroundColor() {
        return foregroundColor.get();
    }

    public void setForegroundColor( Color foregroundColor ) {
        this.foregroundColor.set(foregroundColor);
    }

    /*
     * ---- font ---------------------------------------------------------------
     */
    private final ObjectProperty<Font> font = new SimpleObjectProperty<Font>(this, "font", DEFAULT_FONT) {
        @Override
        protected void invalidated() {
            if ( get() == null ) {
                set(DEFAULT_FONT);
            } else {

                Font f = get();

                if ( signLabel != null ) {
                    signLabel.setFont(f);
                }

                if ( separatorLabel != null ) {
                    separatorLabel.setFont(f);
                }

                integerLabels.stream().forEach(label -> label.setFont(f));
                decimalLabels.stream().forEach(label -> label.setFont(f));

            }
        }
    };

    public Font getFont() {
        return font.get();
    }

    public void setFont( Font font ) {
        this.font.set(font);
    }

    /*
     * ---- graphicVisible -----------------------------------------------------
     */
    private final BooleanProperty graphicVisible = new SimpleBooleanProperty(this, "graphicVisible", false) {
        @Override
        protected void invalidated() {

            boolean gVisible = get();
            updateGraphics();

        }
    };

    public boolean isGraphicVisible() {
        return graphicVisible.get();
    }

    public void setGraphicVisible( boolean graphicVisible ) {
        this.graphicVisible.set(graphicVisible);
    }

    public void updateGraphics() {
        int iDigits = integerIncrementButtons.size();
        int dDigits = decimalDecrementButtons.size();
        double numberOfButtons = (iDigits + dDigits + (hasNegativeSign ? 0.5 : 0) + (hasDotSeparator ? 0.5 : 0));
        double buttonWidgetWidthFraction = numberOfButtons != 0 ? 1.0 / numberOfButtons : 1.0;
        double buttonWidgetHeightFraction = 0.25;

        integerIncrementButtons.stream().forEach(button -> button.setGraphic(isGraphicVisible() ? createButtonGraphic(true, widgetWidth * buttonWidgetWidthFraction, widgetHeight * buttonWidgetHeightFraction) : null));
        integerDecrementButtons.stream().forEach(button -> button.setGraphic(isGraphicVisible() ? createButtonGraphic(false, widgetWidth * buttonWidgetWidthFraction, widgetHeight * buttonWidgetHeightFraction) : null));
        decimalIncrementButtons.stream().forEach(button -> button.setGraphic(isGraphicVisible() ? createButtonGraphic(true, widgetWidth * buttonWidgetWidthFraction, widgetHeight * buttonWidgetHeightFraction) : null));
        decimalDecrementButtons.stream().forEach(button -> button.setGraphic(isGraphicVisible() ? createButtonGraphic(false, widgetWidth * buttonWidgetWidthFraction, widgetHeight * buttonWidgetHeightFraction) : null));
    }

    /*
     * ---- incrementButtonsColor ----------------------------------------------
     */
    private final ObjectProperty<Color> incrementButtonsColor = new SimpleObjectProperty<Color>(this, "incrementButtonsColor", DEFAULT_INCREMENT_BUTTON_COLOR) {
        @Override
        protected void invalidated() {
            if ( get() == null ) {
                set(DEFAULT_INCREMENT_BUTTON_COLOR);
            } else {

                String style = createColorStyle("-fx-base", get());

                integerIncrementButtons.stream().forEach(button -> button.setStyle(style));
                decimalIncrementButtons.stream().forEach(button -> button.setStyle(style));

            }
        }
    };

    public Color getIncrementButtonsColor() {
        return incrementButtonsColor.get();
    }

    public void setIncrementButtonsColor( Color incrementButtonsColor ) {
        this.incrementButtonsColor.set(incrementButtonsColor);
    }

    /*
     * ---- integerDigits ------------------------------------------------------
     */
    private final IntegerProperty integerDigits = new SimpleIntegerProperty(this, "integerDigits", 3) {
        @Override
        protected void invalidated() {
            update(true);
        }
    };

    public int getIntegerDigits() {
        return integerDigits.get();
    }

    public void setIntegerDigits( int integerDigits ) {
        this.integerDigits.set(integerDigits);
    }

    /*
     * ---- invalid ------------------------------------------------------------
     */
    private final BooleanProperty invalid = new SimpleBooleanProperty(this, "invalid", false) {
        @Override
        protected void invalidated() {
            changeLabelsColor(get() ? getInvalidColor() : getForegroundColor());
        }
    };

    public boolean isInvalid() {
        return invalid.get();
    }

    /*
     * ---- invalidColor -------------------------------------------------------
     */
    private final ObjectProperty<Color> invalidColor = new SimpleObjectProperty<Color>(this, "invalidColor", Color.RED) {
        @Override
        protected void invalidated() {
            if ( get() == null ) {
                set(Color.RED);
            } else if ( isInvalid() ) {
                changeLabelsColor(get());
            }
        }
    };

    public Color getInvalidColor() {
        return invalidColor.get();
    }

    public void setInvalidColor( Color invalidColor ) {
        this.invalidColor.set(invalidColor);
    }

    /*
     * ---- spinnerShaped ------------------------------------------------------
     */
    private final BooleanProperty spinnerShaped = new SimpleBooleanProperty(this, "spinnerShaped", false) {
        @Override
        protected void invalidated() {

            boolean sShaped = get();

            integerIncrementButtons.stream().forEach(STYLE_CLASS_REMOVER);
            integerDecrementButtons.stream().forEach(STYLE_CLASS_REMOVER);
            decimalIncrementButtons.stream().forEach(STYLE_CLASS_REMOVER);
            decimalDecrementButtons.stream().forEach(STYLE_CLASS_REMOVER);

            if ( sShaped ) {
                integerIncrementButtons.stream().forEach(button -> button.getStyleClass().add("thumb-wheel-increment-spinner-button"));
                integerDecrementButtons.stream().forEach(button -> button.getStyleClass().add("thumb-wheel-decrement-spinner-button"));
                decimalIncrementButtons.stream().forEach(button -> button.getStyleClass().add("thumb-wheel-increment-spinner-button"));
                decimalDecrementButtons.stream().forEach(button -> button.getStyleClass().add("thumb-wheel-decrement-spinner-button"));
            }

            updateGraphics();

        }
    };

    public boolean isSpinnerShaped() {
        return spinnerShaped.get();
    }

    public void setSpinnerShaped( boolean spinnerShaped ) {
        this.spinnerShaped.set(spinnerShaped);
    }

    /*
     * ---- value --------------------------------------------------------------
     */
    private final DoubleProperty value = new SimpleDoubleProperty(this, "value", 0) {
        @Override
        protected void invalidated() {
            updateValue();
            double val = get();
        }
    };

    public double getValue() {
        return value.get();
    }

    public void setValue( double currentValue ) {
        this.value.set(currentValue);
    }

    /**
     * Updates the labels color.
     *
     * @param color The new {@link Color} value.
     */
    private void changeLabelsColor ( final Color color ) {

        if ( signLabel != null ) {
            signLabel.setTextFill(color);
        }

        if ( separatorLabel != null ) {
            separatorLabel.setTextFill(color);
        }

        integerLabels.stream().forEach(label -> label.setTextFill(color));
        decimalLabels.stream().forEach(label -> label.setTextFill(color));

    }

    /**
     * @param priority     The {@link Priority) of the column.
     * @param percentWidth The percentage of the grid width occupied by the column, or {@code -1}.
     * @return A newly created {@link ColumnConstraints} object.
     */
    private ColumnConstraints createColumnConstraints ( Priority priority, double percentWidth ) {

        ColumnConstraints cc = new ColumnConstraints(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE, USE_COMPUTED_SIZE, priority, HPos.CENTER, true);

        cc.setPercentWidth(percentWidth);

        return cc;

    }

    /**
     * @param percentHeight The percentage of the grid height occupied by the row, or {@code -1}.
     * @return A newly created {@link RowConstraints} object.
     */
    private RowConstraints createRowConstraints ( double percentHeight ) {

        RowConstraints rc = new RowConstraints(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE, USE_COMPUTED_SIZE, SOMETIMES, VPos.CENTER, true);

        rc.setPercentHeight(percentHeight);

        return rc;

    }

    /**
     * Return a configured {@link Button}, taking it from the given {@code pool}
     * or creating it from scratch.
     *
     * @param color           The button {@link Color}.
     * @param valueOffset     The value to be added to current value.
     * @param pool            The pool of already existing {@link Button}s to be recycled.
     * @return A configured {@link Button}.
     */
    private Button createButton(Color color, double valueOffset, Stack<Button> pool ) {

        Button button;

        if ( pool.isEmpty() ) {
            
            button = new Button();

            button.setOnAction(buttonPressedHandler);

        } else {
            button = pool.pop();
        }

        button.setGraphicTextGap(0);
        button.setMaxSize(MAX_VALUE, MAX_VALUE);
        button.setMinSize(0, 0);
        button.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        button.setStyle(createColorStyle("-fx-base", color));
        button.setUserData(valueOffset);
        button.getStyleClass().remove("thumb-wheel-increment-spinner-button");
        button.getStyleClass().remove("thumb-wheel-decrement-spinner-button");

        return button;
    }

    /**
     * Create the button graphic to be used inside a button.
     *
     * @param incrementButton {@code true} if the {@link Button} is for increment.
     * @return A proper mark {@code node}.
     */
    private Node createButtonGraphic (boolean incrementButton, double buttonWidth, double buttonHeight) {

        Region node = new Region();

        node.setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
        node.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);

        double buttonSize = Math.min(buttonWidth, buttonHeight);
        // If the buttons are perfectly square, the graphics-width and -height are 1/3 of the buttons' width and height;
        // if the buttons are not square, the graphics-width and -height tend towards 2/3 of the buttons' width and height
        // as the buttons' width and height tend toward a less and less balanced ratio:
        double graphicsProportion = ((buttonWidth + buttonHeight) / (3.0 * Math.abs(buttonWidth - buttonHeight) + (buttonWidth + buttonHeight))) * ((1.0 / 3.0) + (3.0 * Math.abs(buttonWidth - buttonHeight) / (buttonWidth + buttonHeight)) * (2.0 / 3.0));

        if (isSpinnerShaped()) {
            node.setPrefSize(buttonSize * graphicsProportion, buttonSize * graphicsProportion);
            if (incrementButton) {
                node.getStyleClass().add("thumb-wheel-increment-arrow");
            } else {
                node.getStyleClass().add("thumb-wheel-decrement-arrow");
            }

        } else {
            if (incrementButton) {
                node.setPrefSize(buttonSize * graphicsProportion, buttonSize * graphicsProportion);
                node.getStyleClass().add("thumb-wheel-increment-mark");
            } else {
                node.setPrefSize(buttonSize * graphicsProportion, (2.0/5.0) * buttonSize * graphicsProportion);
                node.getStyleClass().add("thumb-wheel-decrement-mark");
            }
        }

        return node;

    }

    /**
     * Build a properly formatted CSS color property.
     *
     * @param style The color property style name (e.g. {@code "-fx-color"}).
     * @param color The JavaFX {@link Color}.
     * @return A properly formatted CSS color style string.
     */
    private String createColorStyle ( String style, Color color ) {
        if ( color.isOpaque() ) {
            return MessageFormat.format(
                "{0}: rgb({1,number,###}, {2,number,###}, {3,number,###});",
                style,
                (int) ( 255 * color.getRed() ),
                (int) ( 255 * color.getGreen() ),
                (int) ( 255 * color.getBlue() )
            );
        } else {
            return MessageFormat.format(
                "{0}: rgba({1,number,###}, {2,number,###}, {3,number,###}, {4,number,###});",
                style,
                (int) ( 255 * color.getRed() ),
                (int) ( 255 * color.getGreen() ),
                (int) ( 255 * color.getBlue() ),
                (int) ( 255 * color.getOpacity())
            );
        }
    }

    /**
     * Return a configured {@link Label}, taking it from the given {@code pool}
     * or creating it from scratch.
     *
     * @param character The label single character..
     * @param pool      The pool of already existing {@link Button}s to be recycled.
     * @return A configured {@link Label}.
     */
    private Label createLabel( char character, Stack<Label> pool ) {

        Label label;

        if ( pool.isEmpty() ) {

            label = new Label();

        } else {
            label = pool.pop();
        }

        label.setAlignment(Pos.CENTER);
        label.setFont(getFont());
        label.setMaxSize(MAX_VALUE, MAX_VALUE);
        label.setMinSize(0, 0);
        label.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        label.setText(String.valueOf(character));
        label.setTextFill(isInvalid() ? getInvalidColor() : getForegroundColor());

        return label;

    }

    /**
     * Initialize the component, binding various properties.
     */
    private void initialize() {

        getStylesheets().add(getClass().getResource("/styles/thumbwheel.css").toExternalForm());
        getStyleClass().add("thumb-wheel");
        setStyle(createColorStyle("-se-thumbwheel-inner-background", getBackgroundColor()));

        setMargins();

        ObservableList<RowConstraints> rowConstraints = getRowConstraints();

        rowConstraints.add(createRowConstraints(25));
        rowConstraints.add(createRowConstraints(50));
        rowConstraints.add(createRowConstraints(25));

        update(true);

    }

    private void setMargins() {
        double widgetDimension = Math.min(widgetWidth, widgetHeight);
        double margin = Math.max(DEFAULT_MARGIN, 0.01 * widgetDimension);

        setHgap(margin);
        setVgap(margin);
        setPadding(new Insets(margin + 1));
    }

    /**
     * Update this pane.
     *
     * @param updateChildren {@code true} if the children must be updated too.
     */
    protected void update( boolean updateChildren ) {

        int iDigits = getIntegerDigits();
        int dDigits = getDecimalDigits();
        StringBuilder builder = new StringBuilder(iDigits + 1 + dDigits);
        StringBuilder extremaBuilder = new StringBuilder(iDigits + 1 + dDigits);

        for ( int i = 0; i < iDigits; i++ ) {
            builder.append('0');
            extremaBuilder.append('9');
        }

        if ( dDigits > 0 ) {

            hasDotSeparator = dDigits > 0;

            builder.append('.');
            extremaBuilder.append('.');
            
            for ( int i = 0; i < dDigits; i++ ) {
                builder.append('0');
                extremaBuilder.append('9');
            }

        } else {
            hasDotSeparator = false;
        }

        String format = builder.toString();

        valueFormat = new DecimalFormat(format);

        if ( updateChildren ) {
            updateLayout();
        }

        updateValue();

    }

    @SuppressWarnings( "ValueOfIncrementOrDecrementUsed" )
    private void updateLayout() {

        //  --------------------------------------------------------------------
        //  Collect existing labes for recycling.
        //
        Stack<Label> labelsPool = new Stack<>();

        if (signLabel != null) {

            labelsPool.add(signLabel);

            signLabel = null;

        }

        if (separatorLabel != null) {

            labelsPool.add(separatorLabel);

            separatorLabel = null;

        }

        labelsPool.addAll(integerLabels);
        labelsPool.addAll(decimalLabels);

        integerLabels.clear();
        decimalLabels.clear();

        //  --------------------------------------------------------------------
        //  Collect existing buttons for recycling.
        //
        Stack<Button> buttonsPool = new Stack<>();

        buttonsPool.addAll(integerIncrementButtons);
        buttonsPool.addAll(integerDecrementButtons);
        buttonsPool.addAll(decimalIncrementButtons);
        buttonsPool.addAll(decimalDecrementButtons);

        integerIncrementButtons.clear();
        integerDecrementButtons.clear();
        decimalIncrementButtons.clear();
        decimalDecrementButtons.clear();

        //  --------------------------------------------------------------------
        //  Clear current children and column constraints.
        //
        ObservableList<ColumnConstraints> columnConstraints = getColumnConstraints();
        ObservableList<Node> children = getChildren();

        columnConstraints.clear();
        children.clear();

        //  --------------------------------------------------------------------
        //  Create new children and layout them.
        //
        int iDigits = getIntegerDigits();
        int dDigits = getDecimalDigits();
        double digitPercentWidth = 100.0 / (iDigits + dDigits + (hasNegativeSign ? .5 : 0) + (hasDotSeparator ? .5 : 0));
        int columnIndex = 0;

        if (hasNegativeSign) {

            signLabel = createLabel(SIGN_SPACE, labelsPool);

            columnConstraints.add(createColumnConstraints(SOMETIMES, -1));
            add(signLabel, columnIndex++, 1);

        }

        for (int i = 0; i < iDigits; i++) {

            double valueOffset = Math.pow(10, iDigits - i - 1);
            Button iButton = createButton(getIncrementButtonsColor(), valueOffset, buttonsPool);
            Label label = createLabel('0', labelsPool);
            Button dButton = createButton(getDecrementButtonsColor(), -valueOffset, buttonsPool);

            integerIncrementButtons.add(iButton);
            integerLabels.add(label);
            integerDecrementButtons.add(dButton);

            columnConstraints.add(createColumnConstraints(SOMETIMES, digitPercentWidth));
            add(iButton, columnIndex, 0);
            add(label, columnIndex, 1);
            add(dButton, columnIndex, 2);

            if (isSpinnerShaped()) {
                iButton.getStyleClass().add(true ? "thumb-wheel-increment-spinner-button" : "thumb-wheel-decrement-spinner-button");
                dButton.getStyleClass().add(false ? "thumb-wheel-increment-spinner-button" : "thumb-wheel-decrement-spinner-button");
            }

            columnIndex++;
        }

        if (hasDotSeparator) {

            separatorLabel = createLabel(valueFormat.getDecimalFormatSymbols().getDecimalSeparator(), labelsPool);

            columnConstraints.add(createColumnConstraints(SOMETIMES, -1));
            add(separatorLabel, columnIndex++, 1);

            for (int i = 0; i < dDigits; i++) {

                double valueOffset = Math.pow(10, -i - 1);
                Button iButton = createButton(getIncrementButtonsColor(), valueOffset, buttonsPool);
                Label label = createLabel('0', labelsPool);
                Button dButton = createButton(getDecrementButtonsColor(), -valueOffset, buttonsPool);

                decimalIncrementButtons.add(iButton);
                decimalLabels.add(label);
                decimalDecrementButtons.add(dButton);

                columnConstraints.add(createColumnConstraints(SOMETIMES, digitPercentWidth));
                add(iButton, columnIndex, 0);
                add(label, columnIndex, 1);
                add(dButton, columnIndex, 2);

                if (isSpinnerShaped()) {
                    iButton.getStyleClass().add(true ? "thumb-wheel-increment-spinner-button" : "thumb-wheel-decrement-spinner-button");
                    dButton.getStyleClass().add(false ? "thumb-wheel-increment-spinner-button" : "thumb-wheel-decrement-spinner-button");
                }

                columnIndex++;

            }

        }

        updateGraphics();

    }

    private void updateValue() {

        //  --------------------------------------------------------------------
        //  Create string representation of integer and decimal parts.
        //
        double val = getValue();
        int iDigits = getIntegerDigits();
        int dDigits = getDecimalDigits();

        try {

            double fValue = valueFormat.parse(valueFormat.format(val)).doubleValue();
            String sValue = valueFormat.format(Math.abs(fValue));


            if ( hasDotSeparator ) {

                int dotIndex = sValue.indexOf(valueFormat.getDecimalFormatSymbols().getDecimalSeparator());

                integerRepresentation = sValue.substring(0, dotIndex);
                decimalRepresentation = sValue .substring(1 + dotIndex);

            } else {
                integerRepresentation = sValue;
                decimalRepresentation = "";
            }

        } catch ( ParseException ex ) {
            LOGGER.throwing(ThumbWheel.class.getSimpleName(), "updateValue", ex);
        }

        //  --------------------------------------------------------------------
        //  Update labels.
        //

        if (integerRepresentation.length() > integerLabels.size() || (!hasNegativeSign && val < 0)) {
            // Value cannot be represented:
            if (hasNegativeSign) {
                signLabel.setTextFill(invalidColor.getValue());
                signLabel.setText(String.valueOf(( val < 0 ) ? SIGN_MARK : SIGN_SPACE));
            }

            for (int i = 0; i < iDigits; i++) {
                Label integerLabel = integerLabels.get(i);
                integerLabel.setTextFill(invalidColor.get());
                integerLabel.setText(INVALID_MARK);
            }

            for (int i = 0; i < dDigits; i++) {
                Label decimalLabel = decimalLabels.get(i);
                decimalLabel.setTextFill(invalidColor.getValue());
                decimalLabel.setText(INVALID_MARK);
            }
        }
        else {
            if ( hasNegativeSign ) {
                signLabel.setTextFill(foregroundColor.getValue());
                signLabel.setText(String.valueOf(( val < 0 ) ? SIGN_MARK : SIGN_SPACE));
            }

            for ( int i = 0; i < iDigits; i++ ) {
                Label integerLabel = integerLabels.get(i);
                integerLabel.setTextFill(foregroundColor.getValue());
                integerLabel.setText(String.valueOf(integerRepresentation.charAt(i)));
            }

            for ( int i = 0; i < dDigits; i++ ) {
                Label decimalLabel = decimalLabels.get(i);
                decimalLabel.setTextFill(foregroundColor.getValue());
                decimalLabel.setText(String.valueOf(decimalRepresentation.charAt(i)));
            }
        }
    }
}

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
import javafx.beans.property.ReadOnlyBooleanProperty;
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
import javafx.scene.input.ScrollEvent;
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

    private static final Color DEFAULT_DECREMENT_BUTTON_COLOR = Color.web("#d7d7ec");
    private static final Font DEFAULT_FONT = new Label().getFont();
    private static final double DEFAULT_HGAP = 2.0;
    private static final Color DEFAULT_INCREMENT_BUTTON_COLOR = Color.web("#ecd7d7");
    private static final char INVALID_MARK = '\u00D7';
    private static final Logger LOGGER = Logger.getLogger(ThumbWheel.class.getName());
    private static final char SIGN_MARK = '\u2013';
    private static final char SIGN_SPACE = '\u2002';
    private static final double SPINNER_HGAP = 0;
    private static final Consumer<? super Button> STYLE_CLASS_REMOVER = button -> {
        button.getStyleClass().remove("thumb-wheel-increment-spinner-button");
        button.getStyleClass().remove("thumb-wheel-decrement-spinner-button");
    };

    private String decimalRepresentation = "00";
    private final List<Button> decimalDecrementButtons = new ArrayList<>(3);
    private final List<Button> decimalIncrementButtons = new ArrayList<>(3);
    private final List<Label> decimalLabels = new ArrayList<>(2);
    private double effectiveMax = 100;
    private double effectiveMin = 0;
    private boolean hasNegativeSign = false;
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
            setValue(valueFormat.parse(valueFormat.format(getValue() + (double) ((Button) event.getSource()).getUserData())).doubleValue());
        } catch ( ParseException ex ) {
            LOGGER.throwing(ThumbWheel.class.getSimpleName(), "labelScrollHandler", ex);
        }
    };
    private final EventHandler<ScrollEvent> labelScrollHandler = event -> {

        int index = integerLabels.indexOf(event.getSource());
        double deltaY = event.getDeltaY();

        if ( isScrollEnabled() ) {
            try {
                if ( index >= 0 ) {
                    if ( deltaY > 0 ) {
                        setValue(valueFormat.parse(valueFormat.format(getValue() + (double) integerDecrementButtons.get(index).getUserData())).doubleValue());
                    } else {
                        setValue(valueFormat.parse(valueFormat.format(getValue() + (double) integerIncrementButtons.get(index).getUserData())).doubleValue());
                    }
                } else {

                    index = decimalLabels.indexOf(event.getSource());

                    if ( index >= 0 ) {
                        if ( deltaY > 0 ) {
                            setValue(valueFormat.parse(valueFormat.format(getValue() + (double) decimalDecrementButtons.get(index).getUserData())).doubleValue());
                        } else {
                            setValue(valueFormat.parse(valueFormat.format(getValue() + (double) decimalIncrementButtons.get(index).getUserData())).doubleValue());
                        }
                    }

                }
            } catch ( ParseException ex ) {
                LOGGER.throwing(ThumbWheel.class.getSimpleName(), "labelScrollHandler", ex);
            }
        }

    };

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

    public ObjectProperty<Color> backgroundColorProperty() {
        return backgroundColor;
    }

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

    public ObjectProperty<Color> decrementButtonsColorProperty() {
        return decrementButtonsColor;
    }

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

            int val = get();

            if ( needsClamping(val, 0, Byte.MAX_VALUE) ) {

                val = clamp(get(), 0, Byte.MAX_VALUE);

                set(val);

            } else {
                update(true);
            }

        }
    };

    public IntegerProperty decimalDigitsProperty() {
        return decimalDigits;
    }

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

    public ObjectProperty<Color> foregroundColorProperty() {
        return foregroundColor;
    }

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

    public ObjectProperty<Font> fontProperty() {
        return font;
    }

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

            integerIncrementButtons.stream().forEach(button -> button.setGraphic(gVisible ? createButtonGraphic(true) : null));
            integerDecrementButtons.stream().forEach(button -> button.setGraphic(gVisible ? createButtonGraphic(false) : null));
            decimalIncrementButtons.stream().forEach(button -> button.setGraphic(gVisible ? createButtonGraphic(true) : null));
            decimalDecrementButtons.stream().forEach(button -> button.setGraphic(gVisible ? createButtonGraphic(false) : null));

        }
    };

    public BooleanProperty graphicVisibleProperty() {
        return graphicVisible;
    }

    public boolean isGraphicVisible() {
        return graphicVisible.get();
    }

    public void setGraphicVisible( boolean graphicVisible ) {
        this.graphicVisible.set(graphicVisible);
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

    public ObjectProperty<Color> incrementButtonsColorProperty() {
        return incrementButtonsColor;
    }

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

            int val = get();

            if ( needsClamping(val, 1, Byte.MAX_VALUE) ) {

                val = clamp(get(), 1, Byte.MAX_VALUE);

                set(val);

            } else {
                update(true);
            }

        }
    };

    public IntegerProperty integerDigitsProperty() {
        return integerDigits;
    }

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

    public ReadOnlyBooleanProperty invalidProperty() {
        return invalid;
    }

    public boolean isInvalid() {
        return invalid.get();
    }

    private void setInvalid( boolean invalid ) {
        this.invalid.set(invalid);
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

    public ObjectProperty<Color> invalidColorProperty() {
        return invalidColor;
    }

    public Color getInvalidColor() {
        return invalidColor.get();
    }

    public void setInvalidColor( Color invalidColor ) {
        this.invalidColor.set(invalidColor);
    }

    /*
     * ---- maxValue -----------------------------------------------------------
     */
    private final DoubleProperty maxValue = new SimpleDoubleProperty(this, "maxValue", effectiveMax) {
        @Override
        protected void invalidated() {

            double val = get();
            double min = getMinValue();

            if ( needsClamping(val, min, Double.MAX_VALUE) ) {

                val = clamp(val, min, Double.MAX_VALUE);

                set(val);

            } else {

                double cur = getValue();

                if ( needsClamping(cur, min, val) ) {
                    setValue(clamp(cur, min, val));
                } else {
                    update(false);
                }

            }

        }
    };

    public DoubleProperty maxValueProperty() {
        return maxValue;
    }

    public double getMaxValue() {
        return maxValue.get();
    }

    public void setMaxValue( double maxValue ) {
        this.maxValue.set(maxValue);
    }

    /*
     * ---- minValue -----------------------------------------------------------
     */
    private final DoubleProperty minValue = new SimpleDoubleProperty(this, "minValue", effectiveMin) {
        @Override
        protected void invalidated() {

            double val = get();
            double max = getMaxValue();

            if ( needsClamping(val, - Double.MAX_VALUE, max) ) {

                val = clamp(val, - Double.MAX_VALUE, max);

                set(val);

            } else {

                double cur = getValue();

                if ( needsClamping(cur, val, max) ) {
                    setValue(clamp(cur, val, max));
                } else {
                    update(true);
                }

            }

        }
    };

    public DoubleProperty minValueProperty() {
        return minValue;
    }

    public double getMinValue() {
        return minValue.get();
    }

    public void setMinValue( double minValue ) {
        this.minValue.set(minValue);
    }

    /*
     * ---- scrollEnabled ------------------------------------------------------
     */
    private final BooleanProperty scrollEnabled = new SimpleBooleanProperty(this, "scrollEnabled", false);

    public BooleanProperty scrollEnabledProperty() {
        return scrollEnabled;
    }

    public boolean isScrollEnabled() {
        return scrollEnabled.get();
    }

    public void setScrollEnabled( boolean scrollEnabled ) {
        this.scrollEnabled.set(scrollEnabled);
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
                setHgap(SPINNER_HGAP);
                integerIncrementButtons.stream().forEach(button -> button.getStyleClass().add("thumb-wheel-increment-spinner-button"));
                integerDecrementButtons.stream().forEach(button -> button.getStyleClass().add("thumb-wheel-decrement-spinner-button"));
                decimalIncrementButtons.stream().forEach(button -> button.getStyleClass().add("thumb-wheel-increment-spinner-button"));
                decimalDecrementButtons.stream().forEach(button -> button.getStyleClass().add("thumb-wheel-decrement-spinner-button"));
            } else {
                setHgap(DEFAULT_HGAP);
            }

            if ( isGraphicVisible() ) {
                integerIncrementButtons.stream().forEach(button -> button.setGraphic(createButtonGraphic(true)));
                integerDecrementButtons.stream().forEach(button -> button.setGraphic(createButtonGraphic(false)));
                decimalIncrementButtons.stream().forEach(button -> button.setGraphic(createButtonGraphic(true)));
                decimalDecrementButtons.stream().forEach(button -> button.setGraphic(createButtonGraphic(false)));
            }

        }
    };

    public BooleanProperty spinnerShapedProperty() {
        return spinnerShaped;
    }

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

            double val = get();
            double min = getMinValue();
            double max = getMaxValue();

            if ( needsClamping(val, min, max) ) {

                val = clamp(val, min, max);

                set(val);

            } else {
                updateValue();
            }

        }
    };

    public DoubleProperty valueProperty() {
        return value;
    }

    public double getValue() {
        return value.get();
    }

    public void setValue( double currentValue ) {
        this.value.set(currentValue);
    }

    /*
     * ---- instance initializer -----------------------------------------------
     */
    {
        initialize();
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
     * Clamp the given {@code value} inside a range defined by the given minimum
     * and maximum values.
     *
     * @param value The value to be clamped.
     * @param min   The clamp range minimum value.
     * @param max   The clamp range maximum value.
     * @return {@code value} if it's inside the range, otherwise {@code min} if
     *         {@code value} is below the range, or {@code max} if above the range.
     */
    private double clamp ( final double value, final double min, final double max ) {
        if ( value < min ) {
            return min;
        } else if ( value > max ) {
            return max;
        } else {
            return value;
        }
    }

    /**
     * Clamp the given {@code value} inside a range defined by the given minimum
     * and maximum values.
     *
     * @param value The value to be clamped.
     * @param min   The clamp range minimum value.
     * @param max   The clamp range maximum value.
     * @return {@code value} if it's inside the range, otherwise {@code min} if
     *         {@code value} is below the range, or {@code max} if above the range.
     */
    private int clamp ( final int value, final int min, final int max ) {
        if ( value < min ) {
            return min;
        } else if ( value > max ) {
            return max;
        } else {
            return value;
        }
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
     * @param incrementButton {@code true} if the {@link Button} is for increment.
     * @param color           The button {@link Color}.
     * @param valueOffset     The value to be added to current value.
     * @param pool            The pool of already existing {@link Button}s to be recycled.
     * @return A configured {@link Button}.
     */
    private Button createButton( boolean incrementButton, Color color, double valueOffset, Stack<Button> pool ) {

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
        button.setGraphic(isGraphicVisible() ? createButtonGraphic(incrementButton) : null);
        button.getStyleClass().remove("thumb-wheel-increment-spinner-button");
        button.getStyleClass().remove("thumb-wheel-decrement-spinner-button");

        if ( isSpinnerShaped() ) {
            button.getStyleClass().add(incrementButton ? "thumb-wheel-increment-spinner-button" : "thumb-wheel-decrement-spinner-button");
        }

        return button;

    }

    /**
     * Create the button graphic to be used inside a button.
     *
     * @param incrementButton {@code true} if the {@link Button} is for increment.
     * @return A proper mark {@code node}.
     */
    private Node createButtonGraphic ( boolean incrementButton ) {

        Region node = new Region();

        node.setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
        node.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);

        if ( isSpinnerShaped() ) {

            node.setPrefSize(9, 5);

            if ( incrementButton ) {
                node.getStyleClass().add("thumb-wheel-increment-arrow");
            } else {
                node.getStyleClass().add("thumb-wheel-decrement-arrow");
            }

        } else {
            if ( incrementButton ) {
                node.setPrefSize(5, 5);
                node.getStyleClass().add("thumb-wheel-increment-mark");
            } else {
                node.setPrefSize(5, 2);
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

            label.setOnScroll(labelScrollHandler);

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

        setHgap(isSpinnerShaped() ? SPINNER_HGAP : DEFAULT_HGAP);
        setPadding(new Insets(3));

        ObservableList<RowConstraints> rowConstraints = getRowConstraints();

        rowConstraints.add(createRowConstraints(25));
        rowConstraints.add(createRowConstraints(50));
        rowConstraints.add(createRowConstraints(25));

        update(true);

    }

    /**
     * Tell if the given {@code value} needs to be clamped into the range defined
     * by the given minimum and maximum values.
     *
     * @param value The value to be tested.
     * @param min   The clamp range minimum value.
     * @param max   The clamp range maximum value.
     * @return {@code false} if the given value is inside the range, {@code true}
     *         if it needs to be clamped.
     */
    private boolean needsClamping ( final double value, final double min, final double max ) {
        return ( value < min ||  value > max );
    }

    /**
     * Tell if the given {@code value} needs to be clamped into the range defined
     * by the given minimum and maximum values.
     *
     * @param value The value to be tested.
     * @param min   The clamp range minimum value.
     * @param max   The clamp range maximum value.
     * @return {@code false} if the given value is inside the range, {@code true}
     *         if it needs to be clamped.
     */
    private boolean needsClamping ( final int value, final int min, final int max ) {
        return ( value < min ||  value > max );
    }

    /**
     * Update this pane.
     *
     * @param updateChildren {@code true} if the children must be updated too.
     */
    private void update( boolean updateChildren ) {

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

        double min = getMinValue();
        double max = getMaxValue();

        hasNegativeSign = ( min < 0 );

        double eMax = Double.parseDouble(extremaBuilder.toString());

        effectiveMax = Math.min(max, eMax);

        double eMin = hasNegativeSign ? -eMax : 0;

        effectiveMin = Math.max(min, eMin);

//        LOGGER.info(MessageFormat.format(
//            "Something updated"
//                + "\n         Value: {0,number,###############0.###############}"
//                + "\n           Min: {1,number,###############0.###############}"
//                + "\n           Max: {2,number,###############0.###############}"
//                + "\nInteger Digits: {3,number,###############0}"
//                + "\nDecimal Digits: {4,number,###############0}"
//                + "\n Negative Sign: {5}"
//                + "\n Dot Separator: {6}"
//                + "\n        Format: {7}"
//                + "\n Effective Min: {8,number,###############0.###############}"
//                + "\n Effective Max: {9,number,###############0.###############}"
//                + "\nInteger String: {10}"
//                + "\nDecimal String: {11}",
//            getValue(),
//            min,
//            max,
//            iDigits,
//            dDigits,
//            hasNegativeSign,
//            hasDotSeparator,
//            format,
//            effectiveMin,
//            effectiveMax,
//            integerRepresentation,
//            decimalRepresentation
//        ));

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

        if ( signLabel != null ) {

            labelsPool.add(signLabel);

            signLabel = null;

        }

        if ( separatorLabel != null ) {

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
        double digitPercentWidth = 100.0 / ( iDigits + dDigits + ( hasNegativeSign ? .5 : 0) + ( hasDotSeparator ? .5 : 0));
        int columnIndex = 0;

        if ( hasNegativeSign ) {

            signLabel = createLabel(SIGN_SPACE, labelsPool);

            columnConstraints.add(createColumnConstraints(SOMETIMES, -1));
            add(signLabel, columnIndex++, 1);

        }

        for ( int i = 0; i < iDigits; i++ ) {

            double valueOffset = Math.pow(10, iDigits - i - 1);
            Button iButton = createButton(true, getIncrementButtonsColor(), valueOffset, buttonsPool);
            Label label = createLabel('0', labelsPool);
            Button dButton = createButton(false, getDecrementButtonsColor(), - valueOffset, buttonsPool);

            integerIncrementButtons.add(iButton);
            integerLabels.add(label);
            integerDecrementButtons.add(dButton);

            columnConstraints.add(createColumnConstraints(SOMETIMES, digitPercentWidth));
            add(iButton, columnIndex, 0);
            add(label, columnIndex, 1);
            add(dButton, columnIndex, 2);

            columnIndex++;

        }

        if ( hasDotSeparator ) {

            separatorLabel = createLabel(valueFormat.getDecimalFormatSymbols().getDecimalSeparator(), labelsPool);

            columnConstraints.add(createColumnConstraints(SOMETIMES, -1));
            add(separatorLabel, columnIndex++, 1);

            for ( int i = 0; i < dDigits; i++ ) {

                double valueOffset = Math.pow(10, - i - 1);
                Button iButton = createButton(true, getIncrementButtonsColor(), valueOffset, buttonsPool);
                Label label = createLabel('0', labelsPool);
                Button dButton = createButton(false, getDecrementButtonsColor(), - valueOffset, buttonsPool);

                decimalIncrementButtons.add(iButton);
                decimalLabels.add(label);
                decimalDecrementButtons.add(dButton);

                columnConstraints.add(createColumnConstraints(SOMETIMES, digitPercentWidth));
                add(iButton, columnIndex, 0);
                add(label, columnIndex, 1);
                add(dButton, columnIndex, 2);

                columnIndex++;

            }

        }

    }

    private void updateValue() {

        //  --------------------------------------------------------------------
        //  Create string representation of integer and decimal parts.
        //
        double val = getValue();
        int iDigits = getIntegerDigits();
        int dDigits = getDecimalDigits();
        StringBuilder builder = new StringBuilder(iDigits + 1 + dDigits);

        setInvalid(val > effectiveMax || val < effectiveMin);

        if ( isInvalid() ) {

            builder = new StringBuilder(iDigits);

            for ( int i = 0; i < iDigits; i++ ) {
                builder.append(INVALID_MARK);
            }

            integerRepresentation = builder.toString();

            if ( hasDotSeparator ) {

                builder = new StringBuilder(dDigits);

                for ( int i = 0; i < dDigits; i++ ) {
                    builder.append(INVALID_MARK);
                }

                decimalRepresentation = builder.toString();

            } else {
                decimalRepresentation = "";
            }

        } else {

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

        }

        //  --------------------------------------------------------------------
        //  Update labels.
        //
        if ( hasNegativeSign ) {
            signLabel.setText(String.valueOf(( val < 0 ) ? SIGN_MARK : SIGN_SPACE));
        }

        for ( int i = 0; i < iDigits; i++ ) {
            integerLabels.get(i).setText(String.valueOf(integerRepresentation.charAt(i)));
        }

        for ( int i = 0; i < dDigits; i++ ) {
            decimalLabels.get(i).setText(String.valueOf(decimalRepresentation.charAt(i)));
        }

    }

}

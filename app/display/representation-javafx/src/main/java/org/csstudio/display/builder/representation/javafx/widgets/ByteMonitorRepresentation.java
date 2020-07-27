/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import java.util.List;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.properties.StringWidgetProperty;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.model.widgets.ByteMonitorWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.epics.vtype.VType;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.transform.Rotate;

/** Creates JavaFX item for model widget
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class ByteMonitorRepresentation extends RegionBaseRepresentation<Pane, ByteMonitorWidget>
{
    private final DirtyFlag dirty_config = new DirtyFlag();
    private final DirtyFlag dirty_content = new DirtyFlag();
    private final UntypedWidgetPropertyListener lookChangedListener = this::lookChanged;
    private final UntypedWidgetPropertyListener configChangedListener = this::configChanged;
    private final WidgetPropertyListener<VType> contentChangedListener = this::contentChanged;
    private final WidgetPropertyListener<List<StringWidgetProperty>> labelsChangedListener = this::labelsChanged;
    private final WidgetPropertyListener<Boolean> orientationChangedListener = this::orientationChanged;
    private final WidgetPropertyListener<Integer> sizeChangedListener = this::sizeChanged;

    private volatile Color[] colors;

    private volatile Color[] value_colors = null;

    private volatile int startBit = 0;
    private volatile int numBits = 8;
    private volatile boolean bitReverse = false;
    private volatile boolean horizontal = true;
    private volatile boolean square_led = false;

    private volatile Shape[] leds = null;
    private volatile Label[] labels = null;


    @Override
    protected Pane createJFXNode() throws Exception
    {
        colors = createColors();
        final Pane pane = new Pane();
        numBits = model_widget.propNumBits().getValue();
        square_led = model_widget.propSquare().getValue();
        horizontal = model_widget.propHorizontal().getValue();
        addLEDs(pane);
        pane.setManaged(false);
        return pane;
    }

    @Override
    public int[] getBorderRadii()
    {
        if (square_led)
            return null;
        final int r;
        if (horizontal)
            r = model_widget.propWidth().getValue() / numBits / 2;
        else
            r = model_widget.propHeight().getValue() / numBits / 2;
        return new int[] { r, r };
    }

    private void addLEDs(final Pane pane)
    {
        addLEDs(pane, model_widget.propWidth().getValue(),
                model_widget.propHeight().getValue(), horizontal);
    }

    private void addLEDs(final Pane pane, final double w, final double h, final boolean horizontal)
    {
        final Color text_color = JFXUtil.convert(model_widget.propForegroundColor().getValue());
        final Font text_font = JFXUtil.convert(model_widget.propFont().getValue());
        final int save_bits = numBits;
        final boolean save_sq = square_led;
        final Color [] save_colorVals = value_colors;
        final Shape [] leds = new Shape[save_bits];
        final Label [] labels = new Label[save_bits];
        final double gap = 4;
        double led_w, led_h, dx, dy, rad;
        if (horizontal)
        {
            dx = w / save_bits;
            dy = 0;
            led_w = dx;
            led_h = h;
            rad = led_w/2;
        }
        else
        {
            dx = 0;
            dy = h / save_bits;
            led_w = w;
            led_h = dy;
            rad = led_h/2;
        }
        double x = 0.0, y = 0.0;
        for (int i = 0; i < save_bits; i++)
        {
            final Label label;
            final int lbl_index = bitReverse ? i : save_bits - i - 1;
            if (lbl_index < model_widget.propLabels().size())
            {
                label = new Label(model_widget.propLabels().getElement(lbl_index).getValue());
                label.getStyleClass().add("led_label");
                label.setFont(text_font);
                label.setTextFill(text_color);
                label.setManaged(false);
            }
            else
                label = null;

            final Shape led;
            if (save_sq)
            {
                final Rectangle rect = new Rectangle();
                rect.setX(x);
                rect.setY(y);
                rect.setWidth(led_w);
                rect.setHeight(led_h);
                led = rect;
                if (label != null)
                {
                    label.relocate(x, y);
                    label.resize(led_w, led_h);
                    label.setAlignment(Pos.CENTER);
                    if (horizontal)
                        label.setRotate(-90);
                }
            }
            else
            {
                final Ellipse ell = new Ellipse();
                ell.setCenterX(x + rad);
                ell.setCenterY(y + rad);
                ell.setRadiusX(rad);
                ell.setRadiusY(rad);
                led = ell;
                if (label != null)
                {
                    if (horizontal)
                    {
                        label.getTransforms().setAll(new Rotate(-90.0));
                        // label.setBackground(new Background(new BackgroundFill(Color.BISQUE, CornerRadii.EMPTY, Insets.EMPTY)));
                        label.relocate(x, y+led_h);
                        label.resize(led_h - 2*rad - gap, led_w);
                        label.setAlignment(Pos.CENTER_RIGHT);
                    }
                    else
                    {
                        label.relocate(x+2*rad+gap, y);
                        label.resize(led_w-2*rad-gap, led_h);
                    }
                }
            }
            led.getStyleClass().add("led");
            led.setManaged(false);
            if (save_colorVals != null && i < save_colorVals.length)
                led.setFill(toolkit.isEditMode() ? computeEditColors() : save_colorVals[i]);

            leds[i] = led;
            labels[i] = label;
            if (label != null)
                label.layout();
            x += dx;
            y += dy;
        }
        this.leds = leds;
        this.labels = labels;
        pane.getChildren().setAll(leds);
        for (Label label : labels)
            if (label != null)
                pane.getChildren().add(label);
    }

    protected Color[] createColors()
    {
        return new Color[]
        {
            JFXUtil.convert(model_widget.propOffColor().getValue()),
            JFXUtil.convert(model_widget.propOnColor().getValue())
        };
    }

    protected int [] computeColorIndices(final VType value)
    {
        int nBits = numBits;
        final int sBit = startBit;
        if (nBits + sBit > 32)
            nBits = 32 - sBit;
        final boolean save_bitRev = bitReverse;

        final int [] colorIndices = new int [nBits];
        final long number = Integer.toUnsignedLong(VTypeUtil.getValueNumber(value).intValue());
        for (int i = 0; i < nBits; i++)
            colorIndices[ save_bitRev ? i : nBits-1-i] = (number & (1L << (sBit+i))) == 0 ? 0 : 1;
        return colorIndices;
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.propWidth().addPropertyListener(sizeChangedListener);
        model_widget.propHeight().addPropertyListener(sizeChangedListener);

        model_widget.runtimePropValue().addPropertyListener(contentChangedListener);

        model_widget.propOffColor().addUntypedPropertyListener(configChangedListener);
        model_widget.propOnColor().addUntypedPropertyListener(configChangedListener);
        model_widget.propStartBit().addUntypedPropertyListener(configChangedListener);

        model_widget.propBitReverse().addUntypedPropertyListener(lookChangedListener);
        model_widget.propForegroundColor().addUntypedPropertyListener(lookChangedListener);
        model_widget.propFont().addUntypedPropertyListener(lookChangedListener);
        model_widget.propLabels().addPropertyListener(labelsChangedListener);
        model_widget.propNumBits().addUntypedPropertyListener(lookChangedListener);
        model_widget.propHorizontal().addPropertyListener(orientationChangedListener);
        model_widget.propSquare().addUntypedPropertyListener(lookChangedListener);

        //initialization
        labelsChanged(model_widget.propLabels(), null, model_widget.propLabels().getValue());
        configChanged(null, null, null);
        contentChanged(null, null, model_widget.runtimePropValue().getValue());
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propWidth().removePropertyListener(sizeChangedListener);
        model_widget.propHeight().removePropertyListener(sizeChangedListener);
        model_widget.runtimePropValue().removePropertyListener(contentChangedListener);
        model_widget.propOffColor().removePropertyListener(configChangedListener);
        model_widget.propOnColor().removePropertyListener(configChangedListener);
        model_widget.propStartBit().removePropertyListener(configChangedListener);
        model_widget.propBitReverse().removePropertyListener(lookChangedListener);
        model_widget.propForegroundColor().removePropertyListener(lookChangedListener);
        model_widget.propFont().removePropertyListener(lookChangedListener);
        model_widget.propLabels().removePropertyListener(labelsChangedListener);
        model_widget.propNumBits().removePropertyListener(lookChangedListener);
        model_widget.propHorizontal().removePropertyListener(orientationChangedListener);
        model_widget.propSquare().removePropertyListener(lookChangedListener);

        labelsChanged(model_widget.propLabels(), model_widget.propLabels().getValue(), null);

        super.unregisterListeners();
    }

    private void labelsChanged(final WidgetProperty<List<StringWidgetProperty>> prop,
                               final List<StringWidgetProperty> removed, final List<StringWidgetProperty> added)
    {
        if (added != null)
            for (StringWidgetProperty text : added)
                text.addUntypedPropertyListener(lookChangedListener);
        if (removed != null)
            for (StringWidgetProperty text : removed)
                text.removePropertyListener(lookChangedListener);
        lookChangedListener.propertyChanged(null, null, null);
    }

    private void orientationChanged(final WidgetProperty<Boolean> prop, final Boolean old, final Boolean horizontal)
    {
        this.horizontal = horizontal;
        // When interactively changing orientation, swap width <-> height.
        // This will only affect interactive changes once the widget is represented on the screen.
        // Initially, when the widget is loaded from XML, the representation
        // doesn't exist and the original width, height and orientation are applied
        // without triggering a swap.
        if (toolkit.isEditMode())
        {
            final int w = model_widget.propWidth().getValue();
            final int h = model_widget.propHeight().getValue();
            model_widget.propWidth().setValue(h);
            model_widget.propHeight().setValue(w);
        }
        lookChanged(prop, old, horizontal);
    }

    /** Invoked when LED shape, number, or (via orientationChanged) arrangement
     *  changed (square_led, numBits, horizontal)
     *  @param property Ignored
     *  @param old_value Ignored
     *  @param new_value Ignored
     */
    protected void lookChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        bitReverse = model_widget.propBitReverse().getValue();
        numBits = model_widget.propNumBits().getValue();
        horizontal = model_widget.propHorizontal().getValue();
        square_led = model_widget.propSquare().getValue();
        // note: copied to array to safeguard against mid-operation changes
        dirty_config.mark();
        contentChanged(model_widget.runtimePropValue(), null, model_widget.runtimePropValue().getValue());
    }

    private void sizeChanged(final WidgetProperty<Integer> property, final Integer old_value, final Integer new_value)
    {
        // When editing round LEDs, set width & height to get snug alarm border
        if (toolkit.isEditMode()  &&  model_widget.propLabels().size() == 0  && !square_led  &&  numBits > 0)
        {
            if (horizontal  &&  property == model_widget.propWidth())
                Platform.runLater(() -> model_widget.propHeight().setValue(new_value / numBits));
            else if (!horizontal  &&  property == model_widget.propHeight())
                Platform.runLater(() -> model_widget.propWidth().setValue(new_value / numBits));
        }
        dirty_config.mark();
        toolkit.scheduleUpdate(this);
    }

    /** Invoked when color, startBit, or bitReverse properties
     *  changed and current colors need to be re-evaluated
     *  @param property Ignored
     *  @param old_value Ignored
     *  @param new_value Ignored
     */
    protected void configChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        startBit = model_widget.propStartBit().getValue();
        colors = createColors();
        contentChanged(model_widget.runtimePropValue(), null, model_widget.runtimePropValue().getValue());
    }

    private void contentChanged(final WidgetProperty<VType> property, final VType old_value, final VType new_value)
    {
        final int value_indices [] = computeColorIndices(new_value);
        final Color[] new_colorVals = new Color[value_indices.length];
        final Color[] save_colors = colors;
        for (int i = 0; i < value_indices.length; i++)
            new_colorVals[i] = save_colors[value_indices[i]];
        value_colors = new_colorVals;

        dirty_content.mark();
        toolkit.scheduleUpdate(this);
    }

    private Paint computeEditColors()
    {
        final Color[] save_colors = colors;
        return new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                List.of(new Stop(0.0, save_colors[0]),
                        new Stop(0.5, save_colors[0]),
                        new Stop(0.5, save_colors[1]),
                        new Stop(1.0, save_colors[1])));
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_config.checkAndClear())
        {
            final int w = model_widget.propWidth().getValue();
            final int h = model_widget.propHeight().getValue();
            jfx_node.resize(w, h);
            addLEDs(jfx_node, w, h, horizontal);
        }
        if (dirty_content.checkAndClear())
        {
            final Shape[] save_leds = leds;
            final Label[] save_labels = labels;
            final Color[] save_values = value_colors;
            if (save_leds == null  ||  save_values == null)
                return;

            final int N = Math.min(save_leds.length, save_values.length);
            if (toolkit.isEditMode())
            {
                final Paint edit_colors = computeEditColors();
                for (int i = 0; i < N; i++)
                    leds[i].setFill(edit_colors);
            }
            else
            {
                final Color text_color = JFXUtil.convert(model_widget.propForegroundColor().getValue());
                final double text_brightness = BaseLEDRepresentation.getBrightness(text_color);
                for (int i = 0; i < N; i++)
                {
                    leds[i].setFill(save_values[i]);
                    if (save_labels[i] != null)
                    {
                        // Compare brightness of LED with text.
                        final double brightness = BaseLEDRepresentation.getBrightness(save_values[i]);
                        if (Math.abs(text_brightness - brightness) < BaseLEDRepresentation.SIMILARITY_THRESHOLD)
                        {   // Colors of text and LED are very close in brightness.
                            // Make text visible by forcing black resp. white
                            if (brightness > BaseLEDRepresentation.BRIGHT_THRESHOLD)
                                save_labels[i].setTextFill(Color.BLACK);
                            else
                                save_labels[i].setTextFill(Color.WHITE);
                        }
                        else
                            save_labels[i].setTextFill(text_color);
                    }
                }
            }
        }
    }
}
/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
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
import org.csstudio.display.builder.model.properties.StringWidgetProperty;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.model.widgets.ByteMonitorWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.phoebus.vtype.VType;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
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
    private final UntypedWidgetPropertyListener look_listener = this::lookChanged;

    private volatile Color[] colors;

    private volatile Color[] value_colors = null;

    private volatile int startBit = 0;
    private volatile int numBits = 8;
    private volatile boolean bitReverse = false;
    private volatile boolean horizontal = true;
    private volatile boolean square_led = false;

    private volatile Shape[] leds = null;

    @Override
    protected Pane createJFXNode() throws Exception
    {
        colors = createColors();
        final Pane pane = new Pane();
        numBits = model_widget.propNumBits().getValue();
        square_led = model_widget.propSquare().getValue();
        horizontal = model_widget.propHorizontal().getValue();
        addLEDs(pane);
        pane.setMinSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);
        pane.setMaxSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);
        return pane;
    }

    @Override
    public int[] getBorderRadii()
    {
        if (square_led)
            return null;
        if (horizontal)
            return new int[]
            {
                model_widget.propWidth().getValue()/numBits/2,
                model_widget.propHeight().getValue()/2,
            };
        else
            return new int[]
            {
                model_widget.propWidth().getValue()/2,
                model_widget.propHeight().getValue()/numBits/2,
            };
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
                    label.setPrefSize(led_w, led_h);
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
                        label.setPrefSize(led_h - 2*rad - gap, led_w);
                        label.setAlignment(Pos.CENTER_RIGHT);
                    }
                    else
                    {
                        label.relocate(x+2*rad+gap, y);
                        label.setPrefSize(led_w-2*rad-gap, led_h);
                    }
                }
            }
            led.getStyleClass().add("led");
            if (save_colorVals != null && i < save_colorVals.length)
                led.setFill(save_colorVals[i]);

            leds[i] = led;
            labels[i] = label;
            x += dx;
            y += dy;
        }
        this.leds = leds;
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
        final int number = VTypeUtil.getValueNumber(value).intValue();
        for (int i = 0; i < nBits; i++)
            colorIndices[ save_bitRev ? i : nBits-1-i] = number & (1 << (sBit+i));
        return colorIndices;
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.propWidth().addPropertyListener(this::sizeChanged);
        model_widget.propHeight().addPropertyListener(this::sizeChanged);

        model_widget.runtimePropValue().addPropertyListener(this::contentChanged);

        model_widget.propOffColor().addUntypedPropertyListener(this::configChanged);
        model_widget.propOnColor().addUntypedPropertyListener(this::configChanged);
        model_widget.propStartBit().addUntypedPropertyListener(this::configChanged);

        model_widget.propBitReverse().addUntypedPropertyListener(look_listener);
        model_widget.propForegroundColor().addUntypedPropertyListener(look_listener);
        model_widget.propFont().addUntypedPropertyListener(look_listener);
        model_widget.propLabels().addPropertyListener(this::labelsChanged);
        model_widget.propNumBits().addUntypedPropertyListener(look_listener);
        model_widget.propHorizontal().addPropertyListener(this::orientationChanged);
        model_widget.propSquare().addUntypedPropertyListener(look_listener);

        //initialization
        labelsChanged(model_widget.propLabels(), null, model_widget.propLabels().getValue());
        configChanged(null, null, null);
        contentChanged(null, null, model_widget.runtimePropValue().getValue());
    }

    private void labelsChanged(final WidgetProperty<List<StringWidgetProperty>> prop,
                               final List<StringWidgetProperty> removed, final List<StringWidgetProperty> added)
    {
        if (added != null)
            for (StringWidgetProperty text : added)
                text.addUntypedPropertyListener(look_listener);
        if (removed != null)
            for (StringWidgetProperty text : removed)
                text.removePropertyListener(look_listener);
        look_listener.propertyChanged(null, null, null);
    }

    private void orientationChanged(final WidgetProperty<Boolean> prop, final Boolean old, final Boolean horizontal)
    {
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
        {
            value_indices[i] = value_indices[i] <= 0 ? 0 : 1;
            new_colorVals[i] = save_colors[value_indices[i]];
        }
        value_colors = new_colorVals;

        dirty_content.mark();
        toolkit.scheduleUpdate(this);
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_config.checkAndClear())
        {
            final int w = model_widget.propWidth().getValue();
            final int h = model_widget.propHeight().getValue();
            jfx_node.setPrefSize(w, h);
            addLEDs(jfx_node, w, h, horizontal);
        }
        if (dirty_content.checkAndClear())
        {
            final Shape[] save_leds = leds;
            final Color[] save_values = value_colors;
            if (save_leds == null  ||  save_values == null)
                return;

            final int N = Math.min(save_leds.length, save_values.length);
            for (int i = 0; i < N; i++)
                leds[i].setFill(save_values[i]);
        }
    }
}
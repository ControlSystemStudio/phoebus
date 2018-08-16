/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.model.widgets.ByteMonitorWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.phoebus.vtype.VType;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

/** Creates JavaFX item for model widget
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class ByteMonitorRepresentation extends RegionBaseRepresentation<Pane, ByteMonitorWidget>
{
    private final DirtyFlag dirty_size = new DirtyFlag();
    private final DirtyFlag dirty_content = new DirtyFlag();

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

    private void addLEDs(final Pane pane, final int w, final int h, final boolean horizontal)
    {
        final int save_bits = numBits;
        final boolean save_sq = square_led;
        final Color [] save_colorVals = value_colors;
        final Shape [] leds = new Shape[save_bits];
        for (int i = 0; i < save_bits; i++)
        {
            final Shape led;
            if (save_sq)
            {
                final Rectangle rect = new Rectangle();
                rect.setX(horizontal ? i*w/save_bits : 0);
                rect.setY(horizontal ? 0 : i*h/save_bits);
                rect.setWidth(horizontal ? w/save_bits : w);
                rect.setHeight(horizontal ? h : h/save_bits);
                led = rect;
            }
            else
            {
                final Ellipse ell = new Ellipse();
                final int dh = horizontal ? w/save_bits : w;
                final int dv = horizontal ? h : h/save_bits;
                ell.setCenterX(horizontal ? dh/2 + i*dh : dh/2);
                ell.setCenterY(horizontal ? dv/2 : dv/2 + i*dv);
                ell.setRadiusX(dh/2);
                ell.setRadiusY(dv/2);
                led = ell;
            }
            led.getStyleClass().add("led");
            if (save_colorVals != null && i < save_colorVals.length)
                led.setFill(save_colorVals[i]);
            leds[i] = led;
        }
        this.leds = leds;
        pane.getChildren().clear();
        pane.getChildren().addAll(leds);
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
        model_widget.propBitReverse().addUntypedPropertyListener(this::configChanged);

        model_widget.propNumBits().addUntypedPropertyListener(this::lookChanged);
        model_widget.propHorizontal().addUntypedPropertyListener(this::lookChanged);
        model_widget.propSquare().addUntypedPropertyListener(this::lookChanged);

        //initialization
        configChanged(null, null, null);
        lookChanged(null, null, null);
        contentChanged(null, null, model_widget.runtimePropValue().getValue());
    }

    /**
     * Invoked when LED shape, number, or arrangement
     * changed (square_led, numBits, horizontal)
     * @param property Ignored
     * @param old_value Ignored
     * @param new_value Ignored
     */
    protected void lookChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        numBits = model_widget.propNumBits().getValue();
        horizontal = model_widget.propHorizontal().getValue();
        square_led = model_widget.propSquare().getValue();
        // note: copied to array to safeguard against mid-operation changes
        dirty_size.mark();
        contentChanged(model_widget.runtimePropValue(), null, model_widget.runtimePropValue().getValue());
    }

    private void sizeChanged(final WidgetProperty<Integer> property, final Integer old_value, final Integer new_value)
    {
        dirty_size.mark();
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
        bitReverse = model_widget.propBitReverse().getValue();
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
        if (dirty_size.checkAndClear())
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
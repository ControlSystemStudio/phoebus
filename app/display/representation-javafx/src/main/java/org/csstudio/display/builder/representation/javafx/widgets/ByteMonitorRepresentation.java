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

import javafx.geometry.Bounds;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

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

    private void addLEDs(final Pane pane, double w, double h, final boolean horizontal)
    {
        final Color text_color = JFXUtil.convert(model_widget.propForegroundColor().getValue());
        final Font text_font = JFXUtil.convert(model_widget.propFont().getValue());
        final int save_bits = numBits;
        final boolean save_sq = square_led;
        final Color [] save_colorVals = value_colors;
        final Shape [] leds = new Shape[save_bits];
        final Text [] texts = new Text[save_bits];
        double x = 0.0, y = 0.0;
        double dx, dy;
        if (horizontal)
        {
            dx = w / save_bits;
            dy = 0;
            w = dx;
        }
        else
        {
            dx = 0;
            dy = h / save_bits;
            h = dy;
        }
        for (int i = 0; i < save_bits; i++)
        {
            final Shape led;
            if (save_sq)
            {
                final Rectangle rect = new Rectangle();
                rect.setX(x);
                rect.setY(y);
                rect.setWidth(w);
                rect.setHeight(h);
                led = rect;
            }
            else
            {
                final Ellipse ell = new Ellipse();
                ell.setCenterX(x + w/2);
                ell.setCenterY(y + h/2);
                ell.setRadiusX(w/2);
                ell.setRadiusY(h/2);
                led = ell;
            }
            led.getStyleClass().add("led");
            if (save_colorVals != null && i < save_colorVals.length)
                led.setFill(save_colorVals[i]);

            final Text text;
            final int lbl_index = bitReverse ? i : save_bits - i - 1;
            if (lbl_index < model_widget.propLabels().size())
            {
                text = new Text(model_widget.propLabels().getElement(lbl_index).getValue());
                if (horizontal)
                    text.setRotate(-90.0);
                text.setFont(text_font);
                text.applyCss();
                final Bounds bounds = text.getBoundsInLocal();
                text.setX(x + (w - bounds.getWidth())/2);
                text.setY(y + (h + bounds.getHeight())/2);
                text.setFill(text_color);
            }
            else
                text = null;

            leds[i] = led;
            texts[i] = text;
            x += dx;
            y += dy;
        }
        this.leds = leds;
        pane.getChildren().setAll(leds);
        for (Text text : texts)
            if (text != null)
                pane.getChildren().add(text);
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
        model_widget.propLabels().addPropertyListener(this::lablesChanged);
        model_widget.propNumBits().addUntypedPropertyListener(look_listener);
        model_widget.propHorizontal().addPropertyListener(this::orientationChanged);
        model_widget.propSquare().addUntypedPropertyListener(look_listener);

        //initialization
        lablesChanged(model_widget.propLabels(), null, model_widget.propLabels().getValue());
        configChanged(null, null, null);
        contentChanged(null, null, model_widget.runtimePropValue().getValue());
    }

    private void lablesChanged(final WidgetProperty<List<StringWidgetProperty>> prop,
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
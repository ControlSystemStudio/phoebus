/*******************************************************************************
 * Copyright (c) 2015-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.model.widgets.ProgressBarWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.csstudio.javafx.rtplot.internal.util.Log10;
import org.epics.vtype.Display;
import org.epics.vtype.VType;
import org.phoebus.ui.javafx.Styles;

import javafx.scene.control.ProgressBar;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class ProgressBarRepresentation extends RegionBaseRepresentation<ProgressBar, ProgressBarWidget>
{
    private final DirtyFlag dirty_look = new DirtyFlag();
    private final DirtyFlag dirty_value = new DirtyFlag();
    private final UntypedWidgetPropertyListener lookChangedListener = this::lookChanged;
    private final WidgetPropertyListener<Boolean> orientationChangedListener = this::orientationChanged;
    private final UntypedWidgetPropertyListener valueChangedListener = this::valueChanged;
    private final UntypedWidgetPropertyListener propretyChangedListener = this::propertyChanged;

    private volatile double percentage = 0.0;

    @Override
    public ProgressBar createJFXNode() throws Exception
    {
        final ProgressBar bar = new ProgressBar();
        return bar;
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.propFillColor().addUntypedPropertyListener(lookChangedListener);
        model_widget.propBackgroundColor().addUntypedPropertyListener(lookChangedListener);
        model_widget.propWidth().addUntypedPropertyListener(lookChangedListener);
        model_widget.propHeight().addUntypedPropertyListener(lookChangedListener);
        model_widget.propLimitsFromPV().addUntypedPropertyListener(valueChangedListener);
        model_widget.propMinimum().addUntypedPropertyListener(propretyChangedListener);
        model_widget.propMaximum().addUntypedPropertyListener(propretyChangedListener);
        model_widget.propLogScale().addUntypedPropertyListener(valueChangedListener);
        model_widget.runtimePropValue().addUntypedPropertyListener(valueChangedListener);
        model_widget.propHorizontal().addPropertyListener(orientationChangedListener);
        valueChanged(null, null, null);
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propFillColor().removePropertyListener(lookChangedListener);
        model_widget.propBackgroundColor().removePropertyListener(lookChangedListener);
        model_widget.propWidth().removePropertyListener(lookChangedListener);
        model_widget.propHeight().removePropertyListener(lookChangedListener);
        model_widget.propLimitsFromPV().removePropertyListener(valueChangedListener);
        model_widget.propMinimum().removePropertyListener(propretyChangedListener);
        model_widget.propMaximum().removePropertyListener(propretyChangedListener);
        model_widget.propLogScale().removePropertyListener(valueChangedListener);
        model_widget.runtimePropValue().removePropertyListener(valueChangedListener);
        model_widget.propHorizontal().removePropertyListener(orientationChangedListener);
        super.unregisterListeners();
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

    private void propertyChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        lookChanged(property, old_value, new_value );
        valueChanged(property, old_value, new_value );
    }

    private void lookChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_look.mark();
        toolkit.scheduleUpdate(this);
    }

    private void valueChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        final VType vtype = model_widget.runtimePropValue().getValue();

        final boolean limits_from_pv = model_widget.propLimitsFromPV().getValue();

        double min_val = 0;
        double max_val = 0;
        
        // Inverted if low limit and higher than high limit
        if (model_widget.propMaximum().getValue() > model_widget.propMinimum().getValue())
        {
            min_val = model_widget.propMinimum().getValue();
            max_val = model_widget.propMaximum().getValue();
        }
        else
        {
            max_val = model_widget.propMinimum().getValue();
            min_val = model_widget.propMaximum().getValue();
        }
        
        if (limits_from_pv)
        {
            // Try display range from PV
            final Display display_info = Display.displayOf(vtype);
            if (display_info != null)
            {
                min_val = display_info.getDisplayRange().getMinimum();
                max_val = display_info.getDisplayRange().getMaximum();
            }
        }
        // Fall back to 0..100 range
        if (min_val == max_val)
        {
            min_val = 0.0;
            max_val = 100.0;
        }

        // Determine percentage of value within the min..max range
        final double value = VTypeUtil.getValueNumber(vtype).doubleValue();
        final double percentage;

        if (model_widget.propLogScale().getValue())
        {
            final double d = Log10.log10(max_val) - Log10.log10(min_val);
            if (d == 0)
                percentage = Double.NaN;
            else
                percentage = (Log10.log10(value) - Log10.log10(min_val)) / d;
        }
        else
            percentage = (value - min_val) / (max_val - min_val);
        
            // Limit to 0.0 .. 1.0
        if (percentage < 0.0  ||  !Double.isFinite(percentage))
            this.percentage = 0.0;
        else if (percentage > 1.0)
            this.percentage = 1.0;
        else
            this.percentage = percentage;
        dirty_value.mark();
        toolkit.scheduleUpdate(this);
    }


    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_look.checkAndClear())
        {
            boolean horizontal = model_widget.propHorizontal().getValue();
            double width = model_widget.propWidth().getValue();
            double height = model_widget.propHeight().getValue();
            double min_val = model_widget.propMinimum().getValue();
            double max_val = model_widget.propMaximum().getValue();

            if (!horizontal)
            {
                jfx_node.getTransforms().setAll(
                    new Translate(0, height),
                    new Rotate(-90, 0, 0));
                jfx_node.setPrefSize(height, width);

                if (min_val > max_val) 
                {
                    jfx_node.getTransforms().setAll(
                        new Translate(0, height),
                        new Rotate(-90, 0, 0, 0),
                        new Translate(height, 0),
                        new Rotate(180, 0, 0, 0, Rotate.Y_AXIS));
                    jfx_node.setPrefSize(height, width);
                }
            }
            else
            {
                jfx_node.getTransforms().clear();
                jfx_node.setPrefSize(width, height);

                if (min_val > max_val)
                {
                    jfx_node.getTransforms().setAll(
                        new Translate(width, 0),
                        new Rotate(180, 0, 0, 0, Rotate.Y_AXIS));
                }
            }

            // Default 'inset' of .bar uses 7 pixels.
            // A widget sized 15 has 8 pixels left for the bar.
            // Select leaner style where .bar uses full size.
            Styles.update(jfx_node, "SmallBar",
                          Math.min(width, height) <= 15);

            // Could clear style and use setBackground(),
            // but result is very plain.
            // Tweaking the color used by CSS keeps overall style.
            // See also http://stackoverflow.com/questions/13467259/javafx-how-to-change-progressbar-color-dynamically
            final StringBuilder style = new StringBuilder();

            // Color of the progress bar / foreground
            style.append("-fx-accent: ").append(JFXUtil.webRGB(
                    JFXUtil.convert(
                            model_widget.propFillColor().getValue()
                    )
            )).append(" !important; ");

            // Color of the background underneath the progress bar
            // Note per moderna.css the background is actually three layers of color
            // with fx-shadow-highlight-color on the bottom,
            // then fx-text-box-border,
            // and finally fx-control-inner-background on top, all stacked in place with offsets.
            // This gives the illusion of having a bordered box with a shadow instead of actually being a
            // bordered box with a shadow...
            // Fortunately, the bottom-most color (the 'shadow') is already transparent so we can leave it alone
            // Unfortunately, the middle color (the "border" color) is a solid gray color (#ececec), so we must
            // override it with its rgba equivalent so that it has transparency matching the picked background color.
            style.append("-fx-control-inner-background: ")
                    .append(JFXUtil.webRGB(
                            JFXUtil.convert(
                                    model_widget.propBackgroundColor().getValue()))
                            )
                    .append(";");
            style.append("-fx-text-box-border: rgba(236, 236, 236, ")
                    .append(JFXUtil.webAlpha(model_widget.propBackgroundColor().getValue()))
                    .append(");");
            style.append("-fx-shadow-highlight-color: rgba(236, 236, 236, ")
                    .append(JFXUtil.webAlpha(model_widget.propBackgroundColor().getValue()))
                    .append(");");
            jfx_node.setStyle(style.toString());
        }
        if (dirty_value.checkAndClear())
            jfx_node.setProgress(percentage);
    }
}

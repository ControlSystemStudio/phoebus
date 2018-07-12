/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.util.logging.Level;

import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.representation.javafx.JFXUtil;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.skin.SliderSkin;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/** 'Marker' addition to Slider
 *
 *  <p>Adds a section with alarm limit markers to a Slider.
 *
 *  @author Amanda Carpenter - Original MarkerAxis based on javafx.scene.chart.Axis
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SliderMarkers extends Pane
{
    // Fundamental Issue:
    // Slider resp. its SliderSkin lack API to determine the translation
    // of values to pixels, which is required to properly position markers.
    //
    // The original MarkerAxis implementation used a javafx.scene.chart.Axis,
    // assuming that such an axis would utilize the same value <-> pixel
    // mapping as the tickLine inside the SliderSkin.
    // The tickLine inside the SliderSkin, however, is positioned based
    // on the thumb size, so an exact alignment cannot be guaranteed.
    //
    // This implementation simply positions markers as Labels in a Pane,
    // and it does use the correct positioning based on the thumb size.
    // On the downside, it uses private API (com.sun.javafx.scene.control.skin.SliderSkin)
    // and depends on the SliderSkin's internal behavior.
    private final Slider slider;
    private Label lolo_label = new Label("LOLO");
    private Label low_label = new Label("LOW");
    private Label high_label = new Label("HIGH");
    private Label hihi_label = new Label("HIHI");

    private double lolo = Double.NaN, low = Double.NaN, high = Double.NaN, hihi = Double.NaN;

    public SliderMarkers(final Slider slider)
    {
        this.slider = slider;

        Color color = JFXUtil.convert(WidgetColorService.getColor(NamedWidgetColors.ALARM_MAJOR));
        lolo_label.setTextFill(color);
        hihi_label.setTextFill(color);
        color = JFXUtil.convert(WidgetColorService.getColor(NamedWidgetColors.ALARM_MINOR));
        low_label.setTextFill(color);
        high_label.setTextFill(color);

        getChildren().addAll(hihi_label, high_label, low_label, lolo_label);

        // Need to update the markers whenever the slider is resized
        slider.widthProperty().addListener(width -> update());
        slider.heightProperty().addListener(height -> update());
        // .. or when the min/max changes
        slider.minProperty().addListener(min -> update());
        slider.maxProperty().addListener(max -> update());
        // Also need to update when the slider's font or knob size change,
        // but there is no obvious listener for that.
    }

    /** Set location of alarm markers
     *  @param lolo Value for marker or {@link Double#NaN} to hide
     *  @param low Value for marker or {@link Double#NaN} to hide
     *  @param high Value for marker or {@link Double#NaN} to hide
     *  @param hihi Value for marker or {@link Double#NaN} to hide
     */
    public void setAlarmMarkers(final double lolo, final double low, final double high, final double hihi)
    {
        this.lolo = lolo;
        this.low = low;
        this.high = high;
        this.hihi = hihi;
        update();
    }

    /** @param font Font to use for markers */
    public void setFont(final Font font)
    {
        hihi_label.setFont(font);
        high_label.setFont(font);
        low_label.setFont(font);
        lolo_label.setFont(font);
        update();
    }

    /** Update the markers
     *
     *  <p>Is automatically called when the marker values or the
     *  slider's size or min/max change.
     *
     *  <p>Needs to be called when font or knob size of the slider change.
     */
    public void update()
    {
        // An update of the font and thus knob size results
        // in a new value for the 'gap' on the _next_ UI update.
        // Defer the actual update, because otherwise they would be
        // positioned on the _old_ gap.
        Platform.runLater(() -> doUpdate());
    }

    protected void doUpdate()
    {
        final double gap = getScaleGap();
        positionMarker(gap, hihi_label, hihi);
        positionMarker(gap, high_label, high);
        positionMarker(gap, low_label, low);
        positionMarker(gap, lolo_label, lolo);
    }

    /** @return 'gap' between bounds of the slider and its tick line */
    @SuppressWarnings("restriction")
    private double getScaleGap()
    {
        // No public API in Slider to obtain this gap.
        // For the SliderSkin in Java 1.8.0_101 it's half the size
        // of the slider's knob.
        // In the SliderSkin, that's a Pane with style "thumb".
        try
        {
            SliderSkin skin = (SliderSkin) slider.getSkin();
            if (skin != null)
                for (Node node : skin.getChildren())
                {
                    if (node.getStyleClass().contains("thumb"))
                        return node.getBoundsInLocal().getHeight() / 2;
                }
        }
        catch (Throwable ex)
        {
            logger.log(Level.WARNING, "Cannot obtain Slider layout details", ex);
        }
        return 11;
    }

    /** @param gap Scale gap
     *  @param marker Marker to position
     *  @param value Value of the marker, {@link Double#NaN} to hide
     */
    private void positionMarker(final double gap, final Label marker, final double value)
    {
        if (Double.isNaN(value))
        {
            marker.setVisible(false);
            return;
        }

        final double min = slider.getMin(), max = slider.getMax();
        if (slider.getOrientation() == Orientation.VERTICAL)
        {
            final double length = slider.getHeight();
            double pos = gap + (value - min) * (length-2*gap) / (max - min);
            if (min < max)
                pos = length - pos; // 'max' is towards top of screen
            if (pos >= 0  &&  pos < length)
            {   // Position at gap, vertically centered
                marker.relocate(0,  pos - marker.getHeight() / 2);
                marker.setVisible(true);
            }
            else
                marker.setVisible(false);
        }
        else
        {
            final double length = slider.getWidth();
            double pos = gap + (value - min) * (length-2*gap) / (max - min);
            if (min > max)
                pos = length - pos; // 'max' is towards right of screen
            if (pos >= 0  &&  pos < length)
            {   // Position at gap, horizontally centered
                marker.relocate(pos - marker.getWidth() / 2, 0);
                marker.setVisible(true);
            }
            else
                marker.setVisible(false);
        }
    }
}
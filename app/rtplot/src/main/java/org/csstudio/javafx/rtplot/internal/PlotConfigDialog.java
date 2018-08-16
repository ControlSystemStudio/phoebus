/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import org.csstudio.javafx.rtplot.Activator;
import org.csstudio.javafx.rtplot.Axis;
import org.csstudio.javafx.rtplot.RTPlot;
import org.csstudio.javafx.rtplot.YAxis;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;

/** Dialog for runtime changes to a plot
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PlotConfigDialog<XTYPE extends Comparable<XTYPE>>  extends Dialog<Void>
{
    private final RTPlot<XTYPE> plot;

    public PlotConfigDialog(final RTPlot<XTYPE> plot)
    {
        this.plot = plot;

        initModality(Modality.NONE);
        setTitle("Configure");
        setHeaderText("Change plot settings");
        try
        {
            setGraphic(new ImageView(Activator.getIcon("configure")));
        }
        catch (Exception ex)
        {
            // Ignore
        }

        getDialogPane().setContent(createContent());
        getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        setResizable(true);

        setResultConverter(button -> null);
    }

    private Node createContent()
    {
        final GridPane layout = new GridPane();
        layout.setHgap(5);
        layout.setVgap(5);

        // Debug layout
        // layout.setGridLinesVisible(true);

        // Row to use for the next elements
        int row = 0;

        Label label = new Label("Value Axes");
        final Font font = label.getFont();
        final Font section_font = Font.font(font.getFamily(), FontWeight.BOLD, font.getSize());
        label.setFont(section_font);
        layout.add(label, 0, row++);

        for (Axis<?> axis : plot.getYAxes())
            row = addAxisContent(layout, row, axis);

        label = new Label("Horizontal Axis");
        label.setFont(section_font);
        layout.add(label, 0, row++);

        row = addAxisContent(layout, row, plot.getXAxis());

        label = new Label("Plot");
        label.setFont(section_font);
        layout.add(label, 0, row);

        final CheckBox legend = new CheckBox("Show Legend");
        legend.setSelected(plot.isLegendVisible());
        legend.setOnAction(event ->   plot.showLegend(legend.isSelected()) );
        layout.add(legend, 1, row);

        final ScrollPane scroll = new ScrollPane(layout);
        return scroll;
    }

    // TODO Traces: Hide, change color?
//    static ColorPicker createPicker(final Color color)
//    {
//        final ColorPicker picker = new ColorPicker(color);
//        picker.getCustomColors().setAll(RGBFactory.PALETTE);
//        picker.setStyle("-fx-color-label-visible: false ;");
//        return picker;
//    }


    private int addAxisContent(final GridPane layout, int row, final Axis<?> axis)
    {
        layout.add(new Label("Axis Name:"), 0, row);
        final TextField axis_name = new TextField(axis.getName());
        axis_name.setOnAction(event -> axis.setName(axis_name.getText()));
        layout.add(axis_name, 1, row++, 2, 1);

        final CheckBox visible = new CheckBox("Visible");
        visible.setSelected(axis.isVisible());
        visible.setOnAction(event -> axis.setVisible(visible.isSelected()));
        layout.add(visible, 1, row++);

        // Don't support auto-scale for time axis
        // because code that updates the time axis
        // is supposed to handle the 'scrolling'
        if (axis instanceof NumericAxis)
        {
            final NumericAxis num_axis = (NumericAxis) axis;

            Label label = new Label("Start, End:");
            layout.add(label, 0, row);

            final TextField start = new TextField(axis.getValueRange().getLow().toString());
            layout.add(start,  1, row);

            final TextField end = new TextField(axis.getValueRange().getHigh().toString());
            layout.add(end,  2, row++);

            @SuppressWarnings("unchecked")
            final EventHandler<ActionEvent> update_range = event ->
            {
                try
                {
                    num_axis.setValueRange(Double.parseDouble(start.getText()), Double.parseDouble(end.getText()));
                }
                catch (NumberFormatException ex)
                {
                    start.setText(axis.getValueRange().getLow().toString());
                    end.setText(axis.getValueRange().getHigh().toString());
                    return;
                }
                if (axis instanceof YAxisImpl)
                    plot.internalGetPlot().fireYAxisChange((YAxisImpl<XTYPE>)axis);
                else if (axis instanceof HorizontalNumericAxis)
                    plot.internalGetPlot().fireXAxisChange();
            };
            start.setOnAction(update_range);
            end.setOnAction(update_range);

            final CheckBox autoscale = new CheckBox("Auto-scale");
            if (axis.isAutoscale())
            {
                autoscale.setSelected(true);
                start.setDisable(true);
                end.setDisable(true);
            }
            autoscale.setOnAction(event ->
            {
                axis.setAutoscale(autoscale.isSelected());
                start.setDisable(autoscale.isSelected());
                end.setDisable(autoscale.isSelected());
                plot.internalGetPlot().fireAutoScaleChange(axis);
            });
            layout.add(autoscale, 1, row);

            final CheckBox logscale = new CheckBox("Log scale");
            logscale.setSelected(num_axis.isLogarithmic());
            logscale.setOnAction(event ->
            {
                num_axis.setLogarithmic(logscale.isSelected());
                plot.internalGetPlot().fireLogarithmicChange((YAxis<?>)num_axis);
            });
            layout.add(logscale, 2, row++);
        }

        final CheckBox grid = new CheckBox("Grid");
        grid.setSelected(axis.isGridVisible());
        grid.setOnAction(event ->
        {
            axis.setGridVisible(grid.isSelected());
            plot.internalGetPlot().fireGridChange(axis);
        });
        layout.add(grid, 1, row);

        if (axis instanceof YAxis)
        {
            final YAxis<?> yaxis = (YAxis<?>) axis;
            final CheckBox trace_names = new CheckBox("Show Trace Names");
            trace_names.setSelected(yaxis.isUsingTraceNames());
            trace_names.setOnAction(event -> yaxis.useTraceNames(trace_names.isSelected()));
            layout.add(trace_names, 2, row);
        }
        ++row;

        layout.add(new Separator(), 0, row++, 3, 1);

        return row;
    }
}

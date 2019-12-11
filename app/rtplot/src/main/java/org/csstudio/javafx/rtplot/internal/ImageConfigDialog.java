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
import org.csstudio.javafx.rtplot.ColorMappingFunction;
import org.csstudio.javafx.rtplot.Messages;
import org.csstudio.javafx.rtplot.NamedColorMapping;
import org.csstudio.javafx.rtplot.NamedColorMappings;
import org.csstudio.javafx.rtplot.RTImagePlot;
import org.csstudio.javafx.rtplot.data.ValueRange;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;

/** Dialog for runtime changes to an image plot
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ImageConfigDialog  extends Dialog<Void>
{
    private final RTImagePlot plot;

    public ImageConfigDialog(final RTImagePlot plot)
    {
        this.plot = plot;

        initModality(Modality.NONE);
        setTitle(Messages.ImageConfigTitle);
        setHeaderText(Messages.ImageConfigHdr);
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


        Label label = new Label(Messages.ImageConfigRange);
        final Font font = label.getFont();
        final Font section_font = Font.font(font.getFamily(), FontWeight.BOLD, font.getSize());
        label.setFont(section_font);
        layout.add(label, 0, ++row);

        // Only show the color mapping selector when it's currently a named mapping.
        // Suppress when widget has a custom color map.
        final ColorMappingFunction selected_mapping = plot.internalGetImagePlot().getColorMapping();
        if (selected_mapping instanceof NamedColorMapping)
        {
            label = new Label(Messages.ImageConfigMapping);
            layout.add(label, 1, row);

            final ComboBox<String> mappings = new ComboBox<>();
            mappings.setEditable(false);
            mappings.setMaxWidth(Double.MAX_VALUE);
            for (NamedColorMapping mapping : NamedColorMappings.getMappings())
                mappings.getItems().add(mapping.getName());
            mappings.setValue( ((NamedColorMapping)selected_mapping).getName());
            mappings.setOnAction(event ->
            {
                final NamedColorMapping mapping = NamedColorMappings.getMapping(mappings.getValue());
                plot.setColorMapping(mapping);
            });

            layout.add(mappings, 2, row++);
        }

        label = new Label(Messages.ImageConfigMin);
        layout.add(label, 1, row);

        final TextField min = new TextField(Double.toString(plot.getValueRange().getLow()));
        layout.add(min, 2, row);

        label = new Label(Messages.ImageConfigMax);
        layout.add(label, 1, ++row);
        final TextField max = new TextField(Double.toString(plot.getValueRange().getHigh()));
        layout.add(max, 2, row);

        final EventHandler<ActionEvent> update_range = event ->
        {
            try
            {
                plot.setValueRange(Double.parseDouble(min.getText().trim()),
                                   Double.parseDouble(max.getText().trim()));
                plot.internalGetImagePlot().fireChangedValueRange();
            }
            catch (NumberFormatException ex)
            {
                final ValueRange range = plot.getValueRange();
                min.setText(Double.toString(range.getLow()));
                max.setText(Double.toString(range.getHigh()));
            }
        };
        min.setOnAction(update_range);
        max.setOnAction(update_range);

        final CheckBox autoscale = new CheckBox(Messages.ImageConfigAutoScale);
        autoscale.setSelected(plot.isAutoscale());
        min.setDisable(autoscale.isSelected());
        max.setDisable(autoscale.isSelected());
        autoscale.setOnAction(event ->
        {
            plot.setAutoscale(autoscale.isSelected());
            min.setDisable(autoscale.isSelected());
            max.setDisable(autoscale.isSelected());
            plot.internalGetImagePlot().fireChangedAutoScale();
        });
        layout.add(autoscale, 2, ++row);

        final CheckBox show_color_bar = new CheckBox(Messages.ImageConfigShowBar);
        show_color_bar.setSelected(plot.isShowingColorMap());
        show_color_bar.setOnAction(event -> plot.showColorMap(show_color_bar.isSelected()));
        layout.add(show_color_bar, 2, ++row);

        final CheckBox logscale = new CheckBox(Messages.ImageConfigLogScale);
        logscale.setSelected(plot.isLogscale());
        logscale.setOnAction(event ->
        {
            plot.setLogscale(logscale.isSelected());
            plot.internalGetImagePlot().fireChangedLogarithmic();
        });
        layout.add(logscale, 2, ++row);

        label = new Label(Messages.ImageConfigHor);
        label.setFont(section_font);
        layout.add(label, 0, ++row);

        row = addAxisContent(layout, row, plot.getXAxis());

        label = new Label(Messages.ImageConfigVer);
        label.setFont(section_font);
        layout.add(label, 0, ++row);

        row = addAxisContent(layout, row, plot.getYAxis());

        return layout;
    }

    private int addAxisContent(final GridPane layout, int row, final Axis<Double> axis)
    {
        Label label = new Label(Messages.ImageConfigStart);
        layout.add(label, 1, row);

        final TextField start = new TextField(axis.getValueRange().getLow().toString());
        layout.add(start,  2, row++);

        label = new Label(Messages.ImageConfigEnd);
        layout.add(label, 1, row);
        final TextField end = new TextField(axis.getValueRange().getHigh().toString());
        layout.add(end,  2, row++);

        final EventHandler<ActionEvent> update_range = event ->
        {
            try
            {
                axis.setValueRange(Double.parseDouble(start.getText()), Double.parseDouble(end.getText()));
                plot.internalGetImagePlot().fireChangedAxisRange(axis);
            }
            catch (NumberFormatException ex)
            {
                start.setText(axis.getValueRange().getLow().toString());
                end.setText(axis.getValueRange().getHigh().toString());
                return;
            }
        };
        start.setOnAction(update_range);
        end.setOnAction(update_range);

        return row;
    }
}

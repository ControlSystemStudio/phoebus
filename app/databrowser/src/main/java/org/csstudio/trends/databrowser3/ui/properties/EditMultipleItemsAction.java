/*******************************************************************************
 * Copyright (c) 2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import java.util.List;
import java.util.stream.Collectors;

import org.csstudio.javafx.rtplot.LineStyle;
import org.csstudio.javafx.rtplot.PointType;
import org.csstudio.javafx.rtplot.TraceType;
import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.AxisConfig;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.csstudio.trends.databrowser3.model.RequestType;
import org.csstudio.trends.databrowser3.preferences.Preferences;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;

/** MenuItem to edit settings for multiple model items
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class EditMultipleItemsAction extends MenuItem
{
    private static class ItemsConfigDialog extends Dialog<Boolean>
    {
        private final CheckBox set_visible = new CheckBox(Messages.TraceVisibility);
        private final CheckBox visible = new CheckBox();

        private final CheckBox set_color = new CheckBox(Messages.Color);
        private final ColorPicker color = PropertyPanel.ColorTableCell.createPicker(Color.RED);

        private final CheckBox set_period = new CheckBox(Messages.ScanPeriod);
        private final TextField period = new TextField("0.0");

        private final CheckBox set_buffer = new CheckBox(Messages.LiveSampleBufferSize);
        private final TextField buffer = new TextField(Integer.toString(Preferences.buffer_size));

        private final CheckBox set_axis = new CheckBox(Messages.Axis);
        private final ComboBox<String> axis = new ComboBox<>();

        private final CheckBox set_type = new CheckBox(Messages.TraceType);
        private final ComboBox<String> type = new ComboBox<>();

        private final CheckBox set_width = new CheckBox(Messages.TraceLineWidth);
        private final TextField width = new TextField(Integer.toString(Preferences.line_width));

        private final CheckBox set_style = new CheckBox(Messages.TraceLineStyle);
        private final ComboBox<String> style = new ComboBox<>();

        private final CheckBox set_point = new CheckBox(Messages.PointType);
        private final ComboBox<String> point = new ComboBox<>();

        private final CheckBox set_size = new CheckBox(Messages.PointSize);
        private final TextField size = new TextField("2");

        private final CheckBox set_optimized = new CheckBox(Messages.Request_optimized);
        private final CheckBox optimized = new CheckBox();


        public ItemsConfigDialog(final UndoableActionManager undo, final List<ModelItem> items)
        {
            setTitle(Messages.EditItems);
            setResizable(true);

            final Model model = items.get(0).getModel().get();
            getDialogPane().setContent(createContent(model));

            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            setResultConverter(button ->
            {
                if (button == ButtonType.OK)
                {
                    update(model, undo, items);
                    return true;
                }
                else
                    return false;
            });
        }

        private Node createContent(final Model model)
        {
            final GridPane grid = new GridPane();
            grid.setHgap(5.0);
            grid.setVgap(5.0);

            int row = 0;

            grid.add(new Label("Apply"), 0, row++);

            visible.setSelected(true);
            grid.add(set_visible, 0, row);
            grid.add(visible, 1, row++);
            visible.setOnAction(event -> set_visible.setSelected(true));

            grid.add(set_color, 0, row);
            grid.add(color, 1, row++);
            color.setOnAction(event -> set_color.setSelected(true));

            GridPane.setHgrow(period, Priority.ALWAYS);
            grid.add(set_period, 0, row);
            grid.add(period, 1, row++);
            period.setOnKeyPressed(event -> set_period.setSelected(true));

            grid.add(set_buffer, 0, row);
            grid.add(buffer, 1, row++);
            buffer.setOnKeyPressed(event -> set_buffer.setSelected(true));

            axis.setMaxWidth(Double.MAX_VALUE);
            axis.getItems().setAll(model.getAxes()
                                        .stream()
                                        .map(AxisConfig::getResolvedName)
                                        .collect(Collectors.toList()));
            grid.add(set_axis, 0, row);
            grid.add(axis, 1, row++);
            axis.setOnAction(event -> set_axis.setSelected(true));

            type.setMaxWidth(Double.MAX_VALUE);
            type.getItems().setAll(TraceType.getDisplayNames());
            type.getSelectionModel().select(TraceType.AREA.ordinal());
            grid.add(set_type, 0, row);
            grid.add(type, 1, row++);
            type.setOnAction(event -> set_type.setSelected(true));

            grid.add(set_width, 0, row);
            grid.add(width, 1, row++);
            width.setOnKeyPressed(event -> set_width.setSelected(true));

            style.setMaxWidth(Double.MAX_VALUE);
            style.getItems().setAll(LineStyle.getDisplayNames());
            style.getSelectionModel().select(LineStyle.SOLID.ordinal());
            grid.add(set_style, 0, row);
            grid.add(style, 1, row++);
            style.setOnAction(event -> set_style.setSelected(true));

            point.setMaxWidth(Double.MAX_VALUE);
            point.getItems().setAll(PointType.getDisplayNames());
            point.getSelectionModel().select(PointType.NONE.ordinal());
            grid.add(set_point, 0, row);
            grid.add(point, 1, row++);
            point.setOnAction(event -> set_point.setSelected(true));

            grid.add(set_size, 0, row);
            grid.add(size, 1, row++);
            size.setOnKeyPressed(event -> set_size.setSelected(true));

            optimized.setSelected(true);
            grid.add(set_optimized, 0, row);
            grid.add(optimized, 1, row++);
            optimized.setOnAction(event -> set_optimized.setSelected(true));

            return grid;
        }

        private void update(final Model model, final UndoableActionManager undo, final List<ModelItem> items)
        {
            try
            {
                final double secs = Double.parseDouble(period.getText().strip());
                final int buf_size = Integer.parseInt(buffer.getText().strip());
                final int line_width = Integer.parseInt(width.getText().strip());
                final int point_size = Integer.parseInt(size.getText().strip());

                for (ModelItem item : items)
                {
                    if (set_visible.isSelected()  &&  item.isVisible() != visible.isSelected())
                        new ChangeVisibilityCommand(undo, item, visible.isSelected());

                    if (set_color.isSelected()    &&  !item.getPaintColor().equals(color.getValue()))
                        new ChangeColorCommand(undo, item, color.getValue());

                    if (item instanceof PVItem)
                    {
                        final PVItem pv = (PVItem) item;
                        if (set_period.isSelected()  &&  pv.getScanPeriod() != secs)
                            new ChangeSamplePeriodCommand(undo, pv, secs);

                        if (set_buffer.isSelected()  &&  pv.getLiveCapacity() != buf_size)
                            new ChangeLiveCapacityCommand(undo, pv, buf_size);

                        final RequestType request = optimized.isSelected() ? RequestType.OPTIMIZED : RequestType.RAW;
                        if (set_optimized.isSelected()  &&  pv.getRequestType() != request)
                            new ChangeRequestTypeCommand(undo, pv, request);
                    }

                    int sel = axis.getSelectionModel().getSelectedIndex();
                    if (set_axis.isSelected()  &&  sel >= 0  &&  item.getAxisIndex() != sel)
                        new ChangeAxisCommand(undo, item, model.getAxis(sel));

                    final TraceType trace_type = TraceType.values()[type.getSelectionModel().getSelectedIndex()];
                    if (set_type.isSelected()  &&  item.getTraceType() != trace_type)
                        new ChangeTraceTypeCommand(undo, item, trace_type);

                    if (set_width.isSelected()  &&  item.getLineWidth() != line_width)
                        new ChangeLineWidthCommand(undo, item, line_width);

                    final LineStyle line_style = LineStyle.values()[style.getSelectionModel().getSelectedIndex()];
                    if (set_style.isSelected()  &&  item.getLineStyle() != line_style)
                        new ChangeLineStyleCommand(undo, item, line_style);

                    final PointType point_type = PointType.values()[point.getSelectionModel().getSelectedIndex()];
                    if (set_point.isSelected()  &&  item.getPointType() != point_type)
                        new ChangePointTypeCommand(undo, item, point_type);

                    if (set_size.isSelected()  &&  item.getPointSize() != point_size)
                        new ChangePointSizeCommand(undo, item, point_size);
                }
            }
            catch (Exception ex)
            {
                ExceptionDetailsErrorDialog.openError(Messages.EditItems, "Failed to update items", ex);
            }
        }
    }

    public EditMultipleItemsAction(final Node parent,
                                   final Model model, final UndoableActionManager undo,
                                   final List<ModelItem> selected)
    {
        super(Messages.EditItems, Activator.getIcon("configure"));
        setOnAction(event ->
        {
            final ItemsConfigDialog dialog = new ItemsConfigDialog(undo, selected);
            DialogHelper.positionDialog(dialog, parent, -500, -550);
            dialog.initOwner(parent.getScene().getWindow());
            dialog.showAndWait();
        });
    }
}

/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import java.time.Instant;
import java.time.temporal.TemporalAmount;

import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelListener;
import org.csstudio.trends.databrowser3.preferences.Preferences;
import org.csstudio.trends.databrowser3.ui.ChangeTimerangeAction;
import org.phoebus.ui.undo.UndoableActionManager;
import org.phoebus.util.time.TimeParser;
import org.phoebus.util.time.TimeRelativeInterval;
import org.phoebus.util.time.TimestampFormats;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/** Property tab for time axis
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TimeAxisTab extends Tab
{
    private final Model model;

    private static final Tooltip times_tt = new Tooltip(Messages.StartEndDialogTT);
    private final TextField start = new TextField(),
                            end = new TextField();
    private final CheckBox grid = new CheckBox();

    /** Flag to prevent recursion when this tab updates the model and thus triggers the model_listener */
    private boolean updating = false;


    /** Update Tab when model changes (undo, ...) */
    private ModelListener model_listener = new ModelListener()
    {
        @Override
        public void changedTimerange()
        {
            if (updating)
                return;

            final String[] range = model.getTimerangeText();
            start.setText(range[0]);
            end.setText(range[1]);
        }

        @Override
        public void changedTimeAxisConfig()
        {
            if (updating)
                return;
            grid.setSelected(model.isGridVisible());
        }
    };

    TimeAxisTab(final Model model, final UndoableActionManager undo)
    {
        super(Messages.TimeAxis);
        this.model = model;

        final GridPane layout = new GridPane();
        layout.setHgap(5);
        layout.setVgap(5);
        layout.setPadding(new Insets(5));
        // layout.setGridLinesVisible(true); // Debug layout

        layout.add(new Label(Messages.StartTimeLbl), 0, 0);
        GridPane.setHgrow(start, Priority.ALWAYS);
        layout.add(start, 1, 0);

        layout.add(new Label(Messages.EndTimeLbl), 2, 0);
        GridPane.setHgrow(end, Priority.ALWAYS);
        layout.add(end, 3, 0);

        final Button times = new Button(Messages.StartEndDialogBtn, Activator.getIcon("time_range"));
        times.setTooltip(times_tt);
        times.setOnAction(event ->  ChangeTimerangeAction.run(model, layout, undo));
        layout.add(times, 4, 0);

        layout.add(new Label(Messages.GridLbl), 0, 1);
        layout.add(grid, 1, 1);

        final HBox presets = new HBox(5);
        for (Preferences.TimePreset preset : Preferences.time_presets)
        {
            final Button button = new Button(preset.label);
            button.setOnAction(event ->
            {
                updating = true;
                new ChangeTimerangeCommand(model, undo, preset.range);
                updating = false;
                model_listener.changedTimerange();
            });
            presets.getChildren().add(button);
        }
        layout.add(presets, 1, 2, 4, 1);

        setContent(layout);

        model.addListener(model_listener);


        // Thos Initial values
        model_listener.changedTimerange();
        model_listener.changedTimeAxisConfig();

        // Handle entered time range
        final EventHandler<ActionEvent> set_timerange = event ->
        {
            String text = start.getText();
            final Instant abs_start = TimestampFormats.parse(text);
            final TemporalAmount rel_start = TimeParser.parseTemporalAmount(text);

            text = end.getText();
            final Instant abs_end = TimestampFormats.parse(text);

            TimeRelativeInterval range = null;
            if (abs_start != null  &&  abs_end != null)
                range = TimeRelativeInterval.of(abs_start, abs_end);
            else if (rel_start != null)
                range = TimeRelativeInterval.startsAt(rel_start);

            updating = true;
            // If something useful was entered, use it
            if (range != null)
                new ChangeTimerangeCommand(model, undo, range);
            updating = false;
            // In any case, show the result,
            // which might turn an entered "2 mo" into "2 months"
            model_listener.changedTimerange();
        };
        start.setOnAction(set_timerange);
        end.setOnAction(set_timerange);

        grid.setOnAction(event ->
        {
            updating = true;
            new ChangeTimeAxisConfigCommand(model, undo, grid.isSelected());
            updating = false;
        });
    }
}

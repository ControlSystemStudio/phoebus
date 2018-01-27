/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelListener;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

/** Property tab for time axis
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TimeAxisTab extends Tab
{
    private final Model model;

    private final TextField start = new TextField("TODO"),
                            end = new TextField("TODO");
    private final CheckBox grid = new CheckBox();

    /** Flag to prevent recursion when this tab updates the model and thus triggers the model_listener */
    private boolean updating = false;


    /** Update Tab when model changes (undo, ...) */
    private ModelListener model_listener = new ModelListener()
    {
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
        layout.add(start, 1, 0);

        layout.add(new Label(Messages.EndTimeLbl), 2, 0);
        layout.add(end, 3, 0);

        final Button times = new Button(Messages.StartEndDialogBtn);
        layout.add(times, 4, 0);

        // TODO Preferences.getTimespanShortcuts();
        final String[][] shortcuts = new String[][]
        {
            { "30 Minutes", "" },
            { "1 Hour", "" },
            { "12 Hours", "" },
            { "1 Day", "" },
            { "7 Days", "" },
        };
        final HBox shortcut_bar = new HBox(5);
        for (String[] title_range : shortcuts)
        {
            final String range = title_range[1];
            final Button shortcut = new Button(title_range[0]);
            shortcut_bar.getChildren().add(shortcut);
            shortcut.setOnAction(event ->
            {
                System.err.println("TODO: Select " + range);
            });
        }
        layout.add(shortcut_bar, 1, 1, 3, 1);


        layout.add(new Label(Messages.GridLbl), 0, 2);
        layout.add(grid, 1, 2);
        grid.setOnAction(event ->
        {
            updating = true;
            new ChangeTimeAxisConfigCommand(model, undo, grid.isSelected());
            updating = false;
        });

        setContent(layout);

        model.addListener(model_listener);

        // Initial values
        model_listener.changedTimeAxisConfig();
    }
}

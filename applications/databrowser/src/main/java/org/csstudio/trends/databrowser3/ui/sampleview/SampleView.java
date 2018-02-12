/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.sampleview;

import java.util.stream.Collectors;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.PlotSample;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/** Panel for inspecting samples of a trace
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SampleView extends GridPane
{
    private final Model model;
    private final ComboBox<String> items = new ComboBox<>();
    private final TableView<PlotSample> sample_table = new TableView<>();

    public SampleView(final Model model)
    {
        this.model = model;

        final GridPane layout = this;
        layout.setGridLinesVisible(true);
        layout.setHgap(5);
        layout.setVgap(5);

        layout.add(new Label(Messages.SampleView_Item), 0, 0);
        items.setMaxWidth(Double.MAX_VALUE);
        layout.add(items, 1, 0);

        final Button refresh = new Button(Messages.SampleView_Refresh);
        refresh.setOnAction(event -> update());
        layout.add(refresh, 2, 0);

        createSampleTable();
        layout.add(sample_table, 0, 1, 3, 1);

        final ColumnConstraints middle = new ColumnConstraints();
        middle.setHgrow(Priority.ALWAYS);
        middle.setFillWidth(true);
        layout.getColumnConstraints().setAll(new ColumnConstraints(),
                                             middle);

        update();
    }

    private void createSampleTable()
    {
        TableColumn<PlotSample, String> col = new TableColumn<>(Messages.TimeColumn);
        sample_table.getColumns().add(col);

        col = new TableColumn<>(Messages.ValueColumn);
        sample_table.getColumns().add(col);

        col = new TableColumn<>(Messages.SeverityColumn);
        sample_table.getColumns().add(col);

        col = new TableColumn<>(Messages.StatusColumn);
        sample_table.getColumns().add(col);

        col = new TableColumn<>(Messages.SampleView_Source);
        sample_table.getColumns().add(col);

        sample_table.setMaxWidth(Double.MAX_VALUE);
        // TODO Auto-generated method stub
    }

    private void update()
    {
        items.getItems().setAll( model.getItems().stream().map(item -> item.getName()).collect(Collectors.toList()) );
    }
}

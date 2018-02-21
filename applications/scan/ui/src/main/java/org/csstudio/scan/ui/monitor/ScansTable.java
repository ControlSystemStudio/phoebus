/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.monitor;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.csstudio.scan.client.ScanClient;
import org.csstudio.scan.info.ScanInfo;
import org.csstudio.scan.info.ScanServerInfo;
import org.csstudio.scan.info.ScanState;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.util.time.TimestampFormats;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/** Table for {@link ScanInfo}s
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScansTable extends VBox
{
    private final ScanClient scan_client;

    /** Scans to show */
    private final ObservableList<ScanInfoProxy> scans = FXCollections.observableArrayList();

    /** To allow sorting via table column header clicks
     *  while the data is updated, see
     *  https://bugs.openjdk.java.net/browse/JDK-8092759 :
     *  Present data to table as a SortedList,
     *  with comparator of the list bound to the table's comparator
     */
    private final SortedList<ScanInfoProxy> sorted_scans = new SortedList<>(scans);

    /** Table of scan infos (via property-based proxy) */
    private final TableView<ScanInfoProxy> scan_table = new TableView<>(sorted_scans);

    private final Label status = new Label();

    /** Table cell for {@link Instant} */
    private static class InstantCell extends TableCell<ScanInfoProxy, Instant>
    {
        @Override
        protected void updateItem(final Instant instant, final boolean empty)
        {
            super.updateItem(instant, empty);
            if (empty)
                setText("");
            else
                setText(TimestampFormats.formatCompactDateTime(instant));
        }
    }

    /** Table cell for percentage (progress) */
    private static class PercentCell extends TableCell<ScanInfoProxy, Number>
    {
        private ProgressBar progress;

        @Override
        protected void updateItem(final Number percent, final boolean empty)
        {
            super.updateItem(percent, empty);
            final TableRow<ScanInfoProxy> row = getTableRow();
            if (empty  ||  row == null  ||  row.getItem() == null)
                progress = null;
            else
            {
                if (progress == null)
                {
                    progress = new ProgressBar(percent.intValue()/100.0);
                    progress.setMaxWidth(Double.MAX_VALUE);
                }
                else
                    progress.setProgress(percent.intValue()/100.0);

                final Color color = StateCell.getStateColor(row.getItem().state.get());
                progress.setStyle(String.format("-fx-accent: #%02x%02x%02x;",
                                                (int) (color.getRed()*255),
                                                (int) (color.getGreen()*255),
                                                (int) (color.getBlue()*255)));

                final String txt = MessageFormat.format("Executed {0} of {1} commands, i.e. {2} %",
                        row.getItem().info.getPerformedWorkUnits(),
                        row.getItem().info.getTotalWorkUnits(),
                        percent.intValue());
                progress.setTooltip(new Tooltip(txt));
            }
            setGraphic(progress);
        }
    }

    public ScansTable(final ScanClient scan_client)
    {
        this.scan_client = scan_client;

        createTable();
        update((ScanServerInfo) null);

        status.setMaxWidth(Double.MAX_VALUE);
        status.setAlignment(Pos.BASELINE_CENTER);

        VBox.setVgrow(scan_table, Priority.ALWAYS);
        getChildren().setAll(scan_table, status);
        createContextMenu();
    }

    private void createTable()
    {
        final TableColumn<ScanInfoProxy, Number> id_col = new TableColumn<>("ID");
        id_col.setPrefWidth(40);
        id_col.setStyle("-fx-alignment: CENTER-RIGHT;");
        id_col.setCellValueFactory(cell -> cell.getValue().id);
        scan_table.getColumns().add(id_col);

        final TableColumn<ScanInfoProxy, Instant> create_col = new TableColumn<>("Created");
        create_col.setCellValueFactory(cell -> cell.getValue().created);
        create_col.setCellFactory(cell -> new InstantCell());
        scan_table.getColumns().add(create_col);

        final TableColumn<ScanInfoProxy, String> name_col = new TableColumn<>("Name");
        name_col.setCellValueFactory(cell -> cell.getValue().name);
        scan_table.getColumns().add(name_col);

        final TableColumn<ScanInfoProxy, ScanState> state_col = new TableColumn<>("State");
        state_col.setPrefWidth(210);
        state_col.setCellValueFactory(cell -> cell.getValue().state);
        state_col.setCellFactory(cell -> new StateCell(scan_client));
        state_col.setComparator((a, b) ->  rankState(a) - rankState(b));
        scan_table.getColumns().add(state_col);

        final TableColumn<ScanInfoProxy, Number> perc_col = new TableColumn<>("%");
        perc_col.setPrefWidth(50);
        perc_col.setCellValueFactory(cell -> cell.getValue().percent);
        perc_col.setCellFactory(cell -> new PercentCell());
        scan_table.getColumns().add(perc_col);

        final TableColumn<ScanInfoProxy, String>  rt_col = new TableColumn<>("Runtime");
        rt_col.setCellValueFactory(cell -> cell.getValue().runtime);
        scan_table.getColumns().add(rt_col);

        final TableColumn<ScanInfoProxy, Instant> finish_col = new TableColumn<>("Finish");
        finish_col.setCellValueFactory(cell -> cell.getValue().finish);
        finish_col.setCellFactory(cell -> new InstantCell());
        scan_table.getColumns().add(finish_col);

        final TableColumn<ScanInfoProxy, String> cmd_col = new TableColumn<>("Command");
        cmd_col.setPrefWidth(200);
        cmd_col.setCellValueFactory(cell -> cell.getValue().command);
        scan_table.getColumns().add(cmd_col);

        TableColumn<ScanInfoProxy, String> err_col = new TableColumn<>("Error");
        err_col.setCellValueFactory(cell -> cell.getValue().error);
        scan_table.getColumns().add(err_col);

        // Last column fills remaining space
        err_col.prefWidthProperty().bind(scan_table.widthProperty()
                                                   .subtract(id_col.widthProperty())
                                                   .subtract(create_col.widthProperty())
                                                   .subtract(name_col.widthProperty())
                                                   .subtract(state_col.widthProperty())
                                                   .subtract(perc_col.widthProperty())
                                                   .subtract(rt_col.widthProperty())
                                                   .subtract(finish_col.widthProperty())
                                                   .subtract(cmd_col.widthProperty())
                                                   .subtract(2));

        scan_table.setPlaceholder(new Label("No Scans"));

        scan_table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        sorted_scans.comparatorProperty().bind(scan_table.comparatorProperty());
    }

    /** Rank states
     *
     *  <p>Assumes that 'Running' is most interesting and 'Logged' is at the bottom
     *
     *  <p>Intermediate states are ranked somewhat arbitrarily
     *
     *  @param state ScanState
     *  @return
     */
    private static int rankState(final ScanState state)
    {
        switch (state)
        {
        case Running: // Most important, happening right now
            return 6;
        case Paused:  // Very similar to a running state
            return 5;
        case Idle:    // About to run next
            return 4;
        case Failed:  // Of the not running ones, failure is important to know
            return 3;
        case Aborted: // Aborted on purpose
            return 2;
        case Finished:// Water down the bridge
            return 1;
        case Logged:
        default:
            return 0;
        }
    }

    private void createContextMenu()
    {
        final MenuItem abort_all = new MenuItem("Abort All Scans", ImageCache.getImageView(ScansTable.class, "/icons/abort.png"));
        abort_all.setOnAction(event ->
            JobManager.schedule(abort_all.getText(), monitor ->  scan_client.abortScan(-1)));

        final MenuItem remove_completed = new MenuItem("Remove completed Scans",  ImageCache.getImageView(ScansTable.class, "/icons/remove_completed.png"));
        remove_completed.setOnAction(event ->
            JobManager.schedule(remove_completed.getText(), monitor -> scan_client.removeCompletedScans()));

        final ContextMenu menu = new ContextMenu();
        scan_table.setOnContextMenuRequested(event ->
        {
            // Update menu based on selected scan and states of scans in the table
            menu.getItems().clear();

            final List<ScanInfo> selection = scan_table.getSelectionModel().getSelectedItems().stream().map(proxy -> proxy.info).collect(Collectors.toList());
            if (selection.size() == 1  &&  selection.get(0).getState() != ScanState.Logged)
            {
                menu.getItems().add(new ReSubmitScanAction(scan_client, selection.get(0)));
                menu.getItems().add(new SaveScanAction(this, scan_client, selection.get(0)));
                // TODO open scan data monitor,
                // TODO open scan data plot
                // TODO open scan in editor,
            }

            boolean any_to_abort = false;
            boolean any_completed = false;
            for (ScanInfoProxy info : scans)
            {
                final ScanState state = info.state.get();
                if (state.isActive())
                    any_to_abort = true;
                if (state.isDone())
                    any_completed = true;
                if (any_to_abort && any_completed)
                    break;
            }

            if (any_to_abort)
                menu.getItems().add(abort_all);

            for (ScanInfo info : selection)
                if (info.getState().isDone())
                {
                    menu.getItems().add(new RemoveSelectedScansAction(scan_client, selection));
                    break;
                }

            if (any_completed)
                menu.getItems().add(remove_completed);
            menu.show(scan_table.getScene().getWindow(), event.getScreenX(), event.getScreenY());
        });
    }

    /** @param infos {@link ScanInfo}s to display in the table */
    public void update(final List<ScanInfo> infos)
    {
        int i;
        for (i=0; i<infos.size(); ++i)
        {
            if (i < scans.size())
                scans.get(i).updateFrom(infos.get(i));
            else
                scans.add(new ScanInfoProxy(infos.get(i)));
        }
        i = scans.size();
        while (i > infos.size())
            scans.remove(--i);
    }

    public void update(final ScanServerInfo server_info)
    {
        final String text;
        if (server_info == null)
            text = "- No Scan Server Connection -";
        else
            text = "Scan Server " + server_info.getMemoryInfo();
        status.setText(text);
    }
}

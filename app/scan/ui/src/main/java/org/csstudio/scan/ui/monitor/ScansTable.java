/*******************************************************************************
 * Copyright (c) 2018-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.monitor;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.csstudio.scan.client.ScanClient;
import org.csstudio.scan.info.ScanInfo;
import org.csstudio.scan.info.ScanServerInfo;
import org.csstudio.scan.info.ScanState;
import org.csstudio.scan.ui.ScanUIPreferences;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.ui.application.ContextMenuHelper;
import org.phoebus.ui.application.TableHelper;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.util.time.TimestampFormats;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.converter.DefaultStringConverter;

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

    /** Table cell for read-only text, allow copying it out */
    private static class TextCopyCell extends TextFieldTableCell<ScanInfoProxy, String>
    {
        public TextCopyCell()
        {
            super(new DefaultStringConverter());
        }

        @Override
        public void startEdit()
        {
            super.startEdit();
            final TextField text = (TextField) getGraphic();
            if (text != null)
                text.setEditable(false);
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
        showStatusbar(ScanUIPreferences.monitor_status);
    }

    private void createTable()
    {
        // Don't really allow editing, just copying from some of the text fields
        scan_table.setEditable(true);

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
        name_col.setCellFactory(info -> new TextCopyCell());
        name_col.setEditable(true);
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
        cmd_col.setCellFactory(info -> new TextCopyCell());
        cmd_col.setEditable(true);
        scan_table.getColumns().add(cmd_col);

        TableColumn<ScanInfoProxy, String> err_col = new TableColumn<>("Error");
        err_col.setCellValueFactory(cell -> cell.getValue().error);
        err_col.setCellFactory(info -> new TextCopyCell());
        err_col.setEditable(true);
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

    List<TableColumn<ScanInfoProxy,?>> getTableColumns()
    {
        return scan_table.getColumns();
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
        final MenuItem server_info = new MenuItem("Scan Server Info", ImageCache.getImageView(ImageCache.class, "/icons/info.png"));
        server_info.setOnAction(event -> JobManager.schedule(server_info.getText(), this::showServerInfo));

        final MenuItem abort_all = new MenuItem("Abort All Scans", ImageCache.getImageView(ScansTable.class, "/icons/abort.png"));
        abort_all.setOnAction(event ->
            JobManager.schedule(abort_all.getText(), monitor ->  scan_client.abortScan(-1)));

        final MenuItem remove_completed = new MenuItem("Remove completed Scans",  ImageCache.getImageView(ImageCache.class, "/icons/remove_multiple.png"));
        remove_completed.setOnAction(event ->
            JobManager.schedule(remove_completed.getText(), monitor -> scan_client.removeCompletedScans()));

        final MenuItem toggle_status = new MenuItem("", ImageCache.getImageView(ImageCache.class, "/icons/info.png"));
        toggle_status.setOnAction(event -> showStatusbar(! isStatusbarVisible()));

        final ContextMenu menu = new ContextMenu();
        scan_table.setOnContextMenuRequested(event ->
        {
            // Update menu based on selected scan and states of scans in the table
            // Start with benign, "read only" commands, then end with commands that
            // do something like re-submit, abort, remove
            menu.getItems().setAll(server_info, new SeparatorMenuItem());
            if (TableHelper.addContextMenuColumnVisibilityEntries(scan_table, menu))
                menu.getItems().add(new SeparatorMenuItem());

            final List<ScanInfo> selection = scan_table.getSelectionModel().getSelectedItems().stream().map(proxy -> proxy.info).collect(Collectors.toList());
            if (selection.size() == 1)
            {
                // View data, plot, possibly commands
                menu.getItems().add(new OpenScanDataTableAction(selection.get(0).getId()));
                menu.getItems().add(new OpenScanDataPlotAction(selection.get(0).getId()));

                final boolean have_commands = selection.get(0).getState() != ScanState.Logged;
                if (have_commands)
                {
                    menu.getItems().add(new OpenScanEditorAction(selection.get(0).getId()));
                    menu.getItems().add(new SaveScanAction(this, scan_client, selection.get(0)));
                }

                menu.getItems().add(new SeparatorMenuItem());

                // Resubmit
                if (have_commands)
                {
                    menu.getItems().add(new ReSubmitScanAction(scan_client, selection.get(0)));
                    menu.getItems().add(new SeparatorMenuItem());
                }
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

            toggle_status.setText(isStatusbarVisible() ? "Hide Status" : "Show Status");
            menu.getItems().add(toggle_status);

            menu.show(scan_table.getScene().getWindow(), event.getScreenX(), event.getScreenY());
        });
    }

    private void showServerInfo(final JobMonitor monitor)
    {
        Node content;
        try
        {
            final GridPane grid = new GridPane();
            content = grid;

            int row = 0;
            final ScanServerInfo info = scan_client.getServerInfo();
            grid.add(new Label("Version: "), 0, row);
            TextField text = new TextField(info.getVersion());
            text.setEditable(false);
            GridPane.setHgrow(text, Priority.ALWAYS);
            grid.add(text, 1, row);

            grid.add(new Label("Started: "), 0, ++row);
            text = new TextField(TimestampFormats.SECONDS_FORMAT.format(info.getStartTime()));
            text.setEditable(false);
            GridPane.setHgrow(text, Priority.ALWAYS);
            grid.add(text, 1, row);

            grid.add(new Label("Configuration: "), 0, ++row);
            text = new TextField(info.getScanConfig());
            text.setEditable(false);
            GridPane.setHgrow(text, Priority.ALWAYS);
            grid.add(text, 1, row);

            grid.add(new Label("Script Paths: "), 0, ++row);
            text = new TextField(info.getScriptPaths().stream().collect(Collectors.joining(", ")));
            text.setEditable(false);
            GridPane.setHgrow(text, Priority.ALWAYS);
            grid.add(text, 1, row);
        }
        catch (Exception ex)
        {
            final StringBuilder buf = new StringBuilder();
            final ByteArrayOutputStream tmp = new ByteArrayOutputStream();
            final PrintWriter out = new PrintWriter(tmp);
            out.println("Cannot get scan server information,");
            out.println("host " + scan_client.getHost() + ", port " + scan_client.getPort());
            out.println();
            ex.printStackTrace(out);
            out.flush();
            out.close();
            buf.append(tmp.toString());
            content = new TextArea(buf.toString());
        }

        final Node the_content = content;
        Platform.runLater(() ->
        {
            final Alert dlg = new Alert(AlertType.INFORMATION);
            dlg.setTitle("Scan Server Info");
            dlg.setHeaderText("");
            dlg.getDialogPane().setContent(the_content);
            dlg.setResizable(true);
            dlg.getDialogPane().setPrefWidth(800);

            DialogHelper.positionDialog(dlg, this, -200, -200);
            dlg.showAndWait();
        });
    }

    /** @param infos {@link ScanInfo}s to display in the table */
    public void update(final List<ScanInfo> infos)
    {
        int i;
        for (i=0; i<infos.size(); ++i)
        {
            if (i < scans.size())
            {
                final boolean state_changed = scans.get(i).updateFrom(infos.get(i));
                // Did the state change, it's not idle, but the row above is idle?
                if (state_changed                             &&
                    i > 0                                     &&
                    infos.get(i).getState() != ScanState.Idle &&
                    infos.get(i-1).getState() == ScanState.Idle)
                {
                    // Then 'move down' button in row above needs to be disabled,
                    // so force a cell refresh
                    // System.out.println("Must update state display for " + infos.get(i-1));
                    scans.get(i-1).state.set(null);
                    scans.get(i-1).state.set(infos.get(i-1).getState());
                }
            }
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

    private void showStatusbar(final boolean show)
    {
        if (show)
            getChildren().setAll(scan_table, status);
        else
            getChildren().setAll(scan_table);
        ScanUIPreferences.setMonitorStatus(show);
    }

    private boolean isStatusbarVisible()
    {
        return getChildren().size() > 1;
    }

    void save(final Memento memento) {
        int i = 0;
        for (TableColumn<?,?> col : getTableColumns())
            memento.setNumber("COL" + i++, col.getWidth());
        TableHelper.saveColumnVisibilities(scan_table, memento, (col, idx) -> "COL" + idx + "vis");
    }

    void restore(final Memento memento) {
        final List<TableColumn<ScanInfoProxy, ?>> columns = getTableColumns();
        // Don't restore width of the last column, the "Error",
        // because its pref.width is bound to a computation from table width
        // and sum of other columns
        for (int i=0; i<columns.size()-1; ++i)
        {
            final TableColumn<?, ?> col = columns.get(i);
            memento.getNumber("COL" + i).ifPresent(wid -> col.setPrefWidth(wid.doubleValue()));
        }
        TableHelper.restoreColumnVisibilities(scan_table, memento, (col, idx) -> "COL" + idx + "vis");
    }
}

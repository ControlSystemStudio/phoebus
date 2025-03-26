/*******************************************************************************
 * Copyright (c) 2018-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.table;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.scene.control.TableColumnBase;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.ui.AlarmContextMenuHelper;
import org.phoebus.applications.alarm.ui.AlarmUI;
import org.phoebus.applications.alarm.ui.tree.ConfigureComponentAction;
import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.ui.application.ContextMenuHelper;
import org.phoebus.ui.application.ContextMenuService;
import org.phoebus.ui.application.SaveSnapshotAction;
import org.phoebus.ui.application.TableHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.Brightness;
import org.phoebus.ui.javafx.ClearingTextField;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.JFXUtil;
import org.phoebus.ui.javafx.PrintAction;
import org.phoebus.ui.javafx.Screenshot;
import org.phoebus.ui.javafx.ToolbarHelper;
import org.phoebus.ui.selection.AppSelection;
import org.phoebus.ui.spi.ContextMenuEntry;
import org.phoebus.ui.text.RegExHelper;
import org.phoebus.util.text.CompareNatural;
import org.phoebus.util.time.TimestampFormats;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

/** Alarm Table UI
 *
 *  <p>Show list of active and acknowledged alarms.
 *
 *  <p>While implemented as {@link BorderPane},
 *  only the methods defined in here should be called
 *  to interact with it.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmTableUI extends BorderPane
{
    private final AlarmClient client;

    // Sorting:
    //
    // TableView supports sorting as a default when user clicks on columns.
    //
    // LinkedColumnSorter updates the requested sort in the 'other' table.
    // Wrapping the raw data into a SortedList persists the sort order
    // when elements are added/removed in the original data.
    // https://stackoverflow.com/questions/34889111/how-to-sort-a-tableview-programmatically
    //
    // Adding a callback to the observableArrayList instructs the list to also
    // trigger a re-sort when properties of the existing rows change.
    // https://rterp.wordpress.com/2015/05/08/automatically-sort-a-javafx-tableview/
    private final ObservableList<AlarmInfoRow> active_rows = FXCollections.observableArrayList(AlarmInfoRow.CHANGING_PROPERTIES);
    private final ObservableList<AlarmInfoRow> acknowledged_rows = FXCollections.observableArrayList(AlarmInfoRow.CHANGING_PROPERTIES);

    private final SplitPane split;

    private final Label active_count = new Label("Active Alarms");
    private final Label acknowledged_count = new Label("Acknowledged Alarms");

    private final TableView<AlarmInfoRow> active = createTable(active_rows, true);
    private final TableView<AlarmInfoRow> acknowledged = createTable(acknowledged_rows, false);

    final TextField search = new ClearingTextField();

    private final Label no_server = AlarmUI.createNoServerLabel();

    private final Button server_mode = new Button();

    private final Button server_notify = new Button();

    private final ToolBar toolbar;

    /** Enable dragging the PV name from a table cell.
     *  @param cell Table cell
     */
    static void enablePVDrag(TableCell<AlarmInfoRow, ?> cell)
    {
        // Tried to use table.setOnDragDetected() to drag PV names
        // from all selected cells, but with drag enabled on the table
        // it is no longer possible to resize columns:
        // Moving a column divider starts a drag.
        // So now hooking drag to table cell.
        cell.setOnDragDetected(event ->
        {
            // Anything to drag?
            if (cell.getTableRow() == null  ||  cell.getTableRow().getItem() == null)
                return;

            final Dragboard db = cell.startDragAndDrop(TransferMode.COPY);
            final ClipboardContent content = new ClipboardContent();
            content.putString(cell.getTableRow().getItem().pv.get());
            db.setContent(content);
            event.consume();
        });
    }

    /** Table cell for PV, allows dragging the PV name */
    private static class DragPVCell extends TableCell<AlarmInfoRow, String>
    {
        public DragPVCell()
        {
            enablePVDrag(this);
        }

        @Override
        protected void updateItem(String item, boolean empty)
        {
            super.updateItem(item, empty);
            if (empty  ||  item == null)
                setText("");
            else
                setText(item);
        }
    }

    /** Table cell that shows a Severity as Icon */
    private class SeverityIconCell extends TableCell<AlarmInfoRow, SeverityLevel>
    {
        public SeverityIconCell()
        {
            enablePVDrag(this);
        }

        @Override
        protected void updateItem(final SeverityLevel item, final boolean empty)
        {
            super.updateItem(item, empty);

            if (empty  ||  item == null)
                setGraphic(null);
            else
                setGraphic(new ImageView(AlarmUI.getIcon(item)));
        }
    }

    /** Table cell that shows a Severity as text */
    private class SeverityLevelCell extends TableCell<AlarmInfoRow, SeverityLevel>
    {
        public SeverityLevelCell()
        {
            enablePVDrag(this);
        }

        @Override
        protected void updateItem(final SeverityLevel severityLevel, final boolean empty)
        {
            super.updateItem(severityLevel, empty);

            if (empty  ||  severityLevel == null)
            {
                setStyle("-fx-text-fill: black;  -fx-background-color: transparent");
                setText("");
            }
            else
            {
                setText(severityLevel.toString());
                if (AlarmSystem.alarm_table_color_legacy_background)
                {
                    Color legacyBackgroundColor = AlarmUI.getLegacyTableBackground(severityLevel);
                    Color legacyTextColor;
                    if (Brightness.of(legacyBackgroundColor) < Brightness.BRIGHT_THRESHOLD) {
                        legacyTextColor = Color.WHITE;
                    }
                    else {
                        legacyTextColor = Color.BLACK;
                    }
                    setStyle("-fx-alignment: center; -fx-border-color: transparent; -fx-border-width: 2 0 2 0; -fx-background-insets: 2 0 2 0; -fx-text-fill: " + JFXUtil.webRGB(legacyTextColor) + ";  -fx-background-color: " + JFXUtil.webRGB(legacyBackgroundColor));

                }
                else
                {
                    setStyle("-fx-alignment: center; -fx-border-color: transparent; -fx-border-width: 2 0 2 0; -fx-background-insets: 2 0 2 0; -fx-text-fill: " + JFXUtil.webRGB(AlarmUI.getColor(severityLevel)) + ";  -fx-background-color: " + JFXUtil.webRGB(AlarmUI.getBackgroundColor(severityLevel)));
                }
            }
        }
    }

    /** Table cell that shows a time stamp */
    private class TimeCell extends TableCell<AlarmInfoRow, Instant>
    {
        public TimeCell()
        {
            enablePVDrag(this);
        }

        @Override
        protected void updateItem(final Instant item, final boolean empty)
        {
            super.updateItem(item, empty);

            if (empty  ||  item == null)
                setText("");
            else
                setText(TimestampFormats.MILLI_FORMAT.format(item));
        }
    }

    /** @param client Client */
    public AlarmTableUI(final AlarmClient client)
    {
        this.client = client;

        toolbar = createToolbar();

        // When user resizes columns, update them in the 'other' table
        active.setColumnResizePolicy(new LinkedColumnResize(active, acknowledged));
        acknowledged.setColumnResizePolicy(new LinkedColumnResize(acknowledged, active));

        // When user sorts column, apply the same to the 'other' table
        active.getSortOrder().addListener(new LinkedColumnSorter(active, acknowledged));
        acknowledged.getSortOrder().addListener(new LinkedColumnSorter(acknowledged, active));

        // Bind visibility properties of columns between the tables
        for (int i = 0; i < active.getColumns().size(); i++) {
            active.getColumns().get(i).visibleProperty().bindBidirectional(
                    acknowledged.getColumns().get(i).visibleProperty()
            );
        }

        // Insets make ack. count appear similar to the active count,
        // which is laid out based on the ack/unack/search buttons in the toolbar
        acknowledged_count.setPadding(new Insets(10, 0, 10, 5));
        VBox.setVgrow(acknowledged, Priority.ALWAYS);
        final VBox bottom = new VBox(acknowledged_count, acknowledged);

        // Overall layout:
        // Toolbar
        // Top section of split: Active alarms
        // Bottom section o. s.: Ack'ed alarms
        split = new SplitPane(active, bottom);
        split.setOrientation(Orientation.VERTICAL);

        setTop(toolbar);
        setCenter(split);
    }

    ToolBar getToolbar()
    {
        return toolbar;
    }

    private ToolBar createToolbar()
    {
        setMaintenanceMode(false);
        server_mode.setOnAction(event ->  {
            JobManager.schedule(client.isMaintenanceMode() ? "Disable maintenance mode" : "Enable maintenance mode",
                    monitor -> {
                        try {
                            client.setMode(! client.isMaintenanceMode());
                        } catch (Exception e) {
                            Platform.runLater(() -> ExceptionDetailsErrorDialog.openError(e.getMessage(), e));
                        }
                    });

        });

        // Could 'bind',
        //   server_mode.disableProperty().bind(new SimpleBooleanProperty(!AlarmUI.mayModifyMode(client)));
        // but mayModifyModel is not observable, plus setting it only when _disabled_
        // means no actual property is created for the default value, enabled.
        if (!AlarmUI.mayModifyMode(client))
            server_mode.setDisable(true);

        setDisableNotify(false);
        server_notify.setOnAction(event ->  {
            JobManager.schedule(client.isDisableNotify() ? "Enable alarm notification" : "Disable alarm notification", monitor -> {
                try {
                    client.setNotify(! client.isDisableNotify());
                } catch (Exception e) {
                    Platform.runLater(() -> ExceptionDetailsErrorDialog.openError(e.getMessage(), e));
                }
            });
        });
        if (!AlarmUI.mayDisableNotify(client))
            server_notify.setDisable(true);

        final Button acknowledge = new Button("", ImageCache.getImageView(AlarmUI.class, "/icons/acknowledge.png"));
	if (AlarmUI.mayAcknowledge(client))
	    acknowledge.disableProperty().bind(Bindings.isEmpty(active.getSelectionModel().getSelectedItems()));
	else
	    acknowledge.setDisable(true);

        acknowledge.setOnAction(event ->
        {
            for (AlarmInfoRow row : active.getSelectionModel().getSelectedItems())
                JobManager.schedule("ack", monitor -> client.acknowledge(row.item, true));
        });

        final Button unacknowledge = new Button("", ImageCache.getImageView(AlarmUI.class, "/icons/unacknowledge.png"));
	if (AlarmUI.mayAcknowledge(client))
	    unacknowledge.disableProperty().bind(Bindings.isEmpty(acknowledged.getSelectionModel().getSelectedItems()));
	else
	    unacknowledge.setDisable(true);

        unacknowledge.setOnAction(event ->
        {
            for (AlarmInfoRow row : acknowledged.getSelectionModel().getSelectedItems())
                JobManager.schedule("unack", monitor -> client.acknowledge(row.item, false));
        });

        search.setTooltip(new Tooltip("Enter pattern ('vac', 'amp*trip')\nfor PV Name or Description,\npress RETURN to select"));
        search.textProperty().addListener(prop -> selectRows());

    	if (AlarmSystem.disable_notify_visible)
    	    return new ToolBar(active_count,ToolbarHelper.createStrut(), ToolbarHelper.createSpring(), server_mode, server_notify, acknowledge, unacknowledge, search);

    	return new ToolBar(active_count,ToolbarHelper.createStrut(), ToolbarHelper.createSpring(), server_mode, acknowledge, unacknowledge, search);
    }

    /** Show if connected to server or not
     *  @param alive Is server alive?
     */
    void setServerState(final boolean alive)
    {
        final ObservableList<Node> items = toolbar.getItems();
        items.remove(no_server);
        if (! alive)
            // Place to the left of spring, maint, ack, unack, filter,
            // i.e. right of "active alarms" and optional AlarmConfigSelector
            items.add(items.size() - 5, no_server);
    }

    void setMaintenanceMode(final boolean maintenance_mode)
    {
        if (maintenance_mode)
        {
            server_mode.setGraphic(ImageCache.getImageView(AlarmUI.class, "/icons/maintenance_mode.png"));
            server_mode.setTooltip(new Tooltip("Maintenance Mode\nINVALID alarms are not annunciated and automatically acknowledged.\nPress to return to Normal Mode"));
        }
        else
        {
            server_mode.setGraphic(ImageCache.getImageView(AlarmUI.class, "/icons/normal_mode.png"));
            server_mode.setTooltip(new Tooltip("Enable maintenance mode?\n\nIn maintenance mode, INVALID alarms are not annunciated;\nthey are automatically acknowledged.\nThis is meant to reduce the impact of alarm from IOC reboots\nor systems that are turned off for maintenance."));

        }
    }

    void setDisableNotify(final boolean disable_notify)
    {
        if (disable_notify)
        {
            server_notify.setGraphic(ImageCache.getImageView(AlarmUI.class, "/icons/disable_notify.png"));
            server_notify.setTooltip(new Tooltip("Enable email notifications for alarms?\n\nEmail notifications are currently disabled for alarms.\n\nPress to re-enable the email notifications."));
        }
        else
        {
            server_notify.setGraphic(ImageCache.getImageView(AlarmUI.class, "/icons/enable_notify.png"));
            server_notify.setTooltip(new Tooltip("Disable Email notifications for alarms?\n\nEmail notifications for alarms will be disabled."));

        }
    }

    private TableView<AlarmInfoRow> createTable(final ObservableList<AlarmInfoRow> rows,
                                                final boolean active)
    {
        final SortedList<AlarmInfoRow> sorted = new SortedList<>(rows);
        final TableView<AlarmInfoRow> table = new TableView<>(sorted);

        // Ensure that the sorted rows are always updated as the column sorting
        // of the TableView is changed by the user clicking on table headers.
        sorted.comparatorProperty().bind(table.comparatorProperty());

        // Prepare columns.
        final List<TableColumn<AlarmInfoRow, ?>> cols = new ArrayList<>();
        TableColumn<AlarmInfoRow, SeverityLevel> sevcol = new TableColumn<>(/* Icon */);
        sevcol.setPrefWidth(25);
        sevcol.setReorderable(false);
        sevcol.setCellValueFactory(cell -> cell.getValue().severity);
        sevcol.setCellFactory(c -> new SeverityIconCell());
        cols.add(sevcol);

        final TableColumn<AlarmInfoRow, String> pv_col = new TableColumn<>("PV");
        pv_col.setPrefWidth(240);
        pv_col.setReorderable(false);
        pv_col.setCellValueFactory(cell -> cell.getValue().pv);
        pv_col.setCellFactory(c -> new DragPVCell());
        pv_col.setComparator(CompareNatural.INSTANCE);
        cols.add(pv_col);

        TableColumn<AlarmInfoRow, String> col = new TableColumn<>("Description");
        col.setPrefWidth(400);
        col.setReorderable(false);
        col.setCellValueFactory(cell -> cell.getValue().description);
        col.setCellFactory(c -> new DragPVCell());
        col.setComparator(CompareNatural.INSTANCE);
        cols.add(col);

        sevcol = new TableColumn<>("Alarm Severity");
        sevcol.setPrefWidth(130);
        sevcol.setReorderable(false);
        sevcol.setCellValueFactory(cell -> cell.getValue().severity);
        sevcol.setCellFactory(c -> new SeverityLevelCell());
        cols.add(sevcol);

        col = new TableColumn<>("Alarm Message");
        col.setPrefWidth(130);
        col.setReorderable(false);
        col.setCellValueFactory(cell -> cell.getValue().status);
        col.setCellFactory(c -> new DragPVCell());
        cols.add(col);

        TableColumn<AlarmInfoRow, Instant> timecol = new TableColumn<>("Alarm Time");
        timecol.setPrefWidth(200);
        timecol.setReorderable(false);
        timecol.setCellValueFactory(cell -> cell.getValue().time);
        timecol.setCellFactory(c -> new TimeCell());
        cols.add(timecol);

        col = new TableColumn<>("Alarm Value");
        col.setPrefWidth(100);
        col.setReorderable(false);
        col.setCellValueFactory(cell -> cell.getValue().value);
        col.setCellFactory(c -> new DragPVCell());
        cols.add(col);

        sevcol = new TableColumn<>("PV Severity");
        sevcol.setPrefWidth(130);
        sevcol.setReorderable(false);
        sevcol.setCellValueFactory(cell -> cell.getValue().pv_severity);
        sevcol.setCellFactory(c -> new SeverityLevelCell());
        cols.add(sevcol);

        col = new TableColumn<>("PV Message");
        col.setPrefWidth(130);
        col.setReorderable(false);
        col.setCellValueFactory(cell -> cell.getValue().pv_status);
        col.setCellFactory(c -> new DragPVCell());
        cols.add(col);

        // Each column is non-reorderable at runtime to avoid operator surprises.
        // Sites can customize the order via preferences
        for (String header : AlarmSystem.alarm_table_columns)
        {
            // "Icon" is used for the nameless icon column.
            // Other column names used in pref must match header.
            final String actual_header = header.equals("Icon")
                                       ? ""
                                       : header;
            final Optional<TableColumn<AlarmInfoRow, ?>> to_add =
                cols.stream()
                    .filter(c -> actual_header.equals(c.getText()))
                    .findFirst();
            if (to_add.isPresent())
                table.getColumns().add(to_add.get());
            else
                logger.log(Level.SEVERE, "Unknown Alarm Table column '" + header + "' " +
                        " Supported columns are: " + cols.stream().map(TableColumnBase::getText).collect(Collectors.joining(", ")));
        }

        // Initially, sort on PV name
        // - restore(Memento) might change that
        table.getSortOrder().setAll(List.of(pv_col));
        pv_col.setSortType(SortType.ASCENDING);

        table.setPlaceholder(new Label(active ? "No active alarms" : "No acknowledged alarms"));
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        createContextMenu(table, active);

        // Double-click to acknowledge or un-acknowledge
        table.setRowFactory(tv ->
        {
            final TableRow<AlarmInfoRow> row = new TableRow<>();
            row.setOnMouseClicked(event ->
            {
                if (event.getClickCount() == 2  &&  !row.isEmpty() && AlarmUI.mayAcknowledge(client))
                    JobManager.schedule("ack", monitor ->  client.acknowledge(row.getItem().item, active));
            });
            return row;
        });

        return table;
    }

    private void createContextMenu(final TableView<AlarmInfoRow> table, final boolean active)
    {
        final ContextMenu menu = new ContextMenu();
        table.setOnContextMenuRequested(event ->
        {
            final ObservableList<MenuItem> menu_items = menu.getItems();
            menu_items.clear();

            if (TableHelper.addContextMenuColumnVisibilityEntries(table, menu)) {
                menu_items.add(new SeparatorMenuItem());
            }

            final List<AlarmTreeItem<?>> selection = new ArrayList<>();
            for (AlarmInfoRow row : table.getSelectionModel().getSelectedItems())
                selection.add(row.item);

            // Add guidance etc.
            if (new AlarmContextMenuHelper().addSupportedEntries(table, client, menu, selection))
                menu_items.add(new SeparatorMenuItem());

            if (AlarmUI.mayConfigure(client)  &&   selection.size() == 1)
            {
                menu_items.add(new ConfigureComponentAction(table, client, selection.get(0)));
                menu_items.add(new SeparatorMenuItem());
            }
            menu_items.add(new PrintAction(this));
            menu_items.add(new SaveSnapshotAction(table));

            // Add context menu actions based on the selection (i.e. email, logbook, etc...)
            final Selection originalSelection = SelectionService.getInstance().getSelection();
            final List<AppSelection> newSelection = Arrays.asList(AppSelection.of(table, "Alarm Snapshot", list_alarms(), () -> Screenshot.imageFromNode(this)));
            SelectionService.getInstance().setSelection("AlarmUI", newSelection);
            List<ContextMenuEntry> supported = ContextMenuService.getInstance().listSupportedContextMenuEntries();
            supported.stream().forEach(action -> {
                MenuItem menuItem = new MenuItem(action.getName(), new ImageView(action.getIcon()));
                menuItem.setOnAction((e) -> {
                    try
                    {
                        SelectionService.getInstance().setSelection("AlarmUI", newSelection);
                        action.call(table, SelectionService.getInstance().getSelection());
                    } catch (Exception ex)
                    {
                        logger.log(Level.WARNING, "Failed to execute " + action.getName() + " from AlarmUI.", ex);
                    }
                });
                menu_items.add(menuItem);
            });
            SelectionService.getInstance().setSelection("AlarmUI", originalSelection);

            menu.show(table.getScene().getWindow(), event.getScreenX(), event.getScreenY());
        });
    }

    private String list_alarms()
    {
        final StringBuilder buf = new StringBuilder();

        buf.append("Active Alarms\n");
        buf.append("=============\n");
        for (AlarmInfoRow row : active_rows)
            buf.append(row).append("\n");

        buf.append("Acknowledged Alarms\n");
        buf.append("===================\n");
        for (AlarmInfoRow row : active_rows)
            buf.append(row).append("\n");

        return buf.toString();
    }

    void restore(final Memento memento)
    {
        memento.getNumber("POS").ifPresent(pos -> split.setDividerPositions(pos.doubleValue()));

        int i = 0;
        for (TableColumn<AlarmInfoRow, ?> col : active.getColumns())
            memento.getNumber("COL" + i++).ifPresent(wid -> col.setPrefWidth(wid.doubleValue()));

        // visibility is linked to other table, no need to also save/restore other table
        TableHelper.restoreColumnVisibilities(active, memento, (col, idx) -> "COL" + idx + "vis");

        i = memento.getNumber("SORT").orElse(-1).intValue();
        if (i >= 0)
        {
            final TableColumn<AlarmInfoRow, ?> col = active.getColumns().get(i);
            active.getSortOrder().setAll(List.of(col));
            memento.getNumber("DIR").ifPresent(dir -> col.setSortType(SortType.values()[dir.intValue()]));
        }
    }

    void save(final Memento memento)
    {
        memento.setNumber("POS", split.getDividers().get(0).getPosition());

        int i = 0;
        for (TableColumn<AlarmInfoRow, ?> col : active.getColumns())
            memento.setNumber("COL" + i++, col.getWidth());

        // visibility is linked to other table, no need to also save/restore other table
        TableHelper.saveColumnVisibilities(active, memento, (col, idx) -> "COL" + idx + "vis");

        final List<TableColumn<AlarmInfoRow, ?>> sorted = active.getSortOrder();
        if (sorted.size() == 1)
        {
            i = active.getColumns().indexOf(sorted.get(0));
            memento.setNumber("SORT", i);
            memento.setNumber("DIR", active.getColumns().get(i).getSortType().ordinal());
        }
    }

    /** Update the alarm information to show
     *
     *  <p>The provided lists are not retained, but
     *  some of the AlarmInfoRow items may be added
     *  to the table's list of items.
     *
     *  @param active Active alarms
     *  @param acknowledged Acknowledged alarms
     */
    public void update(final List<AlarmInfoRow> active,
                       final List<AlarmInfoRow> acknowledged)
    {
        limitAlarmCount(active, active_count, "Active Alarms: ");
        limitAlarmCount(acknowledged, acknowledged_count, "Acknowledged Alarms: ");
        update(active_rows, active);
        update(acknowledged_rows, acknowledged);
    }

    /** Limit the number of alarms
     *  @param alarms List of alarms, may be trimmed
     *  @param alarm_count Label where count will be shown
     *  @param message Message to use for the count
     */
    private void limitAlarmCount(final List<AlarmInfoRow> alarms,
                                 final Label alarm_count, final String message)
    {
        final int N = alarms.size();
        final StringBuilder buf = new StringBuilder();
        buf.append(message).append(N);
        if (N > AlarmSystem.alarm_table_max_rows)
        {
            buf.append(" (").append(N - AlarmSystem.alarm_table_max_rows).append(" not shown)");
            alarms.subList(AlarmSystem.alarm_table_max_rows, N).clear();
        }
        alarm_count.setText(buf.toString());
    }

    /** Update existing list of items with new input
     *  @param items List to update
     *  @param input New input
     */
    private void update(final ObservableList<AlarmInfoRow> items, final List<AlarmInfoRow> input)
    {
        // Similar, but might trigger a full table redraw:
        // items.setAll(input);

        // Update content of common list entries
        int N = Math.min(items.size(), input.size());
        for (int i=0; i<N; ++i)
            items.get(i).copy(input.get(i));

        N = input.size();
        if (N > items.size())
        {
            // Additional elements if input is larger that existing list
            for (int i=items.size(); i<N; ++i)
                items.add(input.get(i));
        }
        else // Trim items, input has fewer elements
            items.remove(N, items.size());

        selectRows();
    }

    /** Select all rows that match the current 'search' pattern */
    private void selectRows()
    {
        final String glob = search.getText().trim();
        if (glob.isEmpty())
        {
            active.getSelectionModel().clearSelection();
            acknowledged.getSelectionModel().clearSelection();
            return;
        }

        final Pattern pattern = Pattern.compile(RegExHelper.fullRegexFromGlob(glob),
                                                Pattern.CASE_INSENSITIVE);
        selectRows(active, pattern);
        selectRows(acknowledged, pattern);
    }

    private void selectRows(final TableView<AlarmInfoRow> table, final Pattern pattern)
    {
        table.getSelectionModel().clearSelection();

        int i = 0;
        for (AlarmInfoRow row : table.getItems())
        {
            if (pattern.matcher(row.pv.get()).matches()  ||
                pattern.matcher(row.description.get()).matches())
                table.getSelectionModel().select(i);
            ++i;
        }
    }
}

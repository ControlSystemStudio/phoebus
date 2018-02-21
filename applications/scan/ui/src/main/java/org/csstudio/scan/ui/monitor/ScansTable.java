/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The scan engine idea is based on the "ScanEngine" developed
 * by the Software Services Group (SSG),  Advanced Photon Source,
 * Argonne National Laboratory,
 * Copyright (c) 2011 , UChicago Argonne, LLC.
 *
 * This implementation, however, contains no SSG "ScanEngine" source code
 * and is not endorsed by the SSG authors.
 ******************************************************************************/
package org.csstudio.scan.ui.monitor;

import static org.csstudio.scan.ScanSystem.logger;

import java.time.Instant;
import java.util.List;
import java.util.logging.Level;

import org.csstudio.scan.client.ScanClient;
import org.csstudio.scan.info.ScanInfo;
import org.csstudio.scan.info.ScanState;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.util.time.TimestampFormats;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
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

    /** Property-based proxy for a {@link ScanInfo} */
    private static class ScanInfoProxy
    {
        final SimpleLongProperty id;
        final SimpleStringProperty name;
        final SimpleObjectProperty<Instant> created;
        final SimpleObjectProperty<ScanState> state;
        final SimpleIntegerProperty percent;
        final SimpleStringProperty runtime;
        final SimpleObjectProperty<Instant> finish;
        final SimpleStringProperty command;
        final SimpleStringProperty error;

        public ScanInfoProxy(final ScanInfo info)
        {
            id = new SimpleLongProperty(info.getId());
            name = new SimpleStringProperty(info.getName());
            created = new SimpleObjectProperty<>(info.getCreated());
            state = new SimpleObjectProperty<>(info.getState());
            percent = new SimpleIntegerProperty(info.getPercentage());
            runtime = new SimpleStringProperty(info.getRuntimeText());
            finish = new SimpleObjectProperty<>(info.getFinishTime());
            command = new SimpleStringProperty(info.getCurrentCommand());
            error = new SimpleStringProperty(info.getError().orElse(""));
        }

        void updateFrom(final ScanInfo info)
        {
            id.set(info.getId());
            name.set(info.getName());
            created.set(info.getCreated());
            state.set(info.getState());
            percent.set(info.getPercentage());
            runtime.set(info.getRuntimeText());
            finish.set(info.getFinishTime());
            command.set(info.getCurrentCommand());
            error.set(info.getError().orElse(""));
        }
    };

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

    // Like Consumer<long scan id>, but may throw exception
    private static interface ScanAction
    {
        /** Perform action on a scan
         *  @param id ID of scan
         *  @throws Exception on error
         */
        void perform(long id) throws Exception;
    }

    /** Table cell for {@link ScanState} */
    private class StateCell extends TableCell<ScanInfoProxy, ScanState>
    {
        final Label text = new Label();
        Button next, pause, resume, abort, remove;
        final HBox graphics = new HBox(5, text);

        StateCell()
        {
            text.setPrefWidth(80);
        }

        Button createButton(final String icon, final String tooltip, final ScanAction action)
        {
            final Button button = new Button();
            button.setMinSize(ButtonBase.USE_PREF_SIZE, ButtonBase.USE_PREF_SIZE);
            button.setPrefHeight(20);
            button.setGraphic(ImageCache.getImageView(ScansTable.class, icon));
            button.setTooltip(new Tooltip(tooltip));
            button.setOnAction(event ->
            {
                try
                {
                    action.perform(getTableRow().getItem().id.get());
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Failed: " + tooltip, ex);
                }
            });
            return button;
        }

        Button getNext()
        {
            if (next == null)
                next = createButton("/icons/next.png", "Force move to next command", id -> scan_client.nextCommand(id));
            return next;
        }

        Button getPause()
        {
            if (pause == null)
                pause = createButton("/icons/pause.png", "Pause on next command", id -> scan_client.pauseScan(id));
            return pause;
        }

        Button getResume()
        {
            if (resume == null)
                resume = createButton("/icons/resume.png", "Resume execution", id -> scan_client.resumeScan(id));
            return resume;
        }

        Button getAbort()
        {
            if (abort == null)
                abort = createButton("/icons/abort.png", "Abort execution", id -> scan_client.abortScan(id));
            return abort;
        }

        Button getRemove()
        {
            if (remove == null)
                remove = createButton("/icons/delete_obj.png", "Remove this scan", id -> scan_client.removeScan(id));
            return remove;
        }

        private void show(final Button button)
        {
            graphics.getChildren().add(button);
        }

        @Override
        protected void updateItem(final ScanState state, final boolean empty)
        {
            super.updateItem(state, empty);
            if (empty)
                setGraphic(null);
            else
            {
                text.setText(state.toString());
                text.setTextFill(getStateColor(state));

                // Remove all but the label
                int i = graphics.getChildren().size();
                while (i > 1)
                    graphics.getChildren().remove(--i);

                switch (state)
                {
                case Idle:
                    show(getAbort());
                    break;
                case Running:
                    show(getPause());
                    show(getNext());
                    show(getAbort());
                    break;
                case Paused:
                    show(getResume());
                    show(getAbort());
                    break;
                case Aborted:
                case Failed:
                case Finished:
                case Logged:
                    show(getRemove());
                    break;
                }

                setGraphic(graphics);
            }
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
            if (empty)
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

                final Color color = getStateColor(getTableRow().getItem().state.get());
                progress.setStyle(String.format("-fx-accent: #%02x%02x%02x;",
                                                (int) (color.getRed()*255),
                                                (int) (color.getGreen()*255),
                                                (int) (color.getBlue()*255)));
            }
            setGraphic(progress);
        }
    }

    public ScansTable(final ScanClient scan_client)
    {
        this.scan_client = scan_client;
        createTable();
        getChildren().add(scan_table);
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
        state_col.setCellFactory(cell -> new StateCell());
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

        sorted_scans.comparatorProperty().bind(scan_table.comparatorProperty());
    }

    private static Color getStateColor(final ScanState state)
    {
        switch (state)
        {
        case Idle:      return Color.DARKBLUE;
        case Aborted:   return Color.DARKGOLDENROD;
        case Failed:    return Color.RED;
        case Finished:  return Color.DARKGREEN;
        case Paused:    return Color.GRAY;
        case Running:   return Color.GREEN;
        default:        return Color.BLACK;
        }
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
        // TODO Context menu to abort all scans, remove all completed scans,
        //      re-submit scan,
        //      open scan in editor,
        //      open scan data monitor,
        //      open scan data plot

        final MenuItem abort_all = new MenuItem("Abort All Scans");
        final MenuItem remove_completed = new MenuItem("Remove completed Scans");
        final ContextMenu menu = new ContextMenu();

        scan_table.setOnContextMenuRequested(event ->
        {
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

            menu.getItems().clear();
            if (any_to_abort)
                menu.getItems().add(abort_all);
            if (any_completed)
                menu.getItems().add(remove_completed);
            menu.show(scan_table.getScene().getWindow(), event.getScreenX(), event.getScreenY());
        });
    }

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
}

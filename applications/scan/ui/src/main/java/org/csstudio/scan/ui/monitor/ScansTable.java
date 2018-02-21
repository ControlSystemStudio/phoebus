package org.csstudio.scan.ui.monitor;

import java.time.Instant;
import java.util.List;

import org.csstudio.scan.info.ScanInfo;
import org.csstudio.scan.info.ScanState;
import org.phoebus.util.time.TimestampFormats;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

@SuppressWarnings("nls")
public class ScansTable extends VBox
{
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

    /** Table cell for {@link ScanState} */
    private static class StateCell extends TableCell<ScanInfoProxy, ScanState>
    {
        static Color getStateColor(final ScanState state)
        {
            switch (state)
            {
            case Idle:      return Color.DARKBLUE;
            case Aborted:   return Color.DARKGOLDENROD;
            case Failed:    return Color.RED;
            case Finished:  return Color.DARKGREEN;
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
        static int rankState(final ScanState state)
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

        @Override
        protected void updateItem(final ScanState state, final boolean empty)
        {
            super.updateItem(state, empty);
            if (empty)
                setText("");
            else
            {
                setText(state.toString());
                setTextFill(getStateColor(state));
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
            }
            setGraphic(progress);
        }
    }

    private final TableView<ScanInfoProxy> scan_table = new TableView<>();

    public ScansTable()
    {
        createTable();
        getChildren().add(scan_table);
    }

    private void createTable()
    {
        // TODO Auto-generated method stub
        TableColumn<ScanInfoProxy, Number> num_col = new TableColumn<>("ID");
        num_col.setCellValueFactory(cell -> cell.getValue().id);
        scan_table.getColumns().add(num_col);

        TableColumn<ScanInfoProxy, Instant> time_col = new TableColumn<>("Created");
        time_col.setCellValueFactory(cell -> cell.getValue().created);
        time_col.setCellFactory(cell -> new InstantCell());
        scan_table.getColumns().add(time_col);

        TableColumn<ScanInfoProxy, String> col = new TableColumn<>("Name");
        col.setCellValueFactory(cell -> cell.getValue().name);
        scan_table.getColumns().add(col);

        TableColumn<ScanInfoProxy, ScanState> state_col = new TableColumn<>("State");
        state_col.setCellValueFactory(cell -> cell.getValue().state);
        state_col.setCellFactory(cell -> new StateCell());
        state_col.setComparator((a, b) ->  StateCell.rankState(a) - StateCell.rankState(b));
        scan_table.getColumns().add(state_col);

        num_col = new TableColumn<>("%");
        num_col.setCellValueFactory(cell -> cell.getValue().percent);
        num_col.setCellFactory(cell -> new PercentCell());
        scan_table.getColumns().add(num_col);

        col = new TableColumn<>("Runtime");
        col.setCellValueFactory(cell -> cell.getValue().runtime);
        scan_table.getColumns().add(col);

        time_col = new TableColumn<>("Finish");
        time_col.setCellValueFactory(cell -> cell.getValue().finish);
        time_col.setCellFactory(cell -> new InstantCell());
        scan_table.getColumns().add(time_col);

        col = new TableColumn<>("Command");
        col.setCellValueFactory(cell -> cell.getValue().command);
        scan_table.getColumns().add(col);

        col = new TableColumn<>("Error");
        col.setCellValueFactory(cell -> cell.getValue().error);
        scan_table.getColumns().add(col);

        scan_table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    public void update(List<ScanInfo> infos)
    {
        final ObservableList<ScanInfoProxy> items = scan_table.getItems();
        int i;
        for (i=0; i<infos.size(); ++i)
        {
            if (i < items.size())
                items.get(i).updateFrom(infos.get(i));
            else
                items.add(new ScanInfoProxy(infos.get(i)));
        }
        i = items.size();
        while (i > infos.size())
            items.remove(--i);
    }
}

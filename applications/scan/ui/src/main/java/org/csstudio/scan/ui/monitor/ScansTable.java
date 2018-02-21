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

                final Color color = getStateColor(getTableRow().getItem().state.get());
                progress.setStyle(String.format("-fx-accent: #%02x%02x%02x;",
                                                (int) (color.getRed()*255),
                                                (int) (color.getGreen()*255),
                                                (int) (color.getBlue()*255)));
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
        state_col.setPrefWidth(70);
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

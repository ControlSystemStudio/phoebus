/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.sampleview;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.csstudio.trends.databrowser3.model.PlotSample;
import org.csstudio.trends.databrowser3.model.PlotSamples;
import org.phoebus.archive.vtype.DoubleVTypeFormat;
import org.phoebus.archive.vtype.VTypeFormat;
import org.phoebus.archive.vtype.VTypeHelper;
import org.phoebus.ui.pv.SeverityColors;
import org.phoebus.util.time.TimestampFormats;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Panel for inspecting samples of a trace
 *
 * @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SampleView extends VBox {
    private final Model model;
    private final ComboBox<ModelItem> items = new ComboBox<>();
    private final Label sample_count = new Label(Messages.SampleView_Count);
    private final TableView<PlotSample> sample_table = new TableView<>();
    private volatile ModelItem modelItem = null;

    private static class SeverityColoredTableCell extends TableCell<PlotSample, String> {
        @Override
        protected void updateItem(final String item, final boolean empty) {
            super.updateItem(item, empty);
            final TableRow<PlotSample> row = getTableRow();
            if (empty || row == null || row.getItem() == null)
                setText("");
            else {
                setText(item);
                setTextFill(SeverityColors.getTextColor(org.phoebus.core.vtypes.VTypeHelper.getSeverity(row.getItem().getVType())));
            }
        }
    }

    /**
     * @param model Model
     */
    public SampleView(final Model model) {
        this.model = model;

        items.setOnAction(event -> select(items.getSelectionModel().getSelectedItem()));

        ModelItemListCellFactory factory = new ModelItemListCellFactory();
        items.setCellFactory(factory);
        items.setButtonCell(factory.call(null));

        final Button refresh = new Button(Messages.SampleView_Refresh);
        refresh.setTooltip(new Tooltip(Messages.SampleView_RefreshTT));
        refresh.setOnAction(event -> update());

        final Label label = new Label(Messages.SampleView_Item);
        final HBox top_row = new HBox(5, label, items, refresh);
        top_row.setAlignment(Pos.CENTER_LEFT);

        // Combo should fill the available space.
        // Tried HBox.setHgrow(items, Priority.ALWAYS) etc.,
        // but always resulted in shrinking the label and button.
        // -> Explicitly compute combo width from available space
        //    minus padding and size of label, button
        items.prefWidthProperty().bind(top_row.widthProperty().subtract(20).subtract(label.widthProperty()).subtract(refresh.widthProperty()));
        items.prefHeightProperty().bind(refresh.heightProperty());

        createSampleTable();

        top_row.setPadding(new Insets(5));
        sample_count.setPadding(new Insets(5));
        sample_table.setPadding(new Insets(0, 5, 5, 5));
        VBox.setVgrow(sample_table, Priority.ALWAYS);
        getChildren().setAll(top_row, sample_count, sample_table);

        // TODO Add 'export' to sample view? CSV in a format usable by import

        update();
    }


    private void createSampleTable() {
        TableColumn<PlotSample, String> col = new TableColumn<>(Messages.TimeColumn);
        final VTypeFormat format = DoubleVTypeFormat.get();
        col.setCellValueFactory(cell -> new SimpleStringProperty(TimestampFormats.FULL_FORMAT.format(org.phoebus.core.vtypes.VTypeHelper.getTimestamp(cell.getValue().getVType()))));
        sample_table.getColumns().add(col);

        col = new TableColumn<>(Messages.ValueColumn);
        col.setCellValueFactory(cell -> new SimpleStringProperty(format.format(cell.getValue().getVType())));
        sample_table.getColumns().add(col);

        col = new TableColumn<>(Messages.SeverityColumn);
        col.setCellFactory(c -> new SeverityColoredTableCell());
        col.setCellValueFactory(cell -> new SimpleStringProperty(org.phoebus.core.vtypes.VTypeHelper.getSeverity(cell.getValue().getVType()).name()));
        sample_table.getColumns().add(col);

        col = new TableColumn<>(Messages.StatusColumn);
        col.setCellValueFactory(cell -> new SimpleStringProperty(VTypeHelper.getMessage(cell.getValue().getVType())));
        sample_table.getColumns().add(col);

        col = new TableColumn<>(Messages.SampleView_Source);
        col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSource()));
        sample_table.getColumns().add(col);

        sample_table.setMaxWidth(Double.MAX_VALUE);
        sample_table.setPlaceholder(new Label(Messages.SampleView_SelectItem));
        sample_table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void select(final ModelItem modelItem) {
        this.modelItem = modelItem;
        Activator.thread_pool.submit(this::getSamples);
    }

    private void update() {
        final List<ModelItem> model_items = model.getItems();
        items.getItems().setAll(model_items);
        if (modelItem != null)
            items.getSelectionModel().select(modelItem);
        // Update samples off the UI thread
        Activator.thread_pool.submit(this::getSamples);
    }

    private void getSamples() {
        //final ModelItem item = model.getItem(modelItem);
        final ObservableList<PlotSample> samples = FXCollections.observableArrayList();
        if (modelItem != null) {
            final PlotSamples item_samples = modelItem.getSamples();
            try {
                if (item_samples.getLock().tryLock(2, TimeUnit.SECONDS)) {
                    final int N = item_samples.size();
                    for (int i = 0; i < N; ++i)
                        samples.add(item_samples.get(i));
                    item_samples.getLock().unlock();
                }
            } catch (Exception ex) {
                Activator.logger.log(Level.WARNING, "Cannot access samples for " + modelItem.getResolvedName(), ex);
            }
        }
        // Update UI
        Platform.runLater(() -> updateSamples(samples));
    }

    private void updateSamples(final ObservableList<PlotSample> samples) {
        sample_count.setText(Messages.SampleView_Count + " " + samples.size());
        sample_table.setItems(samples);
    }

    /**
     * Cell factory for the combo box. If user has set a non-empty display name, use it in the item
     * list together with the resolved PV name. If not, use only resolved PV name.
     */
    private static class ModelItemListCellFactory implements Callback<ListView<ModelItem>, ListCell<ModelItem>> {
        @Override
        public ListCell<ModelItem> call(ListView<ModelItem> param) {
            return new ListCell<>() {
                @Override
                protected void updateItem(ModelItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null) {
                        if (item.getResolvedName().equals(item.getDisplayName()) || item.getDisplayName().isEmpty()) {
                            setText(item.getResolvedName());
                        } else {
                            setText(item.getDisplayName() + " (" + item.getResolvedName() + ")");
                        }
                    }
                }
            };
        }
    }
}

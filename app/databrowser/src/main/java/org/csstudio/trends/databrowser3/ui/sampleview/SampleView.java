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
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.*;
import org.csstudio.trends.databrowser3.ui.properties.ChangeSampleViewFilterCommand;
import org.epics.vtype.*;
import org.phoebus.archive.vtype.DoubleVTypeFormat;
import org.phoebus.archive.vtype.VTypeFormat;
import org.phoebus.archive.vtype.VTypeHelper;
import org.phoebus.ui.pv.SeverityColors;
import org.phoebus.ui.undo.UndoableActionManager;
import org.phoebus.util.time.TimestampFormats;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Panel for inspecting samples of a trace
 *
 * @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SampleView extends VBox {
    private final Model model;
    private final UndoableActionManager undo;
    private final ComboBox<ModelItemListItem> items = new ComboBox<>();
    private final ComboBox<ItemSampleViewFilter.FilterType> filter_type = new ComboBox<>();
    private final TextField filter_value = new TextField();
    private final Label sample_count = new Label(Messages.SampleView_Count);
    private final TableView<PlotSampleWrapper> sample_table = new TableView<>();
    private volatile ModelItemListItem modelItem = null; // Wrapped ModelItem for the combobox to display all samples from all PVs
    private final ObservableList<PlotSampleWrapper> samples = FXCollections.observableArrayList();
    private final SortedList<PlotSampleWrapper> sorted_samples = new SortedList<>(samples);
    private int all_samples_size;

    private static class SeverityColoredTableCell extends TableCell<PlotSampleWrapper, String> {
        @Override
        protected void updateItem(final String item, final boolean empty) {
            super.updateItem(item, empty);
            final TableRow<PlotSampleWrapper> row = getTableRow();
            if (empty || row == null || row.getItem() == null)
                setText("");
            else {
                setText(item);
                setTextFill(SeverityColors.getTextColor(org.phoebus.core.vtypes.VTypeHelper.getSeverity(row.getItem().getVType())));
            }
        }
    }

    private final ModelListener model_listener = new ModelListener() {
        @Override
        public void itemAdded(ModelItem item) {
            update();
        }

        @Override
        public void itemRemoved(ModelItem item) {
            update();
        }

        @Override
        public void changedItemLook(ModelItem item) {
            update();
        }

        @Override
        public void itemRefreshRequested(PVItem item) {
            update();
        }
    };

    /**
     * @param model Model
     */
    public SampleView(final Model model, UndoableActionManager undo) {
        this.model = model;
        this.undo = undo;
        model.addListener(model_listener);

        items.setOnAction(event -> select(items.getSelectionModel().getSelectedItem()));

        ModelItemListCellFactory factory = new ModelItemListCellFactory();
        items.setCellFactory(factory);
        items.setButtonCell(factory.call(null));

        final Button refresh = new Button(Messages.SampleView_Refresh);
        refresh.setTooltip(new Tooltip(Messages.SampleView_RefreshTT));
        refresh.setOnAction(event -> update());

        final Label label = new Label(Messages.SampleView_Item);
        final HBox top_row = new HBox(8, label, items, refresh);
        top_row.setAlignment(Pos.CENTER_LEFT);

        filter_type.setTooltip(new Tooltip(Messages.SampleView_FilterTypeTT));
        filter_type.getItems().setAll(ItemSampleViewFilter.FilterType.values());
        if (modelItem != null)
            filter_type.setValue(this.modelItem.getModelItem().getSampleViewFilter().getFilterType());
        filter_type.setOnAction(event -> {
            final ItemSampleViewFilter filter = new ItemSampleViewFilter(modelItem.getModelItem().getSampleViewFilter());
            filter.setFilterType(filter_type.getValue());

            new ChangeSampleViewFilterCommand(undo, modelItem.getModelItem(), filter);
        });

        filter_value.setTooltip(new Tooltip(Messages.SampleView_FilterValueTT));
        filter_value.setOnAction(event -> {
            // Make a copy so that we can undo the change
            final ItemSampleViewFilter filter = new ItemSampleViewFilter(modelItem.getModelItem().getSampleViewFilter());
            filter.setFilterValue(Double.parseDouble(filter_value.getText()));

            new ChangeSampleViewFilterCommand(undo, modelItem.getModelItem(), filter);
        });

            // Todo: move to right side of row
        final HBox second_row = new HBox(5, sample_count, new Region(), filter_type, filter_value);


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
        getChildren().setAll(top_row, second_row, sample_table);

        // TODO Add 'export' to sample view? CSV in a format usable by import

        update();
    }


    private void createSampleTable() {
        TableColumn<PlotSampleWrapper, String> col = new TableColumn<>(Messages.TimeColumn);
        final VTypeFormat format = DoubleVTypeFormat.get();
        col.setCellValueFactory(cell -> new SimpleStringProperty(TimestampFormats.FULL_FORMAT.format(org.phoebus.core.vtypes.VTypeHelper.getTimestamp(cell.getValue().getVType()))));
        sample_table.getColumns().add(col);
        sample_table.getSortOrder().add(col);

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

        col = new TableColumn<>(Messages.PVColumn);
        col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPVName()));
        sample_table.getColumns().add(col);

        col = new TableColumn<>(Messages.SampleView_Source);
        col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSource()));
        sample_table.getColumns().add(col);

        sample_table.setMaxWidth(Double.MAX_VALUE);
        sample_table.setPlaceholder(new Label(Messages.SampleView_SelectItem));
        sample_table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        sample_table.setItems(sorted_samples);
        sorted_samples.comparatorProperty().bind(sample_table.comparatorProperty());
    }

    private void select(final ModelItemListItem modelItem) {
        this.modelItem = modelItem;
        if (modelItem != null && !modelItem.isAllSelection()) {
            // Update samples off the UI thread
            Activator.thread_pool.submit(this::getSamples);
        } else{
            // Update samples off the UI thread
            Activator.thread_pool.submit(this::getSamplesAll);
        }
    }

    public void update() {
        final List<ModelItemListItem> model_items = model.getItems().stream().map(ModelItemListItem::new).collect(Collectors.toList());
        items.getItems().setAll(model_items);
        items.getItems().add(new ModelItemListItem()); // Add an item to the combobox to display all samples from all PVs

        if (modelItem == null) {
            return;
        }

        items.getSelectionModel().select(modelItem);
        filter_value.setText(String.valueOf(this.modelItem.getModelItem().getSampleViewFilter().getFilterValue()));
        filter_type.setValue(this.modelItem.getModelItem().getSampleViewFilter().getFilterType());

        // Update samples off the UI thread
        if (modelItem.isAllSelection()) {
            Activator.thread_pool.submit(this::getSamplesAll);
        } else {
            Activator.thread_pool.submit(this::getSamples);
        }
    }

    private void getSamples() {
        //final ModelItem item = model.getItem(item_name);
        final ObservableList<PlotSampleWrapper> samples = FXCollections.observableArrayList();
        if (modelItem != null && !modelItem.isAllSelection()) {
            final PlotSamples item_samples = modelItem.getModelItem().getSamples();
            try {
                if (item_samples.getLock().tryLock(2, TimeUnit.SECONDS)) {
                    final int N = item_samples.size();
                    for (int i = 0; i < N; ++i) {
                        PlotSampleWrapper wrapped_sample = new PlotSampleWrapper(item_samples.get(i), modelItem.getModelItem());
                        samples.add(wrapped_sample);
                    }
                    item_samples.getLock().unlock();
                }
            } catch (Exception ex) {
                Activator.logger.log(Level.WARNING, "Cannot access samples for " + modelItem.getModelItem().getResolvedName(), ex);
            }
        }
        all_samples_size = samples.size();
        final ObservableList<PlotSampleWrapper> filtered_samples = runSampleViewFilter(samples);

        // Update UI
        Platform.runLater(() -> updateSamples(filtered_samples));
    }

    private void getSamplesAll() {
        //final List<ModelItem> items = model.getItems();
        final List<ModelItem> items = this.items.getItems().stream()
                .filter(item -> !item.isAllSelection())
                .map(ModelItemListItem::getModelItem)
                .collect(Collectors.toList());
        final ObservableList<PlotSampleWrapper> samples = FXCollections.observableArrayList();

        if (!items.isEmpty()) {
            for (ModelItem item : items) {
                final PlotSamples item_samples = item.getSamples();
                try {
                    if (item_samples.getLock().tryLock(2, TimeUnit.SECONDS)) {
                        final int N = item_samples.size();
                        for (int i = 0; i < N; ++i) {
                            PlotSampleWrapper wrapped_sample = new PlotSampleWrapper(item_samples.get(i), item);
                            samples.add(wrapped_sample);
                        }
                        item_samples.getLock().unlock();
                    }
                } catch (Exception ex) {
                    Activator.logger.log(Level.WARNING, "Cannot access samples for " + item.getResolvedName(), ex);
                }
            }
        }
        all_samples_size = samples.size();
        final ObservableList<PlotSampleWrapper> filtered_samples = runSampleViewFilter(samples);

        // Update UI
        Platform.runLater(() -> updateSamples(filtered_samples));
    }

    private void updateSamples(ObservableList<PlotSampleWrapper> samples) {
        this.samples.setAll(samples);

        // Display the PVitem name (Column 4)
        sample_table.getColumns().get(4).setVisible(modelItem != null && modelItem.isAllSelection()); // Hide the PVitem name (Column 4) when not needed
        filter_type.setVisible(modelItem != null && !modelItem.isAllSelection());
        filter_value.setVisible(modelItem != null && !modelItem.isAllSelection());

        // Hide samples that are not visible in the plot when viewing all items
        sample_count.setText(Messages.SampleView_Count + " " + all_samples_size
                + " (" + Messages.SampleView_Count_Visible + " " + this.samples.size() + ")");
    }

    private ObservableList<PlotSampleWrapper> runSampleViewFilter(ObservableList<PlotSampleWrapper> samples) {
        final ObservableList<PlotSampleWrapper> new_samples = FXCollections.observableArrayList();

        if (samples.isEmpty()) {
            return new_samples;
        }

        // Store the last viewed sample for each ModelItem,
        // so that we can compare the current sample with the last viewed sample of that ModelItem
        HashMap<ModelItem, PlotSampleWrapper> last_viewed_sample = new HashMap<>();

        for (PlotSampleWrapper sample : samples) {

            if (modelItem.isAllSelection() && !sample.getModelItem().isVisible()) continue;

            last_viewed_sample.putIfAbsent(sample.getModelItem(), sample);
            PlotSampleWrapper previous_sample_for_item = last_viewed_sample.get(sample.getModelItem());

            // Enum samples are compared by their index
            double sample_value = sample.getSample().getValue();
            double previous_sample_value = previous_sample_for_item.getSample().getValue();


            ItemSampleViewFilter.FilterType filter_type = sample.getModelItem().getSampleViewFilter().getFilterType();
            double filter_value = sample.getModelItem().getSampleViewFilter().getFilterValue();

            Alarm alarm = Alarm.alarmOf(sample.getSample().getVType());
            Alarm previous_alarm = Alarm.alarmOf(previous_sample_for_item.getSample().getVType());

            switch (filter_type) {
                case NO_FILTER:
                    new_samples.add(sample);
                    break;
                case ALARM_UP:
                    if (alarm.getSeverity().compareTo(previous_alarm.getSeverity()) > 0) {
                        new_samples.add(sample);
                    }
                    last_viewed_sample.put(sample.getModelItem(), sample);
                    break;
                case ALARM_CHANGES:
                    if (!alarm.getSeverity().equals(previous_alarm.getSeverity())) {
                        new_samples.add(sample);
                        last_viewed_sample.put(sample.getModelItem(), sample);
                    }
                    break;
                case THRESHOLD_UP:
                    // Handle sample bundles
                    if (sample.getVType() instanceof VStatistics) {
                        double previous_sample_value_max = ((VStatistics) previous_sample_for_item.getVType()).getMax();
                        double sample_value_min = ((VStatistics) sample.getVType()).getMin();
                        double sample_value_max = ((VStatistics) sample.getVType()).getMax();

                        // Compare maximum of prev to minimum of current.
                        // also check if threshold was passed within a bundle
                        if ((previous_sample_value_max <= filter_value && sample_value_min > filter_value)
                                || (sample_value_min < filter_value && sample_value_max >= filter_value)) {
                            new_samples.add(sample);
                        }
                        last_viewed_sample.put(sample.getModelItem(), sample);
                        continue;
                    }

                    if (!(sample.getVType() instanceof VNumber || sample.getVType() instanceof VEnum)) {
                        //System.out.println("Cannot compare non-numerical types");
                        new_samples.add(sample);
                        continue;
                    }

                    if (sample_value >= filter_value && previous_sample_value < filter_value) {
                        new_samples.add(sample);
                    }
                    last_viewed_sample.put(sample.getModelItem(), sample);
                    break;
                case THRESHOLD_DOWN:
                    // Handle sample bundles
                    if (sample.getVType() instanceof VStatistics) {
                        double previous_sample_value_min = ((VStatistics) previous_sample_for_item.getVType()).getMin();
                        double sample_value_min = ((VStatistics) sample.getVType()).getMin();
                        double sample_value_max = ((VStatistics) sample.getVType()).getMax();

                        // Compare minimum of prev to maximum of current.
                        // also check if threshold was passed within a bundle
                        if ((previous_sample_value_min > filter_value && sample_value_max <= filter_value)
                                || (sample_value_max > filter_value && sample_value_min <= filter_value)) {
                            new_samples.add(sample);
                        }
                        last_viewed_sample.put(sample.getModelItem(), sample);
                        continue;
                    }

                    if (!(sample.getVType() instanceof VNumber || sample.getVType() instanceof VEnum)) {
                        //System.out.println("Cannot compare non-numerical types");
                        new_samples.add(sample);
                        continue;
                    }

                    if (previous_sample_value > filter_value && sample_value <= filter_value) {
                        new_samples.add(sample);
                    }
                    last_viewed_sample.put(sample.getModelItem(), sample);
                    break;
                case THRESHOLD_CHANGES:
                    if (sample.getVType() instanceof VStatistics) {
                        double previous_sample_value_max = ((VStatistics) previous_sample_for_item.getVType()).getMax();
                        double sample_value_min = ((VStatistics) sample.getVType()).getMin();
                        double sample_value_max = ((VStatistics) sample.getVType()).getMax();

                        // Compare maximum of prev to minimum of current.
                        // also check if threshold was passed within a bundle
                        if ((previous_sample_value_max >= filter_value && sample_value_min < filter_value)
                                || (previous_sample_value_max <= filter_value && sample_value_min > filter_value)
                                || (sample_value_min < filter_value && sample_value_max >= filter_value)) {
                            new_samples.add(sample);
                            last_viewed_sample.put(sample.getModelItem(), sample);
                        }
                        continue;
                    }

                    if (!(sample.getVType() instanceof VNumber || sample.getVType() instanceof VEnum)) {
                        //System.out.println("Cannot compare non-numerical types");
                        new_samples.add(sample);
                        continue;
                    }

                    if ((sample_value >= filter_value && previous_sample_value < filter_value)
                            || (sample_value < filter_value && previous_sample_value >= filter_value)) {
                        new_samples.add(sample);
                        last_viewed_sample.put(sample.getModelItem(), sample);
                    }
                    break;
            }
        }
        return new_samples;
    }

    // For also displaying the PVitem name in the list
    private static class PlotSampleWrapper {
        private final PlotSample sample;
        private final ModelItem model_item;

        public PlotSampleWrapper(final PlotSample sample, final ModelItem model_item) {
            this.sample = sample;
            this.model_item = model_item;
        }

        public PlotSample getSample() {
            return sample;
        }

        public ModelItem getModelItem() {
            return model_item;
        }

        public VType getVType() {
            return sample.getVType();
        }

        public String getSource() {
            return sample.getSource();
        }

        public String getPVName() {
            return model_item.getResolvedName();
        }

        @Override
        public String toString() {
            return sample.toString();
        }
    }

    // For adding an item to the combobox to display all samples from all PVs
    private static class ModelItemListItem {
        private final ModelItem model_item;
        private final boolean is_all_selection;

        public ModelItemListItem(final ModelItem model_item) {
            this.model_item = model_item;
            this.is_all_selection = false;
        }

        public ModelItemListItem() {
            model_item = null;
            this.is_all_selection = true;
        }

        public ModelItem getModelItem() {
            return model_item;
        }

        public boolean isAllSelection() {
            return is_all_selection;
        }

        @Override
        public String toString() {
            if (is_all_selection) {
                return "All Items";
            } else {
                return model_item.getResolvedName();
            }
        }
    }

    /**
     * Cell factory for the combo box. If user has set a non-empty display name, use it in the item
     * list together with the resolved PV name. If not, use only resolved PV name.
     */
    private static class ModelItemListCellFactory implements Callback<ListView<ModelItemListItem>, ListCell<ModelItemListItem>> {
        @Override
        public ListCell<ModelItemListItem> call(ListView<ModelItemListItem> param) {
            return new ListCell<>() {
                @Override
                protected void updateItem(ModelItemListItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null) {
                        if (item.isAllSelection()) {
                            //setText(Messages.SampleView_AllItems);
                            setText("All Items");
                            return;
                        }

                        if (item.getModelItem().getResolvedName().equals(item.getModelItem().getDisplayName()) || item.getModelItem().getDisplayName().isEmpty()) {
                            //System.out.println("item.getModelItem().getResolvedName(): " + item.getModelItem().getResolvedName());
                            setText(item.getModelItem().getResolvedName());
                        } else {
                            setText(item.getModelItem().getDisplayName() + " (" + item.getModelItem().getResolvedName() + ")");
                        }
                    }
                }
            };
        }
    }
}
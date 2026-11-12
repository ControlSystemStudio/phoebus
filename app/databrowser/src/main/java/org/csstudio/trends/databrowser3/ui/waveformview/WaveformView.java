/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.waveformview;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.csstudio.javafx.rtplot.data.TimeDataSearch;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.csstudio.trends.databrowser3.model.ModelListener;
import org.csstudio.trends.databrowser3.model.PlotSample;
import org.csstudio.trends.databrowser3.model.PlotSamples;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VType;
import org.phoebus.archive.vtype.VTypeHelper;
import org.phoebus.ui.javafx.MultiCheckboxCombo;
import org.phoebus.util.time.TimestampFormats;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Inspect waveform samples using the ChartFX waveform history chart.
 * Multiple selected PVs share the sample-time reference but maintain
 * independent waveform histories.
 */
@SuppressWarnings("nls")
public class WaveformView extends VBox {
    private static final int DEFAULT_HISTORY_LENGTH = 5;

    // Temporary runtime configuration. Can later be moved into Data Browser preferences.
    // Example VM option: -Dphoebus.waveform.max_pvs=8
    private static final int MAX_PVS =
            Math.max(1, Integer.getInteger("phoebus.waveform.max_pvs", 4));

    private final Model model;
    private final List<ModelItem> model_items = new ArrayList<>();
    private final MultiCheckboxCombo<ModelItem> items =
            new MultiCheckboxCombo<>(Messages.WaveformViewSelect);
    private boolean updating_selected_items = false;

    private final Spinner<Integer> history_length =
            new Spinner<>(0, 100, DEFAULT_HISTORY_LENGTH);
    private final Label limit_warning = new Label();
    private final Label waveform_warning = new Label();
    private final Separator limit_separator = new Separator(Orientation.VERTICAL);
    private final Separator waveform_separator = new Separator(Orientation.VERTICAL);
    private boolean over_limit_selected = false;
    private final WaveformHistoryChart history_plot =
            new WaveformHistoryChart(DEFAULT_HISTORY_LENGTH);
    private int applied_history_length = DEFAULT_HISTORY_LENGTH;
    private ModelItem selection_item = null;
    private final Slider sample_index = new Slider();
    private boolean positioning_sample_index = false;
    private final TextField timestamp = new TextField();
    private final TextField status = new TextField();

    private final ModelListener model_listener = new ModelListener() {
        @Override
        public void itemAdded(final ModelItem item) {
            updatePVs();
        }

        @Override
        public void itemRemoved(final ModelItem item) {
            model_items.remove(item);
            updatePVs();
        }

        @Override
        public void changedItemLook(final ModelItem item) {
            updatePVs();
        }

        @Override
        public void changedAnnotations() {
            // Draggable annotations are not yet supported by the ChartFX view.
        }

        @Override
        public void changedTimerange() {
            if (!model_items.isEmpty())
                showSelectedSample();
        }
    };

    public WaveformView(final Model model) {
        this.model = model;
        model.addListener(model_listener);

        items.selectedOptions().addListener((Observable o) -> {
            if (!updating_selected_items)
                selectPV(new ArrayList<>(items.getSelectedOptions()));
        });

        final Button refresh = new Button(Messages.SampleView_Refresh);
        refresh.setTooltip(new Tooltip(Messages.SampleView_RefreshTT));
        refresh.setOnAction(event -> updatePVs());

        final Label label = new Label(Messages.SampleView_Item);
        final HBox top_row = new HBox(5, label, items, refresh);
        top_row.setAlignment(Pos.CENTER_LEFT);
        items.prefWidthProperty().bind(top_row.widthProperty()
                .subtract(20).subtract(label.widthProperty())
                .subtract(refresh.widthProperty()));
        items.prefHeightProperty().bind(refresh.heightProperty());

        history_length.setPrefWidth(70);
        history_length.setTooltip(new Tooltip(Messages.WaveformHistoryPreviousTT));
        history_length.valueProperty().addListener((obs, oldValue, value) -> updateHistory());

        configureWarning(limit_warning, limit_separator);
        configureWarning(waveform_warning, waveform_separator);

        history_plot.addToolbarControls(
                new Separator(Orientation.VERTICAL),
                new Label(Messages.WaveformHistoryPrevious), history_length,
                limit_separator, limit_warning,
                waveform_separator, waveform_warning);

        history_plot.setPadding(new Insets(0, 5, 5, 5));
        VBox.setVgrow(history_plot, Priority.ALWAYS);

        sample_index.setBlockIncrement(1);
        sample_index.disableProperty().bind(Bindings.isEmpty(items.selectedOptions()));
        sample_index.setTooltip(new Tooltip(Messages.WaveformTimeSelector));
        sample_index.valueProperty().addListener(observable -> {
            if (!positioning_sample_index)
                showSelectedSample();
        });

        timestamp.setEditable(false);
        status.setEditable(false);
        HBox.setHgrow(timestamp, Priority.ALWAYS);
        HBox.setHgrow(status, Priority.ALWAYS);
        final HBox bottom_row = new HBox(5,
                new Label(Messages.WaveformTimestamp), timestamp,
                new Label(Messages.WaveformStatus), status);
        bottom_row.setAlignment(Pos.CENTER_LEFT);

        top_row.setPadding(new Insets(5));
        bottom_row.setPadding(new Insets(0, 5, 5, 5));
        getChildren().setAll(top_row, history_plot,
                sample_index, bottom_row);

        updatePVs();
    }

    private void updatePVs() {
        final List<ModelItem> selected = new ArrayList<>(items.getSelectedOptions());

        updating_selected_items = true;
        try {
            final List<ModelItem> available = model.getItems();
            items.setOptions(available);
            selected.retainAll(available);
            if (selected.size() > MAX_PVS)
                selected.subList(MAX_PVS, selected.size()).clear();
            items.selectOptions(selected);
        } finally {
            updating_selected_items = false;
        }
        selectPV(selected);
    }

    private void selectPV(final List<ModelItem> requested) {
        final List<ModelItem> selected = new ArrayList<>(requested);
        over_limit_selected = selected.size() > MAX_PVS;
        if (over_limit_selected) {
            selected.subList(MAX_PVS, selected.size()).clear();
            updating_selected_items = true;
            try {
                items.selectOptions(selected);
            } finally {
                updating_selected_items = false;
            }
        }

        model_items.clear();
        model_items.addAll(selected);
        if (model_items.isEmpty()) {
            selection_item = null;
            clearInfo();
            updateToolbarWarning(List.of());
            history_plot.setWaveformSeries(List.of());
            return;
        }
        if (selection_item == null || !model_items.contains(selection_item))
            selection_item = model_items.get(0);

        // Start at the newest sample whenever the PV selection changes.
        // This makes history available for PVs that started recording later.
        final PlotSamples reference = selection_item.getSamples();
        final int last;
        reference.getLock().lock();
        try {
            last = Math.max(0, reference.size() - 1);
        } finally {
            reference.getLock().unlock();
        }

        // Suppress the slider listener while updating its range and value.
        // Show the selected sample once after both properties are consistent.
        positioning_sample_index = true;
        try {
            sample_index.setMax(last);
            sample_index.setValue(last);
        } finally {
            positioning_sample_index = false;
        }
        showSelectedSample();
    }

    private void showSelectedSample() {
        if (model_items.isEmpty()) {
            clearInfo();
            return;
        }
        if (selection_item == null || !model_items.contains(selection_item))
            selection_item = model_items.get(0);

        final PlotSamples reference = selection_item.getSamples();
        final Instant selected_time;
        reference.getLock().lock();
        try {
            final int count = reference.size();
            if (count == 0) {
                clearInfo();
                updateToolbarWarning(List.of());
                history_plot.setWaveformSeries(List.of());
                return;
            }
            final int last = count - 1;
            if (sample_index.getMax() != last) {
                // Changing the slider range may itself fire a value change.
                positioning_sample_index = true;
                try {
                    sample_index.setMax(last);
                } finally {
                    positioning_sample_index = false;
                }
            }
            final int index = Math.max(0, Math.min(last,
                    (int) Math.round(sample_index.getValue())));
            selected_time = reference.get(index).getPosition();
        } finally {
            reference.getLock().unlock();
        }

        final StringBuilder times = new StringBuilder();
        final StringBuilder statuses = new StringBuilder();
        for (ModelItem item : model_items) {
            final PlotSamples samples = item.getSamples();
            final VType value;
            samples.getLock().lock();
            try {
                if (samples.size() == 0)
                    continue;
                final int index = selectedSampleIndex(item, samples, selected_time);
                value = samples.get(index).getVType();
            } finally {
                samples.getLock().unlock();
            }
            if (value == null)
                continue;
            if (times.length() != 0) {
                times.append("; ");
                statuses.append("; ");
            }
            times.append(TimestampFormats.MILLI_FORMAT.format(
                    org.phoebus.core.vtypes.VTypeHelper.getTimestamp(value)));
            statuses.append(MessageFormat.format(Messages.SeverityStatusFmt,
                    org.phoebus.core.vtypes.VTypeHelper.getSeverity(value).toString(),
                    VTypeHelper.getMessage(value)));
        }
        timestamp.setText(times.toString());
        status.setText(statuses.toString());
        updateHistory(selected_time);
    }

    private void updateHistory() {
        if (model_items.isEmpty()) {
            history_plot.setWaveformSeries(List.of());
            return;
        }
        showSelectedSample();
    }

    private void updateHistory(final Instant selected_time) {
        final int previous = history_length.getValue();
        final List<WaveformHistoryChart.WaveformSeries> series = new ArrayList<>();
        final List<String> nonWaveformPvs = new ArrayList<>();

        for (ModelItem item : model_items) {
            final List<double[]> history = new ArrayList<>();
            boolean sawScalarSample = false;
            final PlotSamples samples = item.getSamples();
            samples.getLock().lock();
            try {
                if (samples.size() > 0) {
                    final int closest = selectedSampleIndex(item, samples, selected_time);
                    final int first = Math.max(0, closest - previous);
                    for (int index = first; index <= closest; index++) {
                        final PlotSample sample = samples.get(index);
                        if (!(sample.getVType() instanceof VNumberArray array)) {
                            if (sample.getVType() != null)
                                sawScalarSample = true;
                            continue;
                        }
                        history.add(copyWaveform(array));
                    }
                }
            } finally {
                samples.getLock().unlock();
            }
            if (history.isEmpty() && sawScalarSample)
                nonWaveformPvs.add(item.getResolvedDisplayName());
            series.add(new WaveformHistoryChart.WaveformSeries(
                    item.getResolvedDisplayName(), history));
        }

        // Preserve history depth when it hasn't changed to avoid needless resets.
        if (applied_history_length != previous) {
            history_plot.setHistoryLength(previous);
            applied_history_length = previous;
        }
        history_plot.setWaveformSeries(series);
        updateToolbarWarning(nonWaveformPvs);
    }

    /**
     * Find the index corresponding to the selected time. Call with the
     * PlotSamples lock held and only when the collection is nonempty.
     */
    private int selectedSampleIndex(final ModelItem item,
                                    final PlotSamples samples,
                                    final Instant selectedTime) {
        final int last = samples.size() - 1;
        final int index = item == selection_item
                ? (int) Math.round(sample_index.getValue())
                : new TimeDataSearch().findClosestSample(samples, selectedTime);
        return Math.max(0, Math.min(index, last));
    }

    private static double[] copyWaveform(final VNumberArray array) {
        final double[] values = new double[array.getData().size()];
        for (int index = 0; index < values.length; index++) {
            values[index] = array.getData().getDouble(index);
        }
        return values;
    }

    private static void configureWarning(final Label warning,
                                         final Separator separator) {
        warning.setStyle("-fx-text-fill: #a35b00;");
        warning.setVisible(false);
        warning.managedProperty().bind(warning.visibleProperty());
        warning.setTooltip(new Tooltip());
        separator.visibleProperty().bind(warning.visibleProperty());
        separator.managedProperty().bind(separator.visibleProperty());
    }

    private static void setWarning(final Label warning,
                                   final String summary,
                                   final String details) {
        warning.setText(summary);
        warning.getTooltip().setText(details);
        warning.setVisible(!summary.isEmpty());
    }

    private void updateToolbarWarning(final List<String> nonWaveformPvs) {
        if (over_limit_selected) {
            setWarning(limit_warning,
                    MessageFormat.format(Messages.WaveformHistoryLimitSummary, MAX_PVS),
                    MessageFormat.format(Messages.WaveformHistoryLimitTT, MAX_PVS));
        } else {
            setWarning(limit_warning, "", "");
        }

        if (!nonWaveformPvs.isEmpty()) {
            final String names = String.join(", ", nonWaveformPvs);
            setWarning(waveform_warning,
                    MessageFormat.format(Messages.WaveformHistoryNonWaveformSummary, names),
                    MessageFormat.format(Messages.WaveformHistoryNonWaveformTT, names));
        } else {
            setWarning(waveform_warning, "", "");
        }
    }

    private void clearInfo() {
        timestamp.clear();
        status.clear();
    }
}

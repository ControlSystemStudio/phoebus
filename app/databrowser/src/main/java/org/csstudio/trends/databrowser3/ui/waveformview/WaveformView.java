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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.csstudio.javafx.rtplot.LineStyle;
import org.csstudio.javafx.rtplot.PointType;
import org.csstudio.javafx.rtplot.RTValuePlot;
import org.csstudio.javafx.rtplot.Trace;
import org.csstudio.javafx.rtplot.TraceType;
import org.csstudio.javafx.rtplot.YAxis;
import org.csstudio.javafx.rtplot.data.TimeDataSearch;
import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.AnnotationInfo;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.csstudio.trends.databrowser3.model.ModelListener;
import org.csstudio.trends.databrowser3.model.PlotSample;
import org.csstudio.trends.databrowser3.model.PlotSamples;
import org.csstudio.trends.databrowser3.ui.ToggleToolbarMenuItem;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VType;
import org.phoebus.archive.vtype.VTypeHelper;
import org.phoebus.ui.application.SaveSnapshotAction;
import org.phoebus.ui.javafx.MultiCheckboxCombo;
import org.phoebus.ui.javafx.PrintAction;
import org.phoebus.util.time.TimestampFormats;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Panel for inspecting Waveform (Array) Samples of the current Model
 *  @author Kay Kasemir
 *  @author Will Rogers Original work to show current waveform sample in plot
 *  @author Takashi Nakamoto Allowed original WaveformView to handle multiple items with
 *                           the same name.
 */
@SuppressWarnings("nls")
public class WaveformView extends VBox
{
    /** Text used for the annotation that indicates waveform sample */
    private static final String ANNOTATION_TEXT = Messages.WaveformView;

    private final Model model;

    /** Selected model item in model, or <code>null</code> */
    private final List<ModelItem> model_items = new ArrayList<>();

    /** Waveform for the currently selected sample */
    private final List<WaveformValueDataProvider> waveforms = new ArrayList<>();

    private final MultiCheckboxCombo<ModelItem> items = new MultiCheckboxCombo<>(Messages.WaveformViewSelect);

    /** Is this code updating 'items', so ignore? */
    private boolean updating_selected_items = false;

    private RTValuePlot plot;

    /** Model item that selected the sample */
    private ModelItem selection_item = null;

    /** Index of sample in model_item */
    private final Slider sample_index = new Slider();

    private final TextField timestamp = new TextField(),
                            status = new TextField();

    /** Annotation in plot that indicates waveform sample */
    private final List<AnnotationInfo> waveform_annotations = new ArrayList<>();

    private boolean changing_annotations = false;

    private ScheduledFuture<?> pending_move = null;

    private final ModelListener model_listener = new  ModelListener()
    {
        @Override
        public void itemAdded(final ModelItem item)
        {
            updatePVs();
        }

        @Override
        public void itemRemoved(final ModelItem item)
        {
            model_items.remove(item);
            // Will update the combo to reflect missing item
            updatePVs();
        }

        @Override
        public void changedItemLook(final ModelItem item)
        {
            updatePVs();
        }

        @Override
        public void changedAnnotations()
        {
            if (changing_annotations)
                return;

            // Reacting as the user moves the annotation
            // would be too expensive.
            // Delay, canceling previous request, for "post-selection"
            // type update once the user stops moving the annotation for a little time
            if (pending_move != null)
                pending_move.cancel(false);

            pending_move = Activator.timer.schedule(WaveformView.this::userMovedAnnotation, 500, TimeUnit.MILLISECONDS);
        }

        @Override
        public void changedTimerange()
        {
            // Update selected sample to assert that it's one of the visible ones.
            if (! model_items.isEmpty())
                showSelectedSample();
        }
    };

    public WaveformView(final Model model)
    {
        this.model = model;

        model.addListener(model_listener);

        items.selectedOptions().addListener((Observable o) ->
        {
            if (! updating_selected_items)
                selectPV(items.getSelectedOptions());
        });

        final Button refresh = new Button(Messages.SampleView_Refresh);
        refresh.setTooltip(new Tooltip(Messages.SampleView_RefreshTT));
        refresh.setOnAction(event -> updatePVs());

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

        createPlot();

        sample_index.setBlockIncrement(1);
        sample_index.disableProperty().bind(Bindings.isEmpty(items.selectedOptions()));
        sample_index.setTooltip(new Tooltip(Messages.WaveformTimeSelector));
        sample_index.valueProperty().addListener(p -> showSelectedSample());

        timestamp.setEditable(false);
        status.setEditable(false);
        HBox.setHgrow(timestamp, Priority.ALWAYS);
        HBox.setHgrow(status, Priority.ALWAYS);
        final HBox bottom_row = new HBox(5, new Label(Messages.WaveformTimestamp), timestamp, new Label(Messages.WaveformStatus), status);
        bottom_row.setAlignment(Pos.CENTER_LEFT);

        top_row.setPadding(new Insets(5));
        plot.setPadding(new Insets(0, 5, 5, 5));
        bottom_row.setPadding(new Insets(0, 5, 5, 5));
        VBox.setVgrow(plot, Priority.ALWAYS);
        getChildren().setAll(top_row, plot, sample_index, bottom_row);

        updatePVs();
    }

    private void createPlot()
    {
        plot = new RTValuePlot(true);
        plot.getXAxis().setName(Messages.WaveformIndex);
        plot.getYAxes().get(0).setAutoscale(true);
        plot.getYAxes().get(0).useAxisName(false);
        plot.showLegend(false);
        createContextMenu();
    }

    private void createContextMenu()
    {
        final ContextMenu menu = new ContextMenu();
        plot.setOnContextMenuRequested(event ->
        {
            menu.getItems().setAll(new ToggleToolbarMenuItem(plot));

            final List<Trace<Double>> traces = new ArrayList<>();
            plot.getTraces().forEach(traces::add);
            if (! traces.isEmpty())
                menu.getItems().add(new ToggleLinesMenuItem(plot, traces));


            menu.getItems().addAll(new SeparatorMenuItem(),
                                   new PrintAction(plot),
                                   new SaveSnapshotAction(plot));

            menu.show(getScene().getWindow(), event.getScreenX(), event.getScreenY());
        });
    }

    private void updatePVs()
    {
        // Which PVs were selected?
        final List<ModelItem> select = new ArrayList<>(items.getSelectedOptions());

        updating_selected_items = true;

        // Show PV names of model
        final List<ModelItem> new_items = model.getItems();
        items.setOptions(new_items);

        // Re-select those that were selected and are still valid
        select.retainAll(new_items);
        items.selectOptions(select);

        updating_selected_items = false;

        selectPV(select);
    }

    private void selectPV(final List<ModelItem> selected_items)
    {
        // Delete all existing traces
        for (Trace<Double> trace : plot.getTraces())
            plot.removeTrace(trace);
        waveforms.clear();
        removeAnnotations();

        model_items.clear();
        model_items.addAll(selected_items);

        // No PV?
        if (model_items.isEmpty())
            return;

        // Create trace for each waveform
        for (ModelItem model_item : model_items)
        {
            final WaveformValueDataProvider waveform = new WaveformValueDataProvider();
            waveforms.add(waveform);
            plot.addTrace(model_item.getResolvedDisplayName(), model_item.getUnits(), waveform, model_item.getPaintColor(), TraceType.NONE, 1, LineStyle.SOLID, PointType.CIRCLES, 5, 0);
        }

        // Enable waveform selection and update slider's range
        showSelectedSample();
        // Autoscale Y axes by default.
        for (YAxis<Double> yaxis : plot.getYAxes())
            yaxis.setAutoscale(true);
    }

    private void showSelectedSample()
    {
        // No items?
        if (model_items.isEmpty())
        {
            selection_item = null;
            return;
        }

        // If no item selected for the idx, use the first one
        if (selection_item == null  ||  !model_items.contains(selection_item))
            selection_item = model_items.get(0);

        // Determine time stamp of selected item's sample
        final int idx = (int) Math.round(sample_index.getValue());
        final Instant selected_time;
        PlotSamples samples = selection_item.getSamples();
        samples.getLock().lock();
        try
        {
            selected_time = samples.get(idx).getPosition();
        }
        finally
        {
            samples.getLock().unlock();
        }

        String timestampText = "", statusText = "";
        int n = 0, max_size = 1;
        for (ModelItem model_item : model_items)
        {
            // Get selected sample (= one waveform)
            samples = model_item.getSamples();
            PlotSample sample;
            samples.getLock().lock();
            try
            {
                if (model_item == selection_item)
                {   // idx refers to exact sample of this item
                    sample_index.setMax(samples.size());
                    sample = samples.get(idx);
                }
                else
                {   // Find closest sample based on time stamp
                    final TimeDataSearch search = new TimeDataSearch();
                    final int s = search.findClosestSample(samples, selected_time);
                    sample = samples.get(s);
                }
            }
            finally
            {
                samples.getLock().unlock();
            }

            // Setting the value can be delayed while the plot is being updated
            final VType value = sample.getVType();
            final WaveformValueDataProvider waveform = waveforms.get(n);
            Activator.thread_pool.execute(() -> waveform.setValue(value));
            if (value == null)
                clearInfo();
            else
            {
                updateAnnotation(n, sample.getPosition(), sample.getValue());

                if (n == 0)
                {
                    final int size = value instanceof VNumberArray ? ((VNumberArray)value).getData().size() : 1;
                    if (size > max_size)
                        max_size = size;
                    timestampText = TimestampFormats.MILLI_FORMAT.format(org.phoebus.core.vtypes.VTypeHelper.getTimestamp(value));
                    statusText = MessageFormat.format(Messages.SeverityStatusFmt, org.phoebus.core.vtypes.VTypeHelper.getSeverity(value).toString(), VTypeHelper.getMessage(value));
                }
                else
                {
                    timestampText += "; " + TimestampFormats.MILLI_FORMAT.format(org.phoebus.core.vtypes.VTypeHelper.getTimestamp(value));
                    statusText += "; " + MessageFormat.format(Messages.SeverityStatusFmt, org.phoebus.core.vtypes.VTypeHelper.getSeverity(value).toString(), VTypeHelper.getMessage(value));
                }
            }

            ++n;
        }
        plot.getXAxis().setValueRange(0.0, (double)max_size);

        timestamp.setText(timestampText);
        status.setText(statusText);
        plot.requestUpdate();
    }

    /** Clear all the info fields. */
    private void clearInfo()
    {
        timestamp.setText("");
        status.setText("");
        removeAnnotations();
        selection_item = null;
    }

    private void userMovedAnnotation()
    {
        if (waveform_annotations.isEmpty())
            return;

        // Compare position of waveform_annotations to those in model
        // to determine which annotation the user moved.
        final List<AnnotationInfo> model_annotations = model.getAnnotations();
        for (AnnotationInfo waveform_annotation : waveform_annotations)
            for (AnnotationInfo annotation : model_annotations)
                if (annotation.isInternal()                                          &&
                    annotation.getItemIndex() == waveform_annotation.getItemIndex()  &&
                    ! annotation.getTime().equals(waveform_annotation.getTime()))
            {
                // System.out.println("User moved " + annotation.getText() + "\nfrom " + waveform_annotation.getTime() + "\n  to " + annotation.getTime());

                selection_item = model.getItems().get(annotation.getItemIndex());
                final PlotSamples samples = selection_item.getSamples();
                final TimeDataSearch search = new TimeDataSearch();
                final int idx;
                samples.getLock().lock();
                try
                {
                    idx = search.findClosestSample(samples, annotation.getTime());
                }
                finally
                {
                    samples.getLock().unlock();
                }
                // Update waveform view for that sample on UI thread
                Platform.runLater(() ->
                {
                    if (sample_index.getMax() < idx)
                        sample_index.setMax(idx);
                    sample_index.setValue(idx);
                    showSelectedSample();
                });
                return;
            }
    }

    private void updateAnnotation(final int annotation_index, final Instant time, final double value)
    {
        final List<AnnotationInfo> annotations = new ArrayList<>(model.getAnnotations());
        // Initial annotation offset
        Point2D offset = new Point2D(20, -20);

        final String label = ANNOTATION_TEXT + " " + model_items.get(annotation_index).getDisplayName();

        // If already in model, note its offset and remove
        for (AnnotationInfo annotation : annotations)
        {
            if (annotation.getText().equals(label))
            {   // Update offset to where user last placed it
                offset = annotation.getOffset();
                annotations.remove(annotation);
                break;
            }
        }

        final int item_index = model.getItems().indexOf(model_items.get(annotation_index));
        final AnnotationInfo waveform_annotation = new AnnotationInfo(true, item_index, time, value, offset, label);
        waveform_annotations.add(annotation_index, waveform_annotation);
        annotations.add(waveform_annotation);

        changing_annotations = true;
        model.setAnnotations(annotations);
        changing_annotations = false;
    }

    private void removeAnnotations()
    {
        boolean changes = false;
        // Remove all waveform_annotations from model
        final List<AnnotationInfo> modelAnnotations = new ArrayList<>(model.getAnnotations());
        for (AnnotationInfo waveform_annotation : waveform_annotations)
            if (modelAnnotations.remove(waveform_annotation))
                changes = true;
        waveform_annotations.clear();

        if (changes)
        {
            changing_annotations = true;
            model.setAnnotations(modelAnnotations);
            changing_annotations = false;
        }
    }
}

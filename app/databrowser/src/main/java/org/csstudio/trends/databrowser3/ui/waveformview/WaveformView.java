/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.waveformview;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.csstudio.javafx.rtplot.PointType;
import org.csstudio.javafx.rtplot.RTValuePlot;
import org.csstudio.javafx.rtplot.Trace;
import org.csstudio.javafx.rtplot.TraceType;
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
import org.phoebus.archive.vtype.VTypeHelper;
import org.phoebus.ui.application.SaveSnapshotAction;
import org.phoebus.ui.javafx.PrintAction;
import org.phoebus.util.time.TimestampFormats;
import org.phoebus.vtype.VNumberArray;
import org.phoebus.vtype.VType;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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
    private static final String ANNOTATION_TEXT = "Waveform view";

    private final Model model;

    /** Selected model item in model, or <code>null</code> */
    private ModelItem model_item = null;

    /** Waveform for the currently selected sample */
    private final WaveformValueDataProvider waveform = new WaveformValueDataProvider();

    private final ComboBox<String> items = new ComboBox<>();
    private RTValuePlot plot;
    private final Slider sample_index = new Slider();
    private final TextField timestamp = new TextField(),
                            status = new TextField();

    /** Annotation in plot that indicates waveform sample */
    private AnnotationInfo waveform_annotation;

    private boolean changing_annotations = false;

    private ScheduledFuture<?> pending_move = null;

    private final ModelListener model_listener = new  ModelListener()
    {
        @Override
        public void itemAdded(ModelItem item)
        {
            updatePVs();
        }

        @Override
        public void itemRemoved(ModelItem item)
        {
            if (item == model_item)
                model_item = null;
            // Will update the combo to reflect missing item,
            // then detect model_item change and selectPV(null)
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

            pending_move = Activator.thread_pool.schedule(WaveformView.this::userMovedAnnotation, 500, TimeUnit.MILLISECONDS);
        }

        @Override
        public void changedTimerange()
        {
            // Update selected sample to assert that it's one of the visible ones.
            if (model_item != null)
                showSelectedSample();
        }
    };

    public WaveformView(final Model model)
    {
        this.model = model;

        model.addListener(model_listener);

        items.setOnAction(event -> selectPV(items.getSelectionModel().getSelectedItem()));

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
        sample_index.disableProperty().bind(items.getSelectionModel().selectedItemProperty().isNull());
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

            final Iterator<Trace<Double>> traces = plot.getTraces().iterator();
            if (traces.hasNext())
                menu.getItems().add(new ToggleLinesMenuItem(plot, traces.next()));


            menu.getItems().addAll(new SeparatorMenuItem(),
                                   new PrintAction(plot),
                                   new SaveSnapshotAction(plot));

            menu.show(getScene().getWindow(), event.getScreenX(), event.getScreenY());
        });
    }

    private void updatePVs()
    {
        // Show PV names
        final List<String> model_items = model.getItems().stream().map(item -> item.getName()).collect(Collectors.toList());
        if (! model_items.equals(items.getItems()))
        {
            items.getItems().setAll( model_items );
            if (model_item != null)
                items.getSelectionModel().select(model_item.getName());
        }
        selectPV(items.getSelectionModel().getSelectedItem());
    }

    private void selectPV(final String item_name)
    {
        model_item = model.getItem(item_name);

        // Delete all existing traces
        for (Trace<Double> trace : plot.getTraces())
            plot.removeTrace(trace);

        // No or unknown PV name?
        if (model_item == null)
        {
            items.getSelectionModel().clearSelection();
            removeAnnotation();
            return;
        }

        // Create trace for waveform
        plot.addTrace(model_item.getResolvedDisplayName(), model_item.getUnits(), waveform, model_item.getPaintColor(), TraceType.NONE, 1, PointType.CIRCLES, 5, 0);
        // Enable waveform selection and update slider's range
        showSelectedSample();
        // Autoscale Y axis by default.
        plot.getYAxes().get(0).setAutoscale(true);
    }

    private void showSelectedSample()
    {
        // Get selected sample (= one waveform)
        final PlotSamples samples = model_item.getSamples();
        final int idx = (int) Math.round(sample_index.getValue());
        final PlotSample sample;
        samples.getLock().lock();
        try
        {
            sample_index.setMax(samples.size());
            sample = samples.get(idx);
        }
        finally
        {
            samples.getLock().unlock();
        }
        // Setting the value can be delayed while the plot is being updated
        final VType value = sample.getVType();
        Activator.thread_pool.execute(() -> waveform.setValue(value));
        if (value == null)
            clearInfo();
        else
        {
            updateAnnotation(sample.getPosition(), sample.getValue());
            int size = value instanceof VNumberArray ? ((VNumberArray)value).getData().size() : 1;
            plot.getXAxis().setValueRange(0.0, (double)size);
            timestamp.setText(TimestampFormats.MILLI_FORMAT.format(VTypeHelper.getTimestamp(value)));
            status.setText(MessageFormat.format(Messages.SeverityStatusFmt, VTypeHelper.getSeverity(value).toString(), VTypeHelper.getMessage(value)));
        }
        plot.requestUpdate();
    }

    /** Clear all the info fields. */
    private void clearInfo()
    {
        timestamp.setText("");
        status.setText("");
        removeAnnotation();
    }

    private void userMovedAnnotation()
    {
        if (waveform_annotation == null)
            return;
        for (AnnotationInfo annotation : model.getAnnotations())
        {   // Locate the annotation for this waveform
            if (annotation.isInternal()  &&
                annotation.getItemIndex() == waveform_annotation.getItemIndex() &&
                annotation.getText().equals(waveform_annotation.getText()))
            {   // Locate index of sample for annotation's time stamp
                final PlotSamples samples = model_item.getSamples();
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
    }

    private void updateAnnotation(final Instant time, final double value)
    {
        final List<AnnotationInfo> annotations = new ArrayList<>(model.getAnnotations());
        // Initial annotation offset
        Point2D offset = new Point2D(20, -20);
        // If already in model, note its offset and remove
        for (AnnotationInfo annotation : annotations)
        {
            if (annotation.getText().equals(ANNOTATION_TEXT))
            {   // Update offset to where user last placed it
                offset = annotation.getOffset();
                waveform_annotation = annotation;
                annotations.remove(waveform_annotation);
                break;
            }
        }

        final int item_index = model.getItems().indexOf(model_item);
        waveform_annotation = new AnnotationInfo(true, item_index, time, value, offset, ANNOTATION_TEXT);
        annotations.add(waveform_annotation);
        changing_annotations = true;
        model.setAnnotations(annotations);
        changing_annotations = false;
    }

    private void removeAnnotation()
    {
        final List<AnnotationInfo> modelAnnotations = new ArrayList<>(model.getAnnotations());
        if (modelAnnotations.remove(waveform_annotation))
        {
            changing_annotations = true;
            model.setAnnotations(modelAnnotations);
            changing_annotations = false;
        }
    }
}

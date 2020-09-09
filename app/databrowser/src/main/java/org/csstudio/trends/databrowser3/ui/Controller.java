/*******************************************************************************
 * Copyright (c) 2010-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui;

import static org.csstudio.trends.databrowser3.Activator.logger;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.csstudio.javafx.rtplot.Trace;
import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.archive.ArchiveFetchJob;
import org.csstudio.trends.databrowser3.archive.ArchiveFetchJobListener;
import org.csstudio.trends.databrowser3.model.AnnotationInfo;
import org.csstudio.trends.databrowser3.model.ArchiveDataSource;
import org.csstudio.trends.databrowser3.model.ArchiveRescale;
import org.csstudio.trends.databrowser3.model.AxisConfig;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.csstudio.trends.databrowser3.model.ModelListener;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.csstudio.trends.databrowser3.preferences.Preferences;
import org.csstudio.trends.databrowser3.ui.plot.ModelBasedPlot;
import org.csstudio.trends.databrowser3.ui.plot.PlotListener;
import org.csstudio.trends.databrowser3.ui.properties.AddAxisCommand;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.undo.UndoableActionManager;
import org.phoebus.util.time.TimeInterval;
import org.phoebus.util.time.TimeRelativeInterval;

import javafx.application.Platform;

/** Controller that interfaces the {@link Model} with the {@link ModelBasedPlotSWT}:
 *  <ul>
 *  <li>For each item in the Model, create a trace in the plot.
 *  <li>Perform scrolling of the time axis.
 *      Scrolling is enabled via a model time range ending in relative value of Duration.ZERO.
 *  <li>When the plot is interactively zoomed, update the Model's time range.
 *  <li>Get archived data whenever the time axis changes.
 *  </ul>
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Controller
{
    /** Model with data to display */
    final Model model;

    /** Listener to model that informs this controller */
    private ModelListener model_listener;

    /** GUI for displaying the data */
    final ModelBasedPlot plot;

    /** Prevent loop between model and plot when changing their annotations */
    private boolean changing_annotations = false;

    /** Task executed by update_timer.
     *  Only changed on UI thread
     */
    private volatile ScheduledFuture<?> update_task = null;

    /** Delay to avoid flurry of archive requests */
    final private long archive_fetch_delay = Preferences.archive_fetch_delay;

    /** Delayed task to avoid flurry of archive requests
     *  @see #scheduleArchiveRetrieval(ITimestamp, ITimestamp)
     */
    private ScheduledFuture<?> archive_fetch_delay_task = null;

    /** Currently active archive jobs, used to prevent multiple requests
     *  for the same model item.
     */
    final private List<ArchiveFetchJob> archive_fetch_jobs = new ArrayList<>();

    /** Is the window iconized? */
    // Track the window state?
    // Problem is that the window can change if our 'tab' is moved between windows.
    // Check in each update if the window is visible, then skip it?
    protected volatile boolean window_is_iconized = false;

    /** Should we perform redraws, or is the window hidden and we should suppress them? */
    private boolean suppress_redraws = false;

    private final ArchiveFetchJobListener archive_fetch_job_listener = new ArchiveFetchJobListener()
    {
        @Override
        public void fetchCompleted(final ArchiveFetchJob job)
        {
            synchronized (archive_fetch_jobs)
            {
                archive_fetch_jobs.remove(job);
                // System.out.println("Completed " + job + ", " + archive_fetch_jobs.size() + " left");
                if (!archive_fetch_jobs.isEmpty())
                    return;
            }
            // All completed. Do something to the plot?
            final ArchiveRescale rescale = model.getArchiveRescale();
            if (rescale == ArchiveRescale.STAGGER)
                plot.getPlot().stagger(false);
            else
                doUpdate();
        }

        @Override
        public void archiveFetchFailed(final ArchiveFetchJob job,
                final ArchiveDataSource archive, final Exception error)
        {
            logger.log(Level.WARNING, "No archived data for " + job.getPVItem().getDisplayName(), error);
            // Remove the problematic archive data source, but has to happen in UI thread
            if (Preferences.drop_failed_archives)
                Platform.runLater(() ->  job.getPVItem().removeArchiveDataSource(archive));
        }

        @Override
        public void channelNotFound(final ArchiveFetchJob job, final boolean channelFoundAtLeastOnce,
                final List<ArchiveDataSource> archivesThatFailed)
        {
            logger.log(Level.INFO,
                       () -> "Channel " + job.getPVItem().getResolvedDisplayName() + " not found in " + archivesThatFailed + ", removing data source.");
            // no need to reuse this source if the channel is not in it, but it has to happen in the UI thread, because
            // of the way the listeners of the pv item are implemented
            Platform.runLater(() ->  job.getPVItem().removeArchiveDataSource(archivesThatFailed));

            // if channel was found at least once, we do not need to report anything
            if (!channelFoundAtLeastOnce)
            {
                logger.log(Level.INFO,
                           () -> "Channel " + job.getPVItem().getResolvedDisplayName() + " not found in any of the archived sources.");
            }
        }
    };

    private final PlotListener plot_listener = new PlotListener()
    {
        @Override
        public void timeAxisChanged(final boolean scrolling, final Instant start, final Instant end)
        {
            final TimeRelativeInterval interval;
            if (scrolling)
            {
                final Duration span = Duration.between(start, end);
                interval = TimeRelativeInterval.startsAt(span);
            }
            else
                interval = TimeRelativeInterval.of(start, end);

            // Update model's time range
            model.setTimerange(interval);
            // Controller's ModelListener will fetch new archived data
        }

        @Override
        public void valueAxisChanged(final int index, final double lower, final double upper)
        {   // Update axis range in model, using UI thread because event may come from 'stagger' background thread
            final AxisConfig axis = model.getAxis(index);
            if (axis != null) {
                //only update if the model has that axis. If the trend is empty, the model may not have that axis
                Platform.runLater(() -> axis.setRange(lower, upper));
            }
        }

        @Override
        public void changedAnnotations(final List<AnnotationInfo> annotations)
        {
            if (changing_annotations)
                return;
            changing_annotations = true;
            model.setAnnotations(annotations);
            changing_annotations = false;
        }

        @Override
        public void selectedSamplesChanged()
        {
            model.fireSelectedSamplesChanged();
        }

        @Override
        public void changedToolbar(final boolean visible)
        {
            model.setToolbarVisible(visible);
        }

        @Override
        public void changedLegend(final boolean visible)
        {
            model.setLegendVisible(visible);
        }

        @Override
        public void autoScaleChanged(int index, boolean autoScale)
        {
            final AxisConfig axis = model.getAxis(index);
            if (axis != null)
                Platform.runLater(() -> axis.setAutoScale(autoScale));
        }

        @Override
        public void gridChanged(int index, boolean show_grid)
        {
            if (index == -1)
                Platform.runLater(() -> model.setGridVisible(show_grid));
            else
            {
                final AxisConfig axis = model.getAxis(index);
                if (axis != null)
                    Platform.runLater(() -> axis.setGridVisible(show_grid));
            }
        }

        @Override
        public void logarithmicChanged(int index, boolean use_log)
        {
            final AxisConfig axis = model.getAxis(index);
            if (axis != null)
                Platform.runLater(() -> axis.setLogScale(use_log));
        }

        @Override
        public void timeConfigRequested()
        {
            ChangeTimerangeAction.run(model, plot.getPlot(), plot.getPlot().getUndoableActionManager());
        }

        @Override
        public void droppedNames(final List<String> names)
        {
            // Offer potential PV name in dialog so user can edit/cancel
            // sim://sine sim://ramp sim://noise
            final AddPVDialog dlg = new AddPVDialog(names.size(), model, false);
            DialogHelper.positionDialog(dlg, plot.getPlot(), -200, -200);
            for (int i=0; i<names.size(); ++i)
                dlg.setName(i, names.get(i));
            if (! dlg.showAndWait().orElse(false))
                return;

            final UndoableActionManager undo = plot.getPlot().getUndoableActionManager();
            for (int i=0; i<names.size(); ++i)
            {
                final AxisConfig axis = AddPVDialog.getOrCreateAxis(model, undo, dlg.getAxisIndex(i));
                AddModelItemCommand.forPV(undo,
                        model, dlg.getName(i), dlg.getScanPeriod(i),
                        axis, null);
            }
        }

        @Override
        public void droppedPVNames(List<ProcessVariable> names,
                                   List<ArchiveDataSource> archives)
        {
            // TODO Handle dropped PV names
//            if (names == null)
//            {
//                if (archives == null)
//                    return;
//                // Received only archives. Add to all PVs
//                for (ArchiveDataSource archive : archives)
//                    for (ModelItem item : model.getItems())
//                    {
//                        if (! (item instanceof PVItem))
//                            continue;
//                        final PVItem pv = (PVItem) item;
//                        if (pv.hasArchiveDataSource(archive))
//                            continue;
//                        new AddArchiveCommand(plot.getPlot().getUndoableActionManager(), pv, archive);
//                    }
//            }
//            else
//            {
                // Received PV names, maybe with archive
                final UndoableActionManager undo = plot.getPlot().getUndoableActionManager();

                // When multiple PVs are dropped, assert that there is at least one axis.
                // Otherwise dialog cannot offer adding all PVs onto the same axis.
                if (names.size() > 1  &&  model.getAxisCount() <= 0)
                    new AddAxisCommand(undo, model);

                final AddPVDialog dlg = new AddPVDialog(names.size(), model, false);
                DialogHelper.positionDialog(dlg, plot.getPlot(), -200, -200);
                for (int i=0; i<names.size(); ++i)
                    dlg.setName(i, names.get(i).getName());
                if (! dlg.showAndWait().orElse(false))
                    return;

                for (int i=0; i<names.size(); ++i)
                {
                    final AxisConfig axis = AddPVDialog.getOrCreateAxis(model, undo, dlg.getAxisIndex(i));
                    final ArchiveDataSource archive =
                            (archives == null || i>=archives.size()) ? null : archives.get(i);
                    AddModelItemCommand.forPV(undo,
                            model, dlg.getName(i), dlg.getScanPeriod(i),
                            axis, archive);
                }
//                return;
//            }

        }

        @Override
        public void droppedFilename(File file_name)
        {
            // TODO Handle dropped file name (import data)
//            final FileImportDialog dlg = new FileImportDialog(shell, file_name);
//            if (dlg.open() != Window.OK)
//                return;
//
//            final UndoableActionManager operations_manager = plot.getPlot().getUndoableActionManager();
//
//            // Add to first empty axis, or create new axis
//            final AxisConfig axis = model.getEmptyAxis().orElseGet(
//                    () -> new AddAxisCommand(operations_manager, model).getAxis() );
//
//            // Add archivedatasource for "import:..." and let that load the file
//            final String type = dlg.getType();
//            file_name = dlg.getFileName();
//            final String url = ImportArchiveReaderFactory.createURL(type, file_name);
//            final ArchiveDataSource imported = new ArchiveDataSource(url, 1, type);
//            // Add PV Item with data to model
//            AddModelItemCommand.forPV(shell, operations_manager,
//                    model, dlg.getItemName(), Preferences.getScanPeriod(),
//                    axis, imported);
        }
    };

    /** Initialize
     *  @param shell Shell
     *  @param model Model that has the data
     *  @param plot Plot for displaying the Model
     *  @throws Error when called from non-UI thread
     */
    public Controller(final Model model, final ModelBasedPlot plot)
    {
        this.model = model;
        this.plot = plot;

        createPlotTraces();

        model_listener = new ModelListener()
        {
            @Override
            public void changedTitle()
            {
                String title = model.getTitle().orElse(null);
                if (title != null)
                    title = model.resolveMacros(title);
                plot.getPlot().setTitle(title);
            }

            @Override
            public void changedLayout()
            {
                plot.getPlot().showToolbar(model.isToolbarVisible());
                plot.getPlot().showLegend(model.isLegendVisible());
            }

            @Override
            public void changedTiming()
            {
                plot.getPlot().setScrollStep(model.getScrollStep());
                if (update_task != null)
                    createUpdateTask();
            }

            @Override
            public void changedColorsOrFonts()
            {
                plot.getPlot().setForeground(model.getPlotForeground());
                plot.getPlot().getXAxis().setColor(model.getPlotForeground());
                plot.getPlot().setBackground(model.getPlotBackground());
                plot.getPlot().setTitleFont(model.getTitleFont());
                plot.getPlot().setLegendFont(model.getLegendFont());
                setAxisFonts();
            }

            @Override
            public void changedTimerange()
            {
                final TimeRelativeInterval span = model.getTimerange();
                final TimeInterval abs = span.toAbsoluteInterval();
                if (span.isStartAbsolute())
                    plot.setTimeRange(false, abs.getStart(), abs.getEnd());
                else
                    plot.setTimeRange(true, abs.getStart(), abs.getEnd().plus(model.getScrollStep()));
                // Get matching archived data
                scheduleArchiveRetrieval();
            }

            @Override
            public void changedTimeAxisConfig()
            {
                plot.getPlot().getXAxis().setGridVisible(model.isGridVisible());
            }

            @Override
            public void changedAxis(final Optional<AxisConfig> axis)
            {
                if (axis.isPresent())
                {   // Update specific axis
                    final AxisConfig the_axis = axis.get();
                    int i = 0;
                    for (AxisConfig axis_config : model.getAxes())
                    {
                        if (axis_config == the_axis)
                        {
                            plot.updateAxis(i, the_axis);
                            return;
                        }
                        ++i;
                    }
                }
                else  // New or removed axis: Recreate the whole plot
                    createPlotTraces();
            }

            @Override
            public void itemAdded(final ModelItem item)
            {
                // Item may be added in 'middle' of existing traces
                createPlotTraces();
                // Get archived data for new item (NOP for non-PVs)
                getArchivedData(item);
            }

            @Override
            public void itemRemoved(final ModelItem item)
            {
                plot.removeTrace(item);
            }

            @Override
            public void changedItemVisibility(final ModelItem item)
            {   // Add/remove from plot, but don't need to get archived data
                // When made visible, note that item could be in 'middle'
                // of existing traces, so need to re-create all
                if (item.isVisible())
                    createPlotTraces();
                else // To hide, simply remove
                    plot.removeTrace(item);
            }

            @Override
            public void changedItemLook(final ModelItem item)
            {
                plot.updateTrace(item);
            }

            @Override
            public void changedItemUnits(final ModelItem item)
            {
                plot.updateTrace(item);
            }

            @Override
            public void changedItemDataConfig(final PVItem item, final boolean archive_invalid)
            {
                if (archive_invalid)
                    getArchivedData(item);
            }

            @Override
            public void itemRefreshRequested(final PVItem item)
            {
                getArchivedData(item);
            }

            @Override
            public void changedAnnotations()
            {
                if (changing_annotations)
                    return;
                changing_annotations = true;
                plot.setAnnotations(model.getAnnotations());
                changing_annotations = false;
            }
        };

        model.addListener(model_listener);
        plot.addListener(plot_listener);
    }

    /** @return Data Browser model */
    public Model getModel()
    {
        return model;
    }

    /** @param suppress_redraws <code>true</code> if controller should suppress
     *        redraws because window is hidden
     */
    public void suppressRedraws(final boolean suppress_redraws)
    {
        if (this.suppress_redraws == suppress_redraws)
            return;
        this.suppress_redraws = suppress_redraws;
        if (!suppress_redraws)
            plot.redrawTraces();
    }

    /** Schedule fetching archived data.
     *
     *  <p>When the user moves the time axis around, archive requests for the
     *  new time range are delayed to avoid a flurry of archive
     *  requests while the user is still moving around.
     *  This request is therefore a little delayed, and a follow-up
     *  request will cancel an ongoing, scheduled, request.
     */
    public void scheduleArchiveRetrieval()
    {
        if (archive_fetch_delay_task != null)
            archive_fetch_delay_task.cancel(false);
        // Compiler error "schedule(Runnable, long, TimeUnit) is ambiguous"
        // unless specifically casting getArchivedData to Runnable.
        final Runnable fetch = this::getArchivedData;
        archive_fetch_delay_task = Activator.timer.schedule(fetch, archive_fetch_delay, TimeUnit.MILLISECONDS);
    }

    /** Start model items and initiate scrolling/updates
     *  @throws Exception on error: Already running, problem starting threads, ...
     *  @see #isRunning()
     */
    public void start() throws Exception
    {
        if (isRunning())
            throw new IllegalStateException("Already started");

        final List<Trace<Instant>> traces = new ArrayList<>();
        for (Trace<Instant> trace : plot.getPlot().getTraces())
            traces.add(trace);

        // Initialize scroll step
        plot.getPlot().setScrollStep(model.getScrollStep());
        createUpdateTask();

        model.start();

        // Initial time range setup, schedule archive fetch
        model_listener.changedTimerange();
    }

    /** @return <code>true</code> while running
     *  @see #stop()
     */
    public boolean isRunning()
    {
        return update_task != null;
    }

    /** Create or re-schedule update task
     *  @see #start()
     */
    private void createUpdateTask()
    {
        // Can't actually re-schedule, so stop one that might already be running
        if (update_task != null)
        {
            update_task.cancel(true);
            update_task = null;
        }

        final long update_delay = (long) (model.getUpdatePeriod() * 1000);
        update_task = Activator.timer.scheduleAtFixedRate(this::doUpdate, update_delay, update_delay, TimeUnit.MILLISECONDS);
    }

    private void doUpdate()
    {
        try
        {
            // Skip updates while nobody is watching
            if (window_is_iconized || suppress_redraws)
                return;

            // Check if anything changed, which also updates formulas.
            // When scrolling, need to update even when nothing changed to 'scroll'.
            final TimeRelativeInterval span = model.getTimerange();
            final boolean scrolling = !span.isEndAbsolute();
            if (model.updateItemsAndCheckForNewSamples() || scrolling)
                plot.redrawTraces();
        }
        catch (Throwable ex)
        {
            logger.log(Level.WARNING, "Error in Plot refresh timer", ex);
        }
    }

    /** Stop scrolling and model items
     *  @throws IllegalStateException when not running
     */
    public void stop()
    {
        if (! isRunning())
            throw new IllegalStateException("Not started");
        // Stop ongoing archive access
        synchronized (archive_fetch_jobs)
        {
            for (ArchiveFetchJob job : archive_fetch_jobs)
                job.cancel();
            archive_fetch_jobs.clear();
        }
        // Stop update task
        model.stop();
        model.dispose();
        update_task.cancel(true);
        update_task = null;
    }

    /** (Re-) create traces in plot for each item in the model */
    public void createPlotTraces()
    {
        if (! plot.lockTracesForWriting())
            return;
        try
        {
            plot.removeAll();
            int i = 0;
            for (AxisConfig axis : model.getAxes())
                plot.updateAxis(i++, axis);
            for (ModelItem item : model.getItems())
                if (item.isVisible())
                    plot.addTrace(item);
        }
        finally
        {
            plot.unlockTracesForWriting();
        }
        setAxisFonts();
    }

    /** Set all axis fonts to the scale and label font of the model */
    private void setAxisFonts()
    {
        plot.getPlot().getXAxis().setLabelFont(model.getLabelFont());
        plot.getPlot().getXAxis().setScaleFont(model.getScaleFont());
        final int acount = plot.getTotalAxesCount();
        for (int idx = 0; idx < acount; idx++)
        {
            plot.getPlotAxis(idx).setLabelFont(model.getLabelFont());
            plot.getPlotAxis(idx).setScaleFont(model.getScaleFont());
        }
    }

    /** Initiate archive data retrieval for all model items */
    private void getArchivedData()
    {
        final TimeInterval interval = model.getTimerange().toAbsoluteInterval();
        for (ModelItem item : model.getItems())
            getArchivedData(item, interval.getStart(), interval.getEnd());
    }

    /** Initiate archive data retrieval for a specific model item
     *  @param item Model item. NOP for non-PVItem
     */
    private void getArchivedData(final ModelItem item)
    {
        final TimeInterval interval = model.getTimerange().toAbsoluteInterval();
        getArchivedData(item, interval.getStart(), interval.getEnd());
    }

    /** Initiate archive data retrieval for a specific model item
     *  @param item Model item. NOP for non-PVItem
     *  @param start Start time
     *  @param end End time
     */
    private void getArchivedData(final ModelItem item,
                                 final Instant start, final Instant end)
    {
        if (! isRunning())
            return;

        // Only useful for PVItems with archive data source
        if (!(item instanceof PVItem))
            return;
        final PVItem pv_item = (PVItem) item;
        if (pv_item.getArchiveDataSources().isEmpty())
            return;

        synchronized (archive_fetch_jobs)
        {
            // Cancel ongoing jobs for this item
            for (Iterator<ArchiveFetchJob> iter = archive_fetch_jobs.iterator();  iter.hasNext();  /**/)
            {
                final ArchiveFetchJob job = iter.next();
                if (job.getPVItem() == pv_item)
                {
                    job.cancel();
                    iter.remove();
                }
            }

            // Track new job
            final ArchiveFetchJob new_job = new ArchiveFetchJob(pv_item, start, end, archive_fetch_job_listener);
            archive_fetch_jobs.add(new_job);
        }
    }
}

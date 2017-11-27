/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.jobs;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** Viewer for active {@link Job}s
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class JobViewer implements AppInstance
{
    static JobViewer INSTANCE = null;

    private static final Image ABORT = new Image(JobViewer.class.getResourceAsStream("/icons/abort.gif"));

    private final AppDescriptor app;
    private final DockItem tab;
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();


    JobViewer(final AppDescriptor app)
    {
        this.app = app;

        tab = new DockItem(this, create());
        startUpdates();
        tab.addCloseCheck(() ->
        {
            stopUpdates();
            return true;
        });
        tab.addClosedNotification(() -> INSTANCE = null);
        DockPane.getActiveDockPane().addTab(tab);
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }

    void raise()
    {
        tab.select();
    }

    private static class JobInfo
    {
        Job job;
        StringProperty name;
        StringProperty status;

        JobInfo(final Job job)
        {
            this.job = job;
            name = new SimpleStringProperty(job.getName());
            status = new SimpleStringProperty(job.getMonitor().toString());
        }

        public void update(Job job)
        {
            this.job = job;
            name.set(job.getName());
            status.set(job.getMonitor().toString());
        }
    }

    private static class CancelTableCell extends TableCell<JobInfo, Boolean>
    {
        @Override
        protected void updateItem(final Boolean ignored, final boolean empty)
        {
            super.updateItem(ignored, empty);

            boolean running = ! empty;

            TableRow<JobInfo> row = null;
            if (running)
            {
                row = getTableRow();
                if (row == null)
                    running = false;
            }

            if (running)
            {
                setAlignment(Pos.CENTER_RIGHT);
                final JobInfo info = row.getItem();
                final Button cancel = new Button("Cancel", new ImageView(ABORT));
                cancel.setOnAction(event -> info.job.cancel());
                cancel.setMaxWidth(Double.MAX_VALUE);
                setGraphic(cancel);
            }
            else
                setGraphic(null);
        }
    }

    private final ObservableList<JobInfo> job_infos = FXCollections.observableArrayList();

    private Node create()
    {
        final TableView<JobInfo> table = new TableView<>(job_infos);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        final TableColumn<JobInfo, String> name_col = new TableColumn<>("Name");
        name_col.setCellValueFactory(cell -> cell.getValue().name);
        table.getColumns().add(name_col);

        final TableColumn<JobInfo, String> status_col = new TableColumn<>("Status");
        status_col.setCellValueFactory(cell -> cell.getValue().status);
        table.getColumns().add(status_col);

        final TableColumn<JobInfo, Boolean> stop_col = new TableColumn<>("");
        stop_col.setCellFactory(col -> new CancelTableCell());
        table.getColumns().add(stop_col);

        updateJobs();

        return table;
    }

    private void startUpdates()
    {
        // Schedule periodic updates
        final Runnable update = () ->
        {
            // Actual update is performed on UI thread,
            // but wait with scheduling another update
            // until that UI work completes
            final CountDownLatch done = new CountDownLatch(1);
            Platform.runLater(() ->
            {
                updateJobs();
                done.countDown();
            });
            try
            {
                done.await();
            }
            catch (InterruptedException e)
            {
                // Ignore
            }
        };
        timer.scheduleAtFixedRate(update, 0, 1000, TimeUnit.MILLISECONDS);

    }

    private void stopUpdates()
    {
        timer.shutdown();
    }

    private void updateJobs()
    {
        final List<Job> jobs = JobManager.getJobs();
        for (int i=0; i<jobs.size(); ++i)
        {
            if (i < job_infos.size())
                job_infos.get(i).update(jobs.get(i));
            else
                job_infos.add(new JobInfo(jobs.get(i)));
        }

        if (job_infos.size() > jobs.size())
            job_infos.remove(jobs.size(), job_infos.size());
    }
}

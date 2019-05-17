/*******************************************************************************
 * Copyright (c) 2018-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.update;

import java.io.File;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.framework.workbench.Locations;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.jobs.JobViewerApplication;
import org.phoebus.ui.statusbar.StatusBar;
import org.phoebus.util.time.TimestampFormats;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;

/** Application descriptor for 'Update'
 *
 *  <p>When application is started, it checks for updates.
 *
 *  <p>If updates are available, it adds a status bar button
 *  that allows self-update.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class UpdateApplication implements AppDescriptor
{
    private static final String NAME = "Update";
    private Button start_update = null;

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public void start()
    {
        JobManager.schedule(NAME, monitor ->
        {
            // Wait a little to allow other startup tasks to run
            TimeUnit.SECONDS.sleep(Update.delay);
            if (monitor.isCanceled())
                return;
            final Instant new_version = Update.checkForUpdate(monitor);
            if (new_version != null)
                installUpdateButton(new_version);
        });
    }

    private void installUpdateButton(final Instant new_version)
    {
        start_update = new Button("Update");
        start_update.setOnAction(event -> promptForUpdate(start_update, new_version));
        Platform.runLater(()  ->  StatusBar.getInstance().addItem(start_update));
    }

    private void promptForUpdate(final Node node, final Instant new_version)
    {
        final File install_location = Locations.install();
        // Want to  update install_location, but that's locked on Windows,
        // and replacing the jars of a running application might be bad.
        // So download into a stage area.
        // The start script needs to be aware of this stage area
        // and move it to the install location on startup.
        final File stage_area = new File(install_location, "update");
        final StringBuilder buf = new StringBuilder();
        buf.append("You are running version  ")
           .append(TimestampFormats.DATETIME_FORMAT.format(Update.current_version))
           .append("\n")
           .append("The new version is dated ")
           .append(TimestampFormats.DATETIME_FORMAT.format(new_version))
           .append("\n\n")
           .append("The update will replace the installation in\n")
           .append(install_location)
           .append("\n(").append(stage_area).append(")")
           .append("\nwith the content of ")
           .append(Update.update_url)
           .append("\n\n")
           .append("Do you want to update?\n");

        final Alert prompt = new Alert(AlertType.INFORMATION,
                                       buf.toString(),
                                       ButtonType.OK, ButtonType.CANCEL);
        prompt.setTitle(NAME);
        prompt.setHeaderText("A new version of this software is available");
        prompt.setResizable(true);
        DialogHelper.positionDialog(prompt, node, -600, -350);
        prompt.getDialogPane().setPrefSize(600, 300);
        if (prompt.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK)
        {
            // Show job manager to display progress
            ApplicationService.findApplication(JobViewerApplication.NAME).create();
            // Perform update
            JobManager.schedule(NAME, monitor ->
            {
                Update.downloadAndUpdate(monitor, stage_area);
                Update.adjustCurrentVersion();
                if (! monitor.isCanceled())
                    Platform.runLater(() -> restart(node));
            });
        }
        else
        {
            // Remove the update button
            StatusBar.getInstance().removeItem(start_update);
            start_update = null;
        }
    }

    private void restart(final Node node)
    {
        final String message =
                "The application will now exit,\n" +
                "so you can then start the updated version\n";
        final Alert prompt = new Alert(AlertType.INFORMATION,
                                       message,
                                       ButtonType.OK);
        prompt.setTitle(NAME);
        prompt.setHeaderText("Update completed");
        DialogHelper.positionDialog(prompt, node, -400, -250);
        prompt.showAndWait();
        System.exit(0);
    }

    @Override
    public AppInstance create()
    {
        throw new IllegalStateException("Cannot create instance of " + NAME);
    }
}

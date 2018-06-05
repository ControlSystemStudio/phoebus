/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui;

import java.net.URI;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.TitleDetail;
import org.phoebus.framework.jobs.CommandExecutor;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.ui.application.ApplicationLauncherService;
import org.phoebus.ui.application.PhoebusApplication;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;

/** Action that opens an AlarmTreeItem's related display
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class OpenDisplayAction extends MenuItem
{
    /** @param node Node to position dialog
     *  @param item Alarm item
     *  @param display Info to show
     */
    public OpenDisplayAction(final Node node, final AlarmTreeItem<?> item, final TitleDetail display)
    {
        super(display.title, ImageCache.getImageView(AlarmSystem.class, "/icons/related_display.png"));
        setOnAction(event ->
        {
            // Open display as resource,
            // which includes http://server.site/path/to/display.bob
            // where the file type is handled by an application
            try
            {
                final URI resource = ResourceParser.createResourceURI(display.detail);
                final AppResourceDescriptor app = ApplicationLauncherService.findApplication(resource, false, null);
                if (app != null)
                {
                    app.create(resource);
                    return;
                }
            }
            catch (Exception ex)
            {
                ExceptionDetailsErrorDialog.openError(node, "Display Error", "Cannot open " + display.detail, ex);
                return;
            }

            // For web pages, fall back to web browser
            if (display.detail.startsWith("http:") ||
                display.detail.startsWith("https:"))
            {
                Platform.runLater(() -> PhoebusApplication.INSTANCE.getHostServices().showDocument(display.detail));
                return;
            }

            // Execute external command
            JobManager.schedule(display.title, monitor ->
            {
                final CommandExecutor executor = new CommandExecutor(display.detail, AlarmSystem.command_directory);
                executor.call();
            });
        });
    }
}

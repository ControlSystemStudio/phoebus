/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.persist.ModelLoader;
import org.csstudio.display.builder.representation.javafx.JFXRepresentation;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.jobs.JobManager;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/** PV Table Application
 *  @author Kay Kasemir
 */
public class DisplayRuntimeInstance implements AppInstance
{
    // TODO This is ~ RCP RuntimeViewPart
    private final AppDescriptor app;
    private final BorderPane layout = new BorderPane();
    private final DockItemWithInput dock_item;
    private final DockItemRepresentation representation;
    private Node toolbar;

    DisplayRuntimeInstance(final AppDescriptor app)
    {
        this.app = app;

        representation = new DockItemRepresentation(this);
        toolbar = createToolbar();
        BorderPane.setMargin(toolbar, new Insets(5, 5, 0, 5));
        layout.setTop(toolbar);
        layout.setCenter(representation.createModelRoot());
        dock_item = new DockItemWithInput(this, layout, null, null);
        DockPane.getActiveDockPane().addTab(dock_item);

        dock_item.addClosedNotification(this::stop);
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }

    private Node createToolbar()
    {
        final Separator sep = new Separator();
        HBox.setHgrow(sep, Priority.ALWAYS);
        return new HBox(5,
                        new Label("TODO: TOOLBAR"),
                        sep,
                        new Button("Zoom"),
                        new Button("Back"),
                        new Button("Fore")
                        );
    }

    public void raise()
    {
        dock_item.select();
    }

    public void loadDisplayFile(final DisplayInfo info)
    {
        // Set input ASAP so that other requests to open this
        // resource will find this instance and not start
        // another instance
        dock_item.setInput(info.toURL());

        // Load files in background job
        JobManager.schedule("Load Display", monitor ->
        {
            monitor.beginTask(info.toString());
            try
            {
                String parent_display = null;

                final DisplayModel model = ModelLoader.resolveAndLoadModel(parent_display , info.getPath());
                Platform.runLater(() -> represent(model));
            }
            catch (Exception ex)
            {
                showError("Error loading " + info, ex);
            }
        });
    }

    void represent(final DisplayModel model)
    {
        // TODO disposeModelAndRepresentation(old_model)
        final DisplayInfo info = DisplayInfo.forModel(model);

        final Parent parent = representation.getModelParent();
        JFXRepresentation.getChildren(parent).clear();
        try
        {
            representation.representModel(parent, model);
        }
        catch (Exception ex)
        {
            showError("Cannot represent model", ex);
        }
    }

    void trackCurrentModel(final DisplayModel model)
    {
        // TODO There's much more to tracking the current model,
        // see RuntimeViewPart#trackCurrentModel()
        dock_item.setLabel(model.getDisplayName());
    }

    /** Show error message and stack trace
     *
     *  <p>Replaces the 'Representation' in the center
     *  of the layout with an error message.
     *
     *  @param message Me
     *  @param error
     */
    private void showError(final String message, final Throwable error)
    {
        logger.log(Level.WARNING, message, error);

        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(buf);
        out.append(message).append("\n");
        error.printStackTrace(out);
        final String info = buf.toString();
        Platform.runLater(() ->
        {
            final TextArea text = new TextArea(info);
            text.setEditable(false);
            layout.setCenter(text);
        });
    }

    public void stop()
    {
        // TODO Stop runtime, dispose representation, release model
    }
}

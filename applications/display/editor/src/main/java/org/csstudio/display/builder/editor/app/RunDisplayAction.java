/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.editor.app;

import org.csstudio.display.builder.editor.DisplayEditor;
import org.csstudio.display.builder.editor.Messages;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.javafx.ImageCache;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

/** Action to save display and execute it
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RunDisplayAction extends Button
{
    private final DisplayEditorInstance editor;

    RunDisplayAction(DisplayEditorInstance editor)
    {
        this.editor = editor;
        setGraphic(ImageCache.getImageView(DisplayEditor.class, "/icons/run_tool.png"));
        setTooltip(new Tooltip(Messages.Run));
        setOnAction(event -> run());
    }

    private void run()
    {
        JobManager.schedule(Messages.Run, monitor ->
        {
            // Save if there's something to save
            if (editor.getEditorGUI().getDisplayEditor().getUndoableActionManager().canUndo())
                editor.doSave(monitor);

            // Locate runtime.
            // Could use DisplayRuntimeApplication.NAME,
            // but using string avoids direct dependency on runtime module
            final AppResourceDescriptor runtime = ApplicationService.findApplication("display_runtime");
            // Open in runtime, on UI thread
            Platform.runLater(() -> runtime.create(ResourceParser.getURI(editor.getEditorGUI().getFile())));
        });
    }
}

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
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.javafx.ImageCache;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** Action to save display and execute it
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ExecuteDisplayAction implements Runnable
{
    private static final Image icon = ImageCache.getImage(DisplayEditor.class, "/icons/run_tool.png");
    private final DisplayEditorInstance editor;

    public static Button asButton(final DisplayEditorInstance editor)
    {
        final Runnable action = new ExecuteDisplayAction(editor);
        final Button button = new Button();
        button.setGraphic(new ImageView(icon));
        button.setTooltip(new Tooltip(Messages.Run));
        button.setOnAction(event -> action.run());
        return button;
    }

    public static MenuItem asMenuItem(final DisplayEditorInstance editor)
    {
        final Runnable action = new ExecuteDisplayAction(editor);
        final MenuItem item = new MenuItem(Messages.Run, new ImageView(icon));
        item.setOnAction(event -> action.run());
        return item;
    }

    private ExecuteDisplayAction(final DisplayEditorInstance editor)
    {
        this.editor = editor;
    }

    @Override
    public void run()
    {
        JobManager.schedule(Messages.Run, monitor ->
        {
            // Save if there's something to save
            if (editor.getEditorGUI().getDisplayEditor().getUndoableActionManager().canUndo())
                editor.doSave(monitor);

            // Open in runtime, on UI thread
            // Could use DisplayRuntimeApplication.NAME,
            // but using string avoids direct dependency on runtime module
            Platform.runLater(() -> ApplicationService.createInstance("display_runtime", ResourceParser.getURI(editor.getEditorGUI().getFile())));
        });
    }
}

/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.editor.app;

import org.csstudio.display.builder.editor.Messages;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

/** Action to save display and execute it
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SaveDisplayButton extends Button
{
    public SaveDisplayButton(final DisplayEditorInstance editor)
    {
        setGraphic(ImageCache.getImageView(ImageCache.class, "/icons/save_edit.png"));
        setTooltip(new Tooltip(Messages.SaveDisplay_TT));
        setOnAction(event ->
        {
            JobManager.schedule(Messages.SaveDisplay, monitor ->
            {
                editor.doSave(monitor);
            });
        });
    }
}

/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor;

import java.io.File;

import org.csstudio.display.builder.editor.actions.ActionDescription;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.representation.javafx.FilenameSupport;
import org.phoebus.ui.dialog.OpenFileDialog;

import javafx.stage.Window;
import org.phoebus.util.FileExtensionUtil;

/** Prompt for file name to save model
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class LoadModelAction extends ActionDescription
{
    private final Window window = null;
    private final EditorGUI editor;

    /** @param save_handler Will be invoked with file name */
    public LoadModelAction(final EditorGUI editor)
    {
        super("icons/open.png", Messages.LoadDisplay_TT);
        this.editor = editor;
    }

    @Override
    public void run(final DisplayEditor ignored, final boolean selected)
    {
        File file = new OpenFileDialog().promptForFile(window, Messages.LoadDisplay,
                                                             editor.getFile(), FilenameSupport.file_extensions);
        if (file == null)
            return;

        file = FileExtensionUtil.enforceFileExtension(file, DisplayModel.FILE_EXTENSION);
        editor.loadModel(file);
    }
}

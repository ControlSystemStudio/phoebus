/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor;

import java.io.File;
import java.text.MessageFormat;

import org.csstudio.display.builder.editor.actions.ActionDescription;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.representation.javafx.FilenameSupport;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.dialog.SaveAsDialog;
import org.phoebus.util.FileExtensionUtil;

import javafx.stage.Window;

/** Prompt for file name to save model
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SaveModelAction extends ActionDescription
{
    private final Window window = null;
    private final EditorGUI editor;

    /** @param save_handler Will be invoked with file name */
    public SaveModelAction(final EditorGUI editor)
    {
        super("icons/save.png", Messages.SaveDisplay_TT);
        this.editor = editor;
    }

    @Override
    public void run(final DisplayEditor ignored, final boolean selected)
    {
        File file = new SaveAsDialog().promptForFile(window, Messages.SaveDisplay,
                                                     editor.getFile(), FilenameSupport.file_extensions);
        if (file == null)
            return;

        file = FileExtensionUtil.enforceFileExtension(file, DisplayModel.FILE_EXTENSION);
        try
        {
            editor.saveModelAs(file);
        }
        catch (Exception ex)
        {
            ExceptionDetailsErrorDialog.openError(editor.getParentNode(), Messages.SaveDisplay,
                                                  MessageFormat.format(Messages.SaveDisplayErrorFMT, file),
                                                  ex);
        }
    }
}

/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import java.io.File;
import java.util.function.BiFunction;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.FilenameWidgetProperty;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.representation.javafx.widgets.JFXBaseRepresentation;
import org.phoebus.ui.dialog.OpenFileDialog;

import javafx.scene.Node;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.Window;;

/** Helper for handling {@link FilenameWidgetProperty}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FilenameSupport
{
    public static final ExtensionFilter[] file_extensions = new ExtensionFilter[]
    {
        new ExtensionFilter(Messages.FileTypeAll, "*.*"),
        new ExtensionFilter(Messages.FileTypeDisplays, "*." + DisplayModel.FILE_EXTENSION)
    };

    /** Default file prompt that uses local file system */
    private static final BiFunction<Widget, String, String>  local_file_prompt = (widget, initial) ->
    {
        File file = null;
        try
        {
            final DisplayModel model = widget.getDisplayModel();
            file = new File(ModelResourceUtil.resolveResource(model, initial));
        }
        catch (Exception ex)
        {
            // Can't set initial file name, ignore.
        }
        final Window window = JFXBaseRepresentation.getJFXNode(widget).getScene().getWindow();
        final File selected = new OpenFileDialog().promptForFile(window, "Open File", file, file_extensions);
        if (selected == null)
            return null;
        return selected.getPath();
    };

    private static BiFunction<Widget, String, String> file_prompt = local_file_prompt;

    /** Install function that will be called to prompt for files
     *
     *  Default uses local file system.
     *  RCP plugin can install a workspace-based file dialog.
     *
     *  @param prompt Function that receives a Widget and original file name,
     *                prompts user for new file (via file dialog),
     *                and returns selected new file or <code>null</code>
     */
    public static void setFilePrompt(BiFunction<Widget, String, String> prompt)
    {
        file_prompt = prompt;
    }

    /** Prompt for file name
     *  @param widget Widget that needs a file name
     *  @param initial Initial value
     *  @return Selected file name or <code>null</code>
     */
    public static String promptForRelativePath(final Widget widget, final String initial) throws Exception
    {
        final String filename = file_prompt.apply(widget, initial);
        if (filename == null)
            return null;
        final DisplayModel model = widget.getDisplayModel();
        final String parent_file = model.getUserData(DisplayModel.USER_DATA_INPUT_FILE);
        return ModelResourceUtil.getRelativePath(parent_file, filename);
    }


    /** When running under RCP, the file dialog
     *  is an SWT dialog.
     *  Opening that SWT dialog brings the SWT main window (Eclipse app)
     *  to the front, hiding the JFX ActionsDialog.
     *  When then closing that file dialog, the ActionsDialog
     *  is still active (modal dialog), but hidden behind
     *  the RCP window, so application appears dead
     *  until user can find the hidden ActionsDialog.
     *
     *  This most awful terrible no good hack
     *  brings the ActionsDialog back to the front.
     */
    public static void performMostAwfulTerribleNoGoodHack(final Node node_in_dialog)
    {
        // Found no obvious API to locate the dialog's stage.
        // Debugger showed that the dialog's window is a stage.
        final Window window = node_in_dialog.getScene().getWindow();
        if (window instanceof Stage)
            ((Stage)window).toFront();
        else
            throw new IllegalStateException("Cannot bring ActionsDialog back to front");
    }
}

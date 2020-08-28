/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.csstudio.display.builder.editor.app;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Window;
import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.editor.util.CreateNewDisplayJob;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.representation.javafx.FilenameSupport;
import org.phoebus.framework.adapter.AdapterService;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.dialog.SaveAsDialog;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.ContextMenuEntry;
import org.phoebus.util.FileExtensionUtil;

import java.io.File;
import java.text.MessageFormat;
import java.util.Optional;

/**
 * Context menu entry to create a new display. Selection must be a writable folder.
 */
public class NewDisplayContextMenuEntry implements ContextMenuEntry {

    @Override
    public String getName()
    {
        return Messages.NewDisplay;
    }

    @Override
    public Class<?> getSupportedType()
    {
       return File.class;
    }

    @Override
    public Image getIcon()
    {
        return ImageCache.getImage(DisplayModel.class, "/icons/display.png");
    }

    @Override
    public void call(Selection selection){
        Optional<File> file =
                AdapterService.adapt(SelectionService.getInstance().getSelection().getSelections().get(0),
                        File.class);
        if(file.isEmpty()){
            ExceptionDetailsErrorDialog.openError(Messages.NewDisplayFailed, Messages.NewDisplaySelectionEmpty, null);
            return;
        }

        File targetFolder = file.get();
        if(!targetFolder.isDirectory()){
            targetFolder = targetFolder.getParentFile();
        }
        if(!targetFolder.canWrite()){
            ExceptionDetailsErrorDialog.openError(Messages.NewDisplayFailed, Messages.NewDisplayTargetFolderWriteProtected, null);
            return;
        }

        final Window window = DockPane.getActiveDockPane().getScene().getWindow();
        final File targetFile = new SaveAsDialog().promptForFile(window,
                Messages.NewDisplay,
                new File(targetFolder, "new_display"),
                FilenameSupport.file_extensions);

        if (targetFile == null) {
            return;
        }

        File newDisplayFile;

        // Check if file exists on the file system. This is true only if user selects to overwrite an existing file
        // when prompted by the native file chooser.
        if(targetFile.exists()){
            newDisplayFile = targetFile;
        }
        else{
            newDisplayFile = FileExtensionUtil.enforceFileExtension(targetFile, DisplayModel.FILE_EXTENSION);
            // Check if the file exists on the file system when .bob extension has been enforced.
            // If it does, prompt user to cancel or overwrite.
            if(newDisplayFile.exists()){
                final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle(Messages.NewDisplayOverwriteExistingTitle);
                alert.setHeaderText(MessageFormat.format(Messages.NewDisplayOverwriteExisting, newDisplayFile.getName(), newDisplayFile.getParentFile().getName()));
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get().equals(ButtonType.CANCEL)) {
                    // User selects Cancel, or dismisses prompt
                    return;
                }
            }
        }

        JobManager.schedule(Messages.NewDisplay, new CreateNewDisplayJob(newDisplayFile));
    }
}

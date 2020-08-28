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

package org.csstudio.trends.databrowser3;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.persistence.XMLPersistence;
import org.phoebus.framework.adapter.AdapterService;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.dialog.SaveAsDialog;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.spi.ContextMenuEntry;
import org.phoebus.util.FileExtensionUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.MessageFormat;
import java.util.Optional;

/**
 * Context menu entry for creating a new plot.
 */
public class NewPlotContextMenuEntry implements ContextMenuEntry {

    @Override
    public String getName()
    {
        return Messages.NewPlot;
    }

    @Override
    public Class<?> getSupportedType()
    {
        return File.class;
    }

    @Override
    public Image getIcon()
    {
        return Activator.getImage("databrowser");
    }

    @Override
    public void call(Selection selection){
        Optional<File> file =
                AdapterService.adapt(SelectionService.getInstance().getSelection().getSelections().get(0),
                        File.class);
        if(file.isEmpty()){
            ExceptionDetailsErrorDialog.openError(Messages.NewPlotFailed, Messages.NewPlotSelectionEmpty, null);
            return;
        }

        File targetFolder = file.get();
        if(!targetFolder.isDirectory()){
            targetFolder = targetFolder.getParentFile();
        }
        if(!targetFolder.canWrite()){
            ExceptionDetailsErrorDialog.openError(Messages.NewPlotFailed, Messages.NewPlotTargetFolderWriteProtected, null);
            return;
        }

        ExtensionFilter[] fileExtensions = new ExtensionFilter[]
        {
                new ExtensionFilter(Messages.FileTypeAll, "*.*"),
                new ExtensionFilter(Messages.FileTypePlot, "*.plt")
        };
        final Window window = DockPane.getActiveDockPane().getScene().getWindow();
        final File targetFile = new SaveAsDialog().promptForFile(window,
                Messages.NewPlot,
                new File(targetFolder, "new_plot"),
                fileExtensions);
        if (targetFile == null) {
            return;
        }

        File newPlotFile;

        // Check if file exists on the file system. This is true only if user selects to overwrite an existing file
        // when prompted by the native file chooser.
        if(targetFile.exists()){
            newPlotFile = targetFile;
        }
        else{
            newPlotFile = FileExtensionUtil.enforceFileExtension(targetFile, "plt");
            // Check if the file exists on the file system when .plt extension has been enforced.
            // If it does, prompt user to cancel or overwrite.
            if(newPlotFile.exists()){
                final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle(Messages.NewPlotOverwriteExistingTitle);
                alert.setHeaderText(MessageFormat.format(Messages.NewPlotOverwriteExisting, newPlotFile.getName(), newPlotFile.getParentFile().getName()));
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get().equals(ButtonType.CANCEL)) {
                    // User selects Cancel, or dismisses prompt
                    return;
                }
            }
        }

        try{
            Model model = new Model();
            try(final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(newPlotFile)))
            {
                XMLPersistence.write(model, out);
            }
        }
        catch(Exception e){
            ExceptionDetailsErrorDialog.openError(Messages.NewPlotFailed, Messages.NewPlotFileCreateFailed, e);
            return;
        }

        Platform.runLater(() ->
                ApplicationService.createInstance(DataBrowserApp.NAME, newPlotFile.toURI()));
    }
}

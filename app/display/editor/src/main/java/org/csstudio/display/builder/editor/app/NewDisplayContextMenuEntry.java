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
import javafx.scene.image.Image;
import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.editor.util.CreateNewDisplayJob;
import org.csstudio.display.builder.model.DisplayModel;
import org.phoebus.framework.adapter.AdapterService;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.ContextMenuEntry;

import java.io.File;
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
        Optional<File> folder =
                AdapterService.adapt(SelectionService.getInstance().getSelection().getSelections().get(0),
                        File.class);
        if(folder.isEmpty()){
            ExceptionDetailsErrorDialog.openError(Messages.NewDisplayFailed, Messages.NewDisplaySelectionEmpty, null);
            return;
        }
        else if(!folder.get().isDirectory()){
            ExceptionDetailsErrorDialog.openError(Messages.NewDisplayFailed, Messages.NewDisplayTargetFileNotDirectory, null);
            return;
        }
        else if(!folder.get().canWrite()){
            ExceptionDetailsErrorDialog.openError(Messages.NewDisplayFailed, Messages.NewDisplayTargetFolderWriteProtected, null);
            return;
        }

        final File file = DisplayEditorApplication.promptForFilename(Messages.NewDisplay);
        if (file == null) {
            return;
        }

        JobManager.schedule(Messages.NewDisplay, new CreateNewDisplayJob(file));
    }
}

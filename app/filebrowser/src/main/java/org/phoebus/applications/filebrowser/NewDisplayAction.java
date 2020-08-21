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

package org.phoebus.applications.filebrowser;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;
import org.csstudio.display.builder.editor.util.CreateNewDisplayJob;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.dialog.DialogHelper;

import java.io.File;
import java.text.MessageFormat;

public class NewDisplayAction extends MenuItem {

    /** @param node Node used to position confirmation dialog
     *  @param item Item under which to create a new folder
     */
    public NewDisplayAction(final Node node, final TreeItem<FileInfo> item) {
        super(Messages.NewDisplay, new ImageView(FileTreeCell.newDisplayIcon));

        setOnAction(event ->
        {
            final TextInputDialog prompt = new TextInputDialog();
            prompt.setTitle(getText());
            prompt.setHeaderText(Messages.NewDisplayNamePrompt);
            DialogHelper.positionDialog(prompt, node, 0, 0);
            String newDisplayName = prompt.showAndWait().orElse(null);
            if (newDisplayName == null) {
                return;
            }

            if(!newDisplayName.trim().toLowerCase().endsWith(".bob")){
                newDisplayName += ".bob";
            }

            // Abort if file already exists
            final File newFile = new File(item.getValue().file, newDisplayName);
            if (newFile.exists())
            {
                final Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle(getText());
                alert.setHeaderText(MessageFormat.format(Messages.NewDisplayAlert, newDisplayName, item.getValue().file.getName()));
                DialogHelper.positionDialog(alert, node, 0, 0);
                alert.showAndWait();
                return;
            }

            JobManager.schedule(Messages.NewDisplay, new CreateNewDisplayJob(newFile));
        });
    }
}

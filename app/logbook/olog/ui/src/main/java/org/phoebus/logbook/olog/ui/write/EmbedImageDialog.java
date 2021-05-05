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

package org.phoebus.logbook.olog.ui.write;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import org.phoebus.framework.nls.NLS;
import org.phoebus.logbook.olog.ui.Messages;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple dialog used to select an image from file or from clipboard, and create
 * a suitable Commonmark string to embed into log entry description.
 */
public class EmbedImageDialog extends Dialog<EmbedImageDescriptor> {

    public EmbedImageDialog(){
        super();
        ResourceBundle resourceBundle =  NLS.getMessages(Messages.class);
        FXMLLoader loader =
                new FXMLLoader(getClass().getResource("EmbedImageDialog.fxml"), resourceBundle);
        try {
            DialogPane dialogPane = loader.load();
            EmbedImageDialogController controller = loader.getController();
            setTitle(Messages.EmbedImageDialogTitle);
            setDialogPane(dialogPane);
            setResultConverter(buttonType -> {
                if(buttonType.getButtonData() == ButtonData.OK_DONE){
                    return controller.getEmbedImageDescriptor();
                }
                else{
                    return null;
                }
            });
        } catch (IOException e) {
            Logger.getLogger(EmbedImageDialog.class.getName())
                    .log(Level.SEVERE, "Unable to launch dialog to embedded image.", e);
        }
    }
}

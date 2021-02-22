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

package org.csstudio.display.builder.representation.javafx.actionsdialog;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.properties.OpenDisplayActionInfo;
import org.csstudio.display.builder.representation.javafx.Messages;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.ui.properties.StatisticsTabController;
import org.phoebus.framework.nls.NLS;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper class for the {@link ActionsDialog} action list view items. It contains
 * fields describing the action, and the UI components used to edit the action information.
 */
public class ActionsDialogActionItem {

    private ActionInfo actionInfo;
    private Node actionInfoEditor;

    public ActionsDialogActionItem(ActionInfo actionInfo){
        this.actionInfo = actionInfo;

        try {
            switch(actionInfo.getType()){
                case OPEN_DISPLAY:
                    ResourceBundle resourceBundle =  NLS.getMessages(Messages.class);
                    FXMLLoader fxmlLoader = new FXMLLoader();
                    fxmlLoader.setLocation(this.getClass().getResource("OpenDisplayActionDetails.fxml"));
                    fxmlLoader.setResources(resourceBundle);
                    fxmlLoader.setControllerFactory(clazz -> {
                        try {
                            OpenDisplayActionDetailsController controller =
                                    (OpenDisplayActionDetailsController) clazz.getConstructor(OpenDisplayActionInfo.class)
                                            .newInstance(actionInfo);
                            return controller;

                        } catch (Exception e) {
                            Logger.getLogger(StatisticsTabController.class.getName()).log(Level.SEVERE, "Failed to construct controller statistics tab", e);
                        }
                        return null;
                    });
                    this.actionInfoEditor = fxmlLoader.load();
                    this.actionInfoEditor.setVisible(false);
            }
        } catch (Exception e) {
            Logger.getLogger(ActionsDialogActionItem.class.getName())
                    .log(Level.WARNING, String.format("Unable to create editor for action type \"%s\"", actionInfo.getType()), e);
        }
    }

    public Node getActionInfoEditor(){
        return actionInfoEditor;
    }

    public ActionInfo getActionInfo(){
        return actionInfo;
    }
}

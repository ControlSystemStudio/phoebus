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

import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.properties.ActionInfo.ActionType;
import org.csstudio.display.builder.representation.javafx.Messages;
import org.phoebus.framework.nls.NLS;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

/**
 * Wrapper class for the {@link ActionsDialog} action list view items. It contains
 * fields describing the action, and the UI components used to edit the action information.
 */
public class ActionsDialogActionItem {

    private Node actionInfoEditor;
    private ActionDetailsController controller;
    private String description;
    private ActionType actionType;

    /**
     * Constructor.
     *
     * Note that the {@link ActionInfo} object is not maintained as a field in the class as its fields are
     * read-only.
     * @param widget Widget
     * @param actionInfo ActionInfo
     */
    public ActionsDialogActionItem(Widget widget, ActionInfo actionInfo){
        this.description = actionInfo.getDescription();
        this.actionType = actionInfo.getType();
        ResourceBundle resourceBundle =  NLS.getMessages(Messages.class);
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setResources(resourceBundle);
        try {
            switch(actionInfo.getType()){
                case OPEN_DISPLAY:
                    fxmlLoader.setLocation(this.getClass().getResource("OpenDisplayActionDetails.fxml"));
                    fxmlLoader.setControllerFactory(clazz -> {
                        try {
                           return clazz.getConstructor(Widget.class, ActionInfo.class).newInstance(widget, actionInfo);
                        } catch (Exception e) {
                            Logger.getLogger(ActionsDialogActionItem.class.getName()).log(Level.SEVERE, "Failed to construct OpenDisplayActionDetailsController", e);
                        }
                        return null;
                    });
                    break;
                case WRITE_PV:
                    fxmlLoader.setLocation(this.getClass().getResource("WritePVActionDetails.fxml"));
                    fxmlLoader.setControllerFactory(clazz -> {
                        try {
                           return clazz.getConstructor(ActionInfo.class).newInstance(actionInfo);
                        } catch (Exception e) {
                            Logger.getLogger(ActionsDialogActionItem.class.getName()).log(Level.SEVERE, "Failed to construct WritePVActionDetailsController", e);
                        }
                        return null;
                    });
                    break;
                case EXECUTE_SCRIPT:
                    fxmlLoader.setLocation(this.getClass().getResource("ExecuteScriptActionDetails.fxml"));
                    fxmlLoader.setControllerFactory(clazz -> {
                        try {
                            return clazz.getConstructor(Widget.class, ActionInfo.class).newInstance(widget, actionInfo);
                        } catch (Exception e) {
                            Logger.getLogger(ActionsDialogActionItem.class.getName()).log(Level.SEVERE, "Failed to construct ExecuteScriptDetailsController", e);
                        }
                        return null;
                    });
                    break;
                case EXECUTE_COMMAND:
                    fxmlLoader.setLocation(this.getClass().getResource("ExecuteCommandActionDetails.fxml"));
                    fxmlLoader.setControllerFactory(clazz -> {
                        try {
                            return clazz.getConstructor(Widget.class, ActionInfo.class).newInstance(widget, actionInfo);
                        } catch (Exception e) {
                            Logger.getLogger(ActionsDialogActionItem.class.getName()).log(Level.SEVERE, "Failed to construct ExecuteCommandDetailsController", e);
                        }
                        return null;
                    });
                    break;
                case OPEN_FILE:
                    fxmlLoader.setLocation(this.getClass().getResource("OpenFileActionDetails.fxml"));
                    fxmlLoader.setControllerFactory(clazz -> {
                        try {
                           return clazz.getConstructor(Widget.class, ActionInfo.class).newInstance(widget, actionInfo);
                        } catch (Exception e) {
                            Logger.getLogger(ActionsDialogActionItem.class.getName()).log(Level.SEVERE, "Failed to construct OpenFileDetailsController", e);
                        }
                        return null;
                    });
                    break;
                case OPEN_WEBPAGE:
                    fxmlLoader.setLocation(this.getClass().getResource("OpenWebPageActionDetails.fxml"));
                    fxmlLoader.setControllerFactory(clazz -> {
                        try {
                           return clazz.getConstructor(ActionInfo.class).newInstance(actionInfo);
                        } catch (Exception e) {
                            Logger.getLogger(ActionsDialogActionItem.class.getName()).log(Level.SEVERE, "Failed to construct OpenWebPageDetailsController", e);
                        }
                        return null;
                    });
                    break;
            }
            this.actionInfoEditor = fxmlLoader.load();
            this.controller = fxmlLoader.getController();
            this.actionInfoEditor.setVisible(false);
        } catch (Exception e) {
            Logger.getLogger(ActionsDialogActionItem.class.getName())
                    .log(Level.WARNING, String.format("Unable to create editor for action type \"%s\"", actionInfo.getType()), e);
        }
    }

    /** @return Node for action info */
    public Node getActionInfoEditor(){
        return actionInfoEditor;
    }

    /** @return Description */
    public String getDescription(){
        return description;
    }

    /** @return ActionType */
    public ActionType getActionType(){
        return actionType;
    }

    /**
     * @return The {@link ActionInfo} object maintained by the controller for the editor component.
     */
    public ActionInfo getActionInfo(){
        return controller.getActionInfo();
    }
}

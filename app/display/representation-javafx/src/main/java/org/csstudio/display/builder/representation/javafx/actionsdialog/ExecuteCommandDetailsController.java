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

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.util.logging.Level;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.properties.ExecuteCommandActionInfo;
import org.csstudio.display.builder.representation.javafx.FilenameSupport;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

/** FXML Controller */
public class ExecuteCommandDetailsController implements ActionDetailsController{

    private ExecuteCommandActionInfo executeCommandActionInfo;

    private Widget widget;

    @FXML
    private TextField description;
    @FXML
    private TextField command;

    private StringProperty descriptionProperty = new SimpleStringProperty();
    private StringProperty commandProperty = new SimpleStringProperty();

    /** @param widget Widget
     *  @param actionInfo ActionInfo
     */
    public ExecuteCommandDetailsController(Widget widget, ActionInfo actionInfo){
        this.widget = widget;
        this.executeCommandActionInfo = (ExecuteCommandActionInfo)actionInfo;
    }

    /** Init */
    @FXML
    public void initialize(){
        descriptionProperty.setValue(executeCommandActionInfo.getDescription());
        commandProperty.setValue(executeCommandActionInfo.getCommand());

        description.textProperty().bindBidirectional(descriptionProperty);
        command.textProperty().bindBidirectional(commandProperty);
    }

    /** Prompt for command to execute */
    @FXML
    public void selectCommand(){
        try
        {
            final String path = FilenameSupport.promptForRelativePath(widget, commandProperty.get());
            if (path != null){
                commandProperty.setValue(path);
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot prompt for command/filename", ex);
        }
    }

    /** @return ActionInfo */
    @Override
    public ActionInfo getActionInfo(){
        return new ExecuteCommandActionInfo(descriptionProperty.get(), commandProperty.get());
    }
}

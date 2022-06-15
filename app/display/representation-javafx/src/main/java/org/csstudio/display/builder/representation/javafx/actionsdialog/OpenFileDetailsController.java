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
import org.csstudio.display.builder.model.properties.OpenFileActionInfo;
import org.csstudio.display.builder.representation.javafx.FilenameSupport;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

/** FXML Controller */
public class OpenFileDetailsController implements ActionDetailsController{

    private OpenFileActionInfo openFileActionInfo;
    private Widget widget;

    @FXML
    private TextField description;
    @FXML
    private TextField filePath;

    private StringProperty descriptionProperty = new SimpleStringProperty();
    private StringProperty filePathProperty = new SimpleStringProperty();

    /** @param widget Widget
     *  @param actionInfo ActionInfo
     */
    public OpenFileDetailsController(Widget widget, ActionInfo actionInfo){
        this.widget = widget;
        this.openFileActionInfo = (OpenFileActionInfo)actionInfo;
    }

    /** Init */
    @FXML
    public void initialize(){
        descriptionProperty.setValue(openFileActionInfo.getDescription());
        filePathProperty.setValue(openFileActionInfo.getFile());

        description.textProperty().bindBidirectional(descriptionProperty);
        filePath.textProperty().bindBidirectional(filePathProperty);
    }

    /** Prompt for file */
    @FXML
    public void selectFile(){
        try
        {
            final String path = FilenameSupport.promptForRelativePath(widget, filePathProperty.get());
            if (path != null){
                filePathProperty.setValue(path);
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot prompt for filename", ex);
        }
    }

    /** @return ActionInfo */
    @Override
    public ActionInfo getActionInfo(){
        return new OpenFileActionInfo(descriptionProperty.get(), filePathProperty.get());
    }
}

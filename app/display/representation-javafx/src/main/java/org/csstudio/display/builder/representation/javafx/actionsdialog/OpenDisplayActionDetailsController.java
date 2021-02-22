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

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import org.csstudio.display.builder.model.properties.OpenDisplayActionInfo;
import org.csstudio.display.builder.model.properties.OpenDisplayActionInfo.Target;

public class OpenDisplayActionDetailsController {

    @FXML
    private RadioButton replaceRadioButton;
    @FXML
    private RadioButton newTabRadioButton;
    @FXML
    private RadioButton newWindowRadioButton;
    @FXML
    private TextField description;
    @FXML
    private TextField displayPath;
    @FXML
    private TextField pane;

    private OpenDisplayActionInfo openDisplayActionInfo;

    private StringProperty paneProperty = new SimpleStringProperty();

    public OpenDisplayActionDetailsController(OpenDisplayActionInfo openDisplayActionInfo){
        this.openDisplayActionInfo = openDisplayActionInfo;
    }

    @FXML
    public void initialize(){
        replaceRadioButton.setText(Target.REPLACE.toString());
        newTabRadioButton.setText(Target.TAB.toString());
        newWindowRadioButton.setText(Target.WINDOW.toString());

        description.textProperty().setValue(openDisplayActionInfo.getDescription());
        displayPath.textProperty().setValue(openDisplayActionInfo.getFile());

        pane.textProperty().bind(paneProperty);
        paneProperty.setValue(openDisplayActionInfo.getPane());

    }
}

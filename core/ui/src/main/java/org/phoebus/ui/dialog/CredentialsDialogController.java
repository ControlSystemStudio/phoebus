/*
 * Copyright (C) 2024 European Spallation Source ERIC.
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
package org.phoebus.ui.dialog;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller for the {@link CredentialsDialog} body layout.
 */
public class CredentialsDialogController {
    @FXML
    private TextField username;
    @FXML
    private PasswordField password;
    @FXML
    private Label errorLabel;

    private SimpleStringProperty usernameStringProperty = new SimpleStringProperty();
    private SimpleStringProperty passwordStringProperty = new SimpleStringProperty();
    private SimpleStringProperty errorStringProperty = new SimpleStringProperty();

    @FXML
    public void initialize(){
        username.textProperty().bindBidirectional(usernameStringProperty);
        username.textProperty().addListener((ob, ol, ne) ->{
            errorStringProperty.set(null);
        });
        password.textProperty().bindBidirectional(passwordStringProperty);
        password.textProperty().addListener((ob, ol, ne) ->{
            errorStringProperty.set(null);
        });
        errorLabel.textProperty().bindBidirectional(errorStringProperty);

        errorLabel.visibleProperty().bind(Bindings.createBooleanBinding(() ->
                errorStringProperty.get() != null && !errorStringProperty.get().isBlank(), errorStringProperty));
    }

    /**
     * Convenience: focus on first input field
     */
    public void setFocus(){
        username.requestFocus();
    }

    public SimpleStringProperty getUsernameStringProperty(){
        return usernameStringProperty;
    }

    public SimpleStringProperty getPasswordStringProperty(){
        return passwordStringProperty;
    }

    /**
     * Sets the error string, which makes label visible.
     * @param error Error text
     */
    public void setErrorStringProperty(String error){
        errorStringProperty.set(error);
    }
}

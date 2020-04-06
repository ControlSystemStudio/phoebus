/*
 * Copyright (C) 2019 European Spallation Source ERIC.
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
 *
 */

package org.phoebus.logbook.ui.write;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.phoebus.logbook.ui.LogbookUiPreferences;
import org.phoebus.logbook.ui.Messages;
import org.phoebus.util.time.TimestampFormats;

import java.net.URL;
import java.time.Instant;
import java.util.Arrays;
import java.util.ResourceBundle;

public class FieldsViewController implements Initializable{

    // Credentials of user making entry.
    @FXML
    private Label userFieldLabel;
    @FXML
    private Label passwordFieldLabel;
    @FXML
    private TextField userField;
    @FXML
    private PasswordField passwordField;

    // Date and priority level of log entry.
    @FXML
    private Label dateLabel;
    @FXML
    private Label levelLabel;
    @FXML
    private TextField dateField;
    @FXML
    private ComboBox<String> levelSelector;

    // Title and body of log entry
    @FXML
    private Label titleLabel;
    @FXML
    private Label textLabel;
    @FXML
    private TextField titleField;
    @FXML
    private VBox logbooks;
    @FXML
    private TextArea textArea;

    private LogEntryModel model;

    public FieldsViewController(LogEntryModel logEntryModel){
        this.model = logEntryModel;
    }

    @FXML
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle){
        localize();

        Instant now = Instant.now();
        dateField.setText(TimestampFormats.DATE_FORMAT.format(now));
        model.setDate(now);

        logbooks.getChildren().add(new LogbooksTagsView(model));

        levelSelector.setItems(model.getLevels());

        model.fetchLevels();
        model.addLevelListener((ListChangeListener.Change<? extends String> c) ->
        {
            if (c.next())
            {
                if(!model.getLevels().isEmpty() && model.getLevels().get(0) != null)
                    levelSelector.getSelectionModel().select(model.getLevels().get(0));
                else
                    levelSelector.getSelectionModel().select(Messages.Normal);
            }
        });

        userField.textProperty().addListener((changeListener, oldVal, newVal) ->
        {
            if (newVal.trim().isEmpty())
                userFieldLabel.setTextFill(Color.RED);
            else
                userFieldLabel.setTextFill(Color.BLACK);
        });

        passwordField.textProperty().addListener((changeListener, oldVal, newVal) ->
        {
            if (newVal.trim().isEmpty())
                passwordFieldLabel.setTextFill(Color.RED);
            else
                passwordFieldLabel.setTextFill(Color.BLACK);
        });

        titleField.textProperty().addListener((changeListener, oldVal, newVal) ->
        {
            if (newVal.trim().isEmpty())
                titleLabel.setTextFill(Color.RED);
            else
                titleLabel.setTextFill(Color.BLACK);
        });

        model.getUpdateCredentialsProperty().addListener((changeListener, oldVal, newVal) ->
        {
            // This call back should be running on a background thread. Perform contents on JavaFX application thread.
            Platform.runLater(() ->
            {
                userField.setText(model.getUsername());
                passwordField.setText(model.getPassword());

                // Put focus on first required field that is empty.
                if (userField.getText().isEmpty())
                    userField.requestFocus();
                else if (passwordField.getText().isEmpty())
                    passwordField.requestFocus();
                else
                    titleField.requestFocus();
            });
        });

        userField.requestFocus();
        if (LogbookUiPreferences.save_credentials)
        {
            model.fetchStoredUserCredentials();
        }

        titleField.textProperty().setValue(model.getTitle());
        textArea.textProperty().setValue(model.getText());

        setFieldActions();
        setTextActions();
    }

    private void localize(){
        userFieldLabel.setText(Messages.Username);
        passwordFieldLabel.setText(Messages.Password);
        dateLabel.setText(Messages.Date);
        dateField.setTooltip(new Tooltip(Messages.CurrentDate));
        levelLabel.setText(Messages.Level);
        titleLabel.setText(Messages.Title);
    }

    @FXML
    public void setLevel(){
        model.setLevel(levelSelector.getSelectionModel().getSelectedItem());
    }

    /** Set the username and password fields to update the model's username and password fields on text entry. */
    private void setFieldActions()
    {
        userField.setOnKeyReleased(event ->
        {
            model.setUser(userField.getText());
        });

        passwordField.setOnKeyReleased(event ->
        {
            model.setPassword(passwordField.getText());
        });
    }

    /** Set the title and text fields to update the model's title and text fields on text entry */
    private void setTextActions()
    {
        titleField.setOnKeyReleased(event ->
        {
            model.setTitle(titleField.getText());
        });

        textArea.setOnKeyReleased(event ->
        {
            model.setText(textArea.getText());
        });
    }
}

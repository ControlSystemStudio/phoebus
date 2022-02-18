/**
 * Copyright (C) 2020 Facility for Rare Isotope Beams
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact Information: Facility for Rare Isotope Beam,
 *                      Michigan State University,
 *                      East Lansing, MI 48824-1321
 *                      http://frib.msu.edu
 */
package org.phoebus.applications.saveandrestore.ui.snapshot;

import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Tag;

import java.util.List;

/**
 * {@link SnapshotNewTagDialog} provides a {@link Dialog} having rich information header
 * and more fields for user input. Also, checks if duplicate tag name exists in the snapshot.
 *
 * @author <a href="mailto:changj@frib.msu.edu">Genie Jhang</a>
 */

public class SnapshotNewTagDialog extends Dialog<Pair<String, String>> {
    public SnapshotNewTagDialog(List<Tag> tagList) {
        super();

        setTitle(Messages.createNewTagDialogTitle);

        ButtonType saveTagButton = new ButtonType(Messages.saveTagButtonLabel, ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, saveTagButton);

        GridPane gridPane = new GridPane();
        gridPane.setHgap(5);
        gridPane.setVgap(5);
        gridPane.setPadding(new Insets(5, 5, 5, 5));

        Label tagNameLabel = new Label(Messages.tagNameLabel);
        Label tagCommentLabel = new Label(Messages.tagCommentLabel);

        GridPane.setHalignment(tagNameLabel, HPos.RIGHT);
        GridPane.setHalignment(tagCommentLabel, HPos.RIGHT);

        TextField tagNameTextField = new TextField();
        TextField tagCommentTextField = new TextField();

        tagNameTextField.setMinWidth(400);
        tagCommentTextField.setMinWidth(400);

        gridPane.add(tagNameLabel, 0, 0);
        gridPane.add(tagCommentLabel, 0, 1);
        gridPane.add(tagNameTextField, 1, 0);
        gridPane.add(tagCommentTextField, 1, 1);

        getDialogPane().setContent(gridPane);

        Platform.runLater(() -> {
            tagNameTextField.requestFocus();
            getDialogPane().lookupButton(saveTagButton).setDisable(true);
        });

        tagNameTextField.textProperty().addListener((observableValue, oldString, newString) -> {
            if (!newString.isEmpty()) {
                tagList.stream()
                        .filter(tag -> tag.getName().equals(newString))
                        .findFirst()
                        .ifPresentOrElse(tag ->
                                getDialogPane().lookupButton(saveTagButton).setDisable(true), () -> getDialogPane().lookupButton(saveTagButton).setDisable(false));
            } else {
                Platform.runLater(() -> getDialogPane().lookupButton(saveTagButton).setDisable(true));
            }
        });

        setResultConverter(dialogButton -> {
            if (dialogButton == saveTagButton) {
                return new Pair<>(tagNameTextField.getText(), tagCommentTextField.getText());
            }

            return null;
        });
    }
}

/**
 * Copyright (C) 2020 Facility for Rare Isotope Beams
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * <p>
 * Contact Information: Facility for Rare Isotope Beam,
 * Michigan State University,
 * East Lansing, MI 48824-1321
 * http://frib.msu.edu
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
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;
import org.phoebus.applications.saveandrestore.DirectoryUtilities;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.ui.snapshot.tag.TagUtil;
import org.phoebus.ui.autocomplete.AutocompleteMenu;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link SnapshotNewTagDialog} provides a {@link Dialog} having rich information header
 * and more fields for user input. Also, checks if duplicate tag name exists in the snapshot.
 *
 * @author <a href="mailto:changj@frib.msu.edu">Genie Jhang</a>
 */

public class SnapshotNewTagDialog extends Dialog<Pair<String, String>> {

    private final TextInputControl tagNameTextField;

    public SnapshotNewTagDialog(List<Node> nodes) {
        super();

        // Construct a list of all (unique) tags found in the node list.
        List<Tag> tagList = new ArrayList<>();
        nodes.forEach(node -> {
            if(node.getTags() != null){
                node.getTags().forEach(tag -> {
                    if(!tagList.contains(tag)){
                        tagList.add(tag);
                    }
                });
            }
        });

        setHeaderText(Messages.createNewTagDialogHeader);
        setTitle(Messages.createNewTagDialogTitle);

        ButtonType saveTagButton = new ButtonType(Messages.saveTagButtonLabel, ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, saveTagButton);

        GridPane gridPane = new GridPane();
        gridPane.setHgap(5);
        gridPane.setVgap(5);
        gridPane.setPadding(new Insets(12, 8, 3, 8));

        Label tagNameLabel = new Label(Messages.tagNameLabel);
        Label tagCommentLabel = new Label(Messages.tagCommentLabel);

        GridPane.setHalignment(tagNameLabel, HPos.RIGHT);
        GridPane.setHalignment(tagCommentLabel, HPos.RIGHT);

        tagNameTextField = new TextField();
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
            javafx.scene.Node saveButton = getDialogPane().lookupButton(saveTagButton);
            if (!newString.isEmpty()) {
                Platform.runLater(() ->
                        saveButton.setDisable(!isTagSelectable(nodes, newString)));
            } else {
                Platform.runLater(() -> saveButton.setDisable(true));
            }
        });

        setResultConverter(dialogButton -> {
            if (dialogButton == saveTagButton) {

                return new Pair<>(tagNameTextField.getText(), tagCommentTextField.getText());
            }

            return null;
        });
    }

    /**
     * Adds {@link AutocompleteMenu} to the tag name field such that user may choose among existing tag names.
     *
     * @param autocompleteMenu The {@link AutocompleteMenu} that will be attached to the tag name input field.
     */
    public void configureAutocompleteMenu(AutocompleteMenu autocompleteMenu) {
        autocompleteMenu.attachField(tagNameTextField);
    }

    /**
     * Determines if a {@link Tag} name can be used to tag a list of {@link Node}s.
     * <ul>
     *     <li>A {@link Node} of type {@link NodeType#COMPOSITE_SNAPSHOT} cannot be tagged as golden.</li>
     *     <li>A tag name must not exist on any of the {@link Node}s.</li>
     * </ul>
     * Uniqueness of {@link Tag} is determined by its case-sensitive name.
     * @param nodes A list of {@link Node}s to check.
     * @param tagName A {@link Tag} name
     * @return <code>true</code> if the tag name can be used on all {@link Node}s in the list.
     */
    private boolean isTagSelectable(List<Node> nodes, String tagName){
        for(Node node : nodes){
            if(tagName.equalsIgnoreCase(Tag.GOLDEN)){
                return false;
            }
            if(node.getTags() != null){
                for(Tag tag : node.getTags()){
                    if(tag.getName().equals(tagName)){
                        return false;
                    }
                }
            }
        }
        return true;
    }
}

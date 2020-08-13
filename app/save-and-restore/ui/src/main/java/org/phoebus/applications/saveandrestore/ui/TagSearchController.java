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
package org.phoebus.applications.saveandrestore.ui;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.util.Pair;
import org.phoebus.applications.saveandrestore.DirectoryUtilities;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreService;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * {@link TagSearchController} class provides the controller for TagSearchWindow.fxml
 *
 * @author <a href="mailto:changj@frib.msu.edu">Genie Jhang</a>
 */

public class TagSearchController implements Initializable {

    private BaseSaveAndRestoreController callerController;
    private List<Tag> tagList;

    @FXML
    private TextField keywordTextField;

    @FXML
    private TableView<Tag> resultTableView;

    @FXML
    private TableColumn<Tag, String> tagNameColumn;

    @FXML
    private TableColumn<Tag, String> commentColumn;

    @FXML
    private TableColumn<Tag, String> createdColumn;

    @FXML
    private TableColumn<Tag, String> creatorColumn;

    @Autowired
    private SaveAndRestoreService saveAndRestoreService;

    private void refreshTagList() {
        try {
            tagList = saveAndRestoreService.getAllTags();
        } catch (Exception e) {
            tagList = new ArrayList<>();
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        refreshTagList();
        filterList(null);

        keywordTextField.focusedProperty().addListener((observableValue, aBoolean, isFocused) -> {
            refreshTagList();
            filterList(keywordTextField.getText());
        });

        keywordTextField.textProperty().addListener((observableValue, oldKeyword, newKeyword) -> filterList(newKeyword));

        resultTableView.setRowFactory(tableView -> new TableRow<>() {
            @Override
            protected void updateItem(Tag tag, boolean empty) {
                super.updateItem(tag, empty);
                if (tag == null || empty) {
                    setTooltip(null);
                    setOnMouseClicked(null);
                } else {
                    Node node = saveAndRestoreService.getNode(tag.getSnapshotId());
                    Pair<String, Stack<Node>> pair = DirectoryUtilities.CreateLocationStringAndNodeStack(node, false);
                    setTooltip(new Tooltip(pair.getKey() + " " + Messages.tagSearchEntryToolTip));

                    setOnMouseClicked(action -> {
                        if (action.getClickCount() == 2) {
                            Stack<Node> copiedStack = new Stack<>();
                            pair.getValue().stream().forEach(aNode -> copiedStack.push(aNode));
                            callerController.locateNode(copiedStack);
                        }
                    });
                }
            }
        });

        tagNameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getName()));
        commentColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getComment()));
        createdColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getCreated().toString()));
        createdColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
        creatorColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getUserName()));

        tagNameColumn.setReorderable(false);
        commentColumn.setReorderable(false);
        createdColumn.setReorderable(false);
        creatorColumn.setReorderable(false);
    }

    private void filterList(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            resultTableView.getItems().clear();
            resultTableView.getItems().addAll(tagList);

            return;
        }

        List<Tag> filteredList = tagList.stream()
                .filter(tag -> tag.getName().toLowerCase().contains(keyword.toLowerCase()) || tag.getComment().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());

        resultTableView.getItems().clear();
        resultTableView.getItems().addAll(filteredList);
    }

    public void setCallerController(BaseSaveAndRestoreController callerController) {
        this.callerController = callerController;
    }
}
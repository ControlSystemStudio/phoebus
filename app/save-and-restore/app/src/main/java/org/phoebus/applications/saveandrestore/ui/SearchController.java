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
package org.phoebus.applications.saveandrestore.ui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import org.phoebus.applications.saveandrestore.DirectoryUtilities;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.ui.javafx.ImageCache;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Stack;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * {@link SearchController} class provides the controller for SearchWindow.fxml
 *
 * @author <a href="mailto:changj@frib.msu.edu">Genie Jhang</a>
 */

public class SearchController implements Initializable {

    private SaveAndRestoreController callerController;
    private List<SearchEntry> tableEntries = new ArrayList<>();

    @FXML
    private TextField keywordTextField;

    @FXML
    private CheckBox searchOptionSnapshotName;

    @FXML
    private CheckBox searchOptionSnapshotComment;

    @FXML
    private CheckBox searchOptionTagName;

    @FXML
    private CheckBox searchOptionTagComment;

    @FXML
    private CheckBox searchOptionGoldenOnly;

    @FXML
    private TableView<SearchEntry> resultTableView;

    @FXML
    private TableColumn<SearchEntry, ImageView> typeColumn;

    @FXML
    private TableColumn<SearchEntry, String> nameColumn;

    @FXML
    private TableColumn<SearchEntry, String> commentColumn;

    @FXML
    private TableColumn<SearchEntry, Date> createdColumn;

    @FXML
    private TableColumn<SearchEntry, String> creatorColumn;

    private SaveAndRestoreService saveAndRestoreService;

    private static final Logger LOG = Logger.getLogger(SaveAndRestoreService.class.getName());

    private void refreshList() {
        tableEntries.clear();
        List<Node> snapshotList;
        if (searchOptionSnapshotName.isSelected() || searchOptionSnapshotComment.isSelected()) {
            try {
                snapshotList = saveAndRestoreService.getAllSnapshots();
                snapshotList.stream().map(SearchEntry::create).forEach(tableEntries::add);
                if(searchOptionGoldenOnly.isSelected()){
                    tableEntries = tableEntries.stream().filter(se -> isSnapshotGolden(se.snapshot)).collect(Collectors.toList());
                }

            } catch (Exception e) {
                LOG.warning("Unable to retrieve snapshot list from server. Please check if the latest version of service is running.");
            }
        }

        List<Tag> tagList;
        if (searchOptionTagName.isSelected() || searchOptionTagComment.isSelected()) {
            try {
                tagList = saveAndRestoreService.getAllTags();
                tagList.stream().map(SearchEntry::create).forEach(tableEntries::add);
            } catch (Exception e) {
                LOG.warning("Unable to retrieve tag list from server. Please check if the latest version of service is running.");
            }
        }

        resultTableView.getItems().addAll(tableEntries);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        saveAndRestoreService = SaveAndRestoreService.getInstance();
        refreshList();
        filterList(null);

        keywordTextField.textProperty().addListener((observableValue, oldKeyword, newKeyword) -> filterList(newKeyword));

        searchOptionSnapshotName.selectedProperty().addListener((observableValue, aBoolean, selected) -> filterList(keywordTextField.getText()));
        searchOptionSnapshotComment.selectedProperty().addListener((observableValue, aBoolean, selected) -> filterList(keywordTextField.getText()));
        searchOptionTagName.selectedProperty().addListener((observableValue, aBoolean, selected) -> filterList(keywordTextField.getText()));
        searchOptionTagComment.selectedProperty().addListener((observableValue, aBoolean, selected) -> filterList(keywordTextField.getText()));
        searchOptionGoldenOnly.selectedProperty().addListener((observableValue, aBoolean, selected) -> filterList(keywordTextField.getText()));

        resultTableView.setRowFactory(tableView -> new TableRow<>() {
            @Override
            protected void updateItem(SearchEntry searchEntry, boolean empty) {
                super.updateItem(searchEntry, empty);
                if (searchEntry == null || empty) {
                    setTooltip(null);
                    setOnMouseClicked(null);
                } else {
                    setTooltip(new Tooltip(Messages.searchEntryToolTip));

                    setOnMouseClicked(action -> {
                        if (action.getClickCount() == 2) {
                            Stack<Node> copiedStack = new Stack<>();

                            if (searchEntry.getSnapshot() == null) {
                                searchEntry.setSnapshot(saveAndRestoreService.getNode(searchEntry.getTag().getSnapshotId()));
                            }
                            DirectoryUtilities.CreateLocationStringAndNodeStack(searchEntry.getSnapshot(), false).getValue().forEach(copiedStack::push);
                            callerController.locateNode(copiedStack);
                        }
                    });
                }
            }
        });

        typeColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getEntryImageView()));
        typeColumn.setStyle("-fx-alignment: CENTER;");
        nameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getName()));
        nameColumn.setStyle("-fx-alignment: CENTER-LEFT;");
        commentColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getComment()));
        createdColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper(cell.getValue().getCreated()));
        createdColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
        creatorColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getCreator()));
        creatorColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        typeColumn.setReorderable(false);
        nameColumn.setReorderable(false);
        commentColumn.setReorderable(false);
        createdColumn.setReorderable(false);
        creatorColumn.setReorderable(false);
    }

    private void filterList(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            resultTableView.getItems().clear();
            refreshList();

            return;
        }

        List<SearchEntry> filteredList = tableEntries.parallelStream()
                .filter(entry -> {
                    boolean flag = false;

                    if (entry.getType().equals(EntryType.SNAPSHOT)) {
                        flag |= searchOptionSnapshotName.isSelected() & entry.getName().toLowerCase().contains(keyword.toLowerCase());
                        flag |= searchOptionSnapshotComment.isSelected() & entry.getComment().toLowerCase().contains(keyword.toLowerCase());
                        flag &= !searchOptionGoldenOnly.isSelected() | isSnapshotGolden(entry.snapshot);
                    } else {
                        flag |= searchOptionTagName.isSelected() & entry.getName().toLowerCase().contains(keyword.toLowerCase());
                        flag |= searchOptionTagComment.isSelected() & entry.getComment().toLowerCase().contains(keyword.toLowerCase());
                    }

                    return flag;
                }).collect(Collectors.toList());

        resultTableView.getItems().clear();
        resultTableView.getItems().addAll(filteredList);
    }

    public void setCallerController(SaveAndRestoreController callerController) {
        this.callerController = callerController;
    }
    
    private static boolean isSnapshotGolden(Node node){
        return node.getProperty("golden") != null && Boolean.valueOf(node.getProperty("golden"));
    }

    private enum EntryType {SNAPSHOT, TAG}

    private static class SearchEntry {
        private final EntryType type;
        private Node snapshot = null;
        private Tag tag = null;

        private final String name;
        private final String comment;
        private final Date created;
        private final String creator;

        public static SearchEntry create(Object object) {
            if (object instanceof Node) {
                return new SearchEntry((Node) object, null);
            } else if (object instanceof Tag) {
                return new SearchEntry(null, (Tag) object);
            } else {
                return null;
            }
        }

        private SearchEntry(Node snapshot, Tag tag) {
            if (snapshot != null) {
                type = EntryType.SNAPSHOT;

                this.snapshot = snapshot;

                name = snapshot.getName();
                comment = snapshot.getProperty("comment");
                created = snapshot.getCreated();
                creator = snapshot.getUserName();
            } else {
                type = EntryType.TAG;

                this.tag = tag;

                name = tag.getName();
                comment = tag.getComment();
                created = tag.getCreated();
                creator = tag.getUserName();
            }
        }

        public ImageView getEntryImageView() {
            if (type.equals(EntryType.SNAPSHOT)) {
                if (isSnapshotGolden(snapshot)) {
                    return new ImageView(ImageCache.getImage(SearchController.class, "/icons/save-and-restore/snapshot-golden.png"));
                }
                else {
                    return new ImageView(ImageCache.getImage(SearchController.class, "/icons/save-and-restore/snapshot.png"));
                }
            } else {
                ImageView imageView = new ImageView(ImageCache.getImage(SearchController.class, "/icons/save-and-restore/snapshot-tag.png"));
                imageView.setPreserveRatio(true);
                imageView.setFitHeight(22);

                return imageView;
            }
        }

        public EntryType getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public String getComment() {
            return comment;
        }

        public Date getCreated() {
            return created;
        }

        public String getCreator() {
            return creator;
        }

        public Node getSnapshot() {
            return snapshot;
        }

        public void setSnapshot(Node snapshot) {
            this.snapshot = snapshot;
        }

        public Tag getTag() {
            return tag;
        }
    }
}


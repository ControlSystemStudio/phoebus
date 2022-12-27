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
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import org.phoebus.applications.saveandrestore.DirectoryUtilities;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.model.search.SearchResult;
import org.phoebus.applications.saveandrestore.ui.SearchQueryUtil.Keys;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.util.time.TimestampFormats;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
    private List<Node> tableEntries = new ArrayList<>();

    @FXML
    private TextField keywordTextField;

    @FXML
    private TableView<Node> resultTableView;

    @FXML
    private TableColumn<Node, ImageView> typeColumn;

    @FXML
    private TableColumn<Node, String> nameColumn;

    @FXML
    private TableColumn<Node, String> commentColumn;

    @FXML
    private TableColumn<Node, String> tagsColumn;

    @FXML
    private TableColumn<Node, Date> createdColumn;

    @FXML
    private TableColumn<Node, String> creatorColumn;

    private SaveAndRestoreService saveAndRestoreService;

    private static final Logger LOG = Logger.getLogger(SaveAndRestoreService.class.getName());

    private Map<String, Keys> lookup = Arrays.stream(Keys.values()).collect(Collectors.toMap(Keys::getName, k -> {
        return k;
    }));

    private ObservableMap<Keys, String> searchParameters = FXCollections.<Keys, String>observableHashMap();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        saveAndRestoreService = SaveAndRestoreService.getInstance();

        resultTableView.setRowFactory(tableView -> new TableRow<>() {
            @Override
            protected void updateItem(Node node, boolean empty) {
                super.updateItem(node, empty);
                if (node == null || empty) {
                    setTooltip(null);
                    setOnMouseClicked(null);
                } else {
                    setTooltip(new Tooltip(Messages.searchEntryToolTip));

                    setOnMouseClicked(action -> {
                        if (action.getClickCount() == 2) {
                            Stack<Node> copiedStack = new Stack<>();
                            DirectoryUtilities.CreateLocationStringAndNodeStack(node, false).getValue().forEach(copiedStack::push);
                            callerController.locateNode(copiedStack);
                        }
                    });
                }
            }
        });

        typeColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(getImageView(cell.getValue())));
        typeColumn.setStyle("-fx-alignment: TOP-CENTER;");
        nameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getName()));
        nameColumn.setStyle("-fx-alignment: TOP-LEFT;");
        commentColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getDescription()));
        createdColumn.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper(TimestampFormats.SECONDS_FORMAT.format(cell.getValue().getCreated().toInstant())));
        createdColumn.setStyle("-fx-alignment: TOP-RIGHT;");
        creatorColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getUserName()));
        creatorColumn.setStyle("-fx-alignment: TOP-RIGHT;");

        typeColumn.setReorderable(false);
        nameColumn.setReorderable(false);
        commentColumn.setReorderable(false);
        createdColumn.setReorderable(false);
        creatorColumn.setReorderable(false);

        keywordTextField.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                search();
            }
        });
    }

    public void setCallerController(SaveAndRestoreController callerController) {
        this.callerController = callerController;
    }

    private ImageView getImageView(Node node) {
        switch (node.getNodeType()) {
            case SNAPSHOT:
                if (node.hasTag(Tag.GOLDEN)) {
                    return new ImageView(ImageCache.getImage(SearchController.class, "/icons/save-and-restore/snapshot-golden.png"));
                } else {
                    return new ImageView(ImageCache.getImage(SearchController.class, "/icons/save-and-restore/snapshot.png"));
                }
            case COMPOSITE_SNAPSHOT:
                return new ImageView(ImageCache.getImage(SearchController.class, "/icons/save-and-restore/composite-snapshot.png"));
            case FOLDER:
                return new ImageView(ImageCache.getImage(SearchController.class, "/icons/save-and-restore/folder.png"));
            case CONFIGURATION:
                return new ImageView(ImageCache.getImage(SearchController.class, "/icons/save-and-restore/configuration.png"));
        }
        return null;
    }

    @FXML
    public void showHelp() {
        new HelpViewer().show();
    }

    @FXML
    public void search() {
        List<String> searchTerms = Arrays.asList(keywordTextField.getText().split("&"));
        searchTerms.stream().forEach(s -> {
            String key = s.split("=")[0];
            String value = s.split("=")[1];
            if (lookup.containsKey(key)) {
                searchParameters.put(lookup.get(key), value);
            }
        });

        JobManager.schedule("Save-and-restore Search", monitor -> {
            MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
            searchParameters.entrySet().forEach(e -> {
                        map.add(e.getKey().getName(), e.getValue());
                    }
            );
            try {
                SearchResult searchResult = saveAndRestoreService.search(map);
                if (searchResult.getHitCount() > 0) {
                    tableEntries.clear();
                    tableEntries.addAll(searchResult.getNodes());
                    tableEntries = tableEntries.stream().sorted(nodeComparator()).collect(Collectors.toList());
                    resultTableView.getItems().setAll(tableEntries);
                }
            } catch (Exception e) {
                ExceptionDetailsErrorDialog.openError(
                        resultTableView,
                        Messages.errorGeneric,
                        Messages.searchErrorBody,
                        e
                );
            }
        });
    }

    private Comparator<Node> nodeComparator(){
        return Comparator.comparing((Node n) -> n.getNodeType())
                .thenComparing((Node n) -> n.getName().toLowerCase());
    }
}


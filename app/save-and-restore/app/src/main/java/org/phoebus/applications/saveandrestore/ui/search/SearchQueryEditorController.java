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
 *
 */

package org.phoebus.applications.saveandrestore.ui.search;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.applications.saveandrestore.model.search.SearchQueryUtil;
import org.phoebus.applications.saveandrestore.model.search.SearchQueryUtil.Keys;
import org.phoebus.applications.saveandrestore.ui.ImageRepository;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.ui.dialog.ListSelectionPopOver;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SearchQueryEditorController implements Initializable {

    private SearchAndFilterViewController searchAndFilterViewController;

    @FXML
    private TextField nodeNameTextField;

    @FXML
    private CheckBox nodeTypeFolderCheckBox;

    @FXML
    private CheckBox nodeTypeConfigurationCheckBox;

    @FXML
    private CheckBox nodeTypeSnapshotCheckBox;

    @FXML
    private CheckBox nodeTypeCompositeSnapshotCheckBox;

    @FXML
    private TextField tagsTextField;

    @FXML
    private TextField userTextField;

    @FXML
    private TextField descTextField;

    @FXML
    private TextField startTime;

    @FXML
    private TextField endTime;

    @FXML
    private CheckBox goldenOnlyCheckbox;

    @FXML
    private ImageView goldenImageView;

    @FXML
    private ImageView folderImageView;

    @FXML
    private ImageView configurationImageView;

    @FXML
    private ImageView snapshotImageView;

    @FXML
    private ImageView compositeSnapshotImageView;

    private final SimpleStringProperty nodeNameProperty = new SimpleStringProperty();

    private final SimpleBooleanProperty nodeTypeFolderProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty nodeTypeConfigurationProperty = new SimpleBooleanProperty();

    private final SimpleBooleanProperty nodeTypeSnapshotProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty nodeTypeCompositeSnapshotProperty = new SimpleBooleanProperty();

    private final SimpleStringProperty tagsProperty = new SimpleStringProperty();
    private final SimpleStringProperty userProperty = new SimpleStringProperty();

    private final SimpleStringProperty startTimeProperty = new SimpleStringProperty();
    private final SimpleStringProperty endTimeProperty = new SimpleStringProperty();

    private final SimpleStringProperty descProperty = new SimpleStringProperty();

    private final SimpleBooleanProperty goldenOnlyProperty = new SimpleBooleanProperty();

    private boolean searchDisabled = false;

    private ListSelectionPopOver tagSearchPopover;

    private static final Logger LOGGER = Logger.getLogger(SearchWindowController.class.getName());

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        nodeNameTextField.textProperty().bindBidirectional(nodeNameProperty);
        nodeNameTextField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                updateParametersAndSearch();
            }
        });
        nodeTypeFolderCheckBox.selectedProperty().bindBidirectional(nodeTypeFolderProperty);
        folderImageView.imageProperty().set(ImageRepository.FOLDER);
        nodeTypeFolderCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                updateParametersAndSearch();
            }
        });
        nodeTypeConfigurationCheckBox.selectedProperty().bindBidirectional(nodeTypeConfigurationProperty);
        configurationImageView.imageProperty().set(ImageRepository.CONFIGURATION);
        nodeTypeConfigurationCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                updateParametersAndSearch();
            }
        });
        nodeTypeSnapshotCheckBox.selectedProperty().bindBidirectional(nodeTypeSnapshotProperty);
        snapshotImageView.imageProperty().set(ImageRepository.SNAPSHOT);
        nodeTypeSnapshotCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                updateParametersAndSearch();
            }
        });
        nodeTypeCompositeSnapshotCheckBox.selectedProperty().bindBidirectional(nodeTypeCompositeSnapshotProperty);
        compositeSnapshotImageView.imageProperty().set(ImageRepository.COMPOSITE_SNAPSHOT);
        nodeTypeCompositeSnapshotCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                updateParametersAndSearch();
            }
        });

        goldenImageView.imageProperty().set(ImageRepository.GOLDEN_SNAPSHOT);

        descTextField.textProperty().bindBidirectional(descProperty);
        descTextField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                updateParametersAndSearch();
            }
        });
        userTextField.textProperty().bindBidirectional(userProperty);
        userTextField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                updateParametersAndSearch();
            }
        });
        tagsTextField.textProperty().bindBidirectional(tagsProperty);
        tagsTextField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                updateParametersAndSearch();
            }
        });

        startTime.textProperty().bindBidirectional(startTimeProperty);
        startTime.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                updateParametersAndSearch();
            }
        });

        endTime.textProperty().bindBidirectional(endTimeProperty);
        endTime.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                updateParametersAndSearch();
            }
        });

        tagSearchPopover = ListSelectionPopOver.create(
                (tags, popover) -> {
                    String tagsValue = String.join(",", tags);
                    tagsProperty.setValue(tagsValue);
                    if (popover.isShowing()) {
                        popover.hide();
                        updateParametersAndSearch();
                    }
                },
                (tags, popover) -> {
                    if (popover.isShowing()) {
                        popover.hide();
                    }
                }
        );

        startTime.textProperty().bindBidirectional(startTimeProperty);
    }

    public void setFilter(Filter filter) {
        // Temporarily disable search as setting the types options would otherwise trigger
        // a search for each selection.
        searchDisabled = true;
        Map<String, String> searchParams = SearchQueryUtil.parseHumanReadableQueryString(filter.getQueryString());
        nodeNameProperty.set(searchParams.get(Keys.NAME.getName()));
        userProperty.set(searchParams.get(Keys.USER.getName()));
        descProperty.set(searchParams.get(Keys.DESC.getName()));
        tagsProperty.set(searchParams.get(Keys.TAGS.getName()));
        startTimeProperty.set(searchParams.get(Keys.STARTTIME.getName()));
        endTimeProperty.set(searchParams.get(Keys.ENDTIME.getName()));

        String typeValue = searchParams.get(Keys.TYPE.getName());
        nodeTypeFolderProperty.set(false);
        nodeTypeConfigurationProperty.set(false);
        nodeTypeSnapshotProperty.set(false);
        nodeTypeCompositeSnapshotProperty.set(false);
        if (typeValue != null && !typeValue.isEmpty()) {
            String[] types = typeValue.split(",");
            for (String type : types) {
                if (type.equalsIgnoreCase(NodeType.FOLDER.name())) {
                    nodeTypeFolderProperty.set(true);
                }
                if (type.equalsIgnoreCase(NodeType.CONFIGURATION.name())) {
                    nodeTypeConfigurationProperty.set(true);
                }
                if (type.equalsIgnoreCase(NodeType.SNAPSHOT.name())) {
                    nodeTypeSnapshotProperty.set(true);
                }
                if (type.equalsIgnoreCase(NodeType.COMPOSITE_SNAPSHOT.name())) {
                    nodeTypeCompositeSnapshotProperty.set(true);
                }
            }
        }
        // Enable search...
        searchDisabled = false;
        // ...and trigger it.
        updateParametersAndSearch();
    }

    public void setSearchAndFilterViewController(SearchAndFilterViewController searchAndFilterViewController) {
        this.searchAndFilterViewController = searchAndFilterViewController;
    }

    private void updateParametersAndSearch() {
        if (searchDisabled) {
            return;
        }

        String queryString = buildQueryString();
        searchAndFilterViewController.search(queryString);
    }

    private String buildQueryString() {
        Map<String, String> map = new HashMap<>();
        if (nodeNameProperty.get() != null && !nodeNameProperty.get().isEmpty()) {
            map.put(Keys.NAME.getName(), nodeNameProperty.get());
        }
        if (userProperty.get() != null && !userProperty.get().isEmpty()) {
            map.put(Keys.USER.getName(), userProperty.get());
        }
        if (descProperty.get() != null && !descProperty.get().isEmpty()) {
            map.put(Keys.DESC.getName(), descProperty.get());
        }
        if (tagsProperty.get() != null && !tagsProperty.get().isEmpty()) {
            map.put(Keys.TAGS.getName(), tagsProperty.get());
        }
        List<String> types = new ArrayList<>();
        if (nodeTypeFolderProperty.get()) {
            types.add(NodeType.FOLDER.name().toLowerCase());
        }
        if (nodeTypeConfigurationProperty.get()) {
            types.add(NodeType.CONFIGURATION.name().toLowerCase());
        }
        if (nodeTypeSnapshotProperty.get()) {
            types.add(NodeType.SNAPSHOT.name().toLowerCase());
        }
        if (nodeTypeCompositeSnapshotProperty.get()) {
            types.add(NodeType.COMPOSITE_SNAPSHOT.name().toLowerCase());
        }
        if (!types.isEmpty()) {
            map.put(Keys.TYPE.getName(), String.join(",", types));
        }
        if (startTimeProperty.get() != null && !startTimeProperty.get().isEmpty()) {
            map.put(Keys.STARTTIME.getName(), startTimeProperty.get());
        }
        if (endTimeProperty.get() != null && !endTimeProperty.get().isEmpty()) {
            map.put(Keys.ENDTIME.getName(), endTimeProperty.get());
        }
        if (goldenOnlyProperty.get()) {
            String tags = map.get(Keys.TAGS.getName());
            if (tags == null) {
                map.put(Keys.TAGS.getName(), Tag.GOLDEN);
            } else if(!tags.toLowerCase().contains(Tag.GOLDEN.toLowerCase())){
                map.put(Keys.TAGS.getName(), tags + "," + Tag.GOLDEN);
            }
        }
        return SearchQueryUtil.toQueryString(map);
    }

    @FXML
    public void showTagsSelectionPopover() {
        if (tagSearchPopover.isShowing()) {
            tagSearchPopover.hide();
        } else {
            List<String> selectedTags = Arrays.stream(tagsProperty.getValueSafe().split(","))
                    .map(String::trim)
                    .filter(it -> !it.isEmpty())
                    .collect(Collectors.toList());
            List<String> availableTags = new ArrayList<>();
            try {
                List<String> tagNames = new ArrayList<>();
                SaveAndRestoreService.getInstance().getAllTags().forEach(tag -> {
                    if (!tagNames.contains(tag.getName()) && !tag.getName().equalsIgnoreCase(Tag.GOLDEN)) {
                        tagNames.add(tag.getName());
                    }
                });
                availableTags = tagNames
                        .stream()
                        .sorted(Comparator.comparing(String::toLowerCase))
                        .collect(Collectors.toList());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unable to retrieve all tags from service");
            }
            tagSearchPopover.setAvailable(availableTags, selectedTags);
            tagSearchPopover.setSelected(selectedTags);
            tagSearchPopover.show(tagsTextField);
        }
    }

    @FXML
    public void goldenClicked() {
        goldenOnlyProperty.set(goldenOnlyCheckbox.isSelected());
        updateParametersAndSearch();
    }
}

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
package org.phoebus.applications.saveandrestore.ui.snapshot.tag;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TreeItem;
import javafx.stage.Modality;
import javafx.util.Pair;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.model.TagData;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreController.TagComparator;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.TagProposalProvider;
import org.phoebus.applications.saveandrestore.ui.snapshot.SnapshotNewTagDialog;
import org.phoebus.framework.autocomplete.ProposalService;
import org.phoebus.ui.autocomplete.AutocompleteMenu;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * {@link TagUtil} class provides rich information header of {@link Tag} as {@link Node}
 *
 * @author Genie Jhang <changj@frib.msu.edu>
 */

public class TagUtil {

    /**
     * Determines a list of {@link Tag}s that must occur in all specified {@link org.phoebus.applications.saveandrestore.model.Node}s
     *
     * @param nodes List of {@link org.phoebus.applications.saveandrestore.model.Node}s, each with a <code>null</code>, empty or
     *              non-empty list of {@link Tag}s.
     * @return A potentially empty list of {@link Tag}s.
     */
    public static List<Tag> getCommonTags(List<org.phoebus.applications.saveandrestore.model.Node> nodes) {
        // Construct a list containing all tags across all nodes. May contain duplicates.
        List<Tag> allTags = new ArrayList<>();
        nodes.stream().forEach(n -> {
            if (n.getTags() != null) {
                allTags.addAll(n.getTags());
            }
        });

        // List of common tags is constructed of tags having same number of occurrences as the size of the node list.
        List<Tag> commonTags = new ArrayList<>();
        allTags.forEach(t -> {
            if (Collections.frequency(allTags, t) == nodes.size() && !commonTags.contains(t)) {
                commonTags.add(t);
            }
        });

        return commonTags;
    }

    public static void tagWithComment(Menu parentMenu,
                                      List<org.phoebus.applications.saveandrestore.model.Node> selectedNodes,
                                      Consumer<List<org.phoebus.applications.saveandrestore.model.Node>> callback){
        ObservableList<javafx.scene.control.MenuItem> items = parentMenu.getItems();
        // Need to remove items as they would otherwise be maintained when user selects another node with tags
        if (items.size() > 1) {
            items.remove(1, items.size());
        }
        // For a single Node this list should be the same as the Node's (potentially empty) tag list, though not null.
        List<Tag> commonTags = TagUtil.getCommonTags(selectedNodes);
        // Exclude golden tag as it is removed in separate context menu item.
        commonTags.remove(Tag.builder().name(Tag.GOLDEN).build());
        List<javafx.scene.control.MenuItem> additionalItems = new ArrayList<>();
        if (!commonTags.isEmpty()) {
            additionalItems.add(new SeparatorMenuItem());
            commonTags.sort(new TagComparator());
            commonTags.forEach(tag -> {
                CustomMenuItem tagItem = TagWidget.TagWithCommentMenuItem(tag);
                tagItem.setOnAction(actionEvent -> {
                    Alert confirmation = new Alert(AlertType.CONFIRMATION);
                    confirmation.setTitle(Messages.tagRemoveConfirmationTitle);
                    confirmation.setContentText(Messages.tagRemoveConfirmationContent);
                    Optional<ButtonType> result = confirmation.showAndWait();
                    result.ifPresent(buttonType -> {
                        if (buttonType == ButtonType.OK) {
                            try {
                                TagData tagData = new TagData();
                                tagData.setTag(tag);
                                tagData.setUniqueNodeIds(selectedNodes.stream().map(org.phoebus.applications.saveandrestore.model.Node::getUniqueId).collect(Collectors.toList()));
                                List<org.phoebus.applications.saveandrestore.model.Node> updatedNodes = SaveAndRestoreService.getInstance().deleteTag(tagData);
                                callback.accept(updatedNodes);
                            } catch (Exception e) {
                                Logger.getLogger(TagUtil.class.getName()).log(Level.WARNING, "Failed to remove tag from node", e);
                            }
                        }
                    });
                });
                additionalItems.add(tagItem);
            });
        }
        items.addAll(additionalItems);
    }

    public static List<org.phoebus.applications.saveandrestore.model.Node> addTag(List<org.phoebus.applications.saveandrestore.model.Node> selectedNodes){
        List<String> selectedNodeIds =
                selectedNodes.stream().map(node -> node.getUniqueId()).collect(Collectors.toList());
        SnapshotNewTagDialog snapshotNewTagDialog =
                new SnapshotNewTagDialog(selectedNodes);
        snapshotNewTagDialog.initModality(Modality.APPLICATION_MODAL);

        ProposalService proposalService = new ProposalService(new TagProposalProvider(SaveAndRestoreService.getInstance()));
        AutocompleteMenu autocompleteMenu = new AutocompleteMenu(proposalService);
        snapshotNewTagDialog.configureAutocompleteMenu(autocompleteMenu);
        List<org.phoebus.applications.saveandrestore.model.Node> updatedNodes = new ArrayList<>();
        Optional<Pair<String, String>> result = snapshotNewTagDialog.showAndWait();
        result.ifPresent(items -> {
            Tag aNewTag = Tag.builder()
                    .name(items.getKey())
                    .comment(items.getValue())
                    .created(new Date())
                    .userName(System.getProperty("user.name"))
                    .build();
            try {
                TagData tagData = new TagData();
                tagData.setTag(aNewTag);
                tagData.setUniqueNodeIds(selectedNodeIds);
                updatedNodes.addAll(SaveAndRestoreService.getInstance().addTag(tagData));
            } catch (Exception e) {
                Logger.getLogger(TagUtil.class.getName()).log(Level.WARNING, "Failed to add tag to node");
            }
        });
        return updatedNodes;
    }
}

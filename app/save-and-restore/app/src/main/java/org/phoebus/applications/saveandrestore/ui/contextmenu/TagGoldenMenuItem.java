/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.contextmenu;

import javafx.collections.ObservableList;
import javafx.scene.image.ImageView;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.model.TagData;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreController;
import org.phoebus.ui.javafx.ImageCache;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TagGoldenMenuItem extends SaveAndRestoreMenuItem {

    private final ImageView regularIcon = ImageCache.getImageView(ImageCache.class, "/icons/save-and-restore/snapshot.png");
    private final ImageView goldenIcon = ImageCache.getImageView(ImageCache.class, "/icons/save-and-restore/snapshot-golden.png");

    public TagGoldenMenuItem(SaveAndRestoreController saveAndRestoreController,
                             ObservableList<Node> selectedItemsProperty,
                             Consumer onAction) {
        super(saveAndRestoreController, selectedItemsProperty, onAction);
        setText(Messages.contextMenuTagAsGolden);
        setGraphic(regularIcon);
    }

    @Override
    public void configure() {
        disableProperty().set(saveAndRestoreController.getUserIdentity().isNull().get() ||
                selectedItemsProperty.stream().anyMatch(n -> !n.getNodeType().equals(NodeType.SNAPSHOT)) ||
                !snapshotsTaggedEqual());
        if (selectedItemsProperty.get(0).hasTag(Tag.GOLDEN)) {
            setText(Messages.contextMenuRemoveGoldenTag);
            setGraphic(regularIcon);
            setOnAction(event -> {
                TagData tagData = new TagData();
                tagData.setTag(Tag.builder().name(Tag.GOLDEN).build());
                tagData.setUniqueNodeIds(selectedItemsProperty.stream()
                        .map(Node::getUniqueId).collect(Collectors.toList()));
                saveAndRestoreController.deleteTag(tagData);
            });
        } else {
            setText(Messages.contextMenuTagAsGolden);
            setGraphic(goldenIcon);
            setOnAction(event -> {
                TagData tagData = new TagData();
                tagData.setTag(Tag.builder().name(Tag.GOLDEN).build());
                tagData.setUniqueNodeIds(selectedItemsProperty.stream()
                        .map(Node::getUniqueId).collect(Collectors.toList()));
                saveAndRestoreController.addTag(tagData);
            });
        }
    }

    private boolean snapshotsTaggedEqual() {
        AtomicInteger goldenTagCount = new AtomicInteger();
        selectedItemsProperty.forEach(n -> {
            if (n.hasTag(Tag.GOLDEN)) {
                goldenTagCount.incrementAndGet();
            }
        });
        return goldenTagCount.get() == 0 || goldenTagCount.get() == selectedItemsProperty.size();
    }
}

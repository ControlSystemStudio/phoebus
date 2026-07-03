package org.phoebus.applications.saveandrestore.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.util.time.TimestampFormats;

public class NodeTreeTableCell extends TreeTableCell<Node, Node> {

    private final SaveAndRestoreController saveAndRestoreController;

    public NodeTreeTableCell(SaveAndRestoreController saveAndRestoreController){
        this.saveAndRestoreController = saveAndRestoreController;
    }

    @Override
    public void updateItem(Node node, boolean empty){
        super.updateItem(node, empty);
        if (empty) {
            setText(null);
            setGraphic(null);
            setTooltip(null);
            return;
        }
        // Use custom layout as this makes it easier to set opacity
        HBox hBox = new HBox();
        hBox.setOpacity(saveAndRestoreController.matchesFilter(node) ? 1.0 : 0.4);
        Label nameLabel = new Label(node.getName());
        nameLabel.setPadding(new Insets(0.0, 0.0, 0.0, 5.0));
        setTooltip(null);
        switch (node.getNodeType()) {
            case SNAPSHOT:
                if (node.hasTag(Tag.GOLDEN)) {
                    hBox.getChildren().add(new ImageView(ImageRepository.GOLDEN_SNAPSHOT));
                } else {
                    hBox.getChildren().add(new ImageView(ImageRepository.SNAPSHOT));
                }
                hBox.getChildren().add(nameLabel);
                if (node.getTags() != null && !node.getTags().isEmpty()) {
                    ImageView tagImage = new ImageView(ImageCache.getImage(NodeTreeTableCell.class, "/icons/save-and-restore/snapshot-tags.png"));
                    tagImage.setFitHeight(13);
                    tagImage.setPreserveRatio(true);
                    hBox.getChildren().add(tagImage);
                }

                break;
            case COMPOSITE_SNAPSHOT:
                hBox.getChildren().add(new ImageView(ImageRepository.COMPOSITE_SNAPSHOT));
                hBox.getChildren().add(nameLabel);
                if (node.getTags() != null && !node.getTags().isEmpty()) {
                    ImageView tagImage = new ImageView(ImageCache.getImage(NodeTreeTableCell.class, "/icons/save-and-restore/snapshot-tags.png"));
                    tagImage.setFitHeight(13);
                    tagImage.setPreserveRatio(true);
                    getChildren().add(tagImage);
                }
                break;
            case CONFIGURATION:
                hBox.getChildren().add(new ImageView(ImageRepository.CONFIGURATION));
                hBox.getChildren().add(nameLabel);
                break;
            case FOLDER:
                hBox.getChildren().add(new ImageView(ImageRepository.FOLDER));
                hBox.getChildren().add(nameLabel);
                StringBuilder stringBuilder = new StringBuilder();
                if (node.getCreated() != null) { // Happens if configuration management is accessed from context menu
                    stringBuilder.append(TimestampFormats.SECONDS_FORMAT
                            .format(node.getLastModified() != null ? node.getLastModified().toInstant() : node.getCreated().toInstant())).append(" (").append(node.getUserName()).append(")");
                }
                if (node.getUniqueId().equals(Node.ROOT_FOLDER_UNIQUE_ID)) {
                    setTooltip(new Tooltip(SaveAndRestoreService.getInstance().getServiceIdentifier()));
                }
                else{
                    setTooltip(new Tooltip(stringBuilder.toString()));
                }
                break;
        }

        setGraphic(hBox);
    }
}

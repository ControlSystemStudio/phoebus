package org.phoebus.applications.saveandrestore.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.util.time.TimestampFormats;

import java.util.ArrayList;
import java.util.List;

/**
 * Cell factory for the name column of the {@link javafx.scene.control.TreeTableView}.
 */
public class NodeTreeTableCell extends TreeTableCell<Node, Node> {

    private final SaveAndRestoreController saveAndRestoreController;

    private static final String DRAG_BORDER = "drag-border";
    private static final String DEFAULT_BORDER = "default-border";

    public NodeTreeTableCell(SaveAndRestoreController saveAndRestoreController) {
        this.saveAndRestoreController = saveAndRestoreController;

        setOnDragDetected(event -> {
            if (saveAndRestoreController.getUserIdentity().isNull().get() || !saveAndRestoreController.selectedNodesOfSameType()) {
                return;
            }
            final ClipboardContent content = new ClipboardContent();
            Node node = getItem();
            // Drag-n-drop not supported for root node
            if (node != null &&
                    !node.getUniqueId().equals(Node.ROOT_FOLDER_UNIQUE_ID)) {
                final List<Node> nodes = new ArrayList<>();

                for (TreeItem<Node> sel : getTreeTableView().getSelectionModel().getSelectedItems()) {
                    nodes.add(sel.getValue());
                }
                content.put(SaveAndRestoreApplication.NODE_SELECTION_FORMAT, nodes);
                // Only move supported!
                final Dragboard db = startDragAndDrop(TransferMode.MOVE);
                db.setContent(content);
            }
            event.consume();
        });

        setOnDragOver(event ->
        {
            final Node node = getItem();
            if (node != null) {
                List<Node> sourceNodes = (List<Node>) event.getDragboard().getContent(SaveAndRestoreApplication.NODE_SELECTION_FORMAT);
                if (DragNDropUtil.mayDrop(event.getTransferMode(), node, sourceNodes)) {
                    event.acceptTransferModes(event.getTransferMode());
                    getStyleClass().add(DRAG_BORDER);
                    getStyleClass().remove(DEFAULT_BORDER);
                }
            }
            event.consume();
        });

        setOnDragExited(event ->
        {
            getStyleClass().add(DEFAULT_BORDER); // This one is needed to maintain the vertical divider between columns.
            getStyleClass().remove(DRAG_BORDER);
            event.consume();
        });

        setOnDragDropped(event ->
        {
            Node targetNode = getItem();
            if (targetNode != null) {
                TransferMode transferMode = event.getTransferMode();
                List<Node> sourceNodes = (List<Node>) event.getDragboard().getContent(SaveAndRestoreApplication.NODE_SELECTION_FORMAT);
                if (!DragNDropUtil.mayDrop(transferMode, targetNode, sourceNodes)) {
                    return;
                }
                if (DragNDropUtil.snapshotsOrCompositeSnapshotsOnly(sourceNodes) && targetNode.getNodeType().equals(NodeType.COMPOSITE_SNAPSHOT)) {
                    saveAndRestoreController.editCompositeSnapshot(targetNode, sourceNodes);
                } else {
                    getTreeTableView().getSelectionModel().clearSelection(); // This is needed to help controller implement selection restrictions
                    saveAndRestoreController.moveNodes(sourceNodes, targetNode, transferMode);
                }
            }
            event.setDropCompleted(true);
            event.consume();
        });
    }

    @Override
    public void updateItem(Node node, boolean empty) {
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
                } else {
                    setTooltip(new Tooltip(stringBuilder.toString()));
                }
                break;
        }

        setGraphic(hBox);
    }
}

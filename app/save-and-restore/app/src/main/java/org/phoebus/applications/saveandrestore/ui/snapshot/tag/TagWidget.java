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
package org.phoebus.applications.saveandrestore.ui.snapshot.tag;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreController;
import org.phoebus.ui.javafx.ImageCache;

/**
 * {@link TagWidget} class provides static methods for {@link Tag} entries in
 * snapshot {@link ContextMenu}.
 *
 * Key feature of the class is to provide custom {@link Node} via {@link CustomMenuItem}
 * having the same width as {@link MenuItem} with the largest width.
 *
 * @author <a href="mailto:changj@frib.msu.edu">Genie Jhang</a>
 */

public class TagWidget {
    public static final Image snapshotTagWithCommentIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/save-and-restore/snapshot-tag.png");
    public static final Image snapshotAddTagWithCommentIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/save-and-restore/snapshot-add_tag.png");
    public static final Image snapshotTrashcanIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/save-and-restore/snapshot-trashcan.png");

    /**
     * A basic {@link CustomMenuItem} looking the same as {@link MenuItem} with a graphic.
     * This is required to provide horizontal alignment of {@link Tag} entries.
     *
     * @param image An image for the icon
     * @param text Item label
     * @return
     */
    private static CustomMenuItem getBaseWidget(Image image, String text) {
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(22);
        imageView.setFitHeight(22);

        Label label = new Label(text);

        HBox hBox = new HBox();
        hBox.getStylesheets().add(SaveAndRestoreApplication.class.getResource("/style.css").toExternalForm());
        hBox.setSpacing(4);
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.getChildren().addAll(imageView, label);

        CustomMenuItem menuItem = new CustomMenuItem();
        menuItem.setContent(hBox);

        menuItem.parentPopupProperty().addListener((observableValue, contextMenu, newContextMenu) -> {
            if (newContextMenu != null) {
                newContextMenu.setOnShown(action -> {
                    Double myMax = 0.;

                    for (int index = 0; index < newContextMenu.getItems().size(); index++) {
                        MenuItem item = newContextMenu.getItems().get(index);
                        if (item instanceof CustomMenuItem) {
                            Node node = ((CustomMenuItem) item).getContent();
                            if (node instanceof HBox) {
                                Double hBoxWidth = ((HBox) ((CustomMenuItem) item).getContent()).getWidth();
                                myMax = hBoxWidth > myMax ? hBoxWidth : myMax;
                            }
                        }
                    }

                    UpdatePrefWidth(newContextMenu, myMax);
                });

                newContextMenu.setOnHiding(action -> UpdatePrefWidth(newContextMenu, 0.0));
            }
        });

        return menuItem;
    }

    /**
     * Add {@link Tag} {@link CustomMenuItem} generator
     *
     * @return Add {@link Tag} {@link CustomMenuItem}
     */
    public static CustomMenuItem AddTagWithCommentMenuItem() {
        return getBaseWidget(snapshotAddTagWithCommentIcon, Messages.contextMenuAddTagWithComment);
    }

    /**
     * Empty {@link Tag} {@link CustomMenuItem} generator
     *
     * @return Empty {@link Tag} {@link CustomMenuItem}
     */
    public static CustomMenuItem NoTagMenuItem() {
        return getBaseWidget(null, Messages.contextMenuNoTagWithComment);
    }

    /**
     * {@link Tag} {@link CustomMenuItem} generator with a customized {@link Node}
     *
     * @param tag {@link Tag} to show
     * @return {@link CustomMenuItem} with a customized {@link Node}
     */
    public static CustomMenuItem TagWithCommentMenuItem(Tag tag) {
        ImageView imageView = new ImageView(snapshotTagWithCommentIcon);
        imageView.setFitWidth(22);
        imageView.setFitHeight(22);

        Label tagName = new Label(tag.getName());
        Label tagComment = new Label(tag.getComment());
        tagComment.setFont(Font.font(Font.getDefault().getSize()*0.9));

        Label tagCreator = new Label(tag.getUserName());
        Label tagCreated = new Label(tag.getCreated().toString());
        tagCreated.setFont(Font.font(Font.getDefault().getSize()*0.9));

        VBox vBoxLeft = new VBox();
        vBoxLeft.getStylesheets().add(SaveAndRestoreApplication.class.getResource("/style.css").toExternalForm());
        vBoxLeft.getChildren().addAll(tagName, tagComment);

        VBox vBoxRight = new VBox();
        vBoxRight.setAlignment(Pos.CENTER_RIGHT);
        vBoxRight.getStylesheets().add(SaveAndRestoreApplication.class.getResource("/style.css").toExternalForm());
        vBoxRight.getChildren().addAll(tagCreator, tagCreated);

        ImageView trashcanImageView = new ImageView(snapshotTrashcanIcon);
        trashcanImageView.setId("trashcan");
        trashcanImageView.setFitWidth(20);
        trashcanImageView.setFitHeight(20);

        Pane spacer = new Pane();
        spacer.setMinWidth(10);
        spacer.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox hBox = new HBox();
        hBox.getStylesheets().add(SaveAndRestoreApplication.class.getResource("/style.css").toExternalForm());
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.setSpacing(4);
        hBox.getChildren().addAll(imageView, vBoxLeft, spacer, vBoxRight, trashcanImageView);

        CustomMenuItem menuItem = new CustomMenuItem();
        menuItem.setContent(hBox);

        menuItem.parentPopupProperty().addListener((observableValue, contextMenu, newContextMenu) -> {
            if (newContextMenu != null) {
                newContextMenu.setOnShown(action -> {
                    Double myMax = 0.;

                    for (int index = 0; index < newContextMenu.getItems().size(); index++) {
                        MenuItem item = newContextMenu.getItems().get(index);
                        if (item instanceof CustomMenuItem) {
                            Node node = ((CustomMenuItem) item).getContent();
                            if (node instanceof HBox) {
                                Double hBoxWidth = ((HBox) ((CustomMenuItem) item).getContent()).getWidth();
                                myMax = hBoxWidth > myMax ? hBoxWidth : myMax;
                            }
                        }
                    }

                    UpdatePrefWidth(newContextMenu, myMax);
                });

                newContextMenu.setOnHiding(action -> UpdatePrefWidth(newContextMenu, 0.0));
            }
        });

        return menuItem;
    }

    /**
     * Updates {@link HBox} width in {@link CustomMenuItem} generated by {@link TagWidget} class
     *
     * @param contextMenu {@link ContextMenu} object containing {@link CustomMenuItem}s created by
     *                    {@link TagWidget} class.
     * @param width Width value to update
     */
    private static void UpdatePrefWidth(ContextMenu contextMenu, Double width) {
        contextMenu.getItems().stream()
                .forEach(item -> {
                    if (item instanceof CustomMenuItem) {
                        Node node = ((CustomMenuItem) item).getContent();
                        if (node instanceof HBox) {
                            ((HBox) ((CustomMenuItem) item).getContent()).setPrefWidth(width);
                        }
                    }
                });
    }
}

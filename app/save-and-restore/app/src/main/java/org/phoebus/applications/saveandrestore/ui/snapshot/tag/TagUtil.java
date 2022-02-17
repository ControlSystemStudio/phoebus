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

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreController;
import org.phoebus.ui.javafx.ImageCache;

/**
 * {@link TagUtil} class provides rich information header of {@link Tag} as {@link Node}
 *
 *  @author Genie Jhang <changj@frib.msu.edu>
 */

public class TagUtil {
    public static final Image snapshotAddTagWithCommentIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/save-and-restore/snapshot-add_tag.png");
    public static final Image snapshotRemoveTagWithCommentIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/save-and-restore/snapshot-remove_tag.png");

    /**
     * Generates a {@link Node} for Add {@link Tag} {@link Dialog}.
     *
     * @param locationString Saveset location {@link String}
     * @param snapshotString Snapshot name {@link String}
     * @return a {@link Node} of {@link GridPane} for information
     */
    public static Node CreateAddHeader(String locationString, String snapshotString) {
        GridPane headerPane = new GridPane();
        headerPane.getStyleClass().add("header-panel");
        headerPane.setHgap(5);
        headerPane.setVgap(5);
        headerPane.setPadding(new Insets(15, 15, 15, 15));

        Label locationLabel = new Label("Location:");
        Label snapshotLabel = new Label("Snapshot:");

        Label location = new Label(locationString);
        Label snapshot = new Label(snapshotString);

        headerPane.add(locationLabel, 0, 0);
        headerPane.add(snapshotLabel, 0, 1);

        headerPane.add(location, 1, 0);
        headerPane.add(snapshot, 1, 1);

        GridPane.setHalignment(locationLabel, HPos.RIGHT);
        GridPane.setHalignment(snapshotLabel, HPos.RIGHT);

        ImageView imageView = new ImageView(snapshotAddTagWithCommentIcon);

        GridPane.setValignment(imageView, VPos.CENTER);

        headerPane.add(imageView, 2, 0, 1, 3);

        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setFillWidth(false);
        labelColumn.setHgrow(Priority.NEVER);
        ColumnConstraints textColumn = new ColumnConstraints();
        textColumn.setFillWidth(true);
        textColumn.setHgrow(Priority.ALWAYS);
        ColumnConstraints graphicColumn = new ColumnConstraints();
        graphicColumn.setFillWidth(false);
        graphicColumn.setHgrow(Priority.NEVER);
        headerPane.getColumnConstraints().setAll(labelColumn, textColumn, graphicColumn);

        return headerPane;
    }

    /**
     * Generates a {@link Node} for Remove {@link Tag} {@link Dialog}.
     *
     * @param locationString Saveset location {@link String}
     * @param snapshotString Snapshot name {@link String}
     * @param tag {@link Tag} object to extract {@link Tag} infomation to show
     * @return a {@link Node} of {@link GridPane} for information
     */
    public static Node CreateRemoveHeader(String locationString, String snapshotString, Tag tag) {
        GridPane headerPane = new GridPane();
        headerPane.getStyleClass().add("header-panel");
        headerPane.setHgap(5);
        headerPane.setVgap(5);
        headerPane.setPadding(new Insets(15, 15, 15, 15));

        Label locationLabel = new Label("Location:");
        Label snapshotLabel = new Label("Snapshot:");
        Label tagNameLabel = new Label("Tag name:");
        Label tagCreatedLabel = new Label("Created on:");
        Label tagCreatorLabel = new Label("Created by:");

        Label location = new Label(locationString);
        Label snapshot = new Label(snapshotString);
        Label tagName = new Label(tag.getName());
        Label tagCreated = new Label(tag.getCreated().toString());
        Label tagCreator = new Label(tag.getUserName());

        headerPane.add(locationLabel, 0, 0);
        headerPane.add(snapshotLabel, 0, 1);
        headerPane.add(tagNameLabel, 0, 2);
        headerPane.add(tagCreatedLabel, 0, 3);
        headerPane.add(tagCreatorLabel, 0, 4);

        headerPane.add(location, 1, 0);
        headerPane.add(snapshot, 1, 1);
        headerPane.add(tagName, 1, 2);
        headerPane.add(tagCreated, 1, 3);
        headerPane.add(tagCreator, 1, 4);

        GridPane.setHalignment(locationLabel, HPos.RIGHT);
        GridPane.setHalignment(snapshotLabel, HPos.RIGHT);
        GridPane.setHalignment(tagNameLabel, HPos.RIGHT);
        GridPane.setHalignment(tagCreatedLabel, HPos.RIGHT);
        GridPane.setHalignment(tagCreatorLabel, HPos.RIGHT);

        ImageView imageView = new ImageView(snapshotRemoveTagWithCommentIcon);

        GridPane.setValignment(imageView, VPos.CENTER);

        headerPane.add(imageView, 2, 0, 1, 5);

        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setFillWidth(false);
        labelColumn.setHgrow(Priority.NEVER);
        ColumnConstraints textColumn = new ColumnConstraints();
        textColumn.setFillWidth(true);
        textColumn.setHgrow(Priority.ALWAYS);
        ColumnConstraints graphicColumn = new ColumnConstraints();
        graphicColumn.setFillWidth(false);
        graphicColumn.setHgrow(Priority.NEVER);
        headerPane.getColumnConstraints().setAll(labelColumn, textColumn, graphicColumn);


        return headerPane;
    }
}

/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.pv;

import java.util.Collection;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;
import org.phoebus.pv.RefCountMap.ReferencedEntry;
import org.phoebus.ui.application.Messages;
import org.phoebus.ui.javafx.ImageCache;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;

/** Table that lists PVs, their reference count etc.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVList extends BorderPane
{
    /** TableView row for one PV */
    private static class PVInfo
    {
        final StringProperty name;
        final BooleanProperty connected;
        final IntegerProperty references;

        public PVInfo(final String name, final boolean connected, final int references)
        {
            this.name = new SimpleStringProperty(name);
            this.connected = new SimpleBooleanProperty(connected);
            this.references = new SimpleIntegerProperty(references);
        }
    }

    private final TableView<PVInfo> table = new TableView<>();

    private static final Image connected_icon = new Image(PVList.class.getResourceAsStream("/icons/connected.png"));
    private static final Image disconnected_icon = new Image(PVList.class.getResourceAsStream("/icons/disconnected.png"));

    private static class ConnectedCell extends TableCell<PVInfo, Boolean>
    {
        @Override
        protected void updateItem(final Boolean connected, final boolean empty)
        {
            super.updateItem(connected, empty);
            if (empty)
                setGraphic(null);
            else
                setGraphic(new ImageView(connected ? connected_icon : disconnected_icon));
        }
    }

    public PVList()
    {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label(Messages.PVListPlaceholder));
        createTableColumns();

        Node toolbar = createToolbar();
        setAlignment(toolbar, Pos.CENTER_RIGHT);
        setMargin(toolbar, new Insets(5, 5, 0, 5));
        setMargin(table, new Insets(5));
        setTop(toolbar);
        setCenter(table);

        triggerRefresh();
    }

    private Node createToolbar()
    {
        final Button refresh = new Button();
        refresh.setGraphic(ImageCache.getImageView(PVList.class, "/icons/refresh.png"));
        refresh.setTooltip(new Tooltip(Messages.PVListRefreshTT));
        refresh.setOnAction(event -> triggerRefresh());
        return refresh;
        // If more buttons are added:
        // return new HBox(5, refresh);
    }

    private void createTableColumns()
    {
        final TableColumn<PVInfo, Boolean> conn_col = new TableColumn<>(Messages.PVListTblConnected);
        conn_col.setCellFactory(col -> new ConnectedCell());
        conn_col.setCellValueFactory(cell -> cell.getValue().connected);
        conn_col.setMinWidth(20.0);
        conn_col.setPrefWidth(300.0);
        conn_col.setMaxWidth(500.0);
        table.getColumns().add(conn_col);

        final TableColumn<PVInfo, String> name_col = new TableColumn<>(Messages.PVListTblPVName);
        name_col.setCellValueFactory(cell -> cell.getValue().name);
        table.getColumns().add(name_col);

        final TableColumn<PVInfo, Number> ref_col = new TableColumn<>(Messages.PVListTblReferences);
        ref_col.setCellValueFactory(cell -> cell.getValue().references);
        ref_col.setMaxWidth(500.0);
        table.getColumns().add(ref_col);
    }

    private void triggerRefresh()
    {
        JobManager.schedule(Messages.PVListJobName, monitor ->
        {
            // Background thread to list information
            final ObservableList<PVInfo> items = FXCollections.observableArrayList();
            final Collection<ReferencedEntry<PV>> refs = PVPool.getPVReferences();
            for (ReferencedEntry<PV> ref : refs)
            {
                final PV pv = ref.getEntry();
                items.add(new PVInfo(pv.getName(), pv.read() != null, ref.getReferences()));
            }

            // Update UI
            Platform.runLater(() ->
            {
                table.getItems().clear();
                table.getItems().addAll(items);
            });
        });
    }
}

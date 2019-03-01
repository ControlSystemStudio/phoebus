/**
 * Copyright (C) 2019 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.phoebus.applications.saveandrestore.ui;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.model.SnapshotEntry;
import org.phoebus.applications.saveandrestore.ui.model.VNoData;
import org.phoebus.applications.saveandrestore.ui.model.VSnapshot;
import se.esss.ics.masar.model.*;

import java.util.*;
import java.util.concurrent.Executor;


public class SnapshotController {

    @FXML
    private TextArea commentTextArea;

    @FXML
    private TextField createdBy;

    @FXML
    private TextField createdDate;

    @FXML
    private BorderPane borderPane;

    private Table table;

    private SaveAndRestoreService service;
    private SimpleStringProperty commentTextProperty = new SimpleStringProperty();
    private SimpleStringProperty createdByTextProperty = new SimpleStringProperty();
    private SimpleStringProperty createdDateTextProperty = new SimpleStringProperty();

    private List<VSnapshot> snapshots = new ArrayList<>();
    private final Map<String, String> readbacks = new HashMap<>();
    private final Map<String, TableEntry> items = new LinkedHashMap<>();
    private final BooleanProperty snapshotRestorableProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty snapshotSaveableProperty = new SimpleBooleanProperty(false);
    private final ObjectProperty<VSnapshot> baseSnapshotProperty = new SimpleObjectProperty<>(null);


    private int snapshotId;

    private static Executor UI_EXECUTOR = Platform::runLater;

    public SnapshotController(){
        service = SaveAndRestoreService.getInstance();
    }

    @FXML
    public void initialize() {
        commentTextArea.textProperty().bindBidirectional(commentTextProperty);
        createdBy.textProperty().bind(createdByTextProperty);
        createdDate.textProperty().bind(createdDateTextProperty);

        table = new Table(this);

        borderPane.setCenter(table);

    }

    public String loadData(Node treeNode){
        if(treeNode.getNodeType().equals(NodeType.SNAPSHOT)){
            return loadSnapshot(treeNode.getId());
        }
        else{
            return null;
        }

    }

    private String loadSnapshot(int id){

        try {
            Snapshot snapshot = service.getSnapshot(id);
            Config config = service.getSaveSet(snapshot.getParentId());

            snapshotId = snapshot.getId();
            commentTextProperty.set(snapshot.getComment());
            createdDateTextProperty.set(snapshot.getCreated().toString());
            createdByTextProperty.set(snapshot.getUserName());

            VSnapshot vSnapshot =
                    new VSnapshot(config, snapshotItemsToSnapshotEntries(snapshot.getSnapshotItems()));
            List<TableEntry> tableEntries = setSnapshot(vSnapshot);

            table.updateTable(tableEntries, snapshots, false, false);

            return  config.getName() + " (" + snapshot.getCreated() + ")";

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    @FXML
    public void restore(ActionEvent event){

    }

    @FXML
    public void takeSnapshot(ActionEvent event){
        try {
            Snapshot snapshot = service.takeSnapshot(snapshotId);
            loadSnapshot(snapshot.getId());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void saveSnapshot(ActionEvent event){

    }

    private List<TableEntry> setSnapshot(VSnapshot data) {

        List<SnapshotEntry> entries = data.getEntries();
        synchronized (snapshots) {
            snapshots.add(data);
        }
        UI_EXECUTOR.execute(() -> snapshotRestorableProperty.set(data.getSnapshot().isPresent()));
        String name;
        TableEntry e;
        SnapshotEntry entry;
        for (int i = 0; i < entries.size(); i++) {
            entry = entries.get(i);
            e = new TableEntry();
            name = entry.getPVName();
            e.idProperty().setValue(i + 1);
            e.pvNameProperty().setValue(name);
            e.selectedProperty().setValue(entry.isSelected());
            e.setSnapshotValue(entry.getValue(), 0);
            e.setStoredReadbackValue(entry.getReadbackValue(), 0);
            items.put(name, e);
            readbacks.put(name, entry.getReadbackName());
            e.readbackNameProperty().set(entry.getReadbackName());
            e.readOnlyProperty().set(entry.isReadOnly());
        }
        //connectPVs();
        UI_EXECUTOR.execute(
                () -> snapshotSaveableProperty.set(data.isSaveable()));
        //updateThresholds();
        UI_EXECUTOR.execute(() -> baseSnapshotProperty.set(data));

        //List<TableEntry> ret = filter(items.values(), filter);
        //pvsForDisposal.values().forEach(p -> p.dispose());
        //pvsForDisposal.clear();
        return new ArrayList(items.values());
    }

    private List<SnapshotEntry> snapshotItemsToSnapshotEntries(List<SnapshotItem> snapshotItems){
        List<SnapshotEntry> snapshotEntries = new ArrayList<>();
        for(SnapshotItem snapshotItem : snapshotItems){
            SnapshotEntry snapshotEntry =
                    new SnapshotEntry(snapshotItem.getPvName(), snapshotItem.isFetchStatus() ? snapshotItem.getValue() : VNoData.INSTANCE, true);
            snapshotEntries.add(snapshotEntry);
        }

        return snapshotEntries;
    }
}

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
package org.phoebus.applications.saveandrestore.ui.snapshot;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeTableView;
import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.ui.SingleListenerBooleanProperty;
import org.phoebus.applications.saveandrestore.ui.model.VTypePair;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link TableEntry} wrapper for supporting {@link TreeTableView} for Snapshot PVs
 *
 * @author <a href="mailto:changj@frib.msu.edu">Genie Jhang</a>
 */

public class TreeTableEntry {
    public String name;
    public TableEntry tableEntry;
    public TreeTableEntry parent;
    public final Map<String, TreeTableEntry> children;
    public boolean folder = false;
    public CheckBoxTreeItem<TreeTableEntry> parentCBTI;
    public CheckBoxTreeItem<TreeTableEntry> cbti;
    public SimpleBooleanProperty selected = new SimpleBooleanProperty(this, "selected");
    public SimpleBooleanProperty indeterminate = new SimpleBooleanProperty(this, "indeterminate");
    public BooleanProperty allSelectedCheckBoxProperty = null;

    private final ChangeListener<Boolean> tableEntrySelectedChangeListener_1 = (observableValue, oldValue, newValue) -> selected.set(newValue);
    private final ChangeListener<Boolean> tableEntrySelectedChangeListener_2 = (observableValue, oldValue, newValue) -> cbti.setSelected(newValue);
    private ChangeListener<Boolean> tableEntrySelectedChangeListener_3 = null;
    private final ChangeListener<Boolean> cbtiSelectedChangeListener = (observableValue, oldValue, newValue) -> tableEntry.selectedProperty().set(newValue);
    private ChangeListener<Boolean> equalPropertyChangeListener = null;

    public void initializeEqualPropertyChangeListener(SnapshotController controller) {
        equalPropertyChangeListener = (observableValue, oldValue, newValue) -> {
            if (controller.isHideEqualItems()) {
                if (newValue) {
                    this.remove();
                } else {
                    this.add();
                }
            }
        };
        ((SingleListenerBooleanProperty) tableEntry.liveStoredEqualProperty()).forceAddListener(equalPropertyChangeListener);
    }

    public void initializeChangeListenerForColumnHeaderCheckBox(CheckBox allSelectedCheckBox) {
        allSelectedCheckBoxProperty = allSelectedCheckBox.selectedProperty();

        tableEntrySelectedChangeListener_3 = (observableValue, oldValue, newValue) -> allSelectedCheckBoxProperty.set(newValue ? allSelectedCheckBoxProperty.get() : false);

        ((SingleListenerBooleanProperty) tableEntry.selectedProperty()).forceAddListener(tableEntrySelectedChangeListener_3);
    }

    public void update(TableEntry tableEntry) {
        this.tableEntry.selectedProperty().removeListener(tableEntrySelectedChangeListener_1);

        if (!folder && !tableEntry.readOnlyProperty().get()) {
            this.tableEntry.selectedProperty().removeListener(tableEntrySelectedChangeListener_2);
            this.tableEntry.selectedProperty().removeListener(tableEntrySelectedChangeListener_3);
            this.tableEntry.liveStoredEqualProperty().removeListener(equalPropertyChangeListener);
            cbti.selectedProperty().removeListener(cbtiSelectedChangeListener);
        }

        this.tableEntry = tableEntry;

        ((SingleListenerBooleanProperty) tableEntry.selectedProperty()).forceAddListener(tableEntrySelectedChangeListener_1);

        if (!folder && !tableEntry.readOnlyProperty().get()) {
            ((SingleListenerBooleanProperty) tableEntry.selectedProperty()).forceAddListener(tableEntrySelectedChangeListener_2);
            ((SingleListenerBooleanProperty) tableEntry.selectedProperty()).forceAddListener(tableEntrySelectedChangeListener_3);
            ((SingleListenerBooleanProperty) tableEntry.liveStoredEqualProperty()).forceAddListener(equalPropertyChangeListener);
            cbti.selectedProperty().addListener(cbtiSelectedChangeListener);
        }
    }

    public TreeTableEntry(String name, TableEntry tableEntry, TreeTableEntry parent) {
        this.name = name;
        this.tableEntry = tableEntry;

        if (tableEntry == null) {
            this.folder = true;
        } else {
            ((SingleListenerBooleanProperty) tableEntry.selectedProperty()).forceAddListener(tableEntrySelectedChangeListener_1);
        }

        if (parent != null) {
            this.parent = parent;
            this.parent.children.put(name, this);
            this.parentCBTI = parent.cbti;
        }

        this.children = new HashMap<>();
        this.cbti = new CheckBoxTreeItem<>(this);

        this.cbti.selectedProperty().bindBidirectional(selected);
        this.cbti.indeterminateProperty().bindBidirectional(indeterminate);

        selected.set(true);

        if (!folder && !tableEntry.readOnlyProperty().get()) {
            ((SingleListenerBooleanProperty) tableEntry.selectedProperty()).forceAddListener(tableEntrySelectedChangeListener_2);
            this.cbti.selectedProperty().addListener(cbtiSelectedChangeListener);
        }
    }

    public void add() {
        if (parent == null) {
            return;
        }

        if (!parentCBTI.getChildren().contains(cbti)) {
            if (parent.name == this.name) {
                parent.parentCBTI.getChildren().add(cbti);

                parent.parent.add();
            } else {
                parentCBTI.getChildren().add(cbti);

                parent.add();
            }
        }
    }

    public void remove() {
        if (parent == null) {
            return;
        }

        if (parent.name == this.name) {
            if (parent.parentCBTI.getChildren().contains(cbti)) {
                parent.parentCBTI.getChildren().remove(cbti);

                parent.parent.remove();
            }
        } else {
            if (parentCBTI.getChildren().contains(cbti)) {
                parentCBTI.getChildren().remove(cbti);

                if (parentCBTI.getChildren().isEmpty()) {
                    parent.remove();
                }
            }
        }
    }

    public void clear() {
        if (!cbti.getChildren().isEmpty()) {
            cbti.getChildren().forEach(child -> child.getValue().clear());
        }

        cbti.getChildren().clear();
    }

    public StringProperty pvNameProperty() {
        if (folder) {
            return null;
        }

        return tableEntry.pvNameProperty();
    }

    public ObjectProperty<VType> snapshotValProperty() {
        if (folder) {
            return null;
        }
        return tableEntry.snapshotValProperty();
    }

    public ObjectProperty<VType> liveValueProperty() {
        if (folder) {
            return null;
        }
        return tableEntry.liveValueProperty();
    }

    public StringProperty readbackNameProperty() {
        if (folder) {
            return null;
        }
        return tableEntry.readbackNameProperty();
    }

    public ObjectProperty<Instant> timestampProperty() {
        if (folder) {
            return null;
        }
        return tableEntry.timestampProperty();
    }

    public StringProperty statusProperty() {
        if (folder) {
            return null;
        }
        return tableEntry.statusProperty();
    }

    public StringProperty severityProperty() {
        if (folder) {
            return null;
        }
        return tableEntry.severityProperty();
    }

    public ObjectProperty<VTypePair> storedReadbackProperty() {
        if (folder) {
            return null;
        }
        return tableEntry.storedReadbackProperty();
    }

    public ObjectProperty<VTypePair> liveReadbackProperty() {
        if (folder) {
            return null;
        }
        return tableEntry.liveReadbackProperty();
    }
}

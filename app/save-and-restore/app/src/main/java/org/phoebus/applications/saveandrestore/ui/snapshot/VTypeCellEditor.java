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

package org.phoebus.applications.saveandrestore.ui.snapshot;

import javafx.beans.binding.Bindings;
import javafx.scene.control.TableRow;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.StringConverter;
import org.epics.vtype.Alarm;
import org.epics.vtype.EnumDisplay;
import org.epics.vtype.Time;
import org.epics.vtype.VEnum;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.ui.MultitypeTableCell;
import org.phoebus.saveandrestore.util.Utilities;
import org.phoebus.saveandrestore.util.VNoData;
import org.phoebus.applications.saveandrestore.ui.VTypePair;
import org.phoebus.core.vtypes.VDisconnectedData;
import org.phoebus.ui.javafx.JFXUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * <code>VTypeCellEditor</code> is an editor type for {@link org.epics.vtype.VType} or {@link org.phoebus.applications.saveandrestore.ui.VTypePair}, which allows editing the
 * value as a string.
 *
 * @param <T> {@link org.epics.vtype.VType} or {@link org.phoebus.applications.saveandrestore.ui.VTypePair}
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 */
public class VTypeCellEditor<T> extends MultitypeTableCell<TableEntry, T> {
    private final Tooltip tooltip = new Tooltip();

    VTypeCellEditor() {

        setConverter(new StringConverter<>() {
            @Override
            public String toString(T item) {
                if (item == null) {
                    return "";
                } else if (item instanceof VNumber) {
                    return ((VNumber) item).getValue().toString();
                } else if (item instanceof VNumberArray) {
                    return ((VNumberArray) item).getData().toString();
                } else if (item instanceof VEnum) {
                    return ((VEnum) item).getValue();
                } else if (item instanceof VTypePair) {
                    VType value = ((VTypePair) item).value;
                    if (value instanceof VNumber) {
                        return ((VNumber) value).getValue().toString();
                    } else if (value instanceof VNumberArray) {
                        return ((VNumberArray) value).getData().toString();
                    } else if (value instanceof VEnum) {
                        return ((VEnum) value).getValue();
                    } else {
                        return value.toString();
                    }
                } else {
                    return item.toString();
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            public T fromString(String string) {
                T item = getItem();
                try {
                    if (string == null) {
                        return item;
                    } else if (item instanceof VType) {
                        return (T) Utilities.valueFromString(string, (VType) item);
                    } else if (item instanceof VTypePair t) {
                        if (t.value instanceof VDisconnectedData) {
                            return (T) new VTypePair(t.base, Utilities.valueFromString(string, t.base),
                                    t.threshold);
                        } else {
                            return (T) new VTypePair(t.base, Utilities.valueFromString(string, t.value),
                                    t.threshold);
                        }
                    } else {
                        return item;
                    }
                } catch (IllegalArgumentException e) {
                    return item;
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean isTextFieldType() {
        T item = getItem();
        if (item instanceof VEnum value) {
            getItems().clear();

            List<String> labels = value.getDisplay().getChoices();
            List<T> values = new ArrayList<>(labels.size());
            for (int i = 0; i < labels.size(); i++) {
                values.add((T) VEnum.of(i, EnumDisplay.of(labels), Alarm.none(), Time.now()));
            }
            setItems(values);

            return false;
        } else if (item instanceof VTypePair v) {
            VType type = v.value;
            if (type instanceof VEnum value) {
                getItems().clear();

                List<String> labels = value.getDisplay().getChoices();
                List<T> values = new ArrayList<>(labels.size());
                for (int i = 0; i < labels.size(); i++) {
                    values.add(
                            (T) new VTypePair(v.base, VEnum.of(i, EnumDisplay.of(labels), Alarm.none(), Time.now()), v.threshold));
                }
                setItems(values);

                return false;
            }
        }
        return true;
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        updateItem(getItem(), isEmpty());
    }

    @Override
    public void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        setStyle(TableCellColors.REGULAR_CELL_STYLE);
        if (item == null || empty) {
            setText("");
            setTooltip(null);
        } else {
            if (item == VDisconnectedData.INSTANCE) {
                setText(VDisconnectedData.DISCONNECTED);
                setStyle(TableCellColors.DISCONNECTED_STYLE);
                tooltip.setText("No Value Available");
                setTooltip(tooltip);
            } else if (item == VNoData.INSTANCE) {
                setText(item.toString());
                tooltip.setText("No Value Available");
                setTooltip(tooltip);
            } else if (item instanceof VType) {
                setText(Utilities.valueToString((VType) item));
                tooltip.setText(item.toString());
                setTooltip(tooltip);
            } else if (item instanceof VTypePair pair) {
                if (pair.value == VDisconnectedData.INSTANCE) {
                    setText(VDisconnectedData.DISCONNECTED);
                    setStyle(TableCellColors.DISCONNECTED_STYLE);
                } else if (pair.value == VNoData.INSTANCE) {
                    setText(pair.value.toString());
                } else {
                    setText(Utilities.valueToString(pair.value));
                }

                tooltip.setText(item.toString());
                setTooltip(tooltip);
            }
        }
        TableRow tableRow = getTableRow();
        // If this is a TableEntry row and read-only it should not be editable.
        if (tableRow != null) {
            if (tableRow.getItem() != null && tableRow.getItem() instanceof TableEntry tableEntry) {
                setEditable(tableEntry.readOnlyProperty().not().get());
            }
        }
    }
}

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

import javafx.scene.control.Tooltip;
import org.epics.vtype.VEnumArray;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VStringArray;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.ui.VTypePair;
import org.phoebus.applications.saveandrestore.ui.snapshot.compare.ComparisonDialog;
import org.phoebus.core.vtypes.VDisconnectedData;
import org.phoebus.saveandrestore.util.Utilities;
import org.phoebus.saveandrestore.util.VNoData;
import org.phoebus.ui.dialog.DialogHelper;

import java.util.Formatter;

/**
 * A dedicated CellEditor for displaying delta only.
 * TODO can be simplified further
 *
 * @param <T>
 * @author Kunal Shroff
 */
public class VDeltaCellEditor<S, T> extends VTypeCellEditor<S, T> {

    private final Tooltip tooltip = new Tooltip();

    private boolean showDeltaPercentage = false;

    protected void setShowDeltaPercentage(boolean showDeltaPercentage) {
        this.showDeltaPercentage = showDeltaPercentage;
    }

    public VDeltaCellEditor() {
        super();
    }

    @Override
    public void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        setStyle(TableCellColors.REGULAR_CELL_STYLE);
        if (item == null || empty) {
            setText("");
            setTooltip(null);
            setGraphic(null);
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
            } else if (item instanceof VTypePair pair) {
                if (pair.value == VDisconnectedData.INSTANCE) {
                    setText(VDisconnectedData.DISCONNECTED);
                    setStyle(TableCellColors.DISCONNECTED_STYLE);
                } else if (pair.value == VNoData.INSTANCE) {
                    setText(pair.value.toString());
                } else if(pair.base == VNoData.INSTANCE){
                    setText(VNoData.INSTANCE.toString());
                }
                else {
                    Utilities.VTypeComparison vtc = Utilities.deltaValueToString(pair.value, pair.base, pair.threshold);
                    if (vtc.getValuesEqual() != 0 &&
                            (pair.base instanceof VNumberArray ||
                                    pair.base instanceof VStringArray ||
                                    pair.base instanceof VEnumArray)) {
                        TableEntry tableEntry = (TableEntry) getTableRow().getItem();
                        setText(Messages.clickToCompare);
                        setStyle(TableCellColors.ALARM_MAJOR_STYLE);
                        setOnMouseClicked(e -> {
                            ComparisonDialog comparisonDialog = new ComparisonDialog(tableEntry.getSnapshotVal().get(), tableEntry.getConfigPv().getPvName());
                            DialogHelper.positionDialog(comparisonDialog, getTableView(), -400, -400);
                            comparisonDialog.show();
                        });
                    } else {
                        String percentage = Utilities.deltaValueToPercentage(pair.value, pair.base);
                        if (!percentage.isEmpty() && showDeltaPercentage) {
                            Formatter formatter = new Formatter();
                            setText(formatter.format("%g", Double.parseDouble(vtc.getString())) + " (" + percentage + ")");
                        } else {
                            setText(vtc.getString());
                        }
                        if (!vtc.isWithinThreshold()) {
                            setStyle(TableCellColors.ALARM_MAJOR_STYLE);
                        }
                    }
                }
                tooltip.setText(item.toString());
                setTooltip(tooltip);
            }
        }
    }
}

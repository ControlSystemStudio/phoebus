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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.phoebus.saveandrestore.util.Utilities;
import org.phoebus.saveandrestore.util.VNoData;
import org.phoebus.applications.saveandrestore.ui.VTypePair;
import org.phoebus.core.vtypes.VDisconnectedData;

import java.util.Formatter;

/**
 * A dedicated CellEditor for displaying delta only.
 * TODO can be simplified further
 *
 * @param <T>
 * @author Kunal Shroff
 */
public class VDeltaCellEditor<T> extends VTypeCellEditor<T> {

    private static final Image WARNING_IMAGE = new Image(
            SnapshotController.class.getResourceAsStream("/icons/hprio_tsk.png"));
    private static final Image DISCONNECTED_IMAGE = new Image(
            SnapshotController.class.getResourceAsStream("/icons/showerr_tsk.png"));
    private final Tooltip tooltip = new Tooltip();

    private boolean showDeltaPercentage = false;

    protected void setShowDeltaPercentage(boolean showDeltaPercentage) {
        this.showDeltaPercentage = showDeltaPercentage;
    }

    VDeltaCellEditor() {
        super();
    }

    @Override
    public void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        getStyleClass().remove("diff-cell");
        if (item == null || empty) {
            setText("");
            setTooltip(null);
            setGraphic(null);
        } else {
            if (item == VDisconnectedData.INSTANCE) {
                setText(VDisconnectedData.DISCONNECTED);
                setGraphic(new ImageView(DISCONNECTED_IMAGE));
                tooltip.setText("No Value Available");
                setTooltip(tooltip);
                getStyleClass().add("diff-cell");
            } else if (item == VNoData.INSTANCE) {
                setText(item.toString());
                tooltip.setText("No Value Available");
                setTooltip(tooltip);
            } else if (item instanceof VTypePair pair) {
                if (pair.value == VDisconnectedData.INSTANCE) {
                    setText(VDisconnectedData.DISCONNECTED);
                    if (pair.base != VDisconnectedData.INSTANCE) {
                        getStyleClass().add("diff-cell");
                    }
                    setGraphic(new ImageView(DISCONNECTED_IMAGE));
                } else if (pair.value == VNoData.INSTANCE) {
                    setText(pair.value.toString());
                } else {
                    Utilities.VTypeComparison vtc = Utilities.deltaValueToString(pair.value, pair.base, pair.threshold);
                    String percentage = Utilities.deltaValueToPercentage(pair.value, pair.base);
                    if (!percentage.isEmpty() && showDeltaPercentage) {
                        Formatter formatter = new Formatter();
                        setText(formatter.format("%g", Double.parseDouble(vtc.getString())) + " (" + percentage + ")");
                    } else {
                        setText(vtc.getString());
                    }
                    if (!vtc.isWithinThreshold()) {
                        getStyleClass().add("diff-cell");
                        setGraphic(new ImageView(WARNING_IMAGE));
                    }
                }

                tooltip.setText(item.toString());
                setTooltip(tooltip);
            }
        }
    }
}

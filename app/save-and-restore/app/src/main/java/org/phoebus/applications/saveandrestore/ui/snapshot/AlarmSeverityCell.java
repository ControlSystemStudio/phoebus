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

import javafx.scene.control.TableCell;
import org.epics.vtype.AlarmSeverity;
import org.phoebus.ui.javafx.JFXUtil;
import org.phoebus.ui.pv.SeverityColors;

public class AlarmSeverityCell extends TableCell<TableEntry, String> {

    @Override
    public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setStyle("-fx-text-fill: black;  -fx-background-color: transparent");
            setText(null);
        } else if (item == null || "---".equals(item)) {
            setStyle("-fx-text-fill: black;  -fx-background-color: transparent");
            setText("---");
        } else {
            setText(item);
            AlarmSeverity alarmSeverity = AlarmSeverity.valueOf(item);
            setStyle("-fx-alignment: center; -fx-border-color: transparent; -fx-border-width: 2 0 2 0; -fx-background-insets: 2 0 2 0; -fx-text-fill: " +
                    JFXUtil.webRGB(SeverityColors.getTextColor(alarmSeverity)) + ";  -fx-background-color: " + JFXUtil.webRGB(SeverityColors.getBackgroundColor(alarmSeverity)));
        }
    }
}

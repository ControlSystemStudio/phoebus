/*
 * *
 *  * Copyright (C) 2019 European Spallation Source ERIC.
 *  * <p>
 *  * This program is free software; you can redistribute it and/or
 *  * modify it under the terms of the GNU General Public License
 *  * as published by the Free Software Foundation; either version 2
 *  * of the License, or (at your option) any later version.
 *  * <p>
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  * <p>
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package org.phoebus.applications.saveandrestore.datamigration.git;

import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.ui.model.SnapshotEntry;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class FileReaderHelper {

    public static List<ConfigPv> readSaveSet(InputStream file) throws Exception{

        SaveSetContent saveSetContent = FileUtilities.readFromSaveSet(file);
        List<SaveSetEntry> entries = saveSetContent.getEntries();
        List<ConfigPv> configPVs = new ArrayList<>();
        for(SaveSetEntry entry : entries){
            ConfigPv configPv = ConfigPv.builder()
                    .pvName(entry.getPVName())
                    .readbackPvName((entry.getReadback() == null || entry.getReadback().isEmpty()) ? null : (entry.getReadback()))
                    .readOnly(entry.isReadOnly())
                    .build();
            configPVs.add(configPv);
        }
        return configPVs;
    }

    public static List<SnapshotItem> readSnapshot(InputStream file) throws Exception {

        SnapshotContent snapshotContent = FileUtilities.readFromSnapshot(file);
        List<SnapshotEntry> snapshotEntries = snapshotContent.getEntries();
        List<SnapshotItem> snapshotItems = new ArrayList<>();
        for (SnapshotEntry snapshotEntry : snapshotEntries) {
            ConfigPv configPv = ConfigPv.builder()
                    .pvName(snapshotEntry.getPVName())
                    .readbackPvName((snapshotEntry.getReadbackName() == null || snapshotEntry.getReadbackName().isEmpty()) ? null : (snapshotEntry.getReadbackName()))
                    .readOnly(snapshotEntry.isReadOnly())
                    .build();

            SnapshotItem snapshotItem = SnapshotItem.builder()
                    .configPv(configPv)
                    .value(snapshotEntry.getValue())
                    .readbackValue(snapshotEntry.getReadbackValue())
                    .build();

            snapshotItems.add(snapshotItem);
        }
        return snapshotItems;
    }
}

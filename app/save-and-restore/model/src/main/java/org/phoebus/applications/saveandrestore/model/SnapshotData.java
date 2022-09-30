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
 */

package org.phoebus.applications.saveandrestore.model;

import java.util.List;

/**
 * Class holding data particular to a save-n-restore snapshot {@link Node}.
 */
public class SnapshotData {

    /**
     * This <b>must</b> be set to the same unique id as the {@link Node} object
     * mapping to the snapshot {@link Node}.
     */
    private String uniqueId;

    private List<SnapshotItem> snapshotItems;

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public List<SnapshotItem> getSnapshotItems() {
        return snapshotItems;
    }

    public void setSnasphotItems(List<SnapshotItem> snapshotItems) {
        this.snapshotItems = snapshotItems;
    }

    public static SnapshotData clone(SnapshotData snapshotDataToClone){
        SnapshotData snapshotData = new SnapshotData();
        snapshotData.setSnasphotItems(snapshotDataToClone.getSnapshotItems());
        return snapshotData;
    }
}

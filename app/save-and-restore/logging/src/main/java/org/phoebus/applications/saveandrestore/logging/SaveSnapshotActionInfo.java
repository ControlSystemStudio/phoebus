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

package org.phoebus.applications.saveandrestore.logging;

import java.util.Date;

/**
 * Object wrapping information about a save snapshot action.
 */
public class SaveSnapshotActionInfo {

    private String snapshotName;
    private Date snapshotCreatedDate;
    private String comment;
    private String snapshotUniqueId;
    private String actionPerformedBy;

    public String getSnapshotName() {
        return snapshotName;
    }

    public void setSnapshotName(String snapshotName) {
        this.snapshotName = snapshotName;
    }

    public Date getSnapshotCreatedDate() {
        return snapshotCreatedDate;
    }

    public void setSnapshotCreatedDate(Date snapshotCreatedDate) {
        this.snapshotCreatedDate = snapshotCreatedDate;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getSnapshotUniqueId() {
        return snapshotUniqueId;
    }

    public void setSnapshotUniqueId(String snapshotUniqueId) {
        this.snapshotUniqueId = snapshotUniqueId;
    }

    public String getActionPerformedBy() {
        return actionPerformedBy;
    }

    public void setActionPerformedBy(String actionPerformedBy) {
        this.actionPerformedBy = actionPerformedBy;
    }
}

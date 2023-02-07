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

package org.phoebus.applications.saveandrestore.script;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * A wrapper for various items describing the outcome of a restore operation.
 */
public class RestoreReport {

    /**
     * Unique id of a snaphsot
     */
    private String snapshotId;
    /**
     * "Path" if a snapshot, e.g. /folder1/folder2/saveset1/snapshot1
     */
    private String snapshotPath;
    /**
     * Date when the snapshot was restored
     */
    private Date restoreDate;
    /**
     * Map of PV names and the values to which they were restored.
     */
    private Map<String, Object> restoredPVs;
    /**
     * List of PVs that failed to restore. May be empty.
     */
    private List<String> nonRestoredPVs;
    /**
     * List of PVs that were restored in case one or several write operations fail and if rollback was requested,
     * see {@link org.phoebus.applications.saveandrestore.script.SaveAndRestoreScriptUtil}. May be <code>null</code>.
     */
    private Map<String, Object> rolledBackPVs;

    public RestoreReport(Date restoreDate, String snapshotId, String snapshotPath) {
        this.restoreDate = restoreDate;
        this.snapshotId = snapshotId;
        this.snapshotPath = snapshotPath;
    }

    public void setRolledBackPVs(Map<String, Object> rolledBackPVs) {
        this.rolledBackPVs = rolledBackPVs;
    }

    public void setRestoreDate(Date restoreDate) {
        this.restoreDate = restoreDate;
    }

    public Map<String, Object> getRestoredPVs() {
        return restoredPVs;
    }

    public void setRestoredPVs(Map<String, Object> restoredPVs) {
        this.restoredPVs = restoredPVs;
    }

    public List<String> getNonRestoredPVs() {
        return nonRestoredPVs;
    }

    public void setNonRestoredPVs(List<String> nonRestoredPVs) {
        this.nonRestoredPVs = nonRestoredPVs;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SnapshotData id: ").append(snapshotId).append(System.lineSeparator());
        stringBuilder.append("SnapshotData path: ").append(snapshotPath).append(System.lineSeparator());
        stringBuilder.append("Restore date: ").append(restoreDate).append(System.lineSeparator());
        stringBuilder.append("Restored PVs: ").append(System.lineSeparator());
        restoredPVs.entrySet().stream().forEach(entry -> {
            stringBuilder.append(entry.getKey()).append(" : ").append(entry.getValue()).append(System.lineSeparator());
        });
        if (!nonRestoredPVs.isEmpty()) {
            stringBuilder.append("Non-restored PVs:").append(System.lineSeparator());
            nonRestoredPVs.stream().forEach(pvName -> {
                stringBuilder.append(pvName).append(System.lineSeparator());
            });
        }
        if (rolledBackPVs != null && !rolledBackPVs.isEmpty()) {
            stringBuilder.append("Rolled-back PVs:").append(System.lineSeparator());
            rolledBackPVs.entrySet().stream().forEach(entry -> {
                stringBuilder.append(entry.getKey()).append(" : ").append(entry.getValue()).append(System.lineSeparator());
            });
        }

        return stringBuilder.toString();
    }
}

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

import org.phoebus.framework.adapter.AdapterFactory;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogEntryImpl.LogEntryBuilder;
import org.phoebus.logbook.PropertyImpl;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.phoebus.logbook.LogEntryImpl.LogEntryBuilder.log;

/**
 * Adapts save/restore action information to a log entry.
 */
public class SaveAndRestoreActionAdapterFactory implements AdapterFactory {

    @Override
    public Class getAdaptableObject() {
        return SaveSnapshotActionInfo.class;
    }

    @Override
    public List<? extends Class> getAdapterList() {
        return Arrays.asList(LogEntry.class);
    }

    @Override
    public <T> Optional<T> adapt(Object adaptableObject, Class<T> adapterType) {
        Map<String, String> map = new HashMap<>();
        map.put("file", "file:/" + ((SaveSnapshotActionInfo)adaptableObject).getSnapshotUniqueId() + "?app=saveandrestore");
        map.put("name",  ((SaveSnapshotActionInfo)adaptableObject).getSnapshotName());
        if(adaptableObject instanceof RestoreSnapshotActionInfo){
            RestoreSnapshotActionInfo restoreSnapshotActionInfo = (RestoreSnapshotActionInfo)adaptableObject;
            String title = restoreSnapshotActionInfo.isGolden() ?
                    MessageFormat.format(Messages.GoldenSnapshotRestored, restoreSnapshotActionInfo.getSnapshotName()) :
                    MessageFormat.format(Messages.SnapshotRestored, restoreSnapshotActionInfo.getSnapshotName());
            LogEntryBuilder log = log()
                    .title(title)
                    .appendDescription(getBody(restoreSnapshotActionInfo))
                    .appendProperty(PropertyImpl.of("resource", map));
            return Optional.of(adapterType.cast(log.build()));
        }
        else {
            SaveSnapshotActionInfo saveSnapshotActionInfo = (SaveSnapshotActionInfo)adaptableObject;
            LogEntryBuilder log = log()
                    .title(MessageFormat.format(Messages.SnapshotCreated, saveSnapshotActionInfo.getSnapshotName()))
                    .appendDescription(getBody(saveSnapshotActionInfo))
                    .appendProperty(PropertyImpl.of("resource", map));
            return Optional.of(adapterType.cast(log.build()));
        }
    }

    private String getBody(SaveSnapshotActionInfo restoreSnapshotActionInfo){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Messages.RestoreSnapshotTemplateMessage).append(System.lineSeparator()).append(System.lineSeparator());
        getCommonSnapshotInfo(restoreSnapshotActionInfo, stringBuilder);
        stringBuilder.append("| Saved by | ").append(restoreSnapshotActionInfo.getActionPerformedBy()).append(" |\n\n");

        return stringBuilder.toString();
    }

    private String getBody(RestoreSnapshotActionInfo restoreSnapshotActionInfo){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Messages.SaveSnapshotTemplateMessage).append(System.lineSeparator()).append(System.lineSeparator());
        getCommonSnapshotInfo(restoreSnapshotActionInfo, stringBuilder);
        stringBuilder.append("| Golden | ").append(restoreSnapshotActionInfo.isGolden() ? "yes" : "no").append(" |\n");
        stringBuilder.append("| Restored by | ").append(restoreSnapshotActionInfo.getActionPerformedBy()).append(" |\n\n");

        if(restoreSnapshotActionInfo.getFailedPVs() != null && !restoreSnapshotActionInfo.getFailedPVs().isEmpty()){
            stringBuilder.append(Messages.FailedPVs).append("\n\n");
            // This is needed!
            stringBuilder.append("| |\n");
            // This is needed!
            stringBuilder.append("|-|\n");
            restoreSnapshotActionInfo.getFailedPVs().forEach(p -> stringBuilder.append("| ").append(p).append(" |\n\n"));
        }

        return stringBuilder.toString();
    }

    private void getCommonSnapshotInfo(SaveSnapshotActionInfo saveSnapshotActionInfo, StringBuilder stringBuilder){
        // This is needed!
        stringBuilder.append("| | |\n");
        // This is needed!
        stringBuilder.append("|-|-|\n");
        stringBuilder.append("| Snapshot name | ").append(saveSnapshotActionInfo.getSnapshotName()).append(" |\n");
        stringBuilder.append("| Comment | ").append(saveSnapshotActionInfo.getComment()).append(" |\n");
        stringBuilder.append("| Created | ").append(saveSnapshotActionInfo.getSnapshotCreatedDate()).append(" |\n");
    }
}

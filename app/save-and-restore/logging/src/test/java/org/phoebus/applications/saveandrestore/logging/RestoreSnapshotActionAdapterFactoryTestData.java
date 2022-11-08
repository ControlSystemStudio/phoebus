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


import org.junit.jupiter.api.Test;
import org.phoebus.logbook.LogEntry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RestoreSnapshotActionAdapterFactoryTestData {

    private RestoreSnapshotActionAdapterFactory restoreSnapshotActionAdapterFactory = new RestoreSnapshotActionAdapterFactory();

    @Test
    public void testGetAdaptableObject(){
        assertTrue(restoreSnapshotActionAdapterFactory.getAdaptableObject().isAssignableFrom(RestoreSnapshotActionInfo.class));
    }

    @Test
    public void testGetAdapterList(){
        List<? extends Class> list = restoreSnapshotActionAdapterFactory.getAdapterList();
        assertTrue(list.get(0).isAssignableFrom(LogEntry.class));
    }

    @Test
    public void testAdaptRestoreAction(){
        RestoreSnapshotActionInfo restoreSnapshotActionInfo = new RestoreSnapshotActionInfo();
        restoreSnapshotActionInfo.setSnapshotName("snapshot name");
        restoreSnapshotActionInfo.setFailedPVs(List.of("pv name"));

        LogEntry logEntry = restoreSnapshotActionAdapterFactory.adapt(restoreSnapshotActionInfo, LogEntry.class).get();
        assertTrue(logEntry.getTitle().contains("snapshot name"));
        assertTrue(logEntry.getDescription().contains("snapshot name"));
        assertTrue(logEntry.getDescription().contains("pv name"));
    }
}

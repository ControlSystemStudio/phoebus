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

package org.phoebus.logbook.olog.ui;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import org.junit.Test;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.Property;
import org.phoebus.olog.es.api.model.LogGroupProperty;
import org.phoebus.olog.es.api.model.OlogLog;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class LogEntryTreeHelperTest {

    @Test
    public void testCreateTree(){

        OlogLog o1 = new OlogLog(1L);
        o1.setCreatedDate(Instant.ofEpochMilli(100));

        Property property = LogGroupProperty.create();

        OlogLog o2 = new OlogLog(2L);
        o2.setCreatedDate(Instant.ofEpochMilli(200));
        o2.setProperties(Arrays.asList(property));

        OlogLog o3 = new OlogLog(3L);
        o3.setCreatedDate(Instant.ofEpochMilli(300));
        o3.setProperties(Arrays.asList(property));

        OlogLog o4 = new OlogLog(4L);
        o4.setCreatedDate(Instant.ofEpochMilli(400));
        o4.setProperties(Arrays.asList(property));

        List<LogEntry> logEntries = Arrays.asList(o1, o2, o3, o4);

        ObservableList<TreeItem<LogEntry>> treeItemList =
                LogEntryTreeHelper.createTree(logEntries);

        // Only two elements on top-level
        assertEquals(2, treeItemList.size());

        assertEquals(Long.valueOf(2L), Long.valueOf(treeItemList.get(0).getValue().getId()));
        assertEquals(Long.valueOf(1L), Long.valueOf(treeItemList.get(1).getValue().getId()));

        assertEquals(Long.valueOf(3L),
                Long.valueOf(treeItemList.get(0).getChildren().get(0).getValue().getId()));
        assertEquals(Long.valueOf(4L),
                Long.valueOf(treeItemList.get(0).getChildren().get(1).getValue().getId()));
    }
}

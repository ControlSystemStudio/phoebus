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

import org.junit.Test;
import org.phoebus.applications.saveandrestore.model.Node;

import java.util.Collections;

import static org.junit.Assert.*;

public class SaveAndRestoreEventLoggerTest {

    @Test
    public void testGetSnapshotInfoTable(){
        String table = new SaveAndRestoreEventLogger().getSnapshotInfoTable(new Node());
        String[] rows = table.split("\\n");
        assertTrue(rows[0].startsWith("| "));
        assertTrue(rows[1].startsWith("|-"));
    }

    @Test
    public void testGetPVFailedTable(){
        String table = new SaveAndRestoreEventLogger().getFailedPVsTable(Collections.emptyList());
        String[] rows = table.split("\\n");
        assertTrue(rows[0].startsWith("| "));
        assertTrue(rows[1].startsWith("|-"));
    }
}

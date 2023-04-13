/*
 *
 * Copyright (C) 2023 European Spallation Source ERIC.
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

package org.epics.pva.data.nt;

import org.epics.pva.data.PVAStringArray;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PVATableTest {

    @Test
    void getColumn() throws MustBeArrayException {
        PVATable table = PVATable.PVATableBuilder.aPVATable().name("table")
                .addColumn(new PVAStringArray("pvs", "pv1", "pv2", "pv3"))
                .build();
        PVAStringArray column = table.getColumn("pvs");
        assertEquals("pv2", column.get()[1]);
    }

    @Test
    void fromStructure() throws MustBeArrayException {
        Instant instant = Instant.now();
        PVATable table = PVATable.PVATableBuilder.aPVATable().name("table")
                .alarm(new PVAAlarm())
                .timeStamp(new PVATimeStamp(instant))
                .descriptor("descriptor")
                .addColumn(new PVAStringArray("pvs", "pv1", "pv2", "pv3"))
                .build();

        assertEquals(table, PVATable.fromStructure(table));
    }
}
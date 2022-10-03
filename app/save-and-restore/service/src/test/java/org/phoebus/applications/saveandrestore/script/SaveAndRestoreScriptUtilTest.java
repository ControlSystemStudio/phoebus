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

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.phoebus.applications.saveandrestore.SaveAndRestoreClient;

import java.util.UUID;

import static org.mockito.Mockito.when;

public class SaveAndRestoreScriptUtilTest {

    private static SaveAndRestoreClient saveAndRestoreClient;
    private static final String UNIQUE_ID = UUID.randomUUID().toString();

    @BeforeClass
    public static void init() {
        saveAndRestoreClient = Mockito.mock(SaveAndRestoreClient.class);
        SaveAndRestoreScriptUtil.setSaveAndRestoreClient(saveAndRestoreClient);
    }

    @Test(expected = Exception.class)
    public void testRestoreInvalidSnapshotId() throws Exception {
        Mockito.reset(saveAndRestoreClient);
        when(saveAndRestoreClient.getNode(UNIQUE_ID)).thenThrow(RuntimeException.class);
        SaveAndRestoreScriptUtil.restore(UNIQUE_ID, 1000, 100, false, false);
    }
}

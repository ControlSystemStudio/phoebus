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

package org.csstudio.trends.databrowser3.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class ModelTest {

    @Test
    public void testGetByUniqueId(){

        ModelItem modelItem1 = new PVItem("pvname", 1.0);
        ModelItem modelItem2 = new PVItem("pvname", 1.0);

        Model model = new Model();
        try{
            model.addItem(modelItem1);
            model.addItem(modelItem2);
        }
        catch(Exception e){
            fail("Unexpected exception");
            return;
        }
        String uniqueId1 = modelItem1.getUniqueId();
        String uniqueId2 = modelItem2.getUniqueId();

        assertEquals(modelItem1.getUniqueId(), model.getItemByUniqueId(modelItem1.getUniqueId()).getUniqueId());
        assertEquals(modelItem2.getUniqueId(), model.getItemByUniqueId(modelItem2.getUniqueId()).getUniqueId());

        assertNull(model.getItemByUniqueId(null));
        assertNull(model.getItemByUniqueId("invalid"));
    }
}

/*
 * Copyright (C) 2019 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.applications.saveandrestore.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.phoebus.applications.saveandrestore.ui.model.ObservableSaveSetEntry;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import se.esss.ics.masar.model.ConfigPv;

public class SaveSetControllerTest {

//	@Test
//	public void testListChange() {
//		ConfigPv configPv1 = ConfigPv.builder()
//				.pvName("A")
//				.build();
//		
//		ConfigPv configPv2 = ConfigPv.builder()
//				.pvName("B")
//				.build();
//		
//		ConfigPv configPv3 = ConfigPv.builder()
//				.pvName("C")
//				.build();
//		
//		ObservableList<ObservableSaveSetEntry> list1 =
//				FXCollections.observableArrayList(new ObservableSaveSetEntry(configPv1), new ObservableSaveSetEntry(configPv2), new ObservableSaveSetEntry(configPv3));
//		
//		ObservableList<ObservableSaveSetEntry> list2 =
//				FXCollections.observableArrayList(new ObservableSaveSetEntry(configPv3), new ObservableSaveSetEntry(configPv2), new ObservableSaveSetEntry(configPv1));
//		
//		ObservableList<ObservableSaveSetEntry> list3 =
//				FXCollections.observableArrayList(new ObservableSaveSetEntry(configPv1), new ObservableSaveSetEntry(configPv2));
//		
//		ObservableList<ObservableSaveSetEntry> list4 =
//				FXCollections.observableArrayList(new ObservableSaveSetEntry(configPv1), new ObservableSaveSetEntry(configPv1), new ObservableSaveSetEntry(configPv3));
//	
//		assertTrue(SaveSetController.areListsEqual(list1, list2));
//		assertFalse(SaveSetController.areListsEqual(list1, list3));
//		assertFalse(SaveSetController.areListsEqual(list1, list4));
//		assertFalse(SaveSetController.areListsEqual(list3, list4));
//	}
}

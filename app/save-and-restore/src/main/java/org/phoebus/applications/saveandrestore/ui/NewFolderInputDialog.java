/**
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

import java.util.List;

import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;

public class NewFolderInputDialog extends TextInputDialog {

	private List<String> invalidFolderNames;
	
	public NewFolderInputDialog(List<String> invalidFolderNames) {
		this.invalidFolderNames = invalidFolderNames;
		setTitle("New Folder");
		setContentText("Specify a folder name:");
		setHeaderText(null);
		getDialogPane().lookupButton(ButtonType.OK).setDisable(true);

		getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
			getDialogPane().lookupButton(ButtonType.OK).setDisable(invalidFolderNames.contains(newValue.trim()));
		});
	}
	
}

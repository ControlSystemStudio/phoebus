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

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class MainUI {

	private TreeViewBrowser treeView;
	private Node mainUI;
	private TabPane tabPane;

	public Node getUI() {
		if (mainUI == null) {
			createUI();
		}
		return mainUI;
	}

	private void createUI() {

		//HBox mainHBox = new HBox();
		//mainHBox.getChildren().addAll(createLeftPane(), createRightPane());
		
		SplitPane splitPane = new SplitPane();
		splitPane.setOrientation(Orientation.HORIZONTAL);
		
		splitPane.getItems().addAll(createLeftPane(), createRightPane());
		splitPane.setDividerPositions(0.3);
		mainUI = new BorderPane(splitPane);
	}

	private Node createRightPane() {
		tabPane = new TabPane();
		// HBox.setHgrow(tabPane, Priority.ALWAYS);
		return tabPane;
	}

	private Node createLeftPane() {

		HBox hBox = new HBox();
		treeView = new TreeViewBrowser();
		treeView.loadInitialTreeData();
		// HBox.setHgrow(treeView, Priority.ALWAYS);
		VBox.setVgrow(treeView, Priority.ALWAYS);
		HBox.setHgrow(treeView, Priority.ALWAYS);

		hBox.getChildren().add(treeView);
		HBox.setHgrow(hBox, Priority.SOMETIMES);

		return hBox;
	}

}

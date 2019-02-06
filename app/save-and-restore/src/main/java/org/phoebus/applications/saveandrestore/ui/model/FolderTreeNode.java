/*
 * Copyright (C) 2018 European Spallation Source ERIC.
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

package org.phoebus.applications.saveandrestore.ui.model;

import java.util.Date;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author georgweiss
 * Created 3 Jan 2019
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FolderTreeNode extends TreeNode{
		
	@Builder
	public FolderTreeNode(int id, String name, TreeNodeType type, ObservableList<TreeNode> children) {
		super(id, new SimpleStringProperty(name), null, null, type, children);
	}
	
	@Builder
	public FolderTreeNode(int id, String name, String userName, Date lastModified, TreeNodeType type, ObservableList<TreeNode> children) {
		super(id, new SimpleStringProperty(name), userName, lastModified, type, children);
	}

	@Override
	public boolean isLeaf() {
		return false;
	}
}

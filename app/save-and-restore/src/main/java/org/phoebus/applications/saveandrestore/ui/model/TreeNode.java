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

package org.phoebus.applications.saveandrestore.ui.model;

import java.util.Date;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * @author georgweiss Created 7 Jan 2019
 */

@SuperBuilder
@AllArgsConstructor
@Data
public abstract class TreeNode{

	private int id;
	private SimpleStringProperty name;
	private String userName;
	private Date lastModified;
	private TreeNodeType type;
	
	@Builder.Default
	private ObservableList<TreeNode> children = FXCollections.observableArrayList();
	
	public abstract boolean isLeaf();

	@Override
	public String toString() {
		return name.get();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof TreeNode))
			return false;
		
		TreeNode treeNode = (TreeNode)obj;
		
		return (treeNode.getId() + treeNode.getName().get()).equals(id + name.get());
	}
	
	@Override
	public int hashCode() {
		return (id + name.get()).hashCode();
	}
}

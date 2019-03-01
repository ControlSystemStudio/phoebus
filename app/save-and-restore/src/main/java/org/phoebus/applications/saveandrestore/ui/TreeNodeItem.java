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

import org.phoebus.applications.saveandrestore.ui.model.TreeNode;

import javafx.scene.control.TreeItem;
import se.esss.ics.masar.model.Node;
import se.esss.ics.masar.model.NodeType;

/**
 * Subclass of {@link TreeItem} using {@link TreeNode} (and subclasses) to hold
 * business data.
 * 
 * @author georgweiss Created 3 Jan 2019
 */
public class TreeNodeItem extends TreeItem<Node> implements Comparable<TreeNodeItem>{

	private Node treeNode;

	public TreeNodeItem(Node treeNode) {
		super(treeNode);
		this.treeNode = treeNode;
	}

	@Override
	public boolean isLeaf() {
		return treeNode.getNodeType().equals(NodeType.SNAPSHOT);
	}

	@Override
	public String toString() {
		return treeNode.getName();
	}

	/**
	 * Implements strategy where folders are sorted before save sets, and
	 * equal node types are sorted alphabetically.
	 * @param other The tree item to compare to
	 * @return -1 if this item is a folder and the other item is a save set,
	 * 1 if vice versa, and result of name comparison if node types are equal.
	 */
	@Override
	public int compareTo(TreeNodeItem other) {

		Node thisNode = getValue();
		Node otherNode = other.getValue();

		if(thisNode.getNodeType().equals(NodeType.FOLDER) && otherNode.getNodeType().equals(NodeType.CONFIGURATION)){
			return -1;
		}
		else if(thisNode.getNodeType().equals(NodeType.CONFIGURATION) && otherNode.getNodeType().equals(NodeType.FOLDER)){
			return 1;
		}
		else{
			return treeNode.getName().compareTo(other.getValue().getName());
		}
	}
}

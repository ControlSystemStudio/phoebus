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

package org.phoebus.applications.saveandrestore.data.providers.jmasar;

import java.util.List;
import java.util.stream.Collectors;

import org.phoebus.applications.saveandrestore.data.DataProvider;
import org.phoebus.applications.saveandrestore.data.FolderTreeNode;
import org.phoebus.applications.saveandrestore.data.TreeNode;
import org.phoebus.applications.saveandrestore.data.TreeNodeType;


import se.esss.ics.masar.model.Folder;
import se.esss.ics.masar.model.Node;
import se.esss.ics.masar.model.Snapshot;

public class JMasarDataProvider implements DataProvider {
	
	private JMasarClient jmasarClient;
	
	public JMasarDataProvider() {
		jmasarClient = new JMasarClient();
	}

	@Override
	public TreeNode getRootNode() {
		Folder folder = jmasarClient.getRoot();
		
		return DataConverter.fromJMasarFolder(folder);
	}
	
	@Override
	public List<TreeNode> getChildNodes(FolderTreeNode parentNode){
		if (parentNode.getType().equals(TreeNodeType.SAVESET)) {
			List<Snapshot> snapshots = jmasarClient.getSnapshots(parentNode);
			return snapshots.stream().map(s -> DataConverter.jmasarSnapshot2TreeNode(s)).collect(Collectors.toList());
		} else {
			List<Node> childNodes = jmasarClient.getChildNodes(parentNode.getId());
			return childNodes.stream().map(n -> DataConverter.fromJMasarNode(n)).collect(Collectors.toList());
		}
	}
	
	@Override
	public void rename(TreeNode treeNode, String newName) {
		jmasarClient.rename(treeNode.getId(), newName); 
	}
	
	@Override
	public FolderTreeNode createNewTreeNode(int parentId, TreeNode newTreeNode) {
		
		Folder newFolder = DataConverter.toJMasarFolder(parentId, newTreeNode);
		
		newFolder = jmasarClient.createNewFolder(newFolder);
		
		return DataConverter.fromJMasarNode(newFolder);
	}
}

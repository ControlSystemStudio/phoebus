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

package org.phoebus.applications.saveandrestore.data.providers.jmasar;

import java.util.List;

import org.phoebus.applications.saveandrestore.data.DataProvider;
import org.phoebus.applications.saveandrestore.data.DataProviderException;

import se.esss.ics.masar.model.Config;
import se.esss.ics.masar.model.Node;
import se.esss.ics.masar.model.NodeType;
import se.esss.ics.masar.model.Snapshot;

public class JMasarDataProvider implements DataProvider {

	private JMasarClient jmasarClient;

	public JMasarDataProvider() {
		jmasarClient = new JMasarClient();
	}

	@Override
	public Node getRootNode() {
		return jmasarClient.getRoot();
	}

	@Override
	public List<Node> getChildNodes(Node parentNode) {	
		return jmasarClient.getChildNodes(parentNode.getId());		
	}

	@Override
	public boolean rename(Node treeNode) {
		try {
			jmasarClient.rename(treeNode.getId(), treeNode.getName());
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	@Override
	public Node createNewTreeNode(int parentId, Node node) {
		return jmasarClient.createNewNode(node);
	}
	
	@Override
	public String getServiceIdentifier() {
		return "JMasar service (" + jmasarClient.getServiceUrl() + ")";
	}

	@Override
	public boolean deleteTreeNode(Node treeNode) {

		try {
			switch (treeNode.getNodeType()) {
			case FOLDER:
				jmasarClient.deleteNode(NodeType.FOLDER, treeNode.getId());
				break;
			case CONFIGURATION:
				jmasarClient.deleteNode(NodeType.CONFIGURATION, treeNode.getId());
				break;
			case SNAPSHOT:
				jmasarClient.deleteSnapshot(treeNode.getId());
				break;
			default:
			}
		} catch (DataProviderException e) {
			return false;
		}
		return true;
	}
	
	@Override
	public Config getSaveSet(int id) {
		
		return jmasarClient.getConfiguration(id);
	}
	
	@Override
	public Config saveSaveSet(Config config) {
		return jmasarClient.createConfiguration(config);
	}
	
	@Override
	public Config updateSaveSet(Config config) {
		return jmasarClient.updateConfiguration(config);
	}

	@Override
	public String getServiceVersion() {
		return jmasarClient.getJMasarServiceVersion();
	}

	@Override
	public Snapshot getSnapshot(int id){
		return jmasarClient.getSnapshot(id);
	}

	@Override
	public Snapshot takeSnapshot(int id){
		return jmasarClient.takeSnapshot(Integer.toString(id));
	}
	
}

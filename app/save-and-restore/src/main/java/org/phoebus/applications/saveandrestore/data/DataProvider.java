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

package org.phoebus.applications.saveandrestore.data;

import java.util.List;

import se.esss.ics.masar.model.Config;
import se.esss.ics.masar.model.Node;
import se.esss.ics.masar.model.Snapshot;

public interface DataProvider {

	public Node getRootNode();
	
	public List<Node> getChildNodes(Node node);
	
	public boolean rename(Node treeNode);
	
	public Node createNewTreeNode(int parentId, Node node);
	
	public boolean deleteTreeNode(Node treeNode);
	
	public Config getSaveSet(int id);
	
	public Config saveSaveSet(Config config);
	
	public Config updateSaveSet(Config config);
	
	public String getServiceIdentifier();

	public String getServiceVersion();

	public Snapshot getSnapshot(int id);

	public Snapshot takeSnapshot(int id);
	
}

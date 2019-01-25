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

package org.phoebus.applications.saveandrestore.data.providers.jmasar;

import java.util.List;
import java.util.stream.Collectors;

import org.phoebus.applications.saveandrestore.data.FolderTreeNode;
import org.phoebus.applications.saveandrestore.data.SnapshotTreeNode;
import org.phoebus.applications.saveandrestore.data.TreeNode;
import org.phoebus.applications.saveandrestore.data.TreeNodeType;

import se.esss.ics.masar.model.Folder;
import se.esss.ics.masar.model.Node;
import se.esss.ics.masar.model.NodeType;

public class DataConverter {

	public static TreeNode fromJMasarFolder(Folder folder) {

		FolderTreeNode node = fromJMasarNode(folder);
		List<TreeNode> children = folder.getChildNodes().stream().map(n -> fromJMasarNode(n))
				.collect(Collectors.toList());
		node.setChildren(children);
		return node;
	}
	
	public static Folder toJMasarFolder(int parentId, TreeNode treeNode) {
		
		return Folder.builder()
				.parentId(parentId)
				.name(treeNode.getName())
				.build();
	}

	public static FolderTreeNode fromJMasarNode(Node node) {

		FolderTreeNode treeNode = FolderTreeNode.builder()
				.id(node.getId())
				.name(node.getName())
				.type(node.getNodeType().equals(NodeType.FOLDER) ? TreeNodeType.FOLDER : TreeNodeType.SAVESET)
				.userName(node.getUserName())
				.lastModified(node.getLastModified())
				.build();
		return treeNode;
	}

	public static TreeNode jmasarSnapshot2TreeNode(se.esss.ics.masar.model.Snapshot jmasarSnapshot) {

		SnapshotTreeNode snapshotTreeNode = new SnapshotTreeNode(jmasarSnapshot.getId(), jmasarSnapshot.getName());
		snapshotTreeNode.setUserName(jmasarSnapshot.getUserName());
		snapshotTreeNode.setComment(jmasarSnapshot.getComment());
		snapshotTreeNode.setLastModified(jmasarSnapshot.getCreated());
		return snapshotTreeNode;
	}

//	public static SnapshotEntry fromJMasarSnapshotItem(SnapshotItem snapshotItem) {
//
//		SnapshotEntry snapshotEntry = new SnapshotEntry(snapshotItem.getPvName(),
//				snapshotItem.isFetchStatus() ? VTypesConverter.fromEpicsVType(snapshotItem.getValue()) : null);
//
//		return snapshotEntry;
//	}
//
//	public static SaveSetData fromJMasarConfig(SaveSet saveSet, Config config) {
//
//		List<SaveSetEntry> saveSetEntries = config.getConfigPvList().stream().map(cpv -> fromJMasarConfigPv(cpv))
//				.collect(Collectors.toList());
//		
//		Instant created = Instant.ofEpochMilli(config.getCreated().getTime());
//		SaveSetData saveSetData = new SaveSetData(saveSet, saveSetEntries, config.getDescription(), null, created);
//		
//		return saveSetData;
//	}
//	
//	public static Config toJMasarConfig(SaveSetData saveSetData) {
//		
//		List<ConfigPv> configPvList = 
//				saveSetData.getEntries().stream().map(sse -> toJMasarConfigPv(sse)).collect(Collectors.toList());
//				
//		return Config.builder()
//				.id(Integer.parseInt(saveSetData.getDescriptor().getSaveSetId()))
//				.configPvList(configPvList)
//				.lastModified(saveSetData.getDescriptor().getLastModified())
//				.description(saveSetData.getDescription())
//				.name(saveSetData.getDescriptor().getDisplayName())
//				.userName(saveSetData.getDescriptor().getUserName())
//				.build();
//		
//	}

//	public static SaveSetEntry fromJMasarConfigPv(ConfigPv configPv) {
//		return new SaveSetEntry(configPv.getPvName(), null, null, false, fromJMasarProvider(configPv.getProvider()));
//	}
//
//	public static ConfigPv toJMasarConfigPv(SaveSetEntry saveSetEntry) {
//		return ConfigPv.builder()
//				.provider(toJMasarProvider(saveSetEntry.getEpicsProvider()))
//				.pvName(saveSetEntry.getPVName())
//				.build();
//	}
//
//	private static EpicsProvider fromJMasarProvider(Provider jmasarProvider) {
//		switch (jmasarProvider) {
//		case ca:
//			return EpicsProvider.CA;
//		case pva:
//			return EpicsProvider.PVA;
//		default:
//			return null;
//		}
//	}
//
//	private static Provider toJMasarProvider(EpicsProvider epicsProvider) {
//		switch (epicsProvider) {
//		case CA:
//			return Provider.ca;
//		case PVA:
//			return Provider.pva;
//		default:
//			return null;
//		}
//	}


	
}

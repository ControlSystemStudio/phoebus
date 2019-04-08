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

package org.phoebus.applications.saveandrestore.service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.data.DataProvider;
import org.phoebus.applications.saveandrestore.data.DataProviderException;
import org.phoebus.framework.preferences.PreferencesReader;

import se.esss.ics.masar.model.ConfigPv;
import se.esss.ics.masar.model.Node;
import se.esss.ics.masar.model.SnapshotItem;


public class SaveAndRestoreService {

	private static SaveAndRestoreService instance;
	private ExecutorService executor;
	private DataProvider dataProvider;

	private SaveAndRestoreService() {

		executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

	}

	public static SaveAndRestoreService getInstance() {
		if (instance == null) {
			instance = new SaveAndRestoreService();
			try {
				PreferencesReader prefs = new PreferencesReader(SaveAndRestoreApplication.class,
						"/save_and_restore_preferences.properties");
				String dataProviderClassName = PreferencesReader.replaceProperties(prefs.get("dataprovider"));
				instance.setDataProvider(dataProviderClassName);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return instance;
	}

	public void setDataProvider(String dataProviderClassName) throws Exception {
		Class<?> clazz = Class.forName(dataProviderClassName);

		dataProvider = (DataProvider) clazz.getConstructor().newInstance();
	}

//	public Future<?> execute(Runnable runnable) {
//		return executor.submit(runnable);
//	}

	public Node getRootNode() {

		Future<Node> future = executor.submit(() -> dataProvider.getRootNode());

		try {
			return future.get();
		} catch (Exception ie) {
			ie.printStackTrace();
		}

		return null;
	}

	public Node getNode(String uniqueNodeId) {

		Future<Node> future = executor.submit(() -> dataProvider.getNode(uniqueNodeId));

		try {
			return future.get();
		} catch (Exception ie) {
			ie.printStackTrace();
		}

		return null;
	}

	public List<Node> getChildNodes(Node node) {
		Future<List<Node>> future = executor.submit(() -> dataProvider.getChildNodes(node));

		try {
			return future.get();
		} catch (Exception ie) {
			ie.printStackTrace();
		}

		return null;

	}

	public Node updateNode(Node nodeToUpdate) throws Exception {
		Future<Node> future = executor.submit(() -> dataProvider.updateNode(nodeToUpdate));

		return future.get();
	}

	public Node createNode(String parentsUniqueId, Node newTreeNode) throws Exception {
		Future<Node> future = executor.submit(() -> dataProvider.createNode(parentsUniqueId, newTreeNode));

		return future.get();
	}

	public boolean deleteNode(String uniqueNodeId) throws Exception{
		Future<Boolean> future = executor.submit(() -> {

			return dataProvider.deleteNode(uniqueNodeId);
			
		});
		
		return future.get();

	}

	public List<ConfigPv> getConfigPvs(String uniqueNodeId) throws Exception {

		Future<List<ConfigPv>> future = executor.submit(() -> dataProvider.getConfigPvs(uniqueNodeId));

		return future.get();
	}

	public Node updateSaveSet(Node configToUpdate, List<ConfigPv> configPvList) throws Exception {
		Future<Node> future = executor.submit(() -> dataProvider.updateSaveSet(configToUpdate, configPvList));

		return future.get();
	}


	public String getServiceIdentifier() {
		return dataProvider.getServiceIdentifier();
	}

//	public String getServiceVersion() throws Exception {
//		Future<String> future = executor.submit(() -> {
//
//			return dataProvider.getServiceVersion();
//		});
//
//		return future.get();
//	}

	public List<SnapshotItem> getSnapshotItems(String uniqueNodeId) throws Exception {

		Future<List<SnapshotItem>> future = executor.submit(() -> dataProvider.getSnapshotItems(uniqueNodeId));

		return future.get();
	}

	public Node getParentNode(String uniqueNodeId) throws Exception{
		Future<Node> future = executor.submit(() -> dataProvider.getSaveSetForSnapshot(uniqueNodeId));

		return future.get();
	}

	public Node takeSnapshot(String uniqueNodeId) throws Exception{
		Future<Node> future = executor.submit(() -> dataProvider.takeSnapshot(uniqueNodeId));

		return future.get();
	}

	public void tagSnapshotAsGolden(String uniqueNodeId) throws  Exception {
		Future<Void> future = executor.submit(() -> {

			if (!dataProvider.tagSnapshotAsGolden(uniqueNodeId)) {
				throw new DataProviderException("Unable to tag snapshot as Golden");
			}
			return null;
		});

		future.get();
	}

	public ConfigPv updateSingleConfigPv(String currentPvName, String newPvName, String currentReadbackPvName, String newReadbackPvName) throws Exception{
		Future<ConfigPv> future = executor.submit(() -> dataProvider.updateSingleConfigPv(currentPvName, newPvName, currentReadbackPvName, newReadbackPvName));
		return future.get();
	}

	public Node saveSnapshot(String configUniqueId, List<SnapshotItem> snapshotItems, String snapshotName, String comment) throws Exception{
		Future<Node> future = executor.submit(() -> dataProvider.saveSnapshot(configUniqueId, snapshotItems, snapshotName, comment));

		return future.get();
	}
}

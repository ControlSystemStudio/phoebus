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

import se.esss.ics.masar.model.Config;
import se.esss.ics.masar.model.Node;
import se.esss.ics.masar.model.Snapshot;

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

	public Future<?> execute(Runnable runnable) {
		return executor.submit(runnable);
	}

	public Node getRootNode() {

		Future<Node> future = executor.submit(() -> {

			return dataProvider.getRootNode();
		});

		try {
			return future.get();
		} catch (Exception ie) {
			ie.printStackTrace();
		}

		return null;
	}

	public List<Node> getChildNodes(Node parentNode) {
		Future<List<Node>> future = executor.submit(() -> {

			return dataProvider.getChildNodes(parentNode);
		});

		try {
			return future.get();
		} catch (Exception ie) {
			ie.printStackTrace();
		}

		return null;
	}

	public void rename(Node treeNode) throws Exception {
		Future<Void> future = executor.submit(() -> {

			if(!dataProvider.rename(treeNode)){
				throw new DataProviderException("Unable to rename node");
			}
			return null;
		});

		future.get();
	}

	public Node createNewTreeNode(int parentId, Node newTreeNode) throws Exception {
		Future<Node> future = executor.submit(() -> {

			return dataProvider.createNewTreeNode(parentId, newTreeNode);
		});

		return future.get();
	}

	public boolean deleteNode(Node treeNode) throws Exception{
		Future<Boolean> future = executor.submit(() -> {

			return dataProvider.deleteTreeNode(treeNode);
			
		});
		
		return future.get();

	}

	public Config getSaveSet(int id) throws Exception {

		Future<Config> future = executor.submit(() -> {

			return dataProvider.getSaveSet(id);

		});

		return future.get();
	}

	public Config saveSaveSet(Config config) throws Exception {
		Future<Config> future = executor.submit(() -> {

			return dataProvider.saveSaveSet(config);

		});

		return future.get();
	}
	
	public Config updateSaveSet(Config config) throws Exception {
		Future<Config> future = executor.submit(() -> {

			return dataProvider.updateSaveSet(config);

		});

		return future.get();
	}
	
	public String getServiceIdentifier() {
		return dataProvider.getServiceIdentifier();
	}

	public String getServiceVersion() throws Exception {
		Future<String> future = executor.submit(() -> {

			return dataProvider.getServiceVersion();
		});

		return future.get();
	}

	public Snapshot getSnapshot(int id) throws Exception{
		Future<Snapshot> future = executor.submit(() -> {

			return dataProvider.getSnapshot(id);
		});

		return future.get();
	}

	public Snapshot takeSnapshot(int id) throws Exception{
		Future<Snapshot> future = executor.submit(() -> {

			return dataProvider.takeSnapshot(id);
		});

		return future.get();
	}
}

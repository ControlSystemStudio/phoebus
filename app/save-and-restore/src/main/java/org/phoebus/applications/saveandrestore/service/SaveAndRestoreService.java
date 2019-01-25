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
import org.phoebus.applications.saveandrestore.data.FolderTreeNode;
import org.phoebus.applications.saveandrestore.data.TreeNode;
import org.phoebus.framework.preferences.PreferencesReader;

public class SaveAndRestoreService {

	private static SaveAndRestoreService instance;
	private ExecutorService executor;
	private DataProvider dataProvider;
	
	private SaveAndRestoreService() {
		
		executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
	
	}
	
	public static SaveAndRestoreService getInstance() {
		if(instance == null) {
			instance = new SaveAndRestoreService();
			try {
				PreferencesReader prefs = new PreferencesReader(SaveAndRestoreApplication.class, "/save_and_restore_preferences.properties");
				String dataProviderClassName = PreferencesReader.replaceProperties(prefs.get("dataprovider"));
				instance.setDataProvider(dataProviderClassName);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return instance;
	}
	
	public void setDataProvider(String dataProviderClassName) throws Exception{
		Class<?> clazz = Class.forName(dataProviderClassName);
		
		dataProvider = (DataProvider)clazz.getConstructor().newInstance();
	}
	
	public Future<?> execute(Runnable runnable) {
		return 	executor.submit(runnable);
	}
	
	public TreeNode getRootNode() {
		
		Future<TreeNode> future = executor.submit(() -> {
			
			return dataProvider.getRootNode();
		});
		
		try {
			return future.get();
		}
		catch(Exception ie) {
			ie.printStackTrace();
		}
		
		return null;
	}
	
	public List<TreeNode> getChildNodes(FolderTreeNode parentNode){
		Future<List<TreeNode>> future = executor.submit(() -> {
			
			return dataProvider.getChildNodes(parentNode);
		});
		
		try {
			return future.get();
		}
		catch(Exception ie) {
			ie.printStackTrace();
		}
		
		return null;
	}
	
	public void rename(TreeNode treeNode, String newName) throws Exception{
		Future<Void> future = executor.submit(() -> {
			
			dataProvider.rename(treeNode, newName);
			return null;
		});
		
		future.get();
	}
	
	public TreeNode createNewTreeNode(int parentId, TreeNode newTreeNode) throws Exception {
		Future<TreeNode> future = executor.submit(() -> {
			
			return dataProvider.createNewTreeNode(parentId, newTreeNode);
		});
			
	    return future.get();		
	}
}

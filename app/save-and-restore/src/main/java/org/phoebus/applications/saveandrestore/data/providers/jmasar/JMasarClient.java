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

import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.data.DataProviderException;
import org.phoebus.applications.saveandrestore.data.FolderTreeNode;
import org.phoebus.framework.preferences.PreferencesReader;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;

import se.esss.ics.masar.model.Config;
import se.esss.ics.masar.model.Folder;
import se.esss.ics.masar.model.Node;
import se.esss.ics.masar.model.Snapshot;

public class JMasarClient {

	private Client client;
	private String jmasarServiceUrl;
	
	private static final String CONTENT_TYPE_JSON = "application/json; charset=UTF-8";

	public JMasarClient() {

		DefaultClientConfig defaultClientConfig = new DefaultClientConfig();
		defaultClientConfig.getClasses().add(JacksonJsonProvider.class);
		client = Client.create(defaultClientConfig);
		
		PreferencesReader prefs = new PreferencesReader(SaveAndRestoreApplication.class, "/save_and_restore_preferences.properties");
		jmasarServiceUrl = prefs.get("jmasar.service.url");
	}

	public Folder getRoot(){
		return getCall("/folder/" + Node.ROOT_NODE_ID, Folder.class);
	}
	
	public List<Node> getChildNodes(int id) throws DataProviderException{
		ClientResponse response = getCall("/folder/" + id);		
		Folder folder = response.getEntity(Folder.class);
		return folder.getChildNodes();
	}
	
	public List<Snapshot> getSnapshots(FolderTreeNode treeNode){
		ClientResponse response = getCall("/config/" + treeNode.getId()+ "/snapshots");	
		return response.getEntity(new GenericType<List<Snapshot>>(){});
	}
	
	public Snapshot getSnapshot(int id) {
		return getCall("/snapshot/" + id, Snapshot.class);
	}
	
	public Config getConfiguration(int id){
		return getCall("/config/" + id, Config.class);
	}
	
	public Folder createNewFolder(Folder folder) {
		
		folder.setUserName(getCurrentUsersName());
		
		WebResource webResource = client.resource(jmasarServiceUrl + "/folder");
		
		ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
				.entity(folder, CONTENT_TYPE_JSON)
				.put(ClientResponse.class);
		if (response.getStatus() != 200) {
			throw new DataProviderException(response.getEntity(String.class));
		}
		
		return response.getEntity(Folder.class);
		
	}
	
	public void rename(int nodeId, String newName) {
		WebResource webResource = client.resource(jmasarServiceUrl + "/node/" + nodeId + "/rename")
				.queryParam("username", getCurrentUsersName())
				.queryParam("name", newName);
		
		ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
				.post(ClientResponse.class);
		
		if (response.getStatus() != 200) {
			throw new DataProviderException(response.getEntity(String.class));
		}
	}
	
	public Config saveConfig(Config config) {
		WebResource webResource = client.resource(jmasarServiceUrl + "/config");
		
		ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
				.entity(config, CONTENT_TYPE_JSON)
				.post(ClientResponse.class);
		
		if (response.getStatus() != 200) {
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}
		
		return response.getEntity(Config.class);
	}
	
	public Snapshot takeSnapshot(String configId) {
		WebResource webResource = client.resource(jmasarServiceUrl + "/snapshot/" + configId);
		
		ClientResponse response = webResource.accept(CONTENT_TYPE_JSON).put(ClientResponse.class);
		if (response.getStatus() != 200) {
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}
		
		return response.getEntity(Snapshot.class);
	}
	
	private <T> T getCall(String relativeUrl, Class<T> clazz) {
		
		ClientResponse response = getCall(relativeUrl);
		return response.getEntity(clazz);
	}
	
	private ClientResponse getCall(String relativeUrl){
		WebResource webResource = client.resource(jmasarServiceUrl + relativeUrl);
		
		ClientResponse response = webResource.accept(CONTENT_TYPE_JSON).get(ClientResponse.class);
		if (response.getStatus() != 200) {
			String message = response.getEntity(String.class);
			throw new DataProviderException("Failed : HTTP error code : " + response.getStatus() + ", error message: " + message);
		}
		
		return response;
	}
	
	public void commitSnapshot(String snapshotId, String snapshotName, String comment) {
		WebResource webResource = client.resource(jmasarServiceUrl + "/snapshot/" + snapshotId)
				.queryParam("userName", getCurrentUsersName())
				.queryParam("snapshotName", snapshotName)
				.queryParam("comment", comment);
		
		
		ClientResponse response = webResource.post(ClientResponse.class);
		
		if (response.getStatus() != 200) {
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}
	}
	
	private String getCurrentUsersName(){
		return System.getProperty("user.name");
	}
}

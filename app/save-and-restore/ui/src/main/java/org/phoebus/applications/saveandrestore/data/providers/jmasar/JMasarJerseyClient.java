/**
 * Copyright (C) 2018 European Spallation Source ERIC.
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.applications.saveandrestore.data.providers.jmasar;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.epics.vtype.gson.GsonMessageBodyHandler;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.data.DataProviderException;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.model.UpdateConfigHolder;
import org.phoebus.framework.preferences.PreferencesReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class JMasarJerseyClient implements JMasarClient {

    private static final int DEFAULT_READ_TIMEOUT = 5000; // ms
    private static final int DEFAULT_CONNECT_TIMEOUT = 3000; // ms

    private Client client;

    private static final String CONTENT_TYPE_JSON = "application/json; charset=UTF-8";

    private String jmasarServiceUrl;

    Logger logger = LoggerFactory.getLogger(JMasarJerseyClient.class.getName());

    public JMasarJerseyClient() {

        PreferencesReader preferencesReader = new PreferencesReader(SaveAndRestoreApplication.class, "/save_and_restore_preferences.properties");
        this.jmasarServiceUrl = preferencesReader.get("jmasar.service.url");

        int readTimeout = DEFAULT_READ_TIMEOUT;
        String readTimeoutString = preferencesReader.get("httpClient.readTimeout");
        try {
            readTimeout = Integer.parseInt(readTimeoutString);
            logger.debug("JMasar client using read timeout " + readTimeout + " ms");
        } catch (NumberFormatException e) {
            logger.error("Property httpClient.readTimeout \"" + readTimeoutString + "\" is not a number, using default value " + DEFAULT_READ_TIMEOUT + " ms");
        }

        int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        String connectTimeoutString = preferencesReader.get("httpClient.connectTimeout");
        try {
            connectTimeout = Integer.parseInt(connectTimeoutString);
            logger.debug("JMasar client using connect timeout " + connectTimeout + " ms");
        } catch (NumberFormatException e) {
            logger.error("Property httpClient.connectTimeout \"" + connectTimeoutString + "\" is not a number, using default value " + DEFAULT_CONNECT_TIMEOUT + " ms");
        }

        DefaultClientConfig defaultClientConfig = new DefaultClientConfig();
        defaultClientConfig.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, readTimeout);
        defaultClientConfig.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, connectTimeout);
        defaultClientConfig.getClasses().add(GsonMessageBodyHandler.class);
        client = Client.create(defaultClientConfig);
    }

    public void setServiceUrl(String serviceUrl) {
        this.jmasarServiceUrl = serviceUrl;
    }

    @Override
    public String getServiceUrl() {
        return jmasarServiceUrl;
    }

    @Override
    public Node getRoot() {
        return getCall("/root", Node.class);
    }

    @Override
    public Node getNode(String uniqueNodeId) {
        return getCall("/node/" + uniqueNodeId, Node.class);
    }

    @Override
    public Node getParentNode(String unqiueNodeId) {
        return getCall("/node/" + unqiueNodeId + "/parent", Node.class);
    }

    @Override
    public List<Node> getChildNodes(Node node) throws DataProviderException {
        ClientResponse response;
        if (node.getNodeType().equals(NodeType.CONFIGURATION)) {
            response = getCall("/config/" + node.getUniqueId() + "/snapshots");
        } else {
            response = getCall("/node/" + node.getUniqueId() + "/children");
        }
        return response.getEntity(new GenericType<List<Node>>() {
        });
    }

    @Override
    public List<SnapshotItem> getSnapshotItems(String snapshotUniqueId) {
        ClientResponse response = getCall("/snapshot/" + snapshotUniqueId + "/items");
        return response.getEntity(new GenericType<List<SnapshotItem>>() {
        });
    }

    @Override
    public Node saveSnapshot(String configUniqueId, List<SnapshotItem> snapshotItems, String snapshotName, String comment) {
        WebResource webResource = client.resource(jmasarServiceUrl + "/snapshot/" + configUniqueId)
                .queryParam("snapshotName", snapshotName.replaceAll("%", "%25"))
                .queryParam("comment", comment.replaceAll("%", "%25"))
                .queryParam("userName", getCurrentUsersName());
        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .entity(snapshotItems, CONTENT_TYPE_JSON)
                .put(ClientResponse.class);

        if (response.getStatus() != 200) {
            throw new DataProviderException(response.getEntity(String.class));
        }

        return response.getEntity(Node.class);
    }

    @Override
    public List<ConfigPv> getConfigPvs(String configUniqueId) {
        ClientResponse response = getCall("/config/" + configUniqueId + "/items");
        return response.getEntity(new GenericType<List<ConfigPv>>() {
        });
    }

    @Override
    public Node createNewNode(String parentsUniqueId, Node node) {

        node.setUserName(getCurrentUsersName());

        WebResource webResource = client.resource(jmasarServiceUrl + "/node/" + parentsUniqueId);

        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .entity(node, CONTENT_TYPE_JSON)
                .put(ClientResponse.class);
        if (response.getStatus() != 200) {
            throw new DataProviderException(response.getEntity(String.class));
        }

        return response.getEntity(Node.class);

    }

    @Override
    public Node updateNode(Node nodeToUpdate) {
        return updateNode(nodeToUpdate, false);
    }

    @Override
    public Node updateNode(Node nodeToUpdate, boolean customTimeForMigration) {
        if (nodeToUpdate.getUserName() == null || nodeToUpdate.getUserName().isEmpty()) {
            nodeToUpdate.setUserName(getCurrentUsersName());
        }
        WebResource webResource = client.resource(jmasarServiceUrl + "/node/" + nodeToUpdate.getUniqueId() + "/update")
                .queryParam("customTimeForMigration", customTimeForMigration ? "true" : "false");

        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .entity(nodeToUpdate, CONTENT_TYPE_JSON)
                .post(ClientResponse.class);

        if (response.getStatus() != 200) {
            throw new DataProviderException(response.getEntity(String.class));
        }

        return response.getEntity(Node.class);
    }

    private <T> T getCall(String relativeUrl, Class<T> clazz) {

        ClientResponse response = getCall(relativeUrl);
        return response.getEntity(clazz);
    }

    private ClientResponse getCall(String relativeUrl) {
        WebResource webResource = client.resource(jmasarServiceUrl + relativeUrl);

        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON).get(ClientResponse.class);
        if (response.getStatus() != 200) {
            String message = response.getEntity(String.class);
            throw new DataProviderException("Failed : HTTP error code : " + response.getStatus() + ", error message: " + message);
        }

        return response;
    }

    @Override
    public void deleteNode(String uniqueNodeId) {
        WebResource webResource = client.resource(jmasarServiceUrl + "/node/" + uniqueNodeId);
        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON).delete(ClientResponse.class);
        if (response.getStatus() != 200) {
            String message = response.getEntity(String.class);
            throw new DataProviderException("Failed : HTTP error code : " + response.getStatus() + ", error message: " + message);
        }
    }

    private String getCurrentUsersName() {
        return System.getProperty("user.name");
    }

    @Override
    public Node updateConfiguration(Node configToUpdate, List<ConfigPv> configPvList) {

        configToUpdate.setUserName(getCurrentUsersName());

        WebResource webResource = client.resource(jmasarServiceUrl + "/config/" + configToUpdate.getUniqueId() + "/update");

        UpdateConfigHolder holder = UpdateConfigHolder.builder()
                .config(configToUpdate)
                .configPvList(configPvList)
                .build();

        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .entity(holder, CONTENT_TYPE_JSON)
                .post(ClientResponse.class);
        if (response.getStatus() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
        }

        return response.getEntity(Node.class);
    }

    @Override
    public List<Tag> getAllTags() {
        ClientResponse response = getCall("/tags");
        return response.getEntity(new GenericType<List<Tag>>() {
        });
    }

    @Override
    public List<Node> getAllSnapshots() {
        ClientResponse response = getCall("/snapshots");
        return response.getEntity(new GenericType<List<Node>>() {
        });
    }
}

/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.applications.saveandrestore.service.impl;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.epics.vtype.gson.GsonMessageBodyHandler;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.model.UpdateConfigHolder;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreClient;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreClientException;
import org.phoebus.framework.preferences.PreferencesReader;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SaveAndRestoreJerseyClient implements SaveAndRestoreClient {

    private Client client;
    private static final String CONTENT_TYPE_JSON = "application/json; charset=UTF-8";
    private final Logger logger = Logger.getLogger(SaveAndRestoreJerseyClient.class.getName());

    private String jmasarServiceUrl;
    private int httpClientReadTimeout;
    private int httpClientConnectTimeout;

    private static final int DEFAULT_READ_TIMEOUT = 5000; // ms
    private static final int DEFAULT_CONNECT_TIMEOUT = 3000; // ms

    public SaveAndRestoreJerseyClient() {

        PreferencesReader preferencesReader = new PreferencesReader(SaveAndRestoreApplication.class, "/save_and_restore_preferences.properties");
        this.jmasarServiceUrl = preferencesReader.get("jmasar.service.url");

        httpClientReadTimeout = DEFAULT_READ_TIMEOUT;
        String readTimeoutString = preferencesReader.get("httpClient.readTimeout");
        try {
            httpClientReadTimeout = Integer.parseInt(readTimeoutString);
            logger.log(Level.INFO, "JMasar client using read timeout " + httpClientReadTimeout + " ms");
        } catch (NumberFormatException e) {
            logger.log(Level.INFO, "Property httpClient.readTimeout \"" + readTimeoutString + "\" is not a number, using default value " + DEFAULT_READ_TIMEOUT + " ms");
        }

        httpClientConnectTimeout = DEFAULT_CONNECT_TIMEOUT;
        String connectTimeoutString = preferencesReader.get("httpClient.connectTimeout");
        try {
            httpClientConnectTimeout = Integer.parseInt(connectTimeoutString);
            logger.log(Level.INFO, "JMasar client using connect timeout " + httpClientConnectTimeout + " ms");
        } catch (NumberFormatException e) {
            logger.log(Level.INFO, "Property httpClient.connectTimeout \"" + connectTimeoutString + "\" is not a number, using default value " + DEFAULT_CONNECT_TIMEOUT + " ms");
        }


        DefaultClientConfig defaultClientConfig = new DefaultClientConfig();
        defaultClientConfig.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, httpClientReadTimeout);
        defaultClientConfig.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, httpClientConnectTimeout);
        defaultClientConfig.getClasses().add(GsonMessageBodyHandler.class);
        client = Client.create(defaultClientConfig);
        this.jmasarServiceUrl =     jmasarServiceUrl;
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
    public List<Node> getChildNodes(Node node) throws SaveAndRestoreClientException {
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
            throw new SaveAndRestoreClientException(response.getEntity(String.class));
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
            String message = Messages.createNodeFailed;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                // Ignore
            }
            throw new SaveAndRestoreClientException(message);
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
            String message = Messages.updateNodeFailed;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                // Ignore
            }
            throw new SaveAndRestoreClientException(message);
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
            throw new SaveAndRestoreClientException("Failed : HTTP error code : " + response.getStatus() + ", error message: " + message);
        }

        return response;
    }

    @Override
    @Deprecated
    public void deleteNode(String uniqueNodeId) {
        WebResource webResource = client.resource(jmasarServiceUrl + "/node/" + uniqueNodeId);
        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON).delete(ClientResponse.class);
        if (response.getStatus() != 200) {
            String message = response.getEntity(String.class);
            throw new SaveAndRestoreClientException("Failed : HTTP error code : " + response.getStatus() + ", error message: " + message);
        }
    }

    @Override
    public void deleteNodes(List<String> nodeIds) {
        WebResource webResource = client.resource(jmasarServiceUrl + "/node");
        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .entity(nodeIds, CONTENT_TYPE_JSON)
                .delete(ClientResponse.class);
        if (response.getStatus() != 200) {
            String message = response.getEntity(String.class);
            throw new SaveAndRestoreClientException("Failed : HTTP error code : " + response.getStatus() + ", error message: " + message);
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
            String message = Messages.updateConfigurationFailed;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                // Ignore
            }
            throw new RuntimeException(message);
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

    @Override
    public Node moveNodes(List<String> sourceNodeIds, String targetNodeId) {
        WebResource webResource =
                client.resource(jmasarServiceUrl + "/move")
                        .queryParam("to", targetNodeId)
                        .queryParam("username", getCurrentUsersName());

        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .entity(sourceNodeIds, CONTENT_TYPE_JSON)
                .post(ClientResponse.class);

        if (response.getStatus() != 200) {
            String message = Messages.copyOrMoveNotAllowedBody;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                // Ignore
            }
            throw new SaveAndRestoreClientException(message);
        }
        return response.getEntity(Node.class);
    }

    @Override
    public Node copyNodes(List<String> sourceNodeIds, String targetNodeId) {
        WebResource webResource =
                client.resource(jmasarServiceUrl + "/copy")
                        .queryParam("to", targetNodeId)
                        .queryParam("username", getCurrentUsersName());

        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .entity(sourceNodeIds, CONTENT_TYPE_JSON)
                .post(ClientResponse.class);

        if (response.getStatus() != 200) {
            String message = Messages.copyOrMoveNotAllowedBody;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                // Ignore
            }
            throw new SaveAndRestoreClientException(message);
        }
        return response.getEntity(Node.class);
    }
}

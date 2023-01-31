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

package org.phoebus.applications.saveandrestore.impl;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.phoebus.applications.saveandrestore.SaveAndRestoreClient;
import org.phoebus.applications.saveandrestore.SaveAndRestoreClientException;
import org.phoebus.applications.saveandrestore.model.CompositeSnapshot;
import org.phoebus.applications.saveandrestore.model.CompositeSnapshotData;
import org.phoebus.applications.saveandrestore.model.Configuration;
import org.phoebus.applications.saveandrestore.model.ConfigurationData;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.Snapshot;
import org.phoebus.applications.saveandrestore.model.SnapshotData;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.applications.saveandrestore.model.search.SearchResult;
import org.phoebus.applications.saveandrestore.service.Messages;
import org.phoebus.framework.preferences.PreferencesReader;

import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SaveAndRestoreJerseyClient implements SaveAndRestoreClient {

    private final Client client;
    private static final String CONTENT_TYPE_JSON = "application/json; charset=UTF-8";
    private final Logger logger = Logger.getLogger(SaveAndRestoreJerseyClient.class.getName());

    private final String jmasarServiceUrl;

    private static final int DEFAULT_READ_TIMEOUT = 5000; // ms
    private static final int DEFAULT_CONNECT_TIMEOUT = 3000; // ms

    public SaveAndRestoreJerseyClient() {

        PreferencesReader preferencesReader = new PreferencesReader(SaveAndRestoreClient.class, "/client_preferences.properties");
        this.jmasarServiceUrl = preferencesReader.get("jmasar.service.url");

        int httpClientReadTimeout = DEFAULT_READ_TIMEOUT;
        String readTimeoutString = preferencesReader.get("httpClient.readTimeout");
        try {
            httpClientReadTimeout = Integer.parseInt(readTimeoutString);
            logger.log(Level.INFO, "JMasar client using read timeout " + httpClientReadTimeout + " ms");
        } catch (NumberFormatException e) {
            logger.log(Level.INFO, "Property httpClient.readTimeout \"" + readTimeoutString + "\" is not a number, using default value " + DEFAULT_READ_TIMEOUT + " ms");
        }

        int httpClientConnectTimeout = DEFAULT_CONNECT_TIMEOUT;
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
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setSerializationInclusion(Include.NON_NULL);
        JacksonJsonProvider jacksonJsonProvider = new JacksonJsonProvider(mapper);
        defaultClientConfig.getSingletons().add(jacksonJsonProvider);
        client = Client.create(defaultClientConfig);
    }

    @Override
    public String getServiceUrl() {
        return jmasarServiceUrl;
    }

    @Override
    public Node getRoot() {
        return getCall("/node/" + Node.ROOT_FOLDER_UNIQUE_ID, Node.class);
    }

    @Override
    public Node getNode(String uniqueNodeId) {
        return getCall("/node/" + uniqueNodeId, Node.class);
    }

    @Override
    public List<Node> getCompositeSnapshotReferencedNodes(String uniqueNodeId){
        WebResource webResource = client.resource(jmasarServiceUrl + "/composite-snapshot/" + uniqueNodeId + "/nodes");

        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON).get(ClientResponse.class);
        if (response.getStatus() != 200) {
            String message;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                message = "N/A";
            }
            throw new SaveAndRestoreClientException("Failed : HTTP error code : " + response.getStatus() + ", error message: " + message);
        }

        return response.getEntity(new GenericType<List<Node>>() {
        });
    }

    @Override
    public List<SnapshotItem> getCompositeSnapshotItems(String uniqueNodeId){
        WebResource webResource = client.resource(jmasarServiceUrl + "/composite-snapshot/" + uniqueNodeId + "/items");

        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON).get(ClientResponse.class);
        if (response.getStatus() != 200) {
            String message;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                message = "N/A";
            }
            throw new SaveAndRestoreClientException("Failed : HTTP error code : " + response.getStatus() + ", error message: " + message);
        }

        return response.getEntity(new GenericType<List<SnapshotItem>>() {
        });
    }

    @Override
    public Node getParentNode(String unqiueNodeId) {
        return getCall("/node/" + unqiueNodeId + "/parent", Node.class);
    }

    @Override
    public List<Node> getChildNodes(String uniqueNodeId) throws SaveAndRestoreClientException {
        ClientResponse response = getCall("/node/" + uniqueNodeId + "/children");
        return response.getEntity(new GenericType<List<Node>>() {
        });
    }

    @Override
    public Node createNewNode(String parentNodeId, Node node) {
        node.setUserName(getCurrentUsersName());
        WebResource webResource = client.resource(jmasarServiceUrl + "/node").queryParam("parentNodeId", parentNodeId);
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
        WebResource webResource = client.resource(jmasarServiceUrl + "/node")
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
            String message;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                message = "N/A";
            }
            throw new SaveAndRestoreClientException("Failed : HTTP error code : " + response.getStatus() + ", error message: " + message);
        }

        return response;
    }

    @Override
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
        nodeIds.forEach(this::deleteNode);
    }

    private String getCurrentUsersName() {
        return System.getProperty("user.name");
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

    @Override
    public String getFullPath(String uniqueNodeId) {
        WebResource webResource =
                client.resource(jmasarServiceUrl + "/path/" + uniqueNodeId);
        ClientResponse response = webResource.get(ClientResponse.class);

        if (response.getStatus() != 200) {
            return null;
        }
        return response.getEntity(String.class);
    }

    @Override
    public List<Node> getFromPath(String path) {
        return null;
    }

    @Override
    public ConfigurationData getConfigurationData(String nodeId) {
        ClientResponse clientResponse = getCall("/config/" + nodeId);
        return clientResponse.getEntity(ConfigurationData.class);
    }

    @Override
    public Configuration createConfiguration(String parentNodeId, Configuration configuration) {
        configuration.getConfigurationNode().setUserName(getCurrentUsersName());
        WebResource webResource =
                client.resource(jmasarServiceUrl + "/config")
                        .queryParam("parentNodeId", parentNodeId);
        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .entity(configuration, CONTENT_TYPE_JSON)
                .put(ClientResponse.class);
        if (response.getStatus() != 200) {
            String message = Messages.createConfigurationFailed;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                // Ignore
            }
            throw new SaveAndRestoreClientException(message);
        }
        return response.getEntity(Configuration.class);
    }

    @Override
    public Configuration updateConfiguration(Configuration configuration) {
        WebResource webResource = client.resource(jmasarServiceUrl + "/config");

        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .entity(configuration, CONTENT_TYPE_JSON)
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
        return response.getEntity(Configuration.class);
    }

    @Override
    public SnapshotData getSnapshotData(String nodeId) {
        ClientResponse clientResponse = getCall("/snapshot/" + nodeId);
        return clientResponse.getEntity(SnapshotData.class);
    }

    @Override
    public Snapshot saveSnapshot(String parentNodeId, Snapshot snapshot) {
        snapshot.getSnapshotNode().setUserName(getCurrentUsersName());
        WebResource webResource =
                client.resource(jmasarServiceUrl + "/snapshot")
                        .queryParam("parentNodeId", parentNodeId);
        ClientResponse response;
        try {
            response = webResource.accept(CONTENT_TYPE_JSON)
                    .entity(snapshot, CONTENT_TYPE_JSON)
                    .put(ClientResponse.class);
        } catch (UniformInterfaceException e) {
            throw new RuntimeException(e);
        } catch (ClientHandlerException e) {
            throw new RuntimeException(e);
        }
        if (response.getStatus() != 200) {
            String message = Messages.searchFailed;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                // Ignore
            }
            throw new SaveAndRestoreClientException(message);
        }
        return response.getEntity(Snapshot.class);
    }

    @Override
    public CompositeSnapshot createCompositeSnapshot(String parentNodeId, CompositeSnapshot compositeSnapshot){
        compositeSnapshot.getCompositeSnapshotNode().setUserName(getCurrentUsersName());
        WebResource webResource =
                client.resource(jmasarServiceUrl + "/composite-snapshot")
                        .queryParam("parentNodeId", parentNodeId);
        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .entity(compositeSnapshot, CONTENT_TYPE_JSON)
                .put(ClientResponse.class);
        if (response.getStatus() != 200) {
            String message = Messages.createConfigurationFailed;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                // Ignore
            }
            throw new SaveAndRestoreClientException(message);
        }
        return response.getEntity(CompositeSnapshot.class);
    }

    @Override
    public CompositeSnapshotData getCompositeSnapshotData(String uniqueId){
        ClientResponse clientResponse = getCall("/composite-snapshot/" + uniqueId);
        return clientResponse.getEntity(CompositeSnapshotData.class);
    }

    @Override
    public List<String> checkCompositeSnapshotConsistency(List<String> snapshotNodeIds){
        WebResource webResource =
                client.resource(jmasarServiceUrl + "/composite-snapshot-consistency-check");
        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .entity(snapshotNodeIds, CONTENT_TYPE_JSON)
                .post(ClientResponse.class);
        if (response.getStatus() != 200) {
            String message = Messages.compositeSnapshotConsistencyCheckFailed;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                // Ignore
            }
            throw new SaveAndRestoreClientException(message);
        }
        return response.getEntity(new GenericType<List<String>>() {
        });
    }

    @Override
    public CompositeSnapshot updateCompositeSnapshot(CompositeSnapshot compositeSnapshot){
        WebResource webResource = client.resource(jmasarServiceUrl + "/composite-snapshot");

        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .entity(compositeSnapshot, CONTENT_TYPE_JSON)
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
        return response.getEntity(CompositeSnapshot.class);
    }

    @Override
    public SearchResult search(MultivaluedMap<String, String> searchParams){
        WebResource webResource = client.resource(jmasarServiceUrl + "/search")
                .queryParams(searchParams);
        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .get(ClientResponse.class);
        if (response.getStatus() != 200) {
            String message = Messages.searchFailed;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                // Ignore
            }
            throw new RuntimeException(message);
        }
        return response.getEntity(SearchResult.class);
    }

    @Override
    public Filter saveFilter(Filter filter){
        filter.setUser(getCurrentUsersName());
        WebResource webResource = client.resource(jmasarServiceUrl + "/filter");

        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .entity(filter, CONTENT_TYPE_JSON)
                .put(ClientResponse.class);
        if (response.getStatus() != 200) {
            String message = Messages.saveFilterFailed;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                // Ignore
            }
            throw new RuntimeException(message);
        }
        return response.getEntity(Filter.class);
    }

    @Override
    public List<Filter> getAllFilters(){
        WebResource webResource = client.resource(jmasarServiceUrl + "/filters");
        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .get(ClientResponse.class);
        if (response.getStatus() != 200) {
            String message = Messages.searchFailed;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                // Ignore
            }
            throw new RuntimeException(message);
        }
        return response.getEntity(new GenericType<List<Filter>>(){});
    }

    @Override
    public void deleteFilter(String name){
        // Filter name may contain space chars, need to URL encode these.
        String filterName = name.replace(" ", "%20");
        WebResource  webResource = client.resource(jmasarServiceUrl + "/filter/" + filterName);
        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .delete(ClientResponse.class);
        if (response.getStatus() != 200) {
            String message = Messages.deleteFilterFailed;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                // Ignore
            }
            throw new RuntimeException(message);
        }
    }
}

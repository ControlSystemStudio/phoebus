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

package org.phoebus.applications.saveandrestore.client;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.SaveAndRestoreClientException;
import org.phoebus.applications.saveandrestore.model.*;
import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.applications.saveandrestore.model.search.SearchResult;
import org.phoebus.security.store.SecureStore;
import org.phoebus.security.tokens.AuthenticationScope;
import org.phoebus.security.tokens.ScopedAuthenticationToken;

import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SaveAndRestoreJerseyClient implements org.phoebus.applications.saveandrestore.client.SaveAndRestoreClient {

    private static final String CONTENT_TYPE_JSON = "application/json; charset=UTF-8";
    private final Logger logger = Logger.getLogger(SaveAndRestoreJerseyClient.class.getName());

    private static final int DEFAULT_READ_TIMEOUT = 5000; // ms
    private static final int DEFAULT_CONNECT_TIMEOUT = 5000; // ms

    ObjectMapper mapper = new ObjectMapper();

    private HTTPBasicAuthFilter httpBasicAuthFilter;

    public SaveAndRestoreJerseyClient() {

        mapper.registerModule(new JavaTimeModule());
        mapper.setSerializationInclusion(Include.NON_NULL);
    }

    private Client getClient() {
        int httpClientReadTimeout = Preferences.httpClientReadTimeout > 0 ? Preferences.httpClientReadTimeout : DEFAULT_READ_TIMEOUT;
        logger.log(Level.INFO, "Save&restore client using read timeout " + httpClientReadTimeout + " ms");

        int httpClientConnectTimeout = Preferences.httpClientConnectTimeout > 0 ? Preferences.httpClientConnectTimeout : DEFAULT_CONNECT_TIMEOUT;
        logger.log(Level.INFO, "Save&restore client using connect timeout " + httpClientConnectTimeout + " ms");

        DefaultClientConfig defaultClientConfig = new DefaultClientConfig();
        defaultClientConfig.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, httpClientReadTimeout);
        defaultClientConfig.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, httpClientConnectTimeout);

        JacksonJsonProvider jacksonJsonProvider = new JacksonJsonProvider(mapper);
        defaultClientConfig.getSingletons().add(jacksonJsonProvider);

        Client client = Client.create(defaultClientConfig);

        try {
            SecureStore store = new SecureStore();
            ScopedAuthenticationToken scopedAuthenticationToken = store.getScopedAuthenticationToken(AuthenticationScope.SAVE_AND_RESTORE);
            if (scopedAuthenticationToken != null) {
                String username = scopedAuthenticationToken.getUsername();
                String password = scopedAuthenticationToken.getPassword();
                httpBasicAuthFilter = new HTTPBasicAuthFilter(username, password);
                client.addFilter(httpBasicAuthFilter);
            } else if (httpBasicAuthFilter != null) {
                client.removeFilter(httpBasicAuthFilter);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to retrieve credentials from secure store", e);
        }

        return client;
    }

    @Override
    public String getServiceUrl() {
        return Preferences.jmasarServiceUrl;
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
    public List<Node> getCompositeSnapshotReferencedNodes(String uniqueNodeId) {
        WebResource webResource = getClient().resource(Preferences.jmasarServiceUrl + "/composite-snapshot/" + uniqueNodeId + "/nodes");

        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON).get(ClientResponse.class);
        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
            String message;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                message = "N/A";
            }
            throw new SaveAndRestoreClientException("Failed : HTTP error code : " + response.getStatus() + ", error message: " + message);
        }

        return response.getEntity(new GenericType<>() {
        });
    }

    @Override
    public List<SnapshotItem> getCompositeSnapshotItems(String uniqueNodeId) {
        WebResource webResource = getClient().resource(Preferences.jmasarServiceUrl + "/composite-snapshot/" + uniqueNodeId + "/items");

        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON).get(ClientResponse.class);
        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
            String message;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                message = "N/A";
            }
            throw new SaveAndRestoreClientException("Failed : HTTP error code : " + response.getStatus() + ", error message: " + message);
        }

        return response.getEntity(new GenericType<>() {
        });
    }

    @Override
    public Node getParentNode(String unqiueNodeId) {
        return getCall("/node/" + unqiueNodeId + "/parent", Node.class);
    }

    @Override
    public List<Node> getChildNodes(String uniqueNodeId) throws SaveAndRestoreClientException {
        ClientResponse response = getCall("/node/" + uniqueNodeId + "/children");
        return response.getEntity(new GenericType<>() {
        });
    }

    @Override
    public Node createNewNode(String parentNodeId, Node node) {
        WebResource webResource = getClient().resource(Preferences.jmasarServiceUrl + "/node")
                .queryParam("parentNodeId", parentNodeId);
        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .entity(node, CONTENT_TYPE_JSON)
                .put(ClientResponse.class);
        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
            String message = Messages.createNodeFailed;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to parse response", e);
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
        WebResource webResource = getClient().resource(Preferences.jmasarServiceUrl + "/node")
                .queryParam("customTimeForMigration", customTimeForMigration ? "true" : "false");

        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .entity(nodeToUpdate, CONTENT_TYPE_JSON)
                .post(ClientResponse.class);

        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
            String message = Messages.updateNodeFailed;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to parse response", e);
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
        WebResource webResource = getClient().resource(Preferences.jmasarServiceUrl + relativeUrl);

        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON).get(ClientResponse.class);
        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
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
    public void deleteNodes(List<String> nodeIds) {
        WebResource webResource = getClient().resource(Preferences.jmasarServiceUrl + "/node");
        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .entity(nodeIds, CONTENT_TYPE_JSON)
                .delete(ClientResponse.class);
        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
            String message = response.getEntity(String.class);
            throw new SaveAndRestoreClientException("Failed : HTTP error code : " + response.getStatus() + ", error message: " + message);
        }
    }

    @Override
    public List<Tag> getAllTags() {
        ClientResponse response = getCall("/tags");
        return response.getEntity(new GenericType<>() {
        });
    }

    @Override
    public List<Node> getAllSnapshots() {
        ClientResponse response = getCall("/snapshots");
        return response.getEntity(new GenericType<>() {
        });
    }

    @Override
    public Node moveNodes(List<String> sourceNodeIds, String targetNodeId) {
        WebResource webResource =
                getClient().resource(Preferences.jmasarServiceUrl + "/move")
                        .queryParam("to", targetNodeId);

        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .entity(sourceNodeIds, CONTENT_TYPE_JSON)
                .post(ClientResponse.class);

        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
            String message = Messages.copyOrMoveNotAllowedBody;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to parse response", e);
            }
            throw new SaveAndRestoreClientException(message);
        }
        return response.getEntity(Node.class);
    }

    @Override
    public Node copyNodes(List<String> sourceNodeIds, String targetNodeId) {
        WebResource webResource =
                getClient().resource(Preferences.jmasarServiceUrl + "/copy")
                        .queryParam("to", targetNodeId);

        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .entity(sourceNodeIds, CONTENT_TYPE_JSON)
                .post(ClientResponse.class);

        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
            String message = Messages.copyOrMoveNotAllowedBody;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to parse response", e);
            }
            throw new SaveAndRestoreClientException(message);
        }
        return response.getEntity(Node.class);
    }

    @Override
    public String getFullPath(String uniqueNodeId) {
        WebResource webResource =
                getClient().resource(Preferences.jmasarServiceUrl + "/path/" + uniqueNodeId);
        ClientResponse response = webResource.get(ClientResponse.class);

        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
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
        WebResource webResource =
                getClient().resource(Preferences.jmasarServiceUrl + "/config")
                        .queryParam("parentNodeId", parentNodeId);
        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .entity(configuration, CONTENT_TYPE_JSON)
                .put(ClientResponse.class);
        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
            String message = Messages.createConfigurationFailed;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to parse response", e);
            }
            throw new SaveAndRestoreClientException(message);
        }
        return response.getEntity(Configuration.class);
    }

    @Override
    public Configuration updateConfiguration(Configuration configuration) {
        WebResource webResource = getClient().resource(Preferences.jmasarServiceUrl + "/config");

        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .entity(configuration, CONTENT_TYPE_JSON)
                .post(ClientResponse.class);
        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
            String message = Messages.updateConfigurationFailed;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to parse response", e);
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
    public Snapshot createSnapshot(String parentNodeId, Snapshot snapshot) {
        WebResource webResource =
                getClient().resource(Preferences.jmasarServiceUrl + "/snapshot")
                        .queryParam("parentNodeId", parentNodeId);
        ClientResponse response;
        try {
            response = webResource.accept(CONTENT_TYPE_JSON)
                    .entity(snapshot, CONTENT_TYPE_JSON)
                    .put(ClientResponse.class);
        } catch (UniformInterfaceException e) {
            throw new RuntimeException(e);
        }
        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
            String message = Messages.searchFailed;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to parse response", e);
            }
            throw new SaveAndRestoreClientException(message);
        }
        return response.getEntity(Snapshot.class);
    }

    @Override
    public Snapshot updateSnapshot(Snapshot snapshot) {
        WebResource webResource =
                getClient().resource(Preferences.jmasarServiceUrl + "/snapshot");
        ClientResponse response;
        try {
            response = webResource.accept(CONTENT_TYPE_JSON)
                    .entity(snapshot, CONTENT_TYPE_JSON)
                    .post(ClientResponse.class);
        } catch (UniformInterfaceException e) {
            throw new RuntimeException(e);
        }
        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
            String message = Messages.searchFailed;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to parse response", e);
            }
            throw new SaveAndRestoreClientException(message);
        }
        return response.getEntity(Snapshot.class);
    }


    @Override
    public CompositeSnapshot createCompositeSnapshot(String parentNodeId, CompositeSnapshot compositeSnapshot) {
        WebResource webResource =
                getClient().resource(Preferences.jmasarServiceUrl + "/composite-snapshot")
                        .queryParam("parentNodeId", parentNodeId);
        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .entity(compositeSnapshot, CONTENT_TYPE_JSON)
                .put(ClientResponse.class);
        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
            String message = Messages.createConfigurationFailed;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to parse response", e);
            }
            throw new SaveAndRestoreClientException(message);
        }
        return response.getEntity(CompositeSnapshot.class);
    }

    @Override
    public List<String> checkCompositeSnapshotConsistency(List<String> snapshotNodeIds) {
        WebResource webResource =
                getClient().resource(Preferences.jmasarServiceUrl + "/composite-snapshot-consistency-check");
        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .entity(snapshotNodeIds, CONTENT_TYPE_JSON)
                .post(ClientResponse.class);
        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
            String message = Messages.compositeSnapshotConsistencyCheckFailed;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to parse response", e);
            }
            throw new SaveAndRestoreClientException(message);
        }
        return response.getEntity(new GenericType<>() {
        });
    }

    @Override
    public CompositeSnapshot updateCompositeSnapshot(CompositeSnapshot compositeSnapshot) {
        WebResource webResource = getClient().resource(Preferences.jmasarServiceUrl + "/composite-snapshot");

        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .entity(compositeSnapshot, CONTENT_TYPE_JSON)
                .post(ClientResponse.class);
        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
            String message = Messages.updateConfigurationFailed;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to parse response", e);
            }
            throw new RuntimeException(message);
        }
        return response.getEntity(CompositeSnapshot.class);
    }

    @Override
    public SearchResult search(MultivaluedMap<String, String> searchParams) {
        WebResource webResource = getClient().resource(Preferences.jmasarServiceUrl + "/search")
                .queryParams(searchParams);
        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .get(ClientResponse.class);
        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
            String message = Messages.searchFailed;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to parse response", e);
            }
            throw new RuntimeException(message);
        }
        return response.getEntity(SearchResult.class);
    }

    @Override
    public Filter saveFilter(Filter filter) {
        WebResource webResource = getClient().resource(Preferences.jmasarServiceUrl + "/filter");
        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .entity(filter, CONTENT_TYPE_JSON)
                .put(ClientResponse.class);
        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
            String message = Messages.saveFilterFailed;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to parse response", e);
            }
            throw new RuntimeException(message);
        }
        return response.getEntity(Filter.class);
    }

    @Override
    public List<Filter> getAllFilters() {
        WebResource webResource = getClient().resource(Preferences.jmasarServiceUrl + "/filters");
        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .get(ClientResponse.class);
        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
            String message = Messages.searchFailed;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to parse response", e);
            }
            throw new RuntimeException(message);
        }
        return response.getEntity(new GenericType<>() {
        });
    }

    @Override
    public void deleteFilter(String name) {
        // Filter name may contain space chars, need to URL encode these.
        String filterName = name.replace(" ", "%20");
        WebResource webResource = getClient().resource(Preferences.jmasarServiceUrl + "/filter/" + filterName);
        ClientResponse response = webResource.accept(CONTENT_TYPE_JSON)
                .delete(ClientResponse.class);
        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
            String message = Messages.deleteFilterFailed;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to parse response", e);
            }
            throw new RuntimeException(message);
        }
    }

    /**
     * Adds a tag to a list of unique node ids, see {@link TagData}.
     *
     * @param tagData see {@link TagData}
     * @return A list of updated {@link Node}s. This may contain fewer elements than the list of unique node ids
     * passed in the <code>tagData</code> parameter.
     */
    public List<Node> addTag(TagData tagData) {

        WebResource webResource =
                getClient().resource(Preferences.jmasarServiceUrl + "/tags");
        ClientResponse response;
        try {
            response = webResource.accept(CONTENT_TYPE_JSON)
                    .entity(tagData, CONTENT_TYPE_JSON)
                    .post(ClientResponse.class);
        } catch (UniformInterfaceException e) {
            throw new RuntimeException(e);
        }
        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
            String message = Messages.tagAddFailed;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to parse response", e);
            }
            throw new SaveAndRestoreClientException(message);
        }
        return response.getEntity(new GenericType<>() {
        });
    }

    /**
     * Deletes a tag from a list of unique node ids, see {@link TagData}
     *
     * @param tagData see {@link TagData}
     * @return A list of updated {@link Node}s. This may contain fewer elements than the list of unique node ids
     * passed in the <code>tagData</code> parameter.
     */
    public List<Node> deleteTag(TagData tagData) {
        WebResource webResource =
                getClient().resource(Preferences.jmasarServiceUrl + "/tags");
        ClientResponse response;
        try {
            response = webResource.accept(CONTENT_TYPE_JSON)
                    .entity(tagData, CONTENT_TYPE_JSON)
                    .delete(ClientResponse.class);
        } catch (UniformInterfaceException e) {
            throw new RuntimeException(e);
        }
        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
            String message = Messages.tagAddFailed;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to parse response", e);
            }
            throw new SaveAndRestoreClientException(message);
        }
        return response.getEntity(new GenericType<>() {
        });
    }

    @Override
    public UserData authenticate(String userName, String password) {
        WebResource webResource =
                getClient().resource(Preferences.jmasarServiceUrl + "/login")
                        .queryParam("username", userName)
                        .queryParam("password", password);
        ClientResponse response;
        try {
            response = webResource.accept(CONTENT_TYPE_JSON)
                    .post(ClientResponse.class);
        } catch (UniformInterfaceException e) {
            throw new RuntimeException(e);
        }
        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
            String message = Messages.authenticationFailed;
            try {
                message = new String(response.getEntityInputStream().readAllBytes());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to parse response", e);
            }
            throw new SaveAndRestoreClientException(message);
        }
        return response.getEntity(new GenericType<>() {
        });
    }
}

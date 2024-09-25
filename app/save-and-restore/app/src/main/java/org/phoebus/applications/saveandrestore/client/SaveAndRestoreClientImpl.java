/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.SaveAndRestoreClientException;
import org.phoebus.applications.saveandrestore.model.CompositeSnapshot;
import org.phoebus.applications.saveandrestore.model.Configuration;
import org.phoebus.applications.saveandrestore.model.ConfigurationData;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.RestoreResult;
import org.phoebus.applications.saveandrestore.model.Snapshot;
import org.phoebus.applications.saveandrestore.model.SnapshotData;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.model.TagData;
import org.phoebus.applications.saveandrestore.model.UserData;
import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.applications.saveandrestore.model.search.SearchResult;
import org.phoebus.security.store.SecureStore;
import org.phoebus.security.tokens.AuthenticationScope;
import org.phoebus.security.tokens.ScopedAuthenticationToken;

import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SaveAndRestoreClientImpl implements SaveAndRestoreClient {

    private static final String CONTENT_TYPE_JSON = "application/json; charset=UTF-8";
    private static final Logger logger = Logger.getLogger(SaveAndRestoreClientImpl.class.getName());

    private static final int DEFAULT_READ_TIMEOUT = 5000; // ms
    private static final int DEFAULT_CONNECT_TIMEOUT = 5000; // ms
    private static final Log log = LogFactory.getLog(SaveAndRestoreClientImpl.class);

    private static ObjectMapper objectMapper;

    private static HttpClient client;

    static {
        int httpClientReadTimeout = Preferences.httpClientReadTimeout > 0 ? Preferences.httpClientReadTimeout : DEFAULT_READ_TIMEOUT;
        logger.log(Level.INFO, "Save&restore client using read timeout " + httpClientReadTimeout + " ms");

        int httpClientConnectTimeout = Preferences.httpClientConnectTimeout > 0 ? Preferences.httpClientConnectTimeout : DEFAULT_CONNECT_TIMEOUT;
        logger.log(Level.INFO, "Save&restore client using connect timeout " + httpClientConnectTimeout + " ms");
        CookieHandler.setDefault(new CookieManager());

        client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofMillis(httpClientConnectTimeout))
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private String getBasicAuthenticationHeader() {
        try {
            SecureStore store = new SecureStore();
            ScopedAuthenticationToken scopedAuthenticationToken = store.getScopedAuthenticationToken(AuthenticationScope.SAVE_AND_RESTORE);
            if (scopedAuthenticationToken != null) {
                String username = scopedAuthenticationToken.getUsername();
                String password = scopedAuthenticationToken.getPassword();
                return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to retrieve credentials from secure store", e);
        }
        return null;
    }

    @Override
    public String getServiceUrl() {
        return Preferences.jmasarServiceUrl;
    }

    @Override
    public Node getRoot() {
        return getNode(Node.ROOT_FOLDER_UNIQUE_ID);
    }

    @Override
    public Node getNode(String uniqueNodeId) {
        return getCall("/node/" + uniqueNodeId, Node.class);
    }

    @Override
    public List<Node> getCompositeSnapshotReferencedNodes(String uniqueNodeId) {
        return getCall("/composite-snapshot/" + uniqueNodeId + "/nodes", new TypeReference<>() {
        });
    }

    @Override
    public List<SnapshotItem> getCompositeSnapshotItems(String uniqueNodeId) {
        return getCall("/composite-snapshot/" + uniqueNodeId + "/items", new TypeReference<>() {
        });
    }

    @Override
    public Node getParentNode(String uniqueNodeId) {
        return getCall("/node/" + uniqueNodeId + "/parent", Node.class);
    }

    @Override
    public List<Node> getChildNodes(String uniqueNodeId) throws SaveAndRestoreClientException {
        return getCall("/node/" + uniqueNodeId + "/children", new TypeReference<>() {
        });
    }

    @Override
    public Node createNewNode(String parentsUniqueId, Node node) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.jmasarServiceUrl + "/node?parentNodeId=" + parentsUniqueId))
                    .header("Content-Type", CONTENT_TYPE_JSON)
                    .header("Authorization", getBasicAuthenticationHeader())
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(node)))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SaveAndRestoreClientException(response.body());
            }
            return objectMapper.readValue(response.body(), Node.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Node updateNode(Node nodeToUpdate) {
        return updateNode(nodeToUpdate, false);
    }

    @Override
    public Node updateNode(Node nodeToUpdate, boolean customTimeForMigration) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.jmasarServiceUrl + "/node?customTimeForMigration=" + customTimeForMigration))
                    .header("Content-Type", CONTENT_TYPE_JSON)
                    .header("Authorization", getBasicAuthenticationHeader())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(nodeToUpdate)))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SaveAndRestoreClientException(response.body());
            }
            return objectMapper.readValue(response.body(), Node.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteNodes(List<String> nodeIds) {
        // Native HttpClient does not support body in DELETE, so need to delete one by one...
        nodeIds.forEach(id -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(Preferences.jmasarServiceUrl + "/node/" + id))
                        .DELETE()
                        .header("Content-Type", CONTENT_TYPE_JSON)
                        .header("Authorization", getBasicAuthenticationHeader())
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new SaveAndRestoreClientException("Failed to delete node " + id + ", " + response.body());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public List<Tag> getAllTags() {
        return getCall("/tags", new TypeReference<>() {
        });
    }

    @Override
    public List<Node> getAllSnapshots() {
        return getCall("/snapshots", new TypeReference<>() {
        });
    }

    @Override
    public Node moveNodes(List<String> sourceNodeIds, String targetNodeId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.jmasarServiceUrl + "/move?to=" + targetNodeId))
                    .header("Content-Type", CONTENT_TYPE_JSON)
                    .header("Authorization", getBasicAuthenticationHeader())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(sourceNodeIds)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SaveAndRestoreClientException(response.body());
            }
            return objectMapper.readValue(response.body(), Node.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Node copyNodes(List<String> sourceNodeIds, String targetNodeId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.jmasarServiceUrl + "/copy?to=" + targetNodeId))
                    .header("Content-Type", CONTENT_TYPE_JSON)
                    .header("Authorization", getBasicAuthenticationHeader())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(sourceNodeIds)))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SaveAndRestoreClientException(response.body());
            }
            return objectMapper.readValue(response.body(), Node.class);
        }
        catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getFullPath(String uniqueNodeId) {
        return getCall("/path/" + uniqueNodeId, String.class);
    }

    @Override
    public ConfigurationData getConfigurationData(String nodeId) {
        return getCall("/config/" + nodeId, ConfigurationData.class);
    }

    @Override
    public Configuration createConfiguration(String parentNodeId, Configuration configuration) {

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.jmasarServiceUrl + "/config?parentNodeId=" + parentNodeId))
                    .header("Content-Type", CONTENT_TYPE_JSON)
                    .header("Authorization", getBasicAuthenticationHeader())
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(configuration)))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SaveAndRestoreClientException(response.body());
            }
            return objectMapper.readValue(response.body(), Configuration.class);
        }
        catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public Configuration updateConfiguration(Configuration configuration) {

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.jmasarServiceUrl + "/config"))
                    .header("Content-Type", CONTENT_TYPE_JSON)
                    .header("Authorization", getBasicAuthenticationHeader())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(configuration)))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SaveAndRestoreClientException(response.body());
            }
            return objectMapper.readValue(response.body(), Configuration.class);
        }
        catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public SnapshotData getSnapshotData(String uniqueId) {
        return getCall("/snapshot/" + uniqueId, SnapshotData.class);
    }

    @Override
    public Snapshot createSnapshot(String parentNodeId, Snapshot snapshot) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.jmasarServiceUrl + "/snapshot?parentNodeId=" + parentNodeId))
                    .header("Content-Type", CONTENT_TYPE_JSON)
                    .header("Authorization", getBasicAuthenticationHeader())
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(snapshot)))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SaveAndRestoreClientException(response.body());
            }
            return objectMapper.readValue(response.body(), Snapshot.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Snapshot updateSnapshot(Snapshot snapshot) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.jmasarServiceUrl + "/snapshot"))
                    .header("Content-Type", CONTENT_TYPE_JSON)
                    .header("Authorization", getBasicAuthenticationHeader())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(snapshot)))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SaveAndRestoreClientException(response.body());
            }
            return objectMapper.readValue(response.body(), Snapshot.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompositeSnapshot createCompositeSnapshot(String parentNodeId, CompositeSnapshot compositeSnapshot) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.jmasarServiceUrl + "/composite-snapshot?parentNodeId=" + parentNodeId))
                    .header("Content-Type", CONTENT_TYPE_JSON)
                    .header("Authorization", getBasicAuthenticationHeader())
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(compositeSnapshot)))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SaveAndRestoreClientException(response.body());
            }
            return objectMapper.readValue(response.body(), CompositeSnapshot.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> checkCompositeSnapshotConsistency(List<String> snapshotNodeIds) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.jmasarServiceUrl + "/composite-snapshot-consistency-check"))
                    .header("Content-Type", CONTENT_TYPE_JSON)
                    .header("Authorization", getBasicAuthenticationHeader())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(snapshotNodeIds)))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SaveAndRestoreClientException(response.body());
            }
            return objectMapper.readValue(response.body(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompositeSnapshot updateCompositeSnapshot(CompositeSnapshot compositeSnapshot) {
        return null;
    }

    @Override
    public SearchResult search(MultivaluedMap<String, String> searchParams) {
        return null;
    }

    @Override
    public Filter saveFilter(Filter filter) {
        return null;
    }

    @Override
    public List<Filter> getAllFilters() {
        return getCall("/filters", new TypeReference<>() {
        });
    }

    @Override
    public void deleteFilter(String name) {
        String filterName = name.replace(" ", "%20");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.jmasarServiceUrl + "/filter/" + filterName))
                    .DELETE()
                    .header("Authorization", getBasicAuthenticationHeader())
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException(response.body() != null ? response.body() : Messages.deleteFilterFailed);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Node> addTag(TagData tagData) {
        return List.of();
    }

    @Override
    public List<Node> deleteTag(TagData tagData) {
        return List.of();
    }

    @Override
    public UserData authenticate(String userName, String password) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Preferences.jmasarServiceUrl)
                .append("/login?username=")
                .append(userName)
                .append("&password=")
                .append(password);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(stringBuilder.toString()))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), UserData.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<RestoreResult> restore(List<SnapshotItem> snapshotItems) {
        return List.of();
    }

    @Override
    public List<RestoreResult> restore(String snapshotNodeId) {
        return List.of();
    }

    @Override
    public List<SnapshotItem> takeSnapshot(String configurationNodeId) {
        return List.of();
    }

    private <T> T getCall(String relativeUrl, Class<T> clazz) {
        HttpResponse<String> response = getCall(relativeUrl);
        try {
            return objectMapper.readValue(response.body(), clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T getCall(String relativeUrl, TypeReference<T> typeReference) {
        HttpResponse<String> response = getCall(relativeUrl);
        try {
            return objectMapper.readValue(response.body(), typeReference);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpResponse<String> getCall(String relativeUrl) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Preferences.jmasarServiceUrl + relativeUrl))
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            if (response.statusCode() != 200) {
                if (responseBody == null || responseBody.isEmpty()) {
                    responseBody = "N/A";
                }
                throw new SaveAndRestoreClientException("Failed : HTTP error code : " + response.statusCode() + ", error message: " + responseBody);
            }
            return response;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

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
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.SaveAndRestoreClientException;
import org.phoebus.applications.saveandrestore.model.CompositeSnapshot;
import org.phoebus.applications.saveandrestore.model.Configuration;
import org.phoebus.applications.saveandrestore.model.ConfigurationData;
import org.phoebus.applications.saveandrestore.model.LoginCredentials;
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
import org.phoebus.util.http.QueryParamsHelper;

import javax.ws.rs.core.MultivaluedMap;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SaveAndRestoreClientImpl implements SaveAndRestoreClient {

    private static final String CONTENT_TYPE_JSON = "application/json; charset=UTF-8";
    private static final Logger LOGGER = Logger.getLogger(SaveAndRestoreClientImpl.class.getName());

    private static final int DEFAULT_READ_TIMEOUT = 5000; // ms
    private static final int DEFAULT_CONNECT_TIMEOUT = 5000; // ms

    private static final ObjectMapper OBJECT_MAPPER;

    private static final HttpClient CLIENT;

    static {
        int httpClientReadTimeout = Preferences.httpClientReadTimeout > 0 ? Preferences.httpClientReadTimeout : DEFAULT_READ_TIMEOUT;
        LOGGER.log(Level.INFO, "Save&restore client using read timeout " + httpClientReadTimeout + " ms");

        int httpClientConnectTimeout = Preferences.httpClientConnectTimeout > 0 ? Preferences.httpClientConnectTimeout : DEFAULT_CONNECT_TIMEOUT;
        LOGGER.log(Level.INFO, "Save&restore client using connect timeout " + httpClientConnectTimeout + " ms");
        CookieHandler.setDefault(new CookieManager());

        CLIENT = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofMillis(httpClientConnectTimeout))
                .build();
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
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
            LOGGER.log(Level.WARNING, "Unable to retrieve credentials from secure store", e);
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

    /**
     * {@inheritDoc}
     *
     * @param parentsUniqueId Unique id of the parent {@link Node} for the new {@link Node}
     * @param node            A {@link Node} object that should be created (=persisted).
     * @return {@inheritDoc}
     */
    @Override
    public Node createNewNode(String parentsUniqueId, Node node) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.jmasarServiceUrl + "/node?parentNodeId=" + parentsUniqueId))
                    .header("Content-Type", CONTENT_TYPE_JSON)
                    .header("Authorization", getBasicAuthenticationHeader())
                    .PUT(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(node)))
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SaveAndRestoreClientException(response.body());
            }
            return OBJECT_MAPPER.readValue(response.body(), Node.class);
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
                    .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(nodeToUpdate)))
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SaveAndRestoreClientException(response.body());
            }
            return OBJECT_MAPPER.readValue(response.body(), Node.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param nodeIds List of unique {@link Node} ids.
     */
    @Override
    public void deleteNodes(List<String> nodeIds) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.jmasarServiceUrl + "/node"))
                    .method("DELETE", HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(nodeIds)))
                    .header("Content-Type", CONTENT_TYPE_JSON)
                    .header("Authorization", getBasicAuthenticationHeader())
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SaveAndRestoreClientException("Failed to delete nodes: " + response.body());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public List<Tag> getAllTags() {
        return getCall("/tags", new TypeReference<>() {
        });
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceNodeIds List of unique {@link Node} ids.
     * @param targetNodeId  The unique id of the parent {@link Node} to which the source {@link Node}s are moved.
     * @return
     */
    @Override
    public Node moveNodes(List<String> sourceNodeIds, String targetNodeId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.jmasarServiceUrl + "/move?to=" + targetNodeId))
                    .header("Content-Type", CONTENT_TYPE_JSON)
                    .header("Authorization", getBasicAuthenticationHeader())
                    .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(sourceNodeIds)))
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SaveAndRestoreClientException(response.body());
            }
            return OBJECT_MAPPER.readValue(response.body(), Node.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceNodeIds List of unique {@link Node} ids.
     * @param targetNodeId  The unique id of the parent {@link Node} to which the source {@link Node}s are copied.
     * @return
     */
    @Override
    public Node copyNodes(List<String> sourceNodeIds, String targetNodeId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.jmasarServiceUrl + "/copy?to=" + targetNodeId))
                    .header("Content-Type", CONTENT_TYPE_JSON)
                    .header("Authorization", getBasicAuthenticationHeader())
                    .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(sourceNodeIds)))
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SaveAndRestoreClientException(response.body());
            }
            return OBJECT_MAPPER.readValue(response.body(), Node.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param uniqueNodeId Unique id
     * @return
     */
    @Override
    public String getFullPath(String uniqueNodeId) {
        return getCall("/path/" + uniqueNodeId, String.class);
    }

    @Override
    public ConfigurationData getConfigurationData(String nodeId) {
        return getCall("/config/" + nodeId, ConfigurationData.class);
    }

    /**
     * {@inheritDoc}
     *
     * @param parentNodeId  Non-null and non-empty unique id of an existing parent {@link Node},
     *                      which must be of type {@link org.phoebus.applications.saveandrestore.model.NodeType#FOLDER}.
     * @param configuration {@link ConfigurationData} object
     * @return
     */
    @Override
    public Configuration createConfiguration(String parentNodeId, Configuration configuration) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.jmasarServiceUrl + "/config?parentNodeId=" + parentNodeId))
                    .header("Content-Type", CONTENT_TYPE_JSON)
                    .header("Authorization", getBasicAuthenticationHeader())
                    .PUT(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(configuration)))
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SaveAndRestoreClientException(response.body());
            }
            return OBJECT_MAPPER.readValue(response.body(), Configuration.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param configuration
     * @return
     */
    @Override
    public Configuration updateConfiguration(Configuration configuration) {

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.jmasarServiceUrl + "/config"))
                    .header("Content-Type", CONTENT_TYPE_JSON)
                    .header("Authorization", getBasicAuthenticationHeader())
                    .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(configuration)))
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SaveAndRestoreClientException(response.body());
            }
            return OBJECT_MAPPER.readValue(response.body(), Configuration.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SnapshotData getSnapshotData(String uniqueId) {
        return getCall("/snapshot/" + uniqueId, SnapshotData.class);
    }

    /**
     * {@inheritDoc}
     *
     * @param parentNodeId The unique id of the configuration {@link Node} associated with the {@link Snapshot}
     * @param snapshot     The {@link Snapshot} data object.
     * @return {@inheritDoc}
     */
    @Override
    public Snapshot createSnapshot(String parentNodeId, Snapshot snapshot) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.jmasarServiceUrl + "/snapshot?parentNodeId=" + parentNodeId))
                    .header("Content-Type", CONTENT_TYPE_JSON)
                    .header("Authorization", getBasicAuthenticationHeader())
                    .PUT(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(snapshot)))
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SaveAndRestoreClientException(response.body());
            }
            return OBJECT_MAPPER.readValue(response.body(), Snapshot.class);
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
                    .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(snapshot)))
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SaveAndRestoreClientException(response.body());
            }
            return OBJECT_MAPPER.readValue(response.body(), Snapshot.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param parentNodeId      The parent {@link Node} for the new {@link CompositeSnapshot}
     * @param compositeSnapshot The data object
     * @return A {@link CompositeSnapshot} as persisted by the service.
     */
    @Override
    public CompositeSnapshot createCompositeSnapshot(String parentNodeId, CompositeSnapshot compositeSnapshot) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.jmasarServiceUrl + "/composite-snapshot?parentNodeId=" + parentNodeId))
                    .header("Content-Type", CONTENT_TYPE_JSON)
                    .header("Authorization", getBasicAuthenticationHeader())
                    .PUT(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(compositeSnapshot)))
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SaveAndRestoreClientException(response.body());
            }
            return OBJECT_MAPPER.readValue(response.body(), CompositeSnapshot.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param snapshotNodeIds List of {@link Node} ids corresponding to {@link Node}s of types
     *                        {@link org.phoebus.applications.saveandrestore.model.NodeType#SNAPSHOT}
     *                        and {@link org.phoebus.applications.saveandrestore.model.NodeType#COMPOSITE_SNAPSHOT}
     * @return
     */
    @Override
    public List<String> checkCompositeSnapshotConsistency(List<String> snapshotNodeIds) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.jmasarServiceUrl + "/composite-snapshot-consistency-check"))
                    .header("Content-Type", CONTENT_TYPE_JSON)
                    .header("Authorization", getBasicAuthenticationHeader())
                    .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(snapshotNodeIds)))
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SaveAndRestoreClientException(response.body());
            }
            return OBJECT_MAPPER.readValue(response.body(), new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompositeSnapshot updateCompositeSnapshot(CompositeSnapshot compositeSnapshot) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.jmasarServiceUrl + "/composite-snapshot"))
                    .header("Content-Type", CONTENT_TYPE_JSON)
                    .header("Authorization", getBasicAuthenticationHeader())
                    .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(compositeSnapshot)))
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SaveAndRestoreClientException(response.body());
            }
            return OBJECT_MAPPER.readValue(response.body(), CompositeSnapshot.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SearchResult search(MultivaluedMap<String, String> searchParams) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.jmasarServiceUrl + "/search?" + QueryParamsHelper.mapToQueryParams(searchParams)))
                    .header("Content-Type", CONTENT_TYPE_JSON)
                    .GET()
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SaveAndRestoreClientException(response.body());
            }
            return OBJECT_MAPPER.readValue(response.body(), SearchResult.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Filter saveFilter(Filter filter) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.jmasarServiceUrl + "/filter"))
                    .header("Content-Type", CONTENT_TYPE_JSON)
                    .header("Authorization", getBasicAuthenticationHeader())
                    .PUT(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(filter)))
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SaveAndRestoreClientException(response.body());
            }
            return OBJECT_MAPPER.readValue(response.body(), Filter.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public List<Filter> getAllFilters() {
        return getCall("/filters", new TypeReference<>() {
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteFilter(String name) {
        String filterName = name.replace(" ", "%20");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.jmasarServiceUrl + "/filter/" + filterName))
                    .DELETE()
                    .header("Authorization", getBasicAuthenticationHeader())
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException(response.body() != null ? response.body() : Messages.deleteFilterFailed);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param tagData see {@link TagData}
     * @return
     */
    @Override
    public List<Node> addTag(TagData tagData) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.jmasarServiceUrl + "/tags"))
                    .header("Content-Type", CONTENT_TYPE_JSON)
                    .header("Authorization", getBasicAuthenticationHeader())
                    .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(tagData)))
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SaveAndRestoreClientException(response.body());
            }
            return OBJECT_MAPPER.readValue(response.body(), new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Node> deleteTag(TagData tagData) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.jmasarServiceUrl + "/tags"))
                    .header("Content-Type", CONTENT_TYPE_JSON)
                    .header("Authorization", getBasicAuthenticationHeader())
                    .method("DELETE", HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(tagData)))
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SaveAndRestoreClientException(response.body());
            }
            return OBJECT_MAPPER.readValue(response.body(), new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param userName User's account name
     * @param password User's password
     * @return {@inheritDoc}
     */
    @Override
    public UserData authenticate(String userName, String password) {
        try {
            String stringBuilder = Preferences.jmasarServiceUrl +
                    "/login";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(stringBuilder))
                .header("Content-Type", CONTENT_TYPE_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(new LoginCredentials(userName, password))))
                .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            return OBJECT_MAPPER.readValue(response.body(), UserData.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param snapshotItems A {@link List} of {@link SnapshotItem}s
     * @return {@inheritDoc}
     */
    @Override
    public List<RestoreResult> restore(List<SnapshotItem> snapshotItems) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.jmasarServiceUrl + "/restore/items"))
                    .header("Content-Type", CONTENT_TYPE_JSON)
                    .header("Authorization", getBasicAuthenticationHeader())
                    .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(snapshotItems)))
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SaveAndRestoreClientException(response.body());
            }
            return OBJECT_MAPPER.readValue(response.body(), new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param snapshotNodeId Unique id of a snapshot
     * @return {@inheritDoc}
     */
    @Override
    public List<RestoreResult> restore(String snapshotNodeId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.jmasarServiceUrl + "/restore/node?nodeId=" + snapshotNodeId))
                    .header("Content-Type", CONTENT_TYPE_JSON)
                    .header("Authorization", getBasicAuthenticationHeader())
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new SaveAndRestoreClientException(response.body());
            }
            return OBJECT_MAPPER.readValue(response.body(), new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param configurationNodeId The unique id of the {@link Configuration} for which to take the snapshot
     * @return {@inheritDoc}
     */
    @Override
    public List<SnapshotItem> takeSnapshot(String configurationNodeId) {
        return getCall("/take-snapshot/" + configurationNodeId, new TypeReference<>() {
        });
    }

    private <T> T getCall(String relativeUrl, Class<T> clazz) {
        HttpResponse<String> response = getCall(relativeUrl);
        try {
            return OBJECT_MAPPER.readValue(response.body(), clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T getCall(String relativeUrl, TypeReference<T> typeReference) {
        HttpResponse<String> response = getCall(relativeUrl);
        try {
            return OBJECT_MAPPER.readValue(response.body(), typeReference);
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
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
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

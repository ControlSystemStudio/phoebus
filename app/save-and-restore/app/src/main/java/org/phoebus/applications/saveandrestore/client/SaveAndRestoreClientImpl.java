/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
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

public class SaveAndRestoreClientImpl implements SaveAndRestoreClient{

    private static final String CONTENT_TYPE_JSON = "application/json; charset=UTF-8";
    private static final Logger logger = Logger.getLogger(SaveAndRestoreClientImpl.class.getName());

    private static final int DEFAULT_READ_TIMEOUT = 5000; // ms
    private static final int DEFAULT_CONNECT_TIMEOUT = 5000; // ms

    private static ObjectMapper objectMapper;

    private static HttpClient client;

    static{
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
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Preferences.jmasarServiceUrl + "/node/" + Node.ROOT_FOLDER_UNIQUE_ID))
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public Node getNode(String uniqueNodeId) {
        return null;
    }

    @Override
    public List<Node> getCompositeSnapshotReferencedNodes(String uniqueNodeId) {
        return List.of();
    }

    @Override
    public List<SnapshotItem> getCompositeSnapshotItems(String uniqueNodeId) {
        return List.of();
    }

    @Override
    public Node getParentNode(String unqiueNodeId) {
        return null;
    }

    @Override
    public List<Node> getChildNodes(String uniqueNodeId) throws SaveAndRestoreClientException {
        return List.of();
    }

    @Override
    public Node createNewNode(String parentsUniqueId, Node node) {
        return null;
    }

    @Override
    public Node updateNode(Node nodeToUpdate) {
        return null;
    }

    @Override
    public Node updateNode(Node nodeToUpdate, boolean customTimeForMigration) {
        return null;
    }

    @Override
    public void deleteNodes(List<String> nodeIds) {

    }

    @Override
    public List<Tag> getAllTags() {
        return List.of();
    }

    @Override
    public List<Node> getAllSnapshots() {
        return List.of();
    }

    @Override
    public Node moveNodes(List<String> sourceNodeIds, String targetNodeId) {
        return null;
    }

    @Override
    public Node copyNodes(List<String> sourceNodeIds, String targetNodeId) {
        return null;
    }

    @Override
    public String getFullPath(String uniqueNodeId) {
        return "";
    }

    @Override
    public List<Node> getFromPath(String path) {
        return List.of();
    }

    @Override
    public ConfigurationData getConfigurationData(String nodeId) {
        return null;
    }

    @Override
    public Configuration createConfiguration(String parentNodeId, Configuration configuration) {
        return null;
    }

    @Override
    public Configuration updateConfiguration(Configuration configuration) {
        return null;
    }

    @Override
    public SnapshotData getSnapshotData(String uniqueId) {
        return null;
    }

    @Override
    public Snapshot createSnapshot(String parentNodeId, Snapshot snapshot) {
        return null;
    }

    @Override
    public Snapshot updateSnapshot(Snapshot snapshot) {
        return null;
    }

    @Override
    public CompositeSnapshot createCompositeSnapshot(String parentNodeId, CompositeSnapshot compositeSnapshot) {
        return null;
    }

    @Override
    public List<String> checkCompositeSnapshotConsistency(List<String> snapshotNodeIds) {
        return List.of();
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
        return List.of();
    }

    @Override
    public void deleteFilter(String name) {

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
        return null;
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
}

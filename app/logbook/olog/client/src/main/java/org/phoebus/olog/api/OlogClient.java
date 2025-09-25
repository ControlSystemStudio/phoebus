package org.phoebus.olog.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogEntryLevel;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.LogbookException;
import org.phoebus.logbook.Messages;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.SearchResult;
import org.phoebus.logbook.Tag;
import org.phoebus.security.store.SecureStore;
import org.phoebus.security.tokens.AuthenticationScope;
import org.phoebus.security.tokens.ScopedAuthenticationToken;
import org.phoebus.util.http.HttpRequestMultipartBody;
import org.phoebus.util.http.QueryParamsHelper;
import org.phoebus.util.time.TimeParser;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.Socket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.phoebus.util.time.TimestampFormats.MILLI_FORMAT;

/**
 * A logbook client to tne Olog logbook service
 */
public class OlogClient implements LogClient {

    private static final Logger logger = Logger.getLogger(OlogClient.class.getName());
    private final HttpClient httpClient;
    private final ExecutorService executor;
    private final String basicAuthenticationHeader;

    private static final Logger LOGGER = Logger.getLogger(OlogClient.class.getName());

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * Builder Class to help create a olog client.
     *
     * @author shroffk
     */
    public static class OlogClientBuilder {

        private String username = null;
        private String password = null;
        private final URI uri;

        private OlogClientBuilder() {
            this.uri = URI.create(Preferences.olog_url);
        }

        /**
         * Set the username to be used for HTTP Authentication.
         *
         * @param username User account name
         * @return {@link OlogClientBuilder}
         */
        public OlogClientBuilder username(String username) {
            this.username = username;
            return this;
        }

        /**
         * Set the password to be used for the HTTP Authentication.
         *
         * @param password User's password
         * @return {@link OlogClientBuilder}
         */
        public OlogClientBuilder password(String password) {
            this.password = password;
            return this;
        }

        private ScopedAuthenticationToken getCredentialsFromSecureStore() {
            try {
                SecureStore secureStore = new SecureStore();
                return secureStore.getScopedAuthenticationToken(new OlogAuthenticationScope());
            } catch (Exception e) {
                Logger.getLogger(OlogClient.class.getName()).log(Level.WARNING, "Unable to instantiate SecureStore", e);
                return null;
            }
        }

        public OlogClient build() {
            if (this.username == null || this.password == null) {
                ScopedAuthenticationToken scopedAuthenticationToken = getCredentialsFromSecureStore();
                if (scopedAuthenticationToken != null) {
                    this.username = scopedAuthenticationToken.getUsername();
                    this.password = scopedAuthenticationToken.getPassword();
                }
            }

            HttpClient httpClient;

            if (uri.getScheme().equalsIgnoreCase("https")) {
                try {
                    SSLContext sslContext = SSLContext.getInstance("SSL"); // OR TLS
                    sslContext.init(null, new PromiscuousTrustManager[]{new PromiscuousTrustManager()}, new SecureRandom());
                    if (Preferences.connectTimeout > 0) {
                        httpClient = HttpClient.newBuilder()
                                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                                .followRedirects(HttpClient.Redirect.ALWAYS)
                                .connectTimeout(Duration.ofMillis(Preferences.connectTimeout))
                                .sslContext(sslContext)
                                .build();
                    } else {
                        httpClient = HttpClient.newBuilder()
                                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                                .followRedirects(HttpClient.Redirect.ALWAYS)
                                .connectTimeout(Duration.ofMillis(Preferences.connectTimeout))
                                .sslContext(sslContext)
                                .build();
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING,
                            "Failed to properly create the olog rest client to: " + Preferences.olog_url
                            , e);
                    throw new RuntimeException(e);
                }
            } else {
                if (Preferences.connectTimeout > 0) {
                    httpClient = HttpClient.newBuilder()
                            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                            .followRedirects(HttpClient.Redirect.ALWAYS)
                            .connectTimeout(Duration.ofMillis(Preferences.connectTimeout))
                            .build();
                } else {
                    httpClient = HttpClient.newBuilder()
                            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                            .followRedirects(HttpClient.Redirect.ALWAYS)
                            .connectTimeout(Duration.ofMillis(Preferences.connectTimeout))
                            .build();
                }
            }

            return new OlogClient(httpClient, this.username, this.password);
        }
    }

    public static OlogClientBuilder builder() {
        return new OlogClientBuilder();
    }

    private OlogClient(HttpClient httpClient, String userName, String password) {
        this.httpClient = httpClient;
        executor = Executors.newSingleThreadExecutor();
        basicAuthenticationHeader = "Basic " + Base64.getEncoder().encodeToString((userName != null ? userName : Preferences.username + ":" +
                password != null ? password : Preferences.password).getBytes());

    }

    // A predefined set of levels supported by olog
    private final List<String> levels = Arrays.asList("Urgent", "Suggestion", "Info", "Request", "Problem");

    @Override
    public Collection<LogEntryLevel> listLevels() {
        return levels.stream().map(l -> new LogEntryLevel(l, false)).toList();
    }

    @Override
    public Collection<Logbook> listLogbooks() {
        return wrappedSubmit(new Callable<>() {

            @Override
            public Collection<Logbook> call() throws Exception {

                Collection<Logbook> allLogbooks = new HashSet<>();

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(Preferences.olog_url + "/logbooks"))
                        .header("Accept", MediaType.APPLICATION_XML)
                        .GET()
                        .build();
                HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (httpResponse.statusCode() < 300) {
                    LOGGER.log(Level.WARNING, "Failed to retrieve logbooks list: " + httpResponse.body());
                    throw new OlogException(httpResponse.statusCode(), httpResponse.body());
                }

                XmlLogbooks allXmlLogbooks = OBJECT_MAPPER.readValue(httpResponse.body(), XmlLogbooks.class);
                for (XmlLogbook xmlLogbook : allXmlLogbooks.getLogbooks()) {
                    allLogbooks.add(new OlogLogbook(xmlLogbook));
                }
                return allLogbooks;
            }
        });
    }

    @Override
    public Collection<Tag> listTags() {
        return wrappedSubmit(new Callable<>() {

            @Override
            public Collection<Tag> call() throws Exception {

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(Preferences.olog_url + "/tags"))
                        .header("Accept", MediaType.APPLICATION_XML)
                        .GET()
                        .build();
                HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (httpResponse.statusCode() != 200) {
                    LOGGER.log(Level.WARNING, "Failed to retrieve tags list: " + httpResponse.body());
                    throw new OlogException(httpResponse.statusCode(), httpResponse.body());
                }

                Collection<Tag> allTags = new HashSet<>();
                XmlTags allXmlTags = OBJECT_MAPPER.readValue(httpResponse.body(), XmlTags.class);
                for (XmlTag xmlTag : allXmlTags.getTags()) {
                    allTags.add(new OlogTag(xmlTag));
                }
                return allTags;
            }

        });
    }

    @Override
    public Collection<Property> listProperties() {
        return wrappedSubmit(new Callable<>() {
            @Override
            public Collection<Property> call() throws Exception {

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(Preferences.olog_url + "/properties"))
                        .header("Accept", MediaType.APPLICATION_XML)
                        .header("Accept", MediaType.APPLICATION_JSON)
                        .GET()
                        .build();
                HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (httpResponse.statusCode() != 200) {
                    LOGGER.log(Level.WARNING, "Failed to retrieve properties list: " + httpResponse.body());
                    throw new OlogException(httpResponse.statusCode(), httpResponse.body());
                }

                Collection<Property> allProperties = new HashSet<>();
                XmlProperties xmlProperties = OBJECT_MAPPER.readValue(httpResponse.body(), XmlProperties.class);
                for (XmlProperty xmlProperty : xmlProperties.getProperties()) {
                    allProperties.add(new OlogProperty(xmlProperty));
                }
                return allProperties;
            }
        });
    }

    @Override
    public Collection<String> listAttributes(String propertyName) {
        return getProperty(propertyName).getAttributes().keySet();
    }

    @Override
    public Collection<LogEntry> listLogs() {
        return wrappedSubmit(new Callable<>() {
            @Override
            public Collection<LogEntry> call() throws Exception {


                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(Preferences.olog_url + "/logs"))
                        .header("Accept", MediaType.APPLICATION_XML)
                        .header("Accept", MediaType.APPLICATION_JSON)
                        .GET()
                        .build();
                HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (httpResponse.statusCode() != 200) {
                    LOGGER.log(Level.WARNING, "Failed to retrieve log entries: " + httpResponse.body());
                    throw new OlogException(httpResponse.statusCode(), httpResponse.body());
                }

                XmlLogs xmlLogs = OBJECT_MAPPER.readValue(httpResponse.body(), XmlLogs.class);

                return LogUtil.toLogs(xmlLogs);
            }
        });
    }

    @Override
    public LogEntry getLog(Long logId) {
        return findLogById(logId);
    }

    @Override
    public Collection<Attachment> listAttachments(Long logId) {
        return wrappedSubmit(new Callable<>() {

            @Override
            public Collection<Attachment> call() throws Exception {


                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(Preferences.olog_url + "/attachments"))
                        .header("Accept", MediaType.APPLICATION_XML)
                        .GET()
                        .build();
                HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (httpResponse.statusCode() != 200) {
                    LOGGER.log(Level.WARNING, "Failed to retrieve attachments list: " + httpResponse.body());
                    throw new OlogException(httpResponse.statusCode(), httpResponse.body());
                }
                Collection<Attachment> allAttachments = new HashSet<>();

                XmlAttachments allXmlAttachments = OBJECT_MAPPER.readValue(httpResponse.body(), XmlAttachments.class);
                for (XmlAttachment xmlAttachment : allXmlAttachments.getAttachments()) {
                    allAttachments.add(new OlogAttachment(xmlAttachment));
                }
                return allAttachments;
            }

        });
    }

    @Override
    public InputStream getAttachment(Long logId, Attachment attachment) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.olog_url + "/attachments/" +
                            URLEncoder.encode(attachment.getName(), StandardCharsets.UTF_8).replace("+", "%20")))
                    .header("Accept", MediaType.APPLICATION_XML)
                    .GET()
                    .build();
            HttpResponse<InputStream> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

            if (httpResponse.statusCode() < 300){
                LOGGER.log(Level.WARNING, "Failed to get attachment");
                throw new OlogException("Failed to get attachment");
            }
        } catch (Exception e) {
        }
        return null;
    }

    @Override
    public InputStream getAttachment(Long logId, String attachmentName) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.olog_url + "/attachments/" + logId.toString() + "/" +
                            URLEncoder.encode(attachmentName, StandardCharsets.UTF_8).replace("+", "%20")))
                    .header("Accept", MediaType.APPLICATION_XML)
                    .GET()
                    .build();
            HttpResponse<InputStream> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

            if (httpResponse.statusCode() < 300){
                LOGGER.log(Level.WARNING, "Failed to get attachment");
                throw new OlogException("Failed to get attachment");
            }
        } catch (Exception e) {
        }
        return null;
    }

    @Override
    public Property getProperty(String property) {
        return wrappedSubmit(new Callable<>() {

            @Override
            public Property call() throws Exception {
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(Preferences.olog_url + "/properties/" +
                                URLEncoder.encode(property, StandardCharsets.UTF_8).replace("+", "%20")))
                        .header("Accept", MediaType.APPLICATION_XML)
                        .header("Accept", MediaType.APPLICATION_XML)
                        .GET()
                        .build();
                HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                if(httpResponse.statusCode() < 300){
                    LOGGER.log(Level.WARNING, "Failed to get property " + property);
                    throw new OlogException(httpResponse.statusCode(), httpResponse.body());
                }
                return new OlogProperty(OBJECT_MAPPER.readValue(httpResponse.body(), XmlProperty.class));
            }
        });
    }

    @Override
    public LogEntry set(LogEntry log) throws LogbookException {
        try {
            Collection<LogEntry> result = wrappedSubmit(new SetLogs(log));
            if (result.size() == 1) {
                return result.iterator().next();
            } else {
                throw new LogbookException(Messages.SubmissionFailed);
            }
        } catch (Exception e) {
            throw new LogbookException(e.getCause());
        }
    }

    private class SetLogs implements Callable<Collection<LogEntry>> {
        private final Collection<LogEntry> logs;

        public SetLogs(LogEntry log) {
            this.logs = new ArrayList<>();
            this.logs.add(log);
        }

        @Override
        public Collection<LogEntry> call() {
            Collection<LogEntry> returnLogs = new HashSet<>();
            for (LogEntry log : logs) {
                XmlLogs xmlLogs = new XmlLogs();
                XmlLog xmlLog = new XmlLog(log);
                xmlLog.setLevel("Info");
                xmlLogs.getLogs().add(xmlLog);

                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(Preferences.olog_url + "/logs"))
                            .header("Accept", MediaType.APPLICATION_JSON)
                            .header("Accept", MediaType.APPLICATION_XML)
                            .header("Content-Type", MediaType.APPLICATION_JSON)
                            .header("Authorization", basicAuthenticationHeader)
                            .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(xmlLogs)))
                            .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() < 300) {
                        LOGGER.log(Level.WARNING, "Failed to create log entry");
                        throw new OlogException(response.statusCode(), response.body());
                    }

                    // XXX there is an assumption that the result without an error status will consist of a single created log entry
                    XmlLog createdLog = OBJECT_MAPPER.readValue(response.body(), XmlLogs.class).getLogs().iterator().next();
                    //XmlLog createdLog = clientResponse.getEntity(XmlLogs.class).getLogs().iterator().next();
                    log.getAttachments().forEach(attachment -> {

                        HttpRequestMultipartBody httpRequestMultipartBody = new HttpRequestMultipartBody();
                        httpRequestMultipartBody.addFilePart(attachment.getFile());

                        HttpRequest attachmentRequest = HttpRequest.newBuilder()
                                .uri(URI.create(Preferences.olog_url + "/attachments/" + createdLog.getId()))
                                .header("Accept", MediaType.APPLICATION_XML)
                                .header("Content-Type", httpRequestMultipartBody.getContentType())
                                .header("Authorization", basicAuthenticationHeader)
                                .POST(HttpRequest.BodyPublishers.ofByteArray(httpRequestMultipartBody.getBytes()))
                                .build();

                        try {
                            HttpResponse<String> attachmentResponse = httpClient.send(attachmentRequest, HttpResponse.BodyHandlers.ofString());

                            if (attachmentResponse.statusCode() < 300) {
                                LOGGER.log(Level.WARNING, "Failed to create attachment");
                                throw new OlogException(attachmentResponse.statusCode(), attachmentResponse.body());
                            }

                            createdLog.addXmlAttachment(OBJECT_MAPPER.readValue(attachmentResponse.body(),
                                    XmlAttachment.class));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                    returnLogs.add(new OlogLog(createdLog));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return Collections.unmodifiableCollection(returnLogs);
        }
    }

    @Override
    public LogEntry update(LogEntry log) {
        return wrappedSubmit(new UpdateLog(log));
    }

    private class UpdateLog implements Callable<LogEntry> {
        private final XmlLog log;

        public UpdateLog(LogEntry log) {
            this.log = new XmlLog(log);
        }

        @Override
        public LogEntry call() throws Exception {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.olog_url + "/logs/" + log.getId()))
                    .header("Accept", MediaType.APPLICATION_JSON)
                    .header("Accept", MediaType.APPLICATION_XML)
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .header("Authorization", basicAuthenticationHeader)
                    .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(log)))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() < 300)
                return new OlogLog(OBJECT_MAPPER.readValue(httpResponse.body(), XmlLog.class));
            else
               throw new OlogException(httpResponse.statusCode(), httpResponse.body());
        }
    }

    @Override
    public Collection<LogEntry> update(Collection<LogEntry> logs) {
        return wrappedSubmit(new UpdateLogs(logs));
    }

    private class UpdateLogs implements Callable<Collection<LogEntry>> {
        private final XmlLogs logs;

        public UpdateLogs(Collection<LogEntry> logs) {
            this.logs = new XmlLogs();
            Collection<XmlLog> xmlLogs = new ArrayList<>();
            for (LogEntry log : logs) {
                xmlLogs.add(new XmlLog(log));
            }
            this.logs.setLogs(xmlLogs);
        }

        @Override
        public Collection<LogEntry> call() throws Exception {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.olog_url + "/logs"))
                    .header("Accept", MediaType.APPLICATION_JSON)
                    .header("Accept", MediaType.APPLICATION_XML)
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .header("Authorization", basicAuthenticationHeader)
                    .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(logs)))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() < 300) {
                Collection<LogEntry> logs = new HashSet<>();
                for (XmlLog xmlLog : OBJECT_MAPPER.readValue(httpResponse.body(), XmlLogs.class).getLogs()) {
                    logs.add(new OlogLog(xmlLog));
                }
                return Collections.unmodifiableCollection(logs);
            } else {
                throw new OlogException(httpResponse.statusCode(), httpResponse.body());
            }
        }
    }

    @Override
    public LogEntry findLogById(Long logId) {
        return wrappedSubmit(new Callable<>() {

            @Override
            public LogEntry call() throws Exception {

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(Preferences.olog_url + "/logs/" + logId))
                        .header("Accept", MediaType.APPLICATION_JSON)
                        .header("Accept", MediaType.APPLICATION_XML)
                        .GET()
                        .build();

                HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (httpResponse.statusCode() < 300) {
                    LOGGER.log(Level.WARNING, "Failed to get log id " + logId + ": " + httpResponse.body());
                    throw new OlogException(httpResponse.statusCode(), httpResponse.body());
                }
                return new OlogLog(OBJECT_MAPPER.readValue(httpResponse.body(), XmlLog.class));
            }
        });
    }

    @Override
    public List<LogEntry> findLogsBySearch(String pattern) {
        return wrappedSubmit(new FindLogs("search", pattern));
    }

    @Override
    public List<LogEntry> findLogsByTag(String pattern) {
        return wrappedSubmit(new FindLogs("tag", pattern));
    }

    @Override
    public List<LogEntry> findLogsByLogbook(String logbook) {
        return wrappedSubmit(new FindLogs("logbook", logbook));
    }

    @Override
    public List<LogEntry> findLogsByProperty(String propertyName, String attributeName, String attributeValue) {
        MultivaluedMap<String, String> mMap = new MultivaluedHashMap<>();
        mMap.putSingle(propertyName + "." + attributeName, attributeValue);
        return wrappedSubmit(new FindLogs(mMap));
    }

    @Override
    public List<LogEntry> findLogsByProperty(String propertyName) {
        return wrappedSubmit(new FindLogs("property", propertyName));
    }

    @Override
    public SearchResult search(Map<String, String> map) {
        List<LogEntry> logs = findLogs(map);
        return SearchResult.of(logs, logs.size());
    }

    @Override
    public List<LogEntry> findLogs(Map<String, String> map) {
        return wrappedSubmit(new FindLogs(map));
    }

    private class FindLogs implements Callable<List<LogEntry>> {

        private final MultivaluedMap<String, String> map;

        public FindLogs(String queryParameter, String pattern) {
            MultivaluedMap<String, String> mMap = new MultivaluedHashMap<>();
            mMap.putSingle(queryParameter, pattern);
            this.map = mMap;
        }

        public FindLogs(MultivaluedMap<String, String> map) {
            this.map = map;
        }

        public FindLogs(Map<String, String> map) {
            MultivaluedMap<String, String> mMap = new MultivaluedHashMap<>();
            Iterator<Map.Entry<String, String>> itr = map.entrySet().iterator();
            while (itr.hasNext()) {
                Map.Entry<String, String> entry = itr.next();
                mMap.put(entry.getKey(), Arrays.asList(entry.getValue().split(",")));
            }
            this.map = mMap;
        }

        @Override
        public List<LogEntry> call() throws Exception {
            List<LogEntry> logs = new ArrayList<>();
            // Map Phoebus logbook search parameters to Olog ones
            // desc, title -> search
            // size -> limit
            if (map.containsKey("desc")) {
                map.put("search", map.get("desc"));
            }
            if (map.containsKey("size")) {
                map.put("limit", map.get("size"));
            }
            if (map.containsKey("start")) {
                map.putSingle("start", parseTemporalValue(map.getFirst("start")));
            }
            if (map.containsKey("end")) {
                map.putSingle("end", parseTemporalValue(map.getFirst("end")));
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(Preferences.olog_url + "/logs?" + QueryParamsHelper.mapToQueryParams(map)))
                    .header("Accept", MediaType.APPLICATION_JSON)
                    .header("Accept", MediaType.APPLICATION_XML)
                    .GET()
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() < 300) {
                LOGGER.log(Level.WARNING, "Failed to search for logs: " + httpResponse.body());
                throw new OlogException(httpResponse.statusCode(), httpResponse.body());
            }

            XmlLogs xmlLogs = OBJECT_MAPPER.readValue(httpResponse.body(), XmlLogs.class);
            for (XmlLog xmllog : xmlLogs.getLogs()) {
                OlogLog log = new OlogLog(xmllog);
                if (!xmllog.getXmlAttachments().getAttachments().isEmpty()) {
                    Collection<Attachment> populatedAttachments = xmllog.getXmlAttachments().getAttachments().stream()
                            .map((attachment) -> {
                                OlogAttachment a = new OlogAttachment(attachment);
                                try {
                                    Path temp = Files.createTempFile("phoebus", attachment.getFileName());
                                    Files.copy(getAttachment(log.getId(), attachment.getFileName()), temp,
                                            StandardCopyOption.REPLACE_EXISTING);
                                    a.setFile(temp.toFile());
                                    temp.toFile().deleteOnExit();
                                } catch (IOException e) {
                                    logger.log(Level.WARNING, "failed to retrieve attachment file " + a.getName(), e);
                                }
                                return a;
                            }).collect(Collectors.toList());
                    log.setXmlAttachments(populatedAttachments);
                }
                logs.add(log);
            }
            return Collections.unmodifiableList(logs);
        }
    }

    private static String parseTemporalValue(String value) {
        Object time = TimeParser.parseInstantOrTemporalAmount(value);
        if (time instanceof Instant) {
            return MILLI_FORMAT.format((Instant) time);
        } else if (time instanceof TemporalAmount) {
            return MILLI_FORMAT.format(Instant.now().minus((TemporalAmount) time));
        }
        return "";
    }

    private <T> T wrappedSubmit(Callable<T> callable) {
        try {
            return this.executor.submit(callable).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class PromiscuousTrustManager extends X509ExtendedTrustManager {
        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[0];
        }

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }
    }
}

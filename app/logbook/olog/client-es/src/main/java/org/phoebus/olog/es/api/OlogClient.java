package org.phoebus.olog.es.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.jersey.api.client.ClientResponse.Status;
import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.LogbookException;
import org.phoebus.logbook.Messages;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.Tag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;
import com.sun.jersey.multipart.impl.MultiPartWriter;
import org.phoebus.olog.es.api.model.OlogObjectMappers;
import org.phoebus.olog.es.api.model.OlogLog;

/**
 * A client to the Olog-es webservice
 *
 * @author Kunal Shroff
 */
public class OlogClient implements LogClient {
    private static final Logger logger = Logger.getLogger(OlogClient.class.getName());

    private final WebResource service;

    /**
     * Builder Class to help create a olog client.
     *
     * @author shroffk
     */
    public static class OlogClientBuilder {
        // required
        private URI ologURI = null;

        // optional
        private boolean withHTTPAuthentication = true;

        private ClientConfig clientConfig = null;
        @SuppressWarnings("unused")
        private SSLContext sslContext = null;
        private String protocol = null;
        private String username = null;
        private String password = null;
        private String connectTimeoutAsString = null;

        private OlogProperties properties = new OlogProperties();

        private OlogClientBuilder() {
            this.ologURI = URI.create(this.properties.getPreferenceValue("olog_url"));
            this.protocol = this.ologURI.getScheme();
        }

        /**
         * Creates a {@link OlogClientBuilder} for a CF client to Default URL in the
         * channelfinder.properties.
         *
         * @return
         */
        public static OlogClientBuilder serviceURL() {
            return new OlogClientBuilder();
        }

        /**
         * Enable of Disable the HTTP authentication on the client connection.
         *
         * @param withHTTPAuthentication
         * @return {@link OlogClientBuilder}
         */
        public OlogClientBuilder withHTTPAuthentication(boolean withHTTPAuthentication) {
            this.withHTTPAuthentication = withHTTPAuthentication;
            return this;
        }

        /**
         * Set the username to be used for HTTP Authentication.
         *
         * @param username
         * @return {@link OlogClientBuilder}
         */
        public OlogClientBuilder username(String username) {
            this.username = username;
            return this;
        }

        /**
         * Set the password to be used for the HTTP Authentication.
         *
         * @param password
         * @return {@link OlogClientBuilder}
         */
        public OlogClientBuilder password(String password) {
            this.password = password;
            return this;
        }

        @SuppressWarnings("unused")
        private OlogClientBuilder withSSLContext(SSLContext sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        public OlogClient create() {
            if (this.protocol.equalsIgnoreCase("http")) { //$NON-NLS-1$
                this.clientConfig = new DefaultClientConfig();
            } else if (this.protocol.equalsIgnoreCase("https")) { //$NON-NLS-1$
                OlogTrustManager.setupSSLTrust(this.ologURI.getHost(), this.ologURI.getPort());
                if (this.clientConfig == null) {
                    this.clientConfig = new DefaultClientConfig();
                }
            }
            this.username = ifNullReturnPreferenceValue(this.username, "username");
            this.password = ifNullReturnPreferenceValue(this.password, "password");
            this.connectTimeoutAsString = ifNullReturnPreferenceValue(this.connectTimeoutAsString, "connectTimeout");
            Integer connectTimeout = 0;
            try {
                connectTimeout = Integer.parseInt(connectTimeoutAsString);
            } catch (NumberFormatException e) {
                Logger.getLogger(OlogClientBuilder.class.getPackageName())
                        .warning("connectTimeout preference not set or invalid, using 0 (=infinite)");
            }
            this.clientConfig.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, connectTimeout);
            return new OlogClient(this.ologURI, this.clientConfig, this.withHTTPAuthentication, this.username, this.password);
        }

        private String ifNullReturnPreferenceValue(String value, String key) {
            if (value == null) {
                return this.properties.getPreferenceValue(key);
            } else {
                return value;
            }
        }
    }

    private OlogClient(URI ologURI, ClientConfig config, boolean withHTTPBasicAuthFilter, String username, String password) {
        config.getClasses().add(MultiPartWriter.class);
        Client client = Client.create(config);
        if (withHTTPBasicAuthFilter) {
            client.addFilter(new HTTPBasicAuthFilter(username, password));
        }
        if (Logger.getLogger(OlogClient.class.getName()).isLoggable(Level.ALL)) {
            client.addFilter(new RawLoggingFilter(Logger.getLogger(OlogClient.class.getName())));
        }
        client.setFollowRedirects(true);
        client.setConnectTimeout(3000);
        this.service = client.resource(UriBuilder.fromUri(ologURI).build());
    }

    @Override
    public LogEntry set(LogEntry log) throws LogbookException {
        return save(log, null);
    }

    /**
     * Calls the back-end service to persist the log entry.
     * @param log The log entry to save.
     * @param inReplyTo If non-null, this save operation will treat the <code>log</code> parameter as a reply to
     *                  the log entry represented by <code>inReplyTo</code>.
     * @return The saved log entry.
     * @throws LogbookException E.g. due to invalid log entry data.
     */
    private LogEntry save(LogEntry log, LogEntry inReplyTo) throws LogbookException {
        ClientResponse clientResponse;

        try {
            MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
            queryParams.putSingle("markup", "commonmark");
            if(inReplyTo != null){
                queryParams.putSingle("inReplyTo", Long.toString(inReplyTo.getId()));
            }
            clientResponse = service.path("logs")
                    .queryParams(queryParams)
                    .type(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_XML)
                    .accept(MediaType.APPLICATION_JSON)
                    .put(ClientResponse.class, OlogObjectMappers.logEntrySerializer.writeValueAsString(log));

            if (clientResponse.getStatus() < 300)
            {
                OlogLog createdLog = OlogObjectMappers.logEntryDeserializer.readValue(clientResponse.getEntityInputStream(), OlogLog.class);
                log.getAttachments().stream().forEach(attachment -> {
                    FormDataMultiPart form = new FormDataMultiPart();
                    // Add id only if it is set, otherwise Jersey will complain and cause the submission to fail.
                    if(attachment.getId() != null && !attachment.getId().isEmpty()){
                        form.bodyPart(new FormDataBodyPart("id", attachment.getId()));
                    }
                    form.bodyPart(new FileDataBodyPart("file", attachment.getFile()));
                    form.bodyPart(new FormDataBodyPart("filename", attachment.getName()));
                    form.bodyPart(new FormDataBodyPart("fileMetadataDescription", attachment.getContentType()));

                    ClientResponse attachmentResponse = service.path("logs")
                            .path("attachments")
                            .path(String.valueOf(createdLog.getId()))
                           .type(MediaType.MULTIPART_FORM_DATA)
                           .accept(MediaType.APPLICATION_XML)
                           .accept(MediaType.APPLICATION_JSON)
                           .post(ClientResponse.class, form);
                    if (attachmentResponse.getStatus() > 300)
                    {
                        // TODO failed to add attachments
                        logger.log(Level.SEVERE, "Failed to submit attachment(s), HTTP status: " + attachmentResponse.getStatus());
                    }
                });

                clientResponse = service.path("logs").path(String.valueOf(createdLog.getId()))
                        .type(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .get(ClientResponse.class);
                return OlogObjectMappers.logEntryDeserializer.readValue(clientResponse.getEntityInputStream(), OlogLog.class);
            }
            else if(clientResponse.getStatus() == 401){
                logger.log(Level.SEVERE, "Submission of log entry returned HTTP status, invalid credentials");
                throw new LogbookException(Messages.SubmissionFailedInvalidCredentials);
            }
            else{
                logger.log(Level.SEVERE, "Submission of log entry returned HTTP status" + clientResponse.getStatus() );
                throw new LogbookException(MessageFormat.format(Messages.SubmissionFailedWithHttpStatus, clientResponse.getStatus()));
            }
        } catch (UniformInterfaceException | ClientHandlerException | IOException e) {
            logger.log(Level.SEVERE,"Failed to submit log entry, got client exception", e);
            throw new LogbookException(e);
        }
    }

    @Override
    public LogEntry reply(LogEntry log, LogEntry inReplyTo) throws LogbookException{
        return save(log, inReplyTo);
    }

    /**
     * Returns a LogEntry that exactly matches the logId <code>logId</code>
     *
     * @param logId LogEntry id
     * @return LogEntry object
     */
    @Override
    public LogEntry getLog(Long logId){
        return findLogById(logId);
    }


    @Override
    public LogEntry findLogById(Long logId) {
        try {
            OlogLog ologLog = OlogObjectMappers.logEntryDeserializer.readValue(
                service
                    .path("logs")
                    .path(logId.toString())
                    .accept(MediaType.APPLICATION_JSON).get(String.class), OlogLog.class);
            return ologLog;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Override
    public List<LogEntry> findLogs(Map<String, String> map) throws RuntimeException{
        MultivaluedMap<String, String> mMap = new MultivaluedMapImpl();
        map.forEach((k, v) -> {
            mMap.putSingle(k, v);
        });
        return findLogs(mMap);
    }

    private List<LogEntry> findLogs(String queryParameter, String pattern) throws RuntimeException {
        MultivaluedMap<String, String> mMap = new MultivaluedMapImpl();
        mMap.putSingle(queryParameter, pattern);
        return findLogs(mMap);
    }

    /**
     * Retrieves {@link LogEntry}s matching the search criteria. Note that even if the matching {@link LogEntry}s
     * may have a non-empty list of {@link Attachment}s, the {@link Attachment}s will NOT contains the actual
     * data/content. This is essentially a lazy loading strategy to avoid fetching attachment data at this point.
     * @param searchParams Map of search parameters/expressions
     * @return A list of matching {@link LogEntry}s
     * @throws RuntimeException
     */
    private List<LogEntry> findLogs(MultivaluedMap<String, String> searchParams) throws RuntimeException {
        List<LogEntry> logs = new ArrayList<>();
        if (searchParams.containsKey("limit")) {
            // Check if limit can be parsed as a number. If not, remove it.
            try {
                Integer.parseInt(searchParams.get("limit").get(0));
            } catch (Exception e) {
                logger.warning("Invalid request parameter value for 'limit'");
                searchParams.remove("limit");
            }
        }
        try {
            // Convert List<XmlLog> into List<LogEntry>
            final List <OlogLog> xmls = OlogObjectMappers.logEntryDeserializer.readValue(
                    service.path("logs").queryParams(searchParams)
                    .accept(MediaType.APPLICATION_JSON)
                    .get(String.class),
                    new TypeReference<List <OlogLog>>() {
                    });
            logs = xmls.stream().collect(Collectors.toList());
            return Collections.unmodifiableList(logs);
        } catch (UniformInterfaceException | ClientHandlerException | IOException e) {
            logger.log(Level.WARNING, "failed to retrieve log entries", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<LogEntry> findLogsByLogbook(String logbookName) {
        return findLogs("logbook", logbookName);
    }

    @Override
    public List<LogEntry> findLogsByProperty(String propertyName) {
        return findLogs("property", propertyName);
    }

    @Override
    public List<LogEntry> findLogsByProperty(String propertyName, String attributeName, String attributeValue) {
        HashMap<String, String> map = new HashMap<>();
        map.put(propertyName + "." + attributeName, attributeValue);
        return findLogs(map);
    }

    @Override
    public List<LogEntry> findLogsBySearch(String pattern) {
        return findLogs("search", pattern);
    }

    @Override
    public List<LogEntry> findLogsByTag(String tagName) {
        return findLogs("tag", tagName);
    }

    @Override
    public InputStream getAttachment(Long logId, String attachmentName) {
        ClientResponse response = service
                .path("logs")
                .path("attachments")
                .path(logId.toString())
                .path(attachmentName).get(ClientResponse.class);
        return response.getEntity(InputStream.class);
    }

    @Override
    public Collection<Attachment> listAttachments(Long logId) {
        return getLog(logId).getAttachments();
    }

    @Override
    public Collection<String> listAttributes(String propertyName) {
        try {
            return (Collection<String>) getProperty(propertyName).getAttributes();
        } catch (LogbookException e) {
            logger.log(Level.WARNING, "Unable to get property attribute list from service", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<LogEntry> listLogs() {
        List<LogEntry> logEntries = new ArrayList<>();
        return logEntries;
    }

    /**
     * List of level values as defined in the properties file.
     */
    private List<String> levels;

    /**
     * Service URL as configured by properties.
     */
    private String serviceUrl;

    @Override
    public Collection<String> listLevels() {
        if(levels == null){
            OlogProperties ologProperties = new OlogProperties();
            String[] levelList = ologProperties.getPreferenceValue("levels").split(",");
            levels = Arrays.asList(levelList);
        }
        return levels;
    }

    @Override
    public Collection<Logbook> listLogbooks() {
        try {
            List<Logbook> logbooks = OlogObjectMappers.logEntryDeserializer.readValue(
                    service.path("logbooks").accept(MediaType.APPLICATION_JSON).get(String.class),
                    new TypeReference<List<Logbook>>() {
            });
            return logbooks;
        } catch (UniformInterfaceException | ClientHandlerException | IOException e) {
            logger.log(Level.WARNING, "Unable to get logbooks from service", e);
            return Collections.emptySet();
        }
    }

    @Override
    public Collection<Property> listProperties() {
        try {
            List<Property> properties = OlogObjectMappers.logEntryDeserializer.readValue(
                    service.path("properties").accept(MediaType.APPLICATION_JSON).get(String.class),
                    new TypeReference<List<Property>>() {
            });
            return properties;
        } catch (UniformInterfaceException | ClientHandlerException | IOException e) {
            logger.log(Level.WARNING, "failed to list olog properties", e);
            return Collections.emptySet();
        }
    }

    @Override
    public Collection<Tag> listTags() {
        try {
            List<Tag> tags = OlogObjectMappers.logEntryDeserializer.readValue(
                    service.path("tags").accept(MediaType.APPLICATION_JSON).get(String.class),
                    new TypeReference<List<Tag>>() {
            });
            return tags;
        } catch (UniformInterfaceException | ClientHandlerException | IOException e) {
            logger.log(Level.WARNING, "failed to retrieve olog tags", e);
            return Collections.emptySet();
        }
    }

    @Override
    public String getServiceUrl(){
        if(serviceUrl == null){
            OlogProperties ologProperties = new OlogProperties();
            serviceUrl = ologProperties.getPreferenceValue("olog_url");
        }
        return serviceUrl;
    }

    @Override
    public LogEntry updateLogEntry(LogEntry logEntry) throws LogbookException {
        ClientResponse clientResponse;

        try {
            clientResponse = service.path("logs/" + logEntry.getId())
                    .queryParam("markup", "commonmark")
                    .type(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_XML)
                    .accept(MediaType.APPLICATION_JSON)
                    .post(ClientResponse.class, OlogObjectMappers.logEntrySerializer.writeValueAsString(logEntry));
            return OlogObjectMappers.logEntryDeserializer.readValue(clientResponse.getEntityInputStream(), OlogLog.class);
        }
        catch (Exception e){
            logger.log(Level.SEVERE, "Unable to update log entry id=" + logEntry.getId(), e);
            return null;
        }
    }

    /**
     * Logs in to the Olog service.
     * @param username User name, must not be <code>null</code>.
     * @param password Password, must not be <code>null</code>.
     * @throws Exception if the login fails, e.g. bad credentials or service off-line.
     */
    public void authenticate(String username, String password) throws Exception{
        try {
            ClientResponse clientResponse = service.path("login")
                    .queryParam("username", username)
                    .queryParam("password", password)
                    .post(ClientResponse.class);
            if(clientResponse.getStatus() == Status.UNAUTHORIZED.getStatusCode()){
                throw new Exception("Failed to login: user unauthorized");
            }
            else if(clientResponse.getStatus() != Status.OK.getStatusCode()){
                throw new Exception("Failed to login, got HTTP status " + clientResponse.getStatus());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to log in to Olog service", e);
            throw e;
        }
    }
}

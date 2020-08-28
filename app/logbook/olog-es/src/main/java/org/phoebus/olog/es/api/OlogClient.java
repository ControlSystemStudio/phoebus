package org.phoebus.olog.es.api;

import static org.phoebus.olog.es.api.OlogObjectMappers.logEntryDeserializer;
import static org.phoebus.olog.es.api.OlogObjectMappers.logEntrySerializer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

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
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;
import com.sun.jersey.multipart.impl.MultiPartWriter;

/**
 * A client to the Olog-es webservice
 *
 * @author Eric Berryman taken from shroffk
 */
public class OlogClient implements LogClient {
    private final WebResource service;

    private Logger logger = Logger.getLogger(OlogClient.class.getPackageName());

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
        private TrustManager[] trustManager = new TrustManager[] { new DummyX509TrustManager() };;
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

        private OlogClientBuilder(URI uri) {
            this.ologURI = uri;
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
         * Creates a {@link OlogClientBuilder} for a CF client to URI <code>uri</code>.
         *
         * @param uri
         * @return {@link OlogClientBuilder}
         */
        public static OlogClientBuilder serviceURL(String uri) {
            return new OlogClientBuilder(URI.create(uri));
        }

        /**
         * Creates a {@link OlogClientBuilder} for a CF client to {@link URI}
         * <code>uri</code>.
         *
         * @param uri
         * @return {@link OlogClientBuilder}
         */
        public static OlogClientBuilder serviceURL(URI uri) {
            return new OlogClientBuilder(uri);
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

        /**
         * set the {@link ClientConfig} to be used while creating the Olog-es
         * client connection.
         *
         * @param clientConfig
         * @return {@link OlogClientBuilder}
         */
        public OlogClientBuilder withClientConfig(ClientConfig clientConfig) {
            this.clientConfig = clientConfig;
            return this;
        }

        @SuppressWarnings("unused")
        private OlogClientBuilder withSSLContext(SSLContext sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        /**
         * Set the trustManager that should be used for authentication.
         *
         * @param trustManager
         * @return {@link OlogClientBuilder}
         */
        public OlogClientBuilder withTrustManager(TrustManager[] trustManager) {
            this.trustManager = trustManager;
            return this;
        }

        public OlogClient create() throws Exception {
            if (this.protocol.equalsIgnoreCase("http")) { //$NON-NLS-1$
                this.clientConfig = new DefaultClientConfig();
            } else if (this.protocol.equalsIgnoreCase("https")) { //$NON-NLS-1$
                if (this.clientConfig == null) {
                    SSLContext sslContext = null;
                    try {
                        sslContext = SSLContext.getInstance("SSL"); //$NON-NLS-1$
                        sslContext.init(null, this.trustManager, null);
                    } catch (Exception e) {
                        throw new OlogException();
                    }
                    this.clientConfig = new DefaultClientConfig();
                    this.clientConfig.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES,
                            new HTTPSProperties(new HostnameVerifier() {
                                @Override
                                public boolean verify(String arg0, SSLSession arg1) {
                                    return true;
                                }
                            }, sslContext));
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
    public LogEntry set(LogEntry log) throws LogbookException{
        ClientResponse clientResponse;

        try {
            clientResponse = service.path("logs")
                    .type(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_XML)
                    .accept(MediaType.APPLICATION_JSON)
                    .put(ClientResponse.class, logEntrySerializer.writeValueAsString(log));

            if (clientResponse.getStatus() < 300)
            {
                XmlLog createdLog = logEntryDeserializer.readValue(clientResponse.getEntityInputStream(), XmlLog.class);
                log.getAttachments().stream().forEach(attachment -> {
                    FormDataMultiPart form = new FormDataMultiPart();
                    form.bodyPart(new FileDataBodyPart("file", attachment.getFile()));
                    form.bodyPart(new FormDataBodyPart("filename", attachment.getName()));
                    form.bodyPart(new FormDataBodyPart("fileMetadataDescription", attachment.getContentType()));

                    ClientResponse attachementResponse = service.path("logs").path("attachments").path(String.valueOf(createdLog.getId()))
                           .type(MediaType.MULTIPART_FORM_DATA)
                           .accept(MediaType.APPLICATION_XML)
                           .accept(MediaType.APPLICATION_JSON)
                           .post(ClientResponse.class, form);
                    if (attachementResponse.getStatus() > 300)
                    {
                        // TODO failed to add attachments
                        logger.log(Level.SEVERE, "Failed to submit attachment(s), HTTP status: " + attachementResponse.getStatus());
                    }
                });

                clientResponse = service.path("logs").path(String.valueOf(createdLog.getId()))
                        .type(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .get(ClientResponse.class);
                return logEntryDeserializer.readValue(clientResponse.getEntityInputStream(), XmlLog.class);
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
        XmlLog xmlLog = service
                .path("logs")
                .path(logId.toString())
                .accept(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_JSON)
                .get(XmlLog.class);
        return xmlLog;
    }

    @Override
    public List<LogEntry> findLogs(Map<String, String> map) {
        MultivaluedMap<String, String> mMap = new MultivaluedMapImpl();
        map.forEach((k, v) -> {
            mMap.putSingle(k, v);
        });
        return findLogs(mMap);
    }

    private List<LogEntry> findLogs(String queryParameter, String pattern) {
        MultivaluedMap<String, String> mMap = new MultivaluedMapImpl();
        mMap.putSingle(queryParameter, pattern);
        return findLogs(mMap);
    }

    private List<LogEntry> findLogs(MultivaluedMap<String, String> mMap) {
        List<LogEntry> logs = new ArrayList<>();
        if (!mMap.containsKey("limit")) {
            mMap.putSingle("limit", "100");
        }
        try {
            // Convert List<XmlLog> into List<LogEntry>
            final List<XmlLog> xmls = logEntryDeserializer.readValue(
                    service.path("logs").queryParams(mMap)
                    .accept(MediaType.APPLICATION_JSON)
                    .get(String.class),
                    new TypeReference<List<XmlLog>>() {
                    });
            for (XmlLog xml : xmls)
                logs.add(xml);
            logs.forEach(log -> {
                // fetch attachment??
                // This surely can be done better, move the fetch into a job and only invoke it when the client is trying to render the image
                if (!log.getAttachments().isEmpty()) {
                    Collection<Attachment> populatedAttachment = log.getAttachments().stream()
                      .filter( (attachment) -> {
                          return attachment.getName() != null && !attachment.getName().isEmpty();
                      })
                      .map((attachment) -> {
                        XmlAttachment fileAttachment = new XmlAttachment();
                        fileAttachment.setContentType(attachment.getContentType());
                        fileAttachment.setThumbnail(false);
                        try {
                            Path temp = Files.createTempFile("phoebus", attachment.getName());
                            Files.copy(getAttachment(log.getId(), attachment.getName()), temp, StandardCopyOption.REPLACE_EXISTING);
                            fileAttachment.setFile(temp.toFile());
                            temp.toFile().deleteOnExit();
                        } catch (IOException e) {
                            logger.log(Level.WARNING, "Failed to retrieve attachment " + fileAttachment.getFileName() ,e);
                        }
                        return fileAttachment;
                    }).collect(Collectors.toList());
                    ((XmlLog)log).setXmlAttachments(populatedAttachment);
                }
            });
        } catch (UniformInterfaceException | ClientHandlerException | IOException e) {
            logger.log(Level.WARNING, "failed to retrieve log entries", e);
        }
        return Collections.unmodifiableList(logs);
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
            List<Logbook> logbooks = logEntryDeserializer.readValue(
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
            List<Property> properties = logEntryDeserializer.readValue(
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
            List<Tag> tags = logEntryDeserializer.readValue(
                    service.path("tags").accept(MediaType.APPLICATION_JSON).get(String.class),
                    new TypeReference<List<Tag>>() {
            });
            return tags;
        } catch (UniformInterfaceException | ClientHandlerException | IOException e) {
            logger.log(Level.WARNING, "failed to retrieve olog tags", e);
            return Collections.emptySet();
        }
    }

}

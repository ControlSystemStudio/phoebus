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
import org.phoebus.logbook.Property;
import org.phoebus.logbook.Tag;

import com.fasterxml.jackson.core.JsonProcessingException;
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
 *
 *
 * @author Eric Berryman taken from shroffk
 *
 */
public class OlogClient implements LogClient {
    private final WebResource service;

    /**
     * Builder Class to help create a olog client.
     *
     * @author shroffk
     *
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
         * Creates a {@link OlogClientBuilder} for a CF client to URI <tt>uri</tt>.
         *
         * @param uri
         * @return {@link OlogClientBuilder}
         */
        public static OlogClientBuilder serviceURL(String uri) {
            return new OlogClientBuilder(URI.create(uri));
        }

        /**
         * Creates a {@link OlogClientBuilder} for a CF client to {@link URI}
         * <tt>uri</tt>.
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
         * set the {@link ClientConfig} to be used while creating the channelfinder
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
        this.service = client.resource(UriBuilder.fromUri(ologURI).build());
    }

    @Override
    public org.phoebus.logbook.Attachment add(File arg0, Long arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void delete(LogEntry arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(Collection<LogEntry> arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(Tag arg0, Long arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(Tag arg0, Collection<Long> arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(Logbook arg0, Long arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(Logbook arg0, Collection<Long> arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(Property arg0, Long arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(Property arg0, Collection<Long> arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public InputStream getAttachment(Long arg0, org.phoebus.logbook.Attachment arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LogEntry getLog(Long arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Property getProperty(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Tag set(Tag arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Logbook set(Logbook arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Property set(Property arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Tag set(Tag arg0, Collection<Long> arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Logbook set(Logbook arg0, Collection<Long> arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LogEntry update(LogEntry arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<LogEntry> update(Collection<LogEntry> arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Property update(Property arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Tag update(Tag arg0, Long arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Tag update(Tag arg0, Collection<Long> arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Logbook update(Logbook arg0, Long arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Logbook update(Logbook arg0, Collection<Long> arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LogEntry update(Property arg0, Long arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void delete(Long arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(String arg0, Long arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteLogbook(String arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteProperty(String arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteTag(String arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public LogEntry set(LogEntry log) {
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
                    }
                });

                clientResponse = service.path("logs").path(String.valueOf(createdLog.getId()))
                        .type(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .get(ClientResponse.class);
                return logEntryDeserializer.readValue(clientResponse.getEntityInputStream(), XmlLog.class);
            }

        } catch (UniformInterfaceException | ClientHandlerException | IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Collection<LogEntry> set(Collection<LogEntry> xmlLogs) {
        Collection<LogEntry> createdLogs = new HashSet<>();
        xmlLogs.stream().forEachOrdered(log -> {
            createdLogs.add(set(log));
        });
        return null;
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
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return fileAttachment;
                    }).collect(Collectors.toList());
                    ((XmlLog)log).setXmlAttachments(populatedAttachment);
                }
            });
        } catch (UniformInterfaceException | ClientHandlerException | IOException e) {
            e.printStackTrace();
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
        Collection<Attachment> allAttachments = new HashSet<>();
        // TODO with olog-es we have to fetch the log first, which contains the list of the attachment ids
//        XmlAttachments allXmlAttachments = service.path("attachments")
//                .path(logId.toString())
//                .accept(MediaType.APPLICATION_XML).get(XmlAttachments.class);
//        for (XmlAttachment xmlAttachment : allXmlAttachments.getAttachments()) {
//            allAttachments.add(xmlAttachment);
//        }
        return allAttachments;
    }

    @Override
    public Collection<String> listAttributes(String propertyName) {
        return (Collection<String>) getProperty(propertyName).getAttributes();
    }

    @Override
    public List<LogEntry> listLogs() {
        List<LogEntry> logEntries = new ArrayList<>();
        return logEntries;
    }

    // A predefined set of levels supported by olog
    private final List<String> levels = Arrays.asList("Urgent", "Suggestion", "Info", "Request", "Problem");

    @Override
    public Collection<String> listLevels() {
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
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
            return Collections.emptySet();
        }
    }

}

package org.phoebus.olog.api;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;
import com.sun.jersey.multipart.impl.MultiPartWriter;
import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.LogbookException;
import org.phoebus.logbook.Messages;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.Tag;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.phoebus.applications.logbook.OlogLogbook.logger;
/**
 * A logbook client to tne Olog logbook service
 */
public class OlogClient implements LogClient {

    private final WebResource service;
    private final ExecutorService executor;

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
        private boolean withHTTPAuthentication = false;
        private boolean withRawFilter;

        private ClientConfig clientConfig = null;
        private TrustManager[] trustManager = new TrustManager[] { new DummyX509TrustManager() };;
        @SuppressWarnings("unused")
        private SSLContext sslContext = null;

        private String protocol = null;
        private String username = null;
        private String password = null;
        private String connectTimeoutAsString = null;

        private ExecutorService executor = Executors.newSingleThreadExecutor();

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

        /**
         * Provide your own executor on which the queries are to be made. <br>
         * By default a single threaded executor is used.
         * 
         * @param executor
         * @return {@link OlogClientBuilder}
         */
        public OlogClientBuilder withExecutor(ExecutorService executor) {
            this.executor = executor;
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
                    } catch (NoSuchAlgorithmException e) {
                        throw new OlogException();
                    } catch (KeyManagementException e) {
                        throw new OlogException();
                    }
                    this.clientConfig = new DefaultClientConfig();
                    this.clientConfig.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES,
                            new HTTPSProperties(new HostnameVerifier() {
                                @Override
                                public boolean verify(String hostname, SSLSession session) {
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

            this.withRawFilter = Boolean.valueOf(this.properties.getPreferenceValue("debug"));
            return new OlogClient(this.ologURI,
                    this.clientConfig,
                    this.withHTTPAuthentication,
                    this.username,
                    this.password,
                    this.executor,
                    this.withRawFilter);
        }

        private String ifNullReturnPreferenceValue(String value, String key) {
            if (value == null) {
                return this.properties.getPreferenceValue(key);
            } else {
                return value;
            }
        }

    }

    private OlogClient(URI ologURI, ClientConfig config, boolean withHTTPBasicAuthFilter, String username,
            String password, ExecutorService executor, boolean withRawFilter) {
        this.executor = executor;
        config.getClasses().add(MultiPartWriter.class);
        Client client = Client.create(config);
        if (withHTTPBasicAuthFilter) {
            client.addFilter(new HTTPBasicAuthFilter(username, password));
        }
        if (withRawFilter) {
            client.addFilter(new RawLoggingFilter(Logger.getLogger(OlogClient.class.getName())));
        }
        client.setFollowRedirects(true);
        service = client.resource(UriBuilder.fromUri(ologURI).build());
    }

    // A predefined set of levels supported by olog
    private final List<String> levels = Arrays.asList("Urgent", "Suggestion", "Info", "Request", "Problem");

    @Override
    public Collection<String> listLevels() {
        return levels;
    }
    
    @Override
    public Collection<Logbook> listLogbooks() {
        return wrappedSubmit(new Callable<Collection<Logbook>>() {

            @Override
            public Collection<Logbook> call() throws Exception {

                Collection<Logbook> allLogbooks = new HashSet<Logbook>();
                XmlLogbooks allXmlLogbooks = service.path("logbooks").accept(MediaType.APPLICATION_XML)
                        .get(XmlLogbooks.class);
                for (XmlLogbook xmlLogbook : allXmlLogbooks.getLogbooks()) {
                    allLogbooks.add(new OlogLogbook(xmlLogbook));
                }
                return allLogbooks;
            }

        });
    }

    @Override
    public Collection<Tag> listTags() {
        return wrappedSubmit(new Callable<Collection<Tag>>() {

            @Override
            public Collection<Tag> call() throws Exception {
                Collection<Tag> allTags = new HashSet<Tag>();
                XmlTags allXmlTags = service.path("tags").accept(MediaType.APPLICATION_XML).get(XmlTags.class);
                for (XmlTag xmlTag : allXmlTags.getTags()) {
                    allTags.add(new OlogTag(xmlTag));
                }
                return allTags;
            }

        });
    }

    @Override
    public Collection<Property> listProperties() {
        return wrappedSubmit(new Callable<Collection<Property>>() {
            @Override
            public Collection<Property> call() throws Exception {
                Collection<Property> allProperties = new HashSet<Property>();
                XmlProperties xmlProperties = service.path("properties").accept(MediaType.APPLICATION_XML)
                        .accept(MediaType.APPLICATION_JSON).get(XmlProperties.class);
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
        return wrappedSubmit(new Callable<Collection<LogEntry>>() {
            @Override
            public Collection<LogEntry> call() throws Exception {
                XmlLogs xmlLogs = service.path("logs").accept(MediaType.APPLICATION_XML)
                        .accept(MediaType.APPLICATION_JSON).get(XmlLogs.class);

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
        return wrappedSubmit(new Callable<Collection<Attachment>>() {

            @Override
            public Collection<Attachment> call() throws Exception {
                Collection<Attachment> allAttachments = new HashSet<Attachment>();
                XmlAttachments allXmlAttachments = service.path("attachments").path(logId.toString())
                        .accept(MediaType.APPLICATION_XML).get(XmlAttachments.class);
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
            ClientResponse response = service.path("attachments")
                    .path(logId.toString())
                    .path(attachment.getName())
                    .get(ClientResponse.class);
            return response.getEntity(InputStream.class);
        } catch (Exception e) {
        }
        return null;
    }

    @Override
    public InputStream getAttachment(Long logId, String attachmentName) {
        try {
            ClientResponse response = service
                    .path("attachments")
                    .path(logId.toString())
                    .path(attachmentName)
                    .get(ClientResponse.class);
            return response.getEntity(InputStream.class);
        } catch (Exception e) {
        }
        return null;
    }

    @Override
    public Property getProperty(String property) {
        final String propertyName = property;
        return wrappedSubmit(new Callable<Property>() {

            @Override
            public Property call() throws Exception {
                return new OlogProperty(service.path("properties")
                        .path(propertyName).accept(MediaType.APPLICATION_XML)
                        .accept(MediaType.APPLICATION_JSON)
                        .get(XmlProperty.class));
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
        private Collection<LogEntry> logs;

        public SetLogs(LogEntry log) {
            this.logs = new ArrayList<LogEntry>();
            this.logs.add(log);
        }

        @Override
        public Collection<LogEntry> call() {
            Collection<LogEntry> returnLogs = new HashSet<LogEntry>();
            for (LogEntry log : logs) {
                XmlLogs xmlLogs = new XmlLogs();
                XmlLog xmlLog = new XmlLog(log);
                xmlLog.setLevel("Info");
                xmlLogs.getLogs().add(xmlLog);

                ClientResponse clientResponse = service.path("logs")
                        .accept(MediaType.APPLICATION_XML)
                        .accept(MediaType.APPLICATION_JSON)
                        .post(ClientResponse.class, xmlLogs);

                if (clientResponse.getStatus() < 300) {

                    // XXX there is an assumption that the result without an error status will consist of a single created log entry
                    XmlLog createdLog = clientResponse.getEntity(XmlLogs.class).getLogs().iterator().next();
                    log.getAttachments().forEach(attachment -> {
                        FormDataMultiPart form = new FormDataMultiPart();
                        form.bodyPart(new FileDataBodyPart("file", attachment.getFile()));
                        XmlAttachment createdAttachment = service.path("attachments").path(createdLog.getId().toString())
                                .type(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_XML)
                                .post(XmlAttachment.class, form);
                        createdLog.addXmlAttachment(createdAttachment);
                    });
                    returnLogs.add(new OlogLog(createdLog));

                } else
                    throw new UniformInterfaceException(clientResponse);
                
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
            ClientResponse clientResponse = service.path("logs")
                    .path(String.valueOf(log.getId()))
                    .accept(MediaType.APPLICATION_XML)
                    .accept(MediaType.APPLICATION_JSON)
                    .post(ClientResponse.class, log);
            if (clientResponse.getStatus() < 300)
                return new OlogLog(clientResponse.getEntity(XmlLog.class));
            else
                throw new UniformInterfaceException(clientResponse);
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
            Collection<XmlLog> xmlLogs = new ArrayList<XmlLog>();
            for (LogEntry log : logs) {
                xmlLogs.add(new XmlLog(log));
            }
            this.logs.setLogs(xmlLogs);
        }

        @Override
        public Collection<LogEntry> call() throws Exception {
            ClientResponse clientResponse = service.path("logs").accept(MediaType.APPLICATION_XML)
                    .accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, logs);
            if (clientResponse.getStatus() < 300) {
                // return new Log(clientResponse.getEntity(XmlLog.class));
                Collection<LogEntry> logs = new HashSet<LogEntry>();
                for (XmlLog xmlLog : clientResponse.getEntity(XmlLogs.class).getLogs()) {
                    logs.add(new OlogLog(xmlLog));
                }
                ;
                return Collections.unmodifiableCollection(logs);
            } else {
                throw new UniformInterfaceException(clientResponse);
            }
        }
    }

    @Override
    public LogEntry findLogById(Long logId) {
        return wrappedSubmit(new Callable<LogEntry>() {

            @Override
            public LogEntry call() throws Exception {
                XmlLog xmlLog = service.path("logs").path(logId.toString())
                        .accept(MediaType.APPLICATION_XML)
                        .accept(MediaType.APPLICATION_JSON).get(XmlLog.class);
                return new OlogLog(xmlLog);
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
        MultivaluedMap<String, String> mMap = new MultivaluedMapImpl();
        mMap.putSingle(propertyName + "." + attributeName, attributeValue);
        return wrappedSubmit(new FindLogs(mMap));
    }

    @Override
    public List<LogEntry> findLogsByProperty(String propertyName) {
        return wrappedSubmit(new FindLogs("property", propertyName));
    }

    @Override
    public List<LogEntry> findLogs(Map<String, String> map) {
        return wrappedSubmit(new FindLogs(map));
    }

    private class FindLogs implements Callable<List<LogEntry>> {

        private final MultivaluedMap<String, String> map;

        public FindLogs(String queryParameter, String pattern) {
            MultivaluedMap<String, String> mMap = new MultivaluedMapImpl();
            mMap.putSingle(queryParameter, pattern);
            this.map = mMap;
        }

        public FindLogs(MultivaluedMap<String, String> map) {
            this.map = map;
        }

        public FindLogs(Map<String, String> map) {
            MultivaluedMap<String, String> mMap = new MultivaluedMapImpl();
            Iterator<Map.Entry<String, String>> itr = map.entrySet().iterator();
            while (itr.hasNext()) {
                Map.Entry<String, String> entry = itr.next();
                mMap.put(entry.getKey(), Arrays.asList(entry.getValue().split(",")));
            }
            this.map = mMap;
        }

        @Override
        public List<LogEntry> call() throws Exception {
            List<LogEntry> logs = new ArrayList<LogEntry>();
            XmlLogs xmlLogs = service
                    .path("logs")
                    .queryParams(map)
                    .accept(MediaType.APPLICATION_XML)
                    .accept(MediaType.APPLICATION_JSON)
                    .get(XmlLogs.class);
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

    private <T> T wrappedSubmit(Callable<T> callable) {
        try {
            return this.executor.submit(callable).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            if (e.getCause() != null && e.getCause() instanceof UniformInterfaceException) {
                throw new OlogException((UniformInterfaceException) e.getCause());
            }
            throw new RuntimeException(e);
        }
    }

    private void wrappedSubmit(Runnable runnable) {
        try {
            this.executor.submit(runnable).get(60, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() != null && e.getCause() instanceof UniformInterfaceException) {
                throw new OlogException((UniformInterfaceException) e.getCause());
            }
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

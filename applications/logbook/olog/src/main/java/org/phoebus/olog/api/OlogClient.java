package org.phoebus.olog.api;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

import org.phoebus.logging.Attachment;
import org.phoebus.logging.LogClient;
import org.phoebus.logging.LogEntry;
import org.phoebus.logging.Logbook;
import org.phoebus.logging.Property;
import org.phoebus.logging.Tag;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import com.sun.jersey.core.util.MultivaluedMapImpl;
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
        private boolean withHTTPAuthentication = false;

        private ClientConfig clientConfig = null;
        private TrustManager[] trustManager = new TrustManager[] { new DummyX509TrustManager() };;
        @SuppressWarnings("unused")
        private SSLContext sslContext = null;
        private String protocol = null;
        private String username = null;
        private String password = null;

        private OlogProperties properties = new OlogProperties();

        private static final String DEFAULT_OLOG_URL = "http://localhost:8080/Olog/resources"; //$NON-NLS-1$

        private OlogClientBuilder() {
            this.ologURI = URI.create(this.properties.getPreferenceValue("olog_url", DEFAULT_OLOG_URL));
            this.protocol = this.ologURI.getScheme();
        }

        private OlogClientBuilder(URI uri) {
            this.ologURI = uri;
            this.protocol = this.ologURI.getScheme();
        }

        /**
         * Creates a {@link OlogClientBuilder} for a CF client to Default URL in
         * the channelfinder.properties.
         * 
         * @return
         */
        public static OlogClientBuilder serviceURL() {
            return new OlogClientBuilder();
        }

        /**
         * Creates a {@link OlogClientBuilder} for a CF client to URI
         * <tt>uri</tt>.
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
         * set the {@link ClientConfig} to be used while creating the
         * channelfinder client connection.
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
            this.username = ifNullReturnPreferenceValue(this.username, "username", "username");
            this.password = ifNullReturnPreferenceValue(this.password, "password", "password");
            return new OlogClient(this.ologURI, this.clientConfig, this.withHTTPAuthentication, this.username, this.password);
        }

        private String ifNullReturnPreferenceValue(String value, String key, String Default) {
            if (value == null) {
                return this.properties.getPreferenceValue(key, Default);
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
        client.addFilter(new RawLoggingFilter(Logger.getLogger(OlogClient.class.getName())));
        client.setFollowRedirects(true);
        service = client.resource(UriBuilder.fromUri(ologURI).build());
    }
    
    @Override
    public org.phoebus.logging.Attachment add(File arg0, Long arg1) {
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
    public InputStream getAttachment(Long arg0, org.phoebus.logging.Attachment arg1) {
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
        Collection<LogEntry> result = set(Arrays.asList(log));
        if (result.size() == 1) {
            return result.iterator().next();
        } else {
            throw new OlogException();
        }
    }

    @Override
    public Collection<LogEntry> set(Collection<LogEntry> xmlLogs) {
        ClientResponse clientResponse = service.path("logs").accept(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, xmlLogs);
        if (clientResponse.getStatus() < 300) {
            XmlLogs responseLogs = clientResponse.getEntity(XmlLogs.class);
            Collection<LogEntry> returnLogs = new HashSet<LogEntry>();
            for (XmlLog xmllog : responseLogs.getLogs()) {
                returnLogs.add(xmllog);
            }
            return Collections.unmodifiableCollection(returnLogs);
        } else {
            throw new UniformInterfaceException(clientResponse);
        }
    }


    @Override
    public LogEntry findLogById(Long logId) {
        XmlLog xmlLog = service.path("logs").path(logId.toString())
                .accept(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_JSON).get(XmlLog.class);
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
        List<LogEntry> logs = new ArrayList<LogEntry>();
        XmlLogs xmlLogs = service.path("logs").queryParams(mMap)
                .accept(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_JSON).get(XmlLogs.class);
        for (XmlLog xmllog : xmlLogs.getLogs()) {
            logs.add(xmllog);
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
    public List<LogEntry> findLogsByProperty(String propertyName,
            String attributeName, String attributeValue) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(propertyName + "." + attributeName, attributeValue);
        return findLogs(map);
    }

    @Override
    public List<LogEntry> findLogsBySearch(String arg0) {
        // TODO
        return null;
    }

    @Override
    public List<LogEntry> findLogsByTag(String tagName) {
        return findLogs("tag", tagName);
    }

    @Override
    public InputStream getAttachment(Long logId, String attachmentName) {
        ClientResponse response = service.path("attachments").path(logId.toString()).path(attachmentName)
                .get(ClientResponse.class);
        return response.getEntity(InputStream.class);
    }

    @Override
    public Collection<Attachment> listAttachments(Long logId) {
        Collection<Attachment> allAttachments = new HashSet<Attachment>();
        XmlAttachments allXmlAttachments = service.path("attachments").path(logId.toString())
                .accept(MediaType.APPLICATION_XML).get(XmlAttachments.class);
        for (XmlAttachment xmlAttachment : allXmlAttachments.getAttachments()) {
            allAttachments.add(xmlAttachment);
        }
        return allAttachments;
    }

    @Override
    public Collection<String> listAttributes(String propertyName) {
        return (Collection<String>) getProperty(propertyName).getAttributes();
    }

    @Override
    public Collection<Logbook> listLogbooks() {
        Collection<Logbook> allLogbooks = new HashSet<Logbook>();
        XmlLogbooks allXmlLogbooks = service.path("logbooks").accept(MediaType.APPLICATION_XML).get(XmlLogbooks.class);
        for (XmlLogbook xmlLogbook : allXmlLogbooks.getLogbooks()) {
            allLogbooks.add(xmlLogbook);
        }
        return allLogbooks;
    }

    @Override
    public List<LogEntry> listLogs() {
        XmlLogs xmlLogs = service.path("logs").accept(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_JSON)
                .get(XmlLogs.class);
        List<LogEntry> logEntries = new ArrayList<LogEntry>();
        logEntries.addAll(xmlLogs.getLogs());
        return logEntries;
    }

    @Override
    public Collection<Property> listProperties() {
        Collection<Property> allProperties = new HashSet<Property>();
        XmlProperties xmlProperties = service.path("properties")
                .accept(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_JSON)
                .get(XmlProperties.class);
        for (XmlProperty xmlProperty : xmlProperties.getProperties()) {
            allProperties.add(xmlProperty);
        }
        return allProperties;
    }

    @Override
    public Collection<Tag> listTags() {
        Collection<Tag> allTags = new HashSet<Tag>();
        XmlTags allXmlTags = service.path("tags").accept(MediaType.APPLICATION_XML).get(XmlTags.class);
        for (XmlTag xmlTag : allXmlTags.getTags()) {
            allTags.add(xmlTag);
        }
        return allTags;
    }
}

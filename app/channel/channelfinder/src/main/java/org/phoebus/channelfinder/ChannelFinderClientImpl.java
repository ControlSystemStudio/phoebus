/**
 * Copyright (C) 2010-2012 Brookhaven National Laboratory
 * Copyright (C) 2010-2012 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms.
 */
package org.phoebus.channelfinder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.phoebus.util.http.QueryParamsHelper;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * A Client object to query the channelfinder service for channels based on
 * channel names and/or properties and tags associated with channels.
 *
 * @author shroffk
 */
public class ChannelFinderClientImpl implements ChannelFinderClient {
    private ExecutorService executor;

    private HttpClient httpClient;
    private final String basicAuthenticationHeader;

    private static final String resourceChannels = "resources/channels";
    private static final String resourceProperties = "resources/properties";
    private static final String resourceTags = "resources/tags";

    private static final Logger log = Logger.getLogger(ChannelFinderClientImpl.class.getName());

    /**
     * A Builder class to help create the client to the Channelfinder Service
     *
     * @author shroffk
     */
    public static class CFCBuilder {

        // required
        private final URI uri;

        @SuppressWarnings("unused")
        private SSLContext sslContext = null;

        private final ExecutorService executor = Executors.newSingleThreadExecutor();

        private CFCBuilder() {
            this.uri = URI.create(org.phoebus.channelfinder.Preferences.serviceURL);
        }

        /**
         * Creates a {@link CFCBuilder} for a CF client to Default URL in the
         * channelfinder_preferences.properties.
         *
         * @return {@link CFCBuilder}
         */
        public static CFCBuilder serviceURL() {
            return new CFCBuilder();
        }

        @SuppressWarnings("unused")
        private CFCBuilder withSSLContext(SSLContext sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        /**
         * Will actually create a {@link ChannelFinderClientImpl} object using
         * the configuration informoation in this builder.
         *
         * @return {@link ChannelFinderClientImpl}
         */
        public ChannelFinderClient create() throws ChannelFinderException {
            if (this.uri == null || this.uri.toString().isEmpty()) {
                log.warning("Cannot create a channel finder client as URL is null or empty");
                return null;
            }
            log.info("Creating a channelfinder client to : " + this.uri);

            return new ChannelFinderClientImpl(this.uri, this.executor);
        }
    }

    ChannelFinderClientImpl(URI uri, ExecutorService executor) {

        basicAuthenticationHeader = "Basic " + Base64.getEncoder().encodeToString((org.phoebus.channelfinder.Preferences.username + ":" +
                org.phoebus.channelfinder.Preferences.password).getBytes());

        if (uri.getScheme().equalsIgnoreCase("http")) { //$NON-NLS-1$
            httpClient = HttpClient.newBuilder().build();
        } else if (uri.getScheme().equalsIgnoreCase("https")) {
            try {
                TrustManager PROMISCUOUS_TRUST_MANAGER = new X509ExtendedTrustManager() {
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
                };

                SSLContext sslContext = SSLContext.getInstance("SSL"); // OR TLS
                sslContext.init(null, new TrustManager[]{PROMISCUOUS_TRUST_MANAGER}, new SecureRandom());
                httpClient = HttpClient.newBuilder().sslContext(sslContext).build();
            } catch (Exception e) {
                log.log(Level.WARNING,
                        "Failed to properly create the elastic rest client to: " + org.phoebus.channelfinder.Preferences.serviceURL
                        , e);
                return;
            }
        }

        this.executor = executor;
    }

    /**
     * Get a list of names of all the properties currently present on the
     * channelfinder service.
     *
     * @return list of names of all existing {@link Property}s.
     */
    @Override
    public Collection<String> getAllPropertyNames() {
        Collection<Property> xmlProperties = getAllProperties();
        Collection<String> allNames = new HashSet<>();
        xmlProperties.forEach(Property::getName);
        return allNames;
    }

    /**
     * Get a list of all the properties currently present on the
     * channelfinder service.
     *
     * @return list of all existing {@link Property}s.
     */
    @Override
    public Collection<Property> getAllProperties() {
        return wrappedSubmit(new Callable<List<Property>>() {
            private final ObjectMapper mapper = new ObjectMapper();

            @Override
            public List<Property> call() {
                try {
                    HttpRequest httpRequest = HttpRequest.newBuilder()
                            .uri(URI.create(org.phoebus.channelfinder.Preferences.serviceURL + "/" + resourceProperties))
                            .header("Content-Type", MediaType.APPLICATION_JSON)
                            .GET()
                            .build();

                    HttpResponse<String> httpResponse =
                            httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                    if (httpResponse.statusCode() != 200) {
                        throw new ChannelFinderException(httpResponse.statusCode(), httpResponse.body());
                    }

                    List<XmlProperty> xmlproperties = mapper.readValue(httpResponse.body(),
                            new TypeReference<>() {
                            });
                    return xmlproperties.stream().map(Property::new).collect(Collectors.toList());
                } catch (Exception e) {
                    log.log(Level.WARNING, "Failed to retrieve channelfinder properties", e);
                    return Collections.emptyList();
                }
            }
        });
    }

    /**
     * Get a list of names of all the tags currently present on the
     * channelfinder service.
     *
     * @return a list of names of all the existing {@link Tag}s.
     */
    @Override
    public Collection<String> getAllTagNames() {
        return wrappedSubmit(new Callable<>() {
            private final ObjectMapper mapper = new ObjectMapper();

            @Override
            public Collection<String> call() {
                Collection<String> allTags = new HashSet<>();
                List<XmlTag> xmltags = new ArrayList<>();
                try {

                    HttpRequest httpRequest = HttpRequest.newBuilder()
                            .uri(URI.create(org.phoebus.channelfinder.Preferences.serviceURL + "/" + resourceTags))
                            .header("Content-Type", MediaType.APPLICATION_JSON)
                            .GET()
                            .build();

                    HttpResponse<String> httpResponse =
                            httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                    if (httpResponse.statusCode() != 200) {
                        throw new ChannelFinderException(httpResponse.statusCode(), httpResponse.body());
                    }

                    xmltags = mapper.readValue(httpResponse.body(),
                            new TypeReference<>() {
                            });
                } catch (IOException e) {
                    log.log(Level.WARNING, "Failed to parse the list of tags", e);
                } catch (InterruptedException e) {
                    throw new ChannelFinderException(e.getMessage());
                }
                for (XmlTag xmltag : xmltags) {
                    allTags.add(xmltag.getName());
                }
                return allTags;
            }
        });
    }

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Get a list of names of all the tags currently present on the
     * channelfinder service.
     *
     * @return a list of names of all the existing {@link Tag}s.
     */
    @Override
    public Collection<Tag> getAllTags() {
        return wrappedSubmit(() -> {
            try {
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(org.phoebus.channelfinder.Preferences.serviceURL + "/" + resourceTags))
                        .header("Content-Type", MediaType.APPLICATION_JSON)
                        .GET()
                        .build();

                HttpResponse<String> httpResponse =
                        httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (httpResponse.statusCode() != 200) {
                    throw new ChannelFinderException(httpResponse.statusCode(), httpResponse.body());
                }

                List<XmlTag> xmltags = mapper.readValue(httpResponse.body(),
                        new TypeReference<>() {
                        });
                return xmltags.stream().map(Tag::new).collect(Collectors.toList());
            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to parse the list of tags", e);
                return Collections.emptyList();
            }
        });
    }

    /**
     * Returns a channel that exactly matches the channelName
     * <code>channelName</code>.
     *
     * @param channelName - name of the required channel.
     * @return {@link Channel} with name <code>channelName</code> or null
     * @throws ChannelFinderException - channelfinder exception
     */
    @Override
    public Channel getChannel(String channelName) throws ChannelFinderException {
        return wrappedSubmit(new FindByChannelName(channelName));
    }

    @Deprecated
    public static void resetPreferences() {
        try {
            Preferences.userNodeForPackage(ChannelFinderClientImpl.class).clear();
        } catch (BackingStoreException e) {
            log.log(Level.WARNING, "Failed to retrieve channelfinder preferences", e);
        }
    }

    /**
     * Destructively set a single channel <code>channel</code>, if the channel
     * already exists it will be replaced with the given channel.
     *
     * @param channel the channel to be added
     * @throws ChannelFinderException - channelfinder exception
     */
    @Override
    public void set(Channel.Builder channel) throws ChannelFinderException {
        wrappedSubmit(new SetChannel(channel.toXml()));
    }

    private class SetChannel implements Runnable {
        private final XmlChannel pxmlChannel;

        public SetChannel(XmlChannel xmlChannel) {
            super();
            this.pxmlChannel = xmlChannel;
        }

        @Override
        public void run() {
            ObjectMapper mapper = new ObjectMapper();
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(org.phoebus.channelfinder.Preferences.serviceURL + "/" + resourceChannels + "/" +
                                URLEncoder.encode(pxmlChannel.getName(), StandardCharsets.UTF_8)))
                        .header("Content-Type", MediaType.APPLICATION_JSON)
                        .header("Authorization", basicAuthenticationHeader)
                        .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(this.pxmlChannel)))
                        .build();
                HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (httpResponse.statusCode() != 200) {
                    throw new ChannelFinderException(httpResponse.statusCode(), httpResponse.body());
                }
            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to set channel \"" + pxmlChannel.getName() + "\"", e);
            }
        }
    }

    /**
     * Search for channels who's name match the pattern <code>pattern</code>.<br>
     * The pattern can contain wildcard char * or ?.<br>
     *
     * @param pattern - the search pattern for the channel names
     * @return A Collection of channels who's name match the pattern
     * <code>pattern</code>
     * @throws ChannelFinderException - channelfinder exception
     */
    @Override
    public Collection<Channel> findByName(String pattern) throws ChannelFinderException {
        // return wrappedSubmit(new FindByParam("~name", pattern));
        Map<String, String> searchMap = new HashMap<>();
        searchMap.put("~name", pattern);
        return wrappedSubmit(new FindByMap(searchMap));
    }

    /**
     * Search for channels with properties who's Value match the pattern
     * <code>pattern</code>.<br>
     * The pattern can contain wildcard char * or ?.<br>
     *
     * @param property - the name of the property.
     * @param pattern  - the seatch pattern for the property value.
     * @return A collection of channels containing the property with name
     * <code>propertyName</code> who's value matches the pattern
     * <code> pattern</code>.
     * @throws ChannelFinderException - channelfinder exception
     */
    @Override
    public Collection<Channel> findByProperty(String property, String... pattern) throws ChannelFinderException {
        Map<String, String> propertyPatterns = new HashMap<>();
        if (pattern.length > 0) {
            propertyPatterns.put(property, Arrays.stream(pattern).collect(Collectors.joining(","))); //$NON-NLS-1$
        } else {
            propertyPatterns.put(property, "*"); //$NON-NLS-1$
        }
        return wrappedSubmit(new FindByMap(propertyPatterns));
    }

    /**
     * uery for channels based on the multiple criteria specified in the map.
     * Map.put("~name", "*")<br>
     * Map.put("~tag", "tag1")<br>
     * Map.put("Cell", "1")<br>
     * Map.put("Cell", "2")<br>
     * Map.put("Cell", "3")<br>
     * <p>
     * this will return all channels with name=any name AND tag=tag1 AND
     * property Cell = 1 OR 2 OR 3.
     *
     * @param map - multivalued map of all search criteria
     * @return Collection of channels which satisfy the search map.
     * @throws ChannelFinderException - channelfinder exception
     */
    public Collection<Channel> find(MultivaluedMap<String, String> map) throws ChannelFinderException {
        return wrappedSubmit(new FindByMap(map));
    }


    /**
     * Search for channels with tags who's name match the pattern
     * <code>pattern</code>.<br>
     * The pattern can contain wildcard char * or ?.<br>
     *
     * @param pattern - the search pattern for the tag names
     * @return A Collection of channels which contain tags who's name match the
     * pattern <code>pattern</code>
     * @throws ChannelFinderException - channelfinder exception
     */
    @Override
    public Collection<Channel> findByTag(String pattern) throws ChannelFinderException {
        // return wrappedSubmit(new FindByParam("~tag", pattern));
        List<String> and = Arrays.asList(pattern.split("&"));
        MultivaluedMap<String, String> searchMap = new MultivaluedHashMap<>();
        for (String string : and) {
            searchMap.add("~tag", string);
        }
        return wrappedSubmit(new FindByMap(searchMap));
    }

    /**
     * Destructively set a Tag <code>tag</code> with no associated channels to the
     * database.
     *
     * @param tag - the tag to be set.
     */
    @Override
    public void set(Tag.Builder tag) {
        wrappedSubmit(new SetTag(tag.toXml()));
    }

    /**
     * Set tag <code>tag</code> on the set of channels {channels} and remove it from
     * all others.
     *
     * @param tag          - the tag to be set.
     * @param channelNames - the list of channels to which this tag will be added and
     *                     removed from all others.
     * @throws ChannelFinderException - channelfinder exception
     */
    @Override
    public void set(Tag.Builder tag, Collection<String> channelNames) throws ChannelFinderException {
        wrappedSubmit(new SetTag(tag.toXml(), channelNames));
    }

    /**
     * Destructively set tag <code>tag</code> to channel <code>channelName</code> and
     * remove the tag from all other channels.
     *
     * @param tag         - the tag to be set.
     * @param channelName - the channel to which the tag should be set on.
     * @throws ChannelFinderException - channelfinder exception
     */
    @Override
    public void set(Tag.Builder tag, String channelName) throws ChannelFinderException {
        Collection<String> channelNames = new ArrayList<>();
        channelNames.add(channelName);
        wrappedSubmit(new SetTag(tag.toXml(), channelNames));
    }

    private class SetTag implements Runnable {
        private final XmlTag pxmlTag;

        SetTag(XmlTag xmlTag, Map<String, String> channelTagMap) {
            super();
            this.pxmlTag = xmlTag;
            List<XmlChannel> channels = new ArrayList<>();
            for (Entry<String, String> e : channelTagMap.entrySet()) {
                XmlChannel xmlChannel = new XmlChannel(e.getKey());
                // need a copy to avoid a cycle
                xmlChannel
                        .addXmlProperty(new XmlProperty(this.pxmlTag.getName(), this.pxmlTag.getOwner(), e.getValue()));
                channels.add(xmlChannel);
            }
            this.pxmlTag.setChannels(channels);
        }

        SetTag(XmlTag xmlTag, Collection<String> channelNames) {
            super();
            this.pxmlTag = xmlTag;
            List<XmlChannel> channels = new ArrayList<>();
            for (String channelName : channelNames) {
                XmlChannel xmlChannel = new XmlChannel(channelName);
                xmlChannel.addXmlTag(new XmlTag(this.pxmlTag.getName(), this.pxmlTag.getOwner()));
                channels.add(xmlChannel);
            }
            this.pxmlTag.setChannels(channels);
        }

        public SetTag(XmlTag xmlTag) {
            super();
            this.pxmlTag = xmlTag;
        }

        @Override
        public void run() {
            ObjectMapper mapper = new ObjectMapper();
            try {

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(org.phoebus.channelfinder.Preferences.serviceURL + "/" + resourceTags + "/" +
                                URLEncoder.encode(this.pxmlTag.getName(), StandardCharsets.UTF_8)))
                        .header("Content-Type", MediaType.APPLICATION_JSON)
                        .header("Accept", MediaType.APPLICATION_JSON)
                        .header("Authorization", basicAuthenticationHeader)
                        .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(this.pxmlTag)))
                        .build();

                HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (httpResponse.statusCode() != 200) {
                    throw new ChannelFinderException(httpResponse.statusCode(), httpResponse.body());
                }
            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to process the list of tags ", e);
            }
        }
    }

    /**
     * Destructively set property <code>prop</code> and add it to the channel
     * <code>channelName</code> and remove it from all others.
     *
     * @param prop        - property to be set.
     * @param channelName - the channel to which this property must be added.
     */
    @Override
    public void set(Property.Builder prop, String channelName) {
        Collection<String> ch = new ArrayList<>();
        ch.add(channelName);
        wrappedSubmit(new SetProperty(prop.toXml(), ch));
    }

    /**
     * Destructively set a set of channels, if any channels already exists it is
     * replaced.
     *
     * @param channels set of channels to be added
     * @throws ChannelFinderException - channelfinder exception
     */
    public void set(Collection<Channel.Builder> channels) throws ChannelFinderException {
        wrappedSubmit(new SetChannels(ChannelUtil.toCollectionXmlChannels(channels)));
    }

    private class SetChannels implements Runnable {
        private final List<XmlChannel> pxmlchannels;

        public SetChannels(List<XmlChannel> xmlchannels) {
            super();
            this.pxmlchannels = xmlchannels;
        }

        @Override
        public void run() {
            ObjectMapper mapper = new ObjectMapper();
            OutputStream out = new ByteArrayOutputStream();
            try {
                mapper.writeValue(out, this.pxmlchannels);
                final byte[] data = ((ByteArrayOutputStream) out).toByteArray();
                String test = new String(data);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(org.phoebus.channelfinder.Preferences.serviceURL + "/" + resourceChannels))
                        .header("Content-Type", MediaType.APPLICATION_JSON)
                        .header("Authorization", basicAuthenticationHeader)
                        .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(test)))
                        .build();

                HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (httpResponse.statusCode() != 200) {
                    throw new ChannelFinderException(httpResponse.statusCode(), httpResponse.body());
                }
            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to set channels ", e);
                throw new ChannelFinderException(e);
            }
        }
    }


    /**
     * Destructively set a new property <code>property</code>.
     *
     * @param prop - the property to be set.
     */
    @Override
    public void set(Property.Builder prop) throws ChannelFinderException {
        wrappedSubmit(new SetProperty(prop.toXml()));
    }

    /**
     * Destructively set property <code>prop</code> and add it to the channels
     * <code>channelNames</code> removing it from all other channels. By default all
     * channels will contain the property with the same value specified in the
     * <code>prop</code>.<br>
     * to individually set the value for each channel use channelPropertyMap.
     *
     * @param prop         - the property to be set.
     * @param channelNames - the channels to which this property should be added and
     *                     removed from all others.
     */
    @Override
    public void set(Property.Builder prop, Collection<String> channelNames) {
        wrappedSubmit(new SetProperty(prop.toXml(), channelNames));
    }

    /**
     * Destructively set the property <code>prop</code> and add it to the channels
     * specified in the <code>channelPropertyMap</code>, where the map key is the
     * channel name and the associated value is the property value to be used
     * for that channel.
     *
     * @param prop               - the property to be set.
     * @param channelPropertyMap - map with channel names and property values
     */
    @Override
    public void set(Property.Builder prop, Map<String, String> channelPropertyMap) {
        wrappedSubmit(new SetProperty(prop.toXml(), channelPropertyMap));
    }

    private class SetProperty implements Runnable {
        private final XmlProperty pxmlProperty;
        private final ObjectMapper mapper = new ObjectMapper();

        SetProperty(XmlProperty prop) {
            this.pxmlProperty = prop;
        }

        SetProperty(XmlProperty prop, Map<String, String> channelPropertyMap) {
            super();
            this.pxmlProperty = prop;
            List<XmlChannel> channels = new ArrayList<>();
            for (Entry<String, String> e : channelPropertyMap.entrySet()) {
                XmlChannel xmlChannel = new XmlChannel(e.getKey());
                // need a copy to avoid a cycle
                xmlChannel.addXmlProperty(
                        new XmlProperty(this.pxmlProperty.getName(), this.pxmlProperty.getOwner(), e.getValue()));
                channels.add(xmlChannel);
            }
            this.pxmlProperty.setChannels(channels);
        }

        SetProperty(XmlProperty prop, Collection<String> channelNames) {
            super();
            this.pxmlProperty = prop;
            List<XmlChannel> channels = new ArrayList<>();
            for (String channelName : channelNames) {
                XmlChannel xmlChannel = new XmlChannel(channelName);
                // need a copy to avoid a linking cycle
                xmlChannel.addXmlProperty(new XmlProperty(this.pxmlProperty.getName(), this.pxmlProperty.getOwner(),
                        this.pxmlProperty.getValue()));
                channels.add(xmlChannel);
            }
            this.pxmlProperty.setChannels(channels);
        }

        @Override
        public void run() {
            try {

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(org.phoebus.channelfinder.Preferences.serviceURL + "/" + resourceProperties + "/"
                                + URLEncoder.encode(pxmlProperty.getName(), StandardCharsets.UTF_8)))
                        .header("Content-Type", MediaType.APPLICATION_JSON)
                        .header("Accept", MediaType.APPLICATION_JSON)
                        .header("Authorization", basicAuthenticationHeader)
                        .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(this.pxmlProperty)))
                        .build();

                HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (httpResponse.statusCode() != 200) {
                    throw new ChannelFinderException(httpResponse.statusCode(), httpResponse.body());
                }
            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to process the list of properties ", e);
            }
        }
    }

    /**
     * Query for channels based on the Query string <code>query</code> example:
     * find("&quot;"SR* Cell=1,2 Tags=GolderOrbit,myTag")<br>
     * <p>
     * this will return all channels with names starting with SR AND have
     * property Cell=1 OR 2 AND have tags goldenOrbit AND myTag.<br>
     * <p>
     * IMP: each criteria is logically AND'ed while multiple values for
     * Properties are OR'ed.<br>
     *
     * @param query - channel finder query
     * @return Collection of channels which satisfy the search criteria.
     * @throws ChannelFinderException - channelfinder exception
     */
    @Override
    public Collection<Channel> find(String query) throws ChannelFinderException {
        return wrappedSubmit(new FindByMap(buildSearchMap(query)));
    }

    /**
     * Query for channels based on the multiple criteria specified in the map.
     * Map.put("~name", "*")<br>
     * Map.put("~tag", "tag1")<br>
     * Map.put("Cell", "1,2,3")
     * <p>
     * this will return all channels with name=any name AND tag=tag1 AND
     * property Cell = 1 OR 2 OR 3.
     *
     * @param map - search map
     * @return Collection of channels which satisfy the search map.
     * @throws ChannelFinderException - channelfinder exception
     */
    @Override
    public Collection<Channel> find(Map<String, String> map) throws ChannelFinderException {
        return wrappedSubmit(new FindByMap(map));
    }

    private class FindByChannelName implements Callable<Channel> {

        private final String channelName;
        private final ObjectMapper mapper = new ObjectMapper();

        FindByChannelName(String channelName) {
            super();
            this.channelName = channelName;
        }

        @Override
        public Channel call() {
            mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            try {

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(org.phoebus.channelfinder.Preferences.serviceURL + "/" + resourceChannels + "/" +
                                URLEncoder.encode(channelName, StandardCharsets.UTF_8)))
                        .header("Content-Type", MediaType.APPLICATION_JSON)
                        .GET()
                        .build();

                HttpResponse<String> httpResponse =
                        httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                if (httpResponse.statusCode() != 200) {
                    throw new ChannelFinderException(httpResponse.statusCode(), httpResponse.body());
                }

                return new Channel(mapper.readValue(httpResponse.body(), XmlChannel.class));

            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to find channel \"" + channelName + "\"", e);
                return null;
            }
        }

    }

    private class FindByMap implements Callable<Collection<Channel>> {

        private final MultivaluedMap<String, String> multivaluedMap;
        private final ObjectMapper mapper = new ObjectMapper();

        FindByMap(Map<String, String> map) {
            MultivaluedMap<String, String> multivaluedMap = new MultivaluedHashMap<>();
            for (Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();
                for (String value : entry.getValue().split(",")) {
                    multivaluedMap.put(key, List.of(value.trim()));
                }
            }
            this.multivaluedMap = multivaluedMap;
        }

        FindByMap(MultivaluedMap<String, String> map) {
            this.multivaluedMap = new MultivaluedHashMap<>();
            this.multivaluedMap.putAll(map);
        }

        @Override
        public Collection<Channel> call() {
            Collection<Channel> channels = new HashSet<>();
            List<XmlChannel> xmlchannels = new ArrayList<>();
            long start = System.currentTimeMillis();
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(org.phoebus.channelfinder.Preferences.serviceURL + "/" + resourceChannels + "?" + QueryParamsHelper.mapToQueryParams(multivaluedMap)))
                        .header("Content-Type", MediaType.APPLICATION_JSON)
                        .GET()
                        .build();
                HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (httpResponse.statusCode() != 200) {
                    throw new ChannelFinderException(httpResponse.statusCode(), httpResponse.body());
                }
                xmlchannels = mapper.readValue(httpResponse.body(), new TypeReference<>() {
                });
            } catch (Exception e) {
                log.log(Level.WARNING, "Error creating channels:", e);
            }
            log.log(Level.FINE, "Finished mapping to xml. (Time: " + (System.currentTimeMillis() - start) + " ms)");
            start = System.currentTimeMillis();
            for (XmlChannel xmlchannel : xmlchannels) {
                channels.add(new Channel(xmlchannel));
            }
            log.log(Level.FINE, "Finished creating new channels. (Time: " + (System.currentTimeMillis() - start) + " ms)");
            return Collections.unmodifiableCollection(channels);
        }
    }

    public static MultivaluedMap<String, String> buildSearchMap(String searchPattern) {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        searchPattern = searchPattern.replaceAll(", ", ",");
        String[] searchWords = searchPattern.split("\\s");
        List<String> searchNames = new ArrayList<>();
        for (String searchWord : searchWords) {
            if (!searchWord.contains("=")) {
                // this is a name value
                if (searchWord != null && !searchWord.isEmpty())
                    searchNames.add(searchWord);
            } else {
                // this is a property or tag
                String[] keyValue = searchWord.split("=");
                String key = null;
                String valuePattern;
                try {
                    key = keyValue[0];
                    valuePattern = keyValue[1];

                    boolean isNot = key.endsWith("!");
                    if (isNot) {
                        key = key.substring(0, key.length() - 1);
                    }

                    if (key.equalsIgnoreCase("Tags") || key.equalsIgnoreCase("Tag")) {
                        key = "~tag";
                    }
                    for (String value : valuePattern.split("&")) {
                        map.add(key + (isNot ? "!" : ""), value.trim());
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    if (e.getMessage().equals(String.valueOf(0))) {
                        throw new IllegalArgumentException("= must be preceeded by a propertyName or keyword Tags.");
                    } else if (e.getMessage().equals(String.valueOf(1)))
                        throw new IllegalArgumentException("key: '" + key + "' is specified with no pattern.");
                }

            }
        }
        map.add("~name", searchNames.stream().collect(Collectors.joining("&")));
        return map;
    }

    /**
     * Delete tag <code>tag</code> from the channel with the name
     * <code>channelName</code>
     *
     * @param tag         - the tag to be deleted.
     * @param channelName - the channel from which to delete the tag <code>tag</code>
     * @throws ChannelFinderException - throws exception
     */
    @Override
    public void delete(Tag.Builder tag, String channelName) throws ChannelFinderException {
        wrappedSubmit(new DeleteElementfromChannel(resourceTags, tag // $NON-NLS-1$
                .toXml().getName(), channelName));
    }

    /**
     * Remove the tag <code>tag </code> from all the channels <code>channelNames</code>
     *
     * @param tag          - the tag to be deleted.
     * @param channelNames - the channels from which to delete the tag <code>tag</code>
     * @throws ChannelFinderException - throws exception
     */
    @Override
    public void delete(Tag.Builder tag, Collection<String> channelNames) throws ChannelFinderException {
        // TODO optimize using the /tags/<name> payload with list of channels
        for (String channelName : channelNames) {
            delete(tag, channelName);
        }
    }

    /**
     * Completely Delete {tag} with name = tagName from all channels and the
     * channelfinder service.
     *
     * @param tagName - name of tag to be deleted.
     * @throws ChannelFinderException - throws exception
     */
    @Override
    public void deleteTag(String tagName) throws ChannelFinderException {
        wrappedSubmit(new DeleteElement(resourceTags, tagName));
    }

    /**
     * Completely Delete property with name = propertyName from all channels and
     * the channelfinder service.
     *
     * @param propertyName - name of property to be deleted.
     * @throws ChannelFinderException - throws exception
     */
    @Override
    public void deleteProperty(String propertyName) throws ChannelFinderException {
        wrappedSubmit(new DeleteElement(resourceProperties, propertyName));
    }

    /**
     * Delete the channel identified by <code>channel</code>
     *
     * @param channelName - channel to be removed
     * @throws ChannelFinderException - channelfinder exception
     */
    @Override
    public void deleteChannel(String channelName) throws ChannelFinderException {
        wrappedSubmit(new DeleteElement(resourceChannels, channelName)); // $NON-NLS-1$
    }

    /**
     * Delete the set of channels identified by <code>channels</code>
     *
     * @param channels - channels to be deleted
     * @throws ChannelFinderException - throws exception
     */
    @Deprecated
    @Override
    public void delete(Collection<Channel.Builder> channels) throws ChannelFinderException {
        for (Channel.Builder channel : channels) {
            deleteChannel(channel.build().getName());
        }
    }


    private class DeleteElement implements Runnable {
        private final String elementType;
        private final String elementName;

        DeleteElement(String elementType, String elementName) {
            super();
            this.elementType = elementType;
            this.elementName = elementName;
        }

        @Override
        public void run() {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(org.phoebus.channelfinder.Preferences.serviceURL + "/" +
                                this.elementType + "/" +
                                URLEncoder.encode(this.elementName, StandardCharsets.UTF_8)))
                        .header("Authorization", basicAuthenticationHeader)
                        .DELETE()
                        .build();
                HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (httpResponse.statusCode() != 200) {
                    throw new ChannelFinderException(httpResponse.statusCode(), httpResponse.body());
                }
            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to delete element from channel ", e);
            }
        }
    }


    /**
     * Remove property <code>property</code> from the channel with name
     * <code>channelName</code>
     *
     * @param property    - the property to be deleted.
     * @param channelName - the channel from which to delete the property
     *                    <code>property</code>
     * @throws ChannelFinderException - throws exception
     */
    @Override
    public void delete(Property.Builder property, String channelName) throws ChannelFinderException {
        wrappedSubmit(new DeleteElementfromChannel(resourceProperties, property.build().getName(), channelName));
    }

    /**
     * Remove the property <code>property</code> from the set of channels
     * <code>channelNames</code>
     *
     * @param property     - the property to be deleted.
     * @param channelNames - the channels from which to delete the property
     *                     <code>property</code>
     * @throws ChannelFinderException - throws exception
     */
    @Override
    public void delete(Property.Builder property, Collection<String> channelNames) throws ChannelFinderException {
        for (String channel : channelNames) {
            delete(property, channel);
        }
    }

    private class DeleteElementfromChannel implements Runnable {
        private final String elementType;
        private final String elementName;
        private final String channelName;

        DeleteElementfromChannel(String elementType, String elementName, String channelName) {
            super();
            this.elementType = elementType;
            this.elementName = elementName;
            this.channelName = channelName;
        }

        @Override
        public void run() {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(org.phoebus.channelfinder.Preferences.serviceURL + "/" +
                            this.elementType + "/" +
                            URLEncoder.encode(this.elementName, StandardCharsets.UTF_8) + "/" +
                            URLEncoder.encode(this.channelName, StandardCharsets.UTF_8)))
                    .header("Accept", MediaType.APPLICATION_JSON)
                    .header("Authorization", basicAuthenticationHeader)
                    .DELETE()
                    .build();

            try {
                HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (httpResponse.statusCode() != 200) {
                    throw new ChannelFinderException(httpResponse.statusCode(), httpResponse.body());
                }
            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to delete element from channel ", e);
            }
        }

    }

    private <T> T wrappedSubmit(Callable<T> callable) {
        try {
            return this.executor.submit(callable).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ChannelFinderException) {
                throw (ChannelFinderException) e.getCause();
            }
            if (e.getMessage() != null) {
                throw new ChannelFinderException(e.getMessage());
            }
            throw new RuntimeException(e);
        }
    }

    private void wrappedSubmit(Runnable runnable) {
        try {
            this.executor.submit(runnable).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ChannelFinderException) {
                throw (ChannelFinderException) e.getCause();
            }
            if (e.getMessage() != null) {
                throw new ChannelFinderException(e.getMessage());
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Update existing channel with <code>channel</code>.
     *
     * @param channel - channel builder
     * @throws ChannelFinderException - channelfinder exception
     */
    @Override
    public void update(Channel.Builder channel) throws ChannelFinderException {
        wrappedSubmit(new UpdateChannel(channel.toXml()));
    }

    private class UpdateChannel implements Runnable {
        private final XmlChannel channel;
        private final ObjectMapper mapper = new ObjectMapper();

        UpdateChannel(XmlChannel channel) {
            super();
            this.channel = channel;
        }

        @Override
        public void run() {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(org.phoebus.channelfinder.Preferences.serviceURL + "/" + resourceChannels + "/" +
                                URLEncoder.encode(this.channel.getName(), StandardCharsets.UTF_8)))
                        .header("Content-Type", MediaType.APPLICATION_JSON)
                        .header("Authorization", basicAuthenticationHeader)
                        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(this.channel)))
                        .build();

                HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (httpResponse.statusCode() != 200) {
                    throw new ChannelFinderException(httpResponse.statusCode(), httpResponse.body());
                }
            } catch (Exception e) {
                throw new ChannelFinderException(e);
            }
        }

    }

    /**
     * Update the Tag <code>tag</code> by adding it to the set of the channels with
     * names <code>channelNames</code>, without affecting the other instances of
     * this tag.
     *
     * @param tag          - the tag that needs to be updated.
     * @param channelNames - list of channels to which this tag should be added.
     * @throws ChannelFinderException - channelfinder exception
     */
    @Override
    public void update(Tag.Builder tag, Collection<String> channelNames) throws ChannelFinderException {
        wrappedSubmit(new UpdateTag(tag.toXml(), channelNames));
    }

    /**
     * Update Tag <code>tag </code> by adding it to Channel with name
     * <code>channelName</code>, without affecting the other instances of this tag.
     *
     * @param tag         the tag to be added
     * @param channelName Name of the channel to which the tag is to be added
     * @throws ChannelFinderException - channelfinder exception
     */
    @Override
    public void update(Tag.Builder tag, String channelName) throws ChannelFinderException {
        wrappedSubmit(new UpdateTag(tag.toXml(), channelName));
    }


    private class UpdateTag implements Runnable {
        private final XmlTag pxmlTag;
        private final ObjectMapper mapper = new ObjectMapper();

        @SuppressWarnings("unused")
        UpdateTag(XmlTag xmlTag) {
            super();
            this.pxmlTag = xmlTag;
        }

        UpdateTag(XmlTag xmlTag, String ChannelName) {
            super();
            this.pxmlTag = xmlTag;
            List<XmlChannel> channels = new ArrayList<>();
            channels.add(new XmlChannel(ChannelName));
            this.pxmlTag.setChannels(channels);
        }

        UpdateTag(XmlTag xmlTag, Collection<String> channelNames) {
            super();
            this.pxmlTag = xmlTag;
            List<XmlChannel> channels = new ArrayList<>();
            for (String channelName : channelNames) {
                channels.add(new XmlChannel(channelName, ""));
            }
            xmlTag.setChannels(channels);
        }

        @Override
        public void run() {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(org.phoebus.channelfinder.Preferences.serviceURL + "/" + resourceTags + "/" +
                                URLEncoder.encode(this.pxmlTag.getName(), StandardCharsets.UTF_8)))
                        .header("Content-Type", MediaType.APPLICATION_JSON)
                        .header("Authorization", basicAuthenticationHeader)
                        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(this.pxmlTag)))
                        .build();

                HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (httpResponse.statusCode() != 200) {
                    throw new ChannelFinderException(httpResponse.statusCode(), httpResponse.body());
                }
            } catch (JsonProcessingException e) {
                log.log(Level.WARNING, "Failed to update tag ", e);
            } catch (IOException | InterruptedException e) {
                throw new ChannelFinderException(e.getMessage());
            }
        }
    }

    /**
     * @param property     - property builder
     * @param channelNames - list of channel names
     * @throws ChannelFinderException - channelfinder exception
     */
    @Override
    public void update(Property.Builder property, Collection<String> channelNames) throws ChannelFinderException {
        wrappedSubmit(new UpdateProperty(property.toXml(), channelNames));
    }

    private class UpdateProperty implements Runnable {
        private final XmlProperty pxmlProperty;
        private final ObjectMapper mapper = new ObjectMapper();

        @SuppressWarnings("unused")
        UpdateProperty(XmlProperty xmlProperty) {
            super();
            this.pxmlProperty = xmlProperty;
        }

        UpdateProperty(XmlProperty xmlProperty, Collection<String> channelNames) {
            super();
            this.pxmlProperty = xmlProperty;
            List<XmlChannel> channels = new ArrayList<>();
            for (String channelName : channelNames) {
                XmlChannel xmlChannel = new XmlChannel(channelName);
                // need a defensive copy to avoid A cycle
                xmlChannel.addXmlProperty(
                        new XmlProperty(xmlProperty.getName(), xmlProperty.getOwner(), xmlProperty.getValue()));
                channels.add(xmlChannel);
            }
            xmlProperty.setChannels(channels);
        }

        UpdateProperty(XmlProperty xmlProperty, Map<String, String> channelPropValueMap) {
            super();
            this.pxmlProperty = xmlProperty;
            List<XmlChannel> channels = new ArrayList<>();
            for (Entry<String, String> e : channelPropValueMap.entrySet()) {
                XmlChannel xmlChannel = new XmlChannel(e.getKey());
                // need a defensive copy to avoid A cycle
                xmlChannel.addXmlProperty(new XmlProperty(xmlProperty.getName(), xmlProperty.getOwner(), e.getValue()));
                channels.add(xmlChannel);
            }
            xmlProperty.setChannels(channels);
        }

        @Override
        public void run() {

            try {
                String s = mapper.writeValueAsString(this.pxmlProperty);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(org.phoebus.channelfinder.Preferences.serviceURL + "/" + resourceProperties + "/" +
                                URLEncoder.encode(this.pxmlProperty.getName(), StandardCharsets.UTF_8)))
                        .header("Content-Type", MediaType.APPLICATION_JSON)
                        .header("Accept", MediaType.APPLICATION_JSON)
                        //.header("Authorization", basicAuthenticationHeader)
                        .POST(HttpRequest.BodyPublishers.ofString(s))
                        .build();
                HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (httpResponse.statusCode() != 200) {
                    throw new ChannelFinderException(httpResponse.statusCode(), httpResponse.body());
                }
            } catch (JsonProcessingException e) {
                log.log(Level.WARNING, "Failed to update property ", e);
            } catch (IOException | InterruptedException e) {
                throw new ChannelFinderException(e.getMessage());
            }
        }
    }

    /**
     * @param property            - property builder
     * @param channelPropValueMap - channel property value map
     * @throws ChannelFinderException - channelfinder exception
     */
    public void update(Property.Builder property, Map<String, String> channelPropValueMap)
            throws ChannelFinderException {
        wrappedSubmit(new UpdateProperty(property.toXml(), channelPropValueMap));
    }

    /**
     * Update Property <code>property</code> by adding it to the channel
     * <code>channelName</code>, without affecting the other channels.
     *
     * @param property    - the property to be updated
     * @param channelName - the channel to which this property should be added or
     *                    updated.
     * @throws ChannelFinderException - channelfinder exception
     */
    public void update(Property.Builder property, String channelName) throws ChannelFinderException {
        wrappedSubmit(new UpdateChannelProperty(property.toXml(), channelName));
    }

    private class UpdateChannelProperty implements Runnable {
        private final XmlProperty pxmlProperty;
        private final ObjectMapper mapper = new ObjectMapper();

        UpdateChannelProperty(XmlProperty xmlProperty, String channelName) {
            super();
            this.pxmlProperty = xmlProperty;
            XmlChannel xmlChannel = new XmlChannel(channelName);
            List<XmlChannel> channels = new ArrayList<>();
            channels.add(xmlChannel);
            // need a defensive copy to avoid A cycle
            xmlChannel.addXmlProperty(
                    new XmlProperty(xmlProperty.getName(), xmlProperty.getOwner(), xmlProperty.getValue()));
            xmlProperty.setChannels(channels);
        }

        @Override
        public void run() {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(org.phoebus.channelfinder.Preferences.serviceURL + "/" + resourceProperties + "/" +
                                URLEncoder.encode(this.pxmlProperty.getName(), StandardCharsets.UTF_8)))
                        .header("Content-Type", MediaType.APPLICATION_JSON)
                        .header("Accept", MediaType.APPLICATION_JSON)
                        .header("Authorization", basicAuthenticationHeader)
                        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(this.pxmlProperty)))
                        .build();
                HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (httpResponse.statusCode() != 200) {
                    throw new ChannelFinderException(httpResponse.statusCode(), httpResponse.body());
                }
            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to process the channel property ", e);
                throw new ChannelFinderException(e);
            }
        }
    }

    @Override
    public Collection<Channel> getAllChannels() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(org.phoebus.channelfinder.Preferences.serviceURL + "/" + resourceChannels))
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .GET()
                    .build();

            HttpResponse<String> httpResponse =
                    httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() != 200) {
                throw new ChannelFinderException(httpResponse.statusCode(), httpResponse.body());
            }
            List<XmlChannel> xmlChannels =
                    mapper.readValue(httpResponse.body(), new TypeReference<>() {
                    });
            Collection<Channel> set = new HashSet<>();
            for (XmlChannel channel : xmlChannels) {
                set.add(new Channel(channel));
            }
            return set;
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to retrieve all channels", e);
            return Collections.emptyList();
        }
    }

    /**
     * close
     */
    public void close() {
        this.executor.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!this.executor.awaitTermination(60, TimeUnit.SECONDS)) {
                this.executor.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!this.executor.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate"); //$NON-NLS-1$
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            this.executor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }
}

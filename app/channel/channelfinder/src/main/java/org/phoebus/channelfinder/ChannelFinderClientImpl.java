/**
 * Copyright (C) 2010-2012 Brookhaven National Laboratory
 * Copyright (C) 2010-2012 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms.
 */
package org.phoebus.channelfinder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.phoebus.util.http.QueryParamsHelper;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
            public List<Property> call()  {
                List<XmlProperty> xmlproperties = new ArrayList<>();

                try {
                    HttpRequest httpRequest = HttpRequest.newBuilder()
                            .uri(URI.create(org.phoebus.channelfinder.Preferences.serviceURL + "/" + resourceProperties))
                            .header("Content-Type", MediaType.APPLICATION_JSON)
                            .GET()
                            .build();

                    HttpResponse<String> httpResponse =
                            httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                    if(httpResponse.statusCode() != 200){
                        throw new ChannelFinderException(httpResponse.statusCode(), httpResponse.body());
                    }

                    xmlproperties = mapper.readValue(httpResponse.body(),
                            new TypeReference<>() {
                            });

                } catch (Exception e) {
                    log.log(Level.WARNING, "Failed to retrieve channelfinder properties", e);
                }
                return xmlproperties.stream().map(Property::new).collect(Collectors.toList());
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

                    if(httpResponse.statusCode() != 200){
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

                if(httpResponse.statusCode() != 200){
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
            return xmltags.stream().map(Tag::new).collect(Collectors.toList());

        });
    }

    @Deprecated
    public static void resetPreferences() {
        try {
            Preferences.userNodeForPackage(ChannelFinderClientImpl.class).clear();
        } catch (BackingStoreException e) {
            log.log(Level.WARNING, "Failed to retrieve channelfinder preferences", e);
        }
    }

    public void set(Tag.Builder tag, Map<String, String> channelTagMap) {
        wrappedSubmit(new SetTag(tag.toXml(), channelTagMap));
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

        @Override
        public void run() {
            ObjectMapper mapper = new ObjectMapper();
            try {

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(org.phoebus.channelfinder.Preferences.serviceURL + "/" + resourceTags + "/" + this.pxmlTag.getName()))
                        .header("Content-Type", MediaType.APPLICATION_JSON)
                        .header("Accept", MediaType.APPLICATION_JSON)
                        .header("Authorization", basicAuthenticationHeader)
                        .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(this.pxmlTag)))
                        .build();

                HttpResponse<String > httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if(httpResponse.statusCode() != 200){
                    throw new ChannelFinderException(httpResponse.statusCode(), httpResponse.body());
                }
            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to process the list of tags ", e);
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
                if(httpResponse.statusCode() != 200){
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
                            this.elementName + "/" +
                            this.channelName))
                    .header("Accept", MediaType.APPLICATION_JSON)
                    .header("Authorization", basicAuthenticationHeader)
                    .DELETE()
                    .build();

            try {
                HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if(httpResponse.statusCode() != 200){
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
            if (e.getMessage() != null) {
                throw new ChannelFinderException(e.getMessage());
            }
            throw new RuntimeException(e);
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
    public void update(Tag.Builder tag, Collection<String> channelNames) throws ChannelFinderException {
        wrappedSubmit(new UpdateTag(tag.toXml(), channelNames));
    }

    /**
     * @param property     - property builder
     * @param channelNames - list of channel names
     * @throws ChannelFinderException - channelfinder exception
     */
    public void update(Property.Builder property, Collection<String> channelNames) throws ChannelFinderException {
        wrappedSubmit(new UpdateProperty(property.toXml(), channelNames));
    }


    private class UpdateTag implements Runnable {
        private final XmlTag pxmlTag;
        private final ObjectMapper mapper = new ObjectMapper();

        @SuppressWarnings("unused")
        UpdateTag(XmlTag xmlTag) {
            super();
            this.pxmlTag = xmlTag;
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
                        .uri(URI.create(org.phoebus.channelfinder.Preferences.serviceURL + "/" + resourceTags + "/" + this.pxmlTag.getName()))
                        .header("Content-Type", MediaType.APPLICATION_JSON)
                        .header("Authorization", basicAuthenticationHeader)
                        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(this.pxmlTag)))
                        .build();

                HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if(httpResponse.statusCode() != 200){
                    throw new ChannelFinderException(httpResponse.statusCode(), httpResponse.body());
                }
            } catch (JsonProcessingException e) {
                log.log(Level.WARNING, "Failed to update tag ", e);
            } catch (IOException | InterruptedException e) {
                throw new ChannelFinderException(e.getMessage());
            }
        }
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

        @Override
        public void run() {

            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(org.phoebus.channelfinder.Preferences.serviceURL + "/" + resourceProperties + "/" + this.pxmlProperty.getName()))
                        .header("Content-Type", MediaType.APPLICATION_JSON)
                        .header("Accept", MediaType.APPLICATION_JSON)
                        .header("Authorization", basicAuthenticationHeader)
                        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(this.pxmlProperty)))
                        .build();
                HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if(httpResponse.statusCode() != 200){
                    throw new ChannelFinderException(httpResponse.statusCode(), httpResponse.body());
                }
            } catch (JsonProcessingException e) {
                log.log(Level.WARNING, "Failed to update property ", e);
            } catch (IOException | InterruptedException e) {
                throw new ChannelFinderException(e.getMessage());
            }
        }
    }
}

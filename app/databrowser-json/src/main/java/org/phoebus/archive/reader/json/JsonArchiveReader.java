/*******************************************************************************
 * Copyright (c) 2013-2024 aquenos GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.phoebus.archive.reader.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.UnknownChannelException;
import org.phoebus.archive.reader.ValueIterator;
import org.phoebus.archive.reader.json.internal.JsonArchiveInfoReader;
import org.phoebus.archive.reader.json.internal.JsonValueIterator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Cleaner;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

/**
 * <p>
 * Archive reader implementation that connects to an archive server using an
 * HTTP / JSON based protocol. Typically, this reader is used together with the
 * JSON archive server. However, it will work with any compliant HTTP server.
 * </p>
 *
 * <p>
 * Instances of this class are thread-safe.
 * </p>
 */
public class JsonArchiveReader implements ArchiveReader {

    private final static BigInteger ONE_BILLION = BigInteger
            .valueOf(1000000000L);

    private final Cleaner cleaner;
    private final String description;
    private final String http_url;
    private final Map<JsonValueIterator, Cleaner.Cleanable> iterators;
    private final JsonFactory json_factory;
    private final int key;
    private final Logger logger;
    private final JsonArchivePreferences preferences;

    /**
     * <p>
     * Creates an archive reader that requests samples from the specified URL.
     * The URL must start with the scheme "json" followed by the HTTP or
     * HTTPS URL of the archive server. The URL must include the context path,
     * but not include the servlet path.
     * </p>
     *
     * <p>
     * For example, the URL <code>json:http://localhost:8080/</code> will
     * expect the archive server to run on port 8080 of the same computer and
     * will use the URL
     * <code>http://localhost:8080/archive/&lt;key&gt;/channels-by-pattern/&lt;pattern&gt;</code>
     * when searching for channels.
     * </p>
     *
     * <p>
     * If not specified, the <code>key</code> is assumes to be <code>1</code>.
     * The key can be specified by adding <code>;key=&lt;key&gt;</code> to the
     * archive URL (e.g. <code>json:http://localhost:8080/;key=2</code>).
     * </p>
     *
     * @param url
     *  archive URL with the scheme "json" followed by a valid HTTP HTTPS URL.
     * @param preferences
     *  preferences that are used by this archive reader.
     * @throws IllegalArgumentException
     *  if the specified URL is invalid.
     */
    public JsonArchiveReader(String url, JsonArchivePreferences preferences) {
        // Initialize the logger first.
        this.logger = Logger.getLogger(getClass().getName());
        // The URL must start with the json: prefix.
        if (!url.startsWith("json:")) {
            throw new IllegalArgumentException(
                    "The URL \""
                            + url
                            + "\" is not a valid archive URL, because it does "
                            + "not start with \"json:\".");
        }
        // Remove the prefix.
        var http_url = url.substring(5);
        // Extract the key=… part, if present.
        var key = 1;
        var semicolon_index = http_url.indexOf(';');
        if (semicolon_index != -1) {
            final var args_part = http_url.substring(semicolon_index + 1);
            http_url = http_url.substring(0, semicolon_index);
            if (args_part.startsWith("key=")) {
                try {
                    key = Integer.parseInt(args_part.substring(4));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "The URL \""
                                    + url
                                    + "\" is not a valid archive URL, because "
                                    + "the argument \";"
                                    + args_part
                                    + "\" is invalid.");
                }
            }
        }
        // We want the base URL to always have a trailing slash, so that we
        // have a common basis for constructing specific URLs.
        if (!http_url.endsWith("/")) {
            http_url = http_url + "/";
        }
        // Initialize the class fields.
        this.cleaner = Cleaner.create();
        this.http_url = http_url;
        this.iterators = new WeakHashMap<>();
        this.json_factory = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS).build();
        // We want to ensure that the underlying input stream is closed when
        // closing a parser. This should be the default, but it is better to be
        // sure.
        this.json_factory.enable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        this.key = key;
        this.preferences = Objects.requireNonNull(preferences);
        // We have to initialize most fields before we can retrieve the
        // description.
        this.description = retrieveArchiveDescription();
    }

    @Override
    public void cancel() {
        synchronized (iterators) {
            for (JsonValueIterator i : iterators.keySet()) {
                // We only call cancel. The iterator is going to be removed
                // from the map when it is closed.
                i.cancel();
            }
        }
    }

    @Override
    public void close() {
        // We do nothing here, because we do not hold any expensive resources
        // that need to be closed.
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Collection<String> getNamesByPattern(String glob_pattern)
            throws Exception {
        final var url = "/" + key + "/channels-by-pattern/"
                + URLEncoder.encode(glob_pattern, StandardCharsets.UTF_8);
        try (final var parser = doGetJson(url)) {
            var token = parser.nextToken();
            if (token == null) {
                throw new IOException("Unexpected end of stream.");
            }
            if (token != JsonToken.START_ARRAY) {
                throw new JsonParseException(
                        parser,
                        "Expected START_ARRAY but got " + token,
                        parser.getTokenLocation());
            }
            final var channel_names = new LinkedList<String>();
            while (true) {
                token = parser.nextToken();
                if (token == null) {
                    throw new IOException("Unexpected end of stream.");
                }
                if (token == JsonToken.END_ARRAY) {
                    break;
                }
                if (token == JsonToken.VALUE_STRING) {
                    String channel_name = parser.getText();
                    channel_names.add(channel_name);
                } else {
                    throw new JsonParseException(
                            parser,
                            "Expected VALUE_STRING but got " + token,
                            parser.getTokenLocation());
                }
            }
            return channel_names;
        }
    }

    @Override
    public ValueIterator getOptimizedValues(
            String name, Instant start, Instant end, int count)
            throws UnknownChannelException, Exception {
        return getValues(name, start, end, count);
    }

    @Override
    public ValueIterator getRawValues(
            String name, Instant start, Instant end)
            throws UnknownChannelException, Exception {
        return getValues(name, start, end, null);
    }

    /**
     * Converts a {@link BigInteger} representing the number of nanoseconds
     * since epoch to an {@link Instant}.
     *
     * @param timestamp
     *  number of nanoseconds since UNIX epoch (January 1st, 1970,
     *  00:00:00 UTC).
     * @return
     *  instant representing the <code>timestamp</code>.
     */
    private static BigInteger timestampToBigInteger(final Instant timestamp) {
        return BigInteger.valueOf(timestamp.getNano()).add(
                BigInteger.valueOf(timestamp.getEpochSecond()).multiply(
                        ONE_BILLION));
    }

    /**
     *  <p>
     *  Sends a <code>GET</code> request to the archive source and returns the
     *  response.
     *  </p>
     *
     * @param url
     *  URL which shall be requested. Must start with a forward slash and be
     *  relative to the base HTTP url configured for this reader.
     * @return
     *  input stream that provides the HTTP server’s response.
     * @throws IOException
     *  if the URL is malformed, the connection cannot be opened, or the input
     *  stream cannot be retrieved.
     */
    private InputStream doGet(String url) throws IOException {
        final var request_url = this.http_url + "archive" + url;
        final var connection = new URL(request_url).openConnection();
        connection.addRequestProperty("Accept-Encoding", "gzip, deflate");
        connection.connect();
        final var content_encoding = connection.getHeaderField(
                "Content-Encoding");
        final var input_stream = connection.getInputStream();
        try {
            if (content_encoding != null) {
                if (content_encoding.equals("gzip")) {
                    return new GZIPInputStream(input_stream);
                } else if (content_encoding.equals("deflate")) {
                    return new DeflaterInputStream(input_stream);
                }
            }
            return input_stream;
        } catch (IOException | RuntimeException e) {
            input_stream.close();
            throw e;
        }
    }

    /**
     *  <p>
     *  Sends a <code>GET</code> request to the archive source and returns a
     *  JSON parser for the response.
     *  </p>
     *
     * @param url
     *  URL which shall be requested. Must start with a forward slash and be
     *  relative to the base HTTP url configured for this reader.
     * @return
     *  JSON parser that parses HTTP server’s response.
     * @throws IOException
     *  if the URL is malformed, the connection cannot be opened, or the JSON
     *  parser cannot be created.
     */
    private JsonParser doGetJson(String url) throws IOException {
        final var input_stream = doGet(url);
        try {
            return json_factory.createParser(input_stream);
        } catch (IOException | RuntimeException e) {
            // If we could not create the parser, we have to close the input
            // stream. Otherwise, the input stream is going to be closed when
            // the parser is closed.
            input_stream.close();
            throw e;
        }
    }

    /**
     * Sends a request for samples to the archive server and returns an
     * iterator providing the samples.
     *
     * @param name
     *  channel name in the archive.
     * @param start
     *  beginning of the time period for which samples shall be retrieved.
     * @param end
     *  end of the time period for which samples shall be retrieved.
     * @param count
     *  approximate number of samples that shall be retrieved. If
     *  <code>null</code> raw samples shall be retrieved.
     * @return
     *  iterator iterating over the samples for the specified time period in
     *  ascending order by time.
     * @throws IOException
     *  if there is an error while requesting the samples. If an error occurs
     *  later, while using the iterator, no exception is thrown and the
     *  iterator’s hasNext() method simply returns <code>false</code>.
     * @throws UnknownChannelException
     *  if the specified channel is not present in the archive.
     */
    private JsonValueIterator getValues(
            final String name,
            final Instant start,
            final Instant end,
            final Integer count)
            throws IOException, UnknownChannelException {
        // Construct the request URL.
        final var sb = new StringBuilder();
        sb.append("/");
        sb.append(key);
        sb.append("/samples/");
        sb.append(URLEncoder.encode(name, StandardCharsets.UTF_8));
        sb.append("?start=");
        sb.append(timestampToBigInteger(start));
        sb.append("&end=");
        sb.append(timestampToBigInteger(end));
        if (count != null) {
            sb.append("&count=");
            sb.append(count);
        }
        final var request_url = sb.toString();
        // Send the request and create the JSON parser for the response.
        final JsonParser parser;
        try {
            parser = doGetJson(request_url);
        } catch (FileNotFoundException e) {
            throw new UnknownChannelException(name);
        }
        // Before creating the iterator, we have to advance the parser to the
        // first token.
        try {
            parser.nextToken();
        } catch (IOException | RuntimeException e) {
            parser.close();
            throw e;
        }
        // Prepare the cleanup action. This action is executed when the
        // iterator is closed or garbage collected.
        final Runnable iterator_cleanup_action = () -> {
            try {
                parser.close();
            } catch (IOException e) {
                // We ignore an exception that happens on cleanup.
            }
        };
        // Create an iterator based on the JSON parser.
        try {
            final var iterator = new JsonValueIterator(
                    parser,
                    this::unregisterValueIterator,
                    request_url,
                    preferences.honor_zero_precision());
            // We register the iterator. This has two purposes: First, we have to
            // be able to call its cancel() method. Second, we need to close the
            // parser when the iterator is closed or garbage collected. We do
            // not register the iterator if it has no more elements. In this
            // case, it might already be closed (and if it is not, we close it
            // now), so we do not have run any cleanup actions either and if we
            // registered it, it would never be unregistered because it is
            // already closed.
            if (iterator.hasNext()) {
                registerValueIterator(iterator, iterator_cleanup_action);
            } else {
                // The iterator should already be closed, but calling the
                // close() method anyway does not hurt.
                iterator.close();
            }
            return iterator;
        } catch (IOException | RuntimeException e) {
            // If we cannot create the iterator, we have to close the parser
            // now. First, it is not going to be used for anything else.
            // Second, the iterator does not exist, so it will not be closed
            // when the iterator is closed.
            parser.close();
            throw e;
        }
    }

    /**
     * Registers a value iterator with this reader. This method is only
     * intended for use by the {@link JsonValueIterator} constructor.
     *
     * @param iterator
     *  iterator that is calling this method.
     * @param cleanup_action
     *  cleanup action that shall be run when the iterator is garbage
     *  collected or when {@link #unregisterValueIterator(JsonValueIterator)}
     *  is called for the iterator.
     */
    private void registerValueIterator(
            JsonValueIterator iterator, Runnable cleanup_action) {
        // If the iterator has not been closed properly, we have to ensure that
        // we close the JSON parser and input stream. Usually, this will happen
        // when unregisterValueIterator is called, which is called by the
        // iterator’s close method. However, if close is never called for some
        // reason, registering the cleanup action ensures that the external
        // resources are freed. We cannot explicitly remove the iterator from
        // our iterators map in this case, but this is not a problem because
        // the WeakHashMap will automatically remove entries when the key is
        // garbage collected.
        final var cleanable = cleaner.register(iterator, cleanup_action);
        synchronized (iterators) {
            iterators.put(iterator, cleanable);
        }
    }

    /**
     * Retrieves the archive description from the archive server. If the
     * description cannot be received, a warning is logged and a generic
     * description is returned.
     *
     * @return
     *  the description for the archive specified by the URL and archive key or
     *  a generic description if the archive information cannot be retrieved
     *  from the server.
     * @throws IllegalArgumentException
     *  if the server sends valid archive information, but it does not contain
     *  any information for the specified archive key.
     */
    private String retrieveArchiveDescription() {
        try (final var parser = doGetJson("/")) {
            // We have to advance to the first token before calling
            // readArchiveInfos(…).
            parser.nextToken();
            final var archive_infos = JsonArchiveInfoReader
                    .readArchiveInfos(parser);
            for (final var archive_info : archive_infos) {
                if (archive_info.archive_key() == key) {
                    return archive_info.archive_description();
                }
            }
            throw new IllegalArgumentException(
                    "The server at \""
                            + http_url
                            + "\" does not provide an archive with the key "
                            + key
                            + ".");
        } catch (IOException e) {
            logger.log(
                    Level.WARNING,
                    "Could not load archive information from server for URL \""
                            + http_url
                            + "\".");
            // If we cannot get the archive description, we still want to
            // initialize the archive reader. Maybe there is a temporary
            // network problem and the archive reader will work correctly
            // later. So, instead of throwing an exception, we rather use a
            // generic description instead of the one retrieved from the
            // server.
            return "Provides archive access over HTTP/JSON.";
        }
    }

    /**
     * Unregister an iterator that has previously been registered. This method
     * is called when the iterator is closed.
     *
     * @param iterator
     *  iterator that was previously registered using
     *  {@link #registerValueIterator(JsonValueIterator, Runnable)}.
     */
    private void unregisterValueIterator(JsonValueIterator iterator) {
        final Cleaner.Cleanable cleanable;
        synchronized (iterators) {
            cleanable = iterators.remove(iterator);
        }
        if (cleanable != null) {
            cleanable.clean();
        }
    }

}

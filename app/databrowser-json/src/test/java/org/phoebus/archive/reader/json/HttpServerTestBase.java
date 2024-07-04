/*******************************************************************************
 * Copyright (c) 2024 aquenos GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.phoebus.archive.reader.json;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Base class for tests that need an HTTP server.
 */
public class HttpServerTestBase {

    /**
     * Information about an HTTP request.
     *
     * @param headers request headers.
     * @param method request method.
     * @param uri request URI.
     */
    public record HttpRequest(
            Headers headers,
            String method,
            URI uri) {
    }

    private static HttpServer http_server;

    /**
     * Parse a query string, returning the individual parameters. This function
     * cannot handle query strings with duplicate parameters or parameters that
     * do not have a value.
     *
     * @param query_string query string that shall be parsed.
     * @return
     *  map mapping parameter names to their respective (decoded) values.
     * @throws IllegalArgumentException
     *  if the query string is malformed, containers value-less parameters, or
     *  contains duplicate parameters.
     */
    public static Map<String, String> parseQueryString(
            final String query_string) {

        return Arrays.stream(query_string.split("&"))
                .collect(Collectors.toMap(
                        k -> k.split("=")[0],
                        k -> URLDecoder.decode(k.split("=")[1], StandardCharsets.UTF_8)));
//
//        return Maps.transformValues(
//                Splitter
//                        .on('&')
//                        .withKeyValueSeparator('=')
//                        .split(query_string),
//                (value) -> URLDecoder.decode(value, StandardCharsets.UTF_8));
    }

    /**
     * Returns the port of the HTTP server that is started for the tests. Must
     * only be called after {@link #startHttpServer()} and before
     * {@link #stopHttpServer()}.
     *
     * @return TCP port where the HTTP server is listening.
     */
    protected static int getHttpServerPort() {
        return http_server.getAddress().getPort();
    }

    /**
     * Start the HTTP server that is needed for the tests. Must be called
     * before running the tests.
     */
    @BeforeAll
    protected static void startHttpServer() {
        try {
            http_server = HttpServer.create(
                    new InetSocketAddress(
                            InetAddress.getByName("127.0.0.1"), 0),
                    0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        http_server.start();
    }

    /**
     * Start the HTTP server that is needed for the tests. Must be called
     * before running the tests.
     */
    @AfterAll
    protected static void stopHttpServer() {
        http_server.stop(1);
        http_server = null;
    }

    /**
     * Runs a function while providing an HTTP service for the archive
     * information. This only works when the HTTP server has previously been
     * started and has not been stopped yet.
     *
     * @param archive_info_json
     *  content that is returned by the HTTP handler that serves the path
     *  <code>/archive/</code> below the base URL that is passed to
     *  <code>request_func</code>.
     * @param request_func
     *  function that is called, passing the base URL of the provided archive
     *  service.
     */
    protected static void withArchiveInfo(
            final String archive_info_json,
            final Consumer<String> request_func) {
        final HttpHandler info_handler = (http_exchange) -> {
            if (!http_exchange.getRequestURI().getPath().equals("/archive/")) {
                http_exchange.sendResponseHeaders(404, -1);
                return;
            }
            http_exchange.getResponseHeaders().add(
                    "Content-Type", "application/json;charset=UTF-8");
            http_exchange.sendResponseHeaders(200, 0);
            try (final var writer = new OutputStreamWriter(
                    http_exchange.getResponseBody(), StandardCharsets.UTF_8)) {
                writer.write(archive_info_json);
            }
        };
        final var info_context = http_server.createContext(
                "/archive", info_handler);
        try {
            request_func.accept("http://127.0.0.1:" + getHttpServerPort());
        } finally {
            http_server.removeContext(info_context);
        }
    }

    /**
     * Runs a function while providing an HTTP service providing archived
     * samples. This only works when the HTTP server has previously been
     * started and has not been stopped yet. In addition to providing samples,
     * this function also provides rudimentary archive information for the
     * specified <code>archive_key</code>.
     *
     * @param archive_key
     *  numerical key that identifies the archive that is provided.
     * @param channel_name
     *  channel name for which samples are provided.
     * @param samples_json
     *  content that is returned by the HTTP handler that serves the path
     *  <code>/archive/&lt;archive_key&gt;/samples/&lt;channel_name&gt;</code>
     *  below the base URL that is passed to the
     * @param request_func
     *  function that is called, passing the base URL of the provided archive
     *  service.
     * @return
     *  list with information about the requests that were made to the samples
     *  service. Requests to the archive-info service are not included.
     */
    protected static List<HttpRequest> withSamples(
            final int archive_key,
            final String channel_name,
            final String samples_json,
            final Consumer<String> request_func) {
        final LinkedList<HttpRequest> http_requests = new LinkedList<>();
        final HttpHandler samples_handler = (http_exchange) -> {
            http_requests.add(new HttpRequest(
                    http_exchange.getRequestHeaders(),
                    http_exchange.getRequestMethod(),
                    http_exchange.getRequestURI()));
            http_exchange.getResponseHeaders().add(
                    "Content-Type", "application/json;charset=UTF-8");
            http_exchange.sendResponseHeaders(200, 0);
            try (final var writer = new OutputStreamWriter(
                    http_exchange.getResponseBody(), StandardCharsets.UTF_8)) {
                writer.write(samples_json);
            }
        };
        final var samples_path =
                "/archive/" + archive_key + "/samples/" + channel_name;
        final var samples_context = http_server.createContext(
                samples_path, samples_handler);
        final var archive_info_json =
                "[{\"key\":"
                        + archive_key
                        + ", \"name\": \"Test\""
                        + ", \"description\":\"Test description\"}]";
        // We also provide some rudimentary archive information in order to
        // avoid a warning being logged when creating the JsonArchiveReader.
        withArchiveInfo(archive_info_json, (base_url) -> {
            try {
                request_func.accept(base_url);
            } finally {
                http_server.removeContext(samples_context);
            }
        });
        return http_requests;
    }

}

package org.phoebus.framework.util;

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A utility class for parsing user defined resources
 *
 * @author Kunal Shroff
 *
 */
@SuppressWarnings("nls")
public class ResourceParser {

    /** Resource query key for PV names */
    public static final String PV_ARG = "pv";

    /** Resource query key for file names */
    public static final String FILE_ARG = "file";

    /**
     * Creates a {@link URL} for the user entered resource path.
     *
     * @param resourcePath
     * @return A URL representing the a resource location
     */
    public static URL createResourceURL(String resourcePath) {
        URI uri = URI.create(resourcePath);
        try {
            if (uri.getScheme() != null) {
                return uri.toURL();
            } else {
                // Treat it like a file resource
                //
                // By default the following is used by the default Paths.get(...) which should
                // be the directory from which the program was executed
                // FileSystem fs = FileSystems.getDefault();
                // System.out.println(fs.toString());
                Path fileResource = Paths.get(resourcePath);
                return fileResource.toUri().toURL();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Create a URI representing the user entered app launch string
     *
     * @param appLaunch
     * @return A URI representing the app and its startup arguments.
     */
    public static URI createAppURI(String resourcePath) {
        URI uri = URI.create(resourcePath);
        return uri;
    }

    /**
     * Get file for URL
     *
     * @param url {@link URL}
     * @return {@link File} if URL represents a file, otherwise <code>null</code>
     */
    public static File getFile(final URL url) throws Exception {
        if (url == null || !url.getProtocol().equals("file"))
            return null;
        return new File(url.toURI());
    }

    /**
     * @param file {@link File}
     * @return {@link URL} for that file
     */
    public static URL getURL(final File file) {
        return createResourceURL(file.getAbsolutePath());
    }

    /**
     * Parse the app name from the given resource string
     *
     * @return String app name
     */
    public static String parseAppName(String resource) {
        return parseAppName(createAppURI(resource));
    }

    /**
     * Parse the app name from the given resource URL
     *
     * @return String app name
     */
    public static String parseAppName(URI resource) {
        return resource.getPath();
    }

    /**
     * Parse the query segment of a URI and return a {@link Map}
     *
     * @param uri resource path URI
     * @return {@link Map} a map of all the query parameters
     */
    public static Map<String, List<String>> parseQueryArgs(URI uri) {
        if (uri.getQuery() == null || uri.getQuery().isEmpty()) {
            return Collections.emptyMap();
        }
        return Arrays.stream(uri.getQuery().split("&")).map(ResourceParser::splitQueryParameter).collect(Collectors
                .groupingBy(SimpleImmutableEntry::getKey, LinkedHashMap::new, mapping(Map.Entry::getValue, toList())));
    }

    /**
     * Parse the query segment of a URL and return a {@link Map}
     *
     * @param url resource path URL
     * @return {@link Map} a map of all the query parameters
     */
    public static Map<String, List<String>> parseQueryArgs(URL url) {
        if (url.getQuery() == null || url.getQuery().isEmpty()) {
            return Collections.emptyMap();
        }
        return Arrays.stream(url.getQuery().split("&")).map(ResourceParser::splitQueryParameter).collect(Collectors
                .groupingBy(SimpleImmutableEntry::getKey, LinkedHashMap::new, mapping(Map.Entry::getValue, toList())));
    }

    private static SimpleImmutableEntry<String, String> splitQueryParameter(String it) {
        final int idx = it.indexOf("=");
        final String key = idx > 0 ? it.substring(0, idx) : it;
        final String value = idx > 0 && it.length() > idx + 1 ? it.substring(idx + 1) : null;
        return new SimpleImmutableEntry<>(key, value);
    }
}

package org.phoebus.framework.util;

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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.mapping;

/**
 * A utility class for parsing user defined resources
 *
 * @author Kunal Shroff
 *
 */
@SuppressWarnings("nls")
public class ResourcePathParser {

    /**
     * Validates the user entered resource path and returns a {@link URL}
     *
     *
     * @param resourcePath
     * @return
     */
    public static URL createValidURL(String resourcePath) {
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
     * Get file for URL
     *
     * @param url
     *            {@link URL}
     * @return {@link File} if URL represents a file, otherwise <code>null</code>
     */
    public static File getFile(final URL url) throws Exception {
        if (url == null || !url.getProtocol().equals("file"))
            return null;
        return new File(url.toURI());
    }

    /**
     * @param file
     *            {@link File}
     * @return {@link URL} for that file
     */
    public static URL getURL(final File file) {
        return createValidURL(file.getAbsolutePath());
    }

    /**
     * 
     * @param url
     * @return
     */
    public static Map<String, List<String>> splitQuery(URL url) {
        if (url.getQuery() == null || url.getQuery().isEmpty()) {
            return Collections.emptyMap();
        }
        return Arrays.stream(url.getQuery().split("&"))
                .map(ResourcePathParser::splitQueryParameter)
                .collect(Collectors.groupingBy
                        (SimpleImmutableEntry::getKey, LinkedHashMap::new, mapping(Map.Entry::getValue, toList())));
    }

    private static SimpleImmutableEntry<String, String> splitQueryParameter(String it) {
        final int idx = it.indexOf("=");
        final String key = idx > 0 ? it.substring(0, idx) : it;
        final String value = idx > 0 && it.length() > idx + 1 ? it.substring(idx + 1) : null;
        return new SimpleImmutableEntry<>(key, value);
    }
}

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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A utility class for parsing user defined resources
 *
 * @author Kunal Shroff
 * @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ResourceParser {
    /** URI schema for PV names */
    public static final String PV_SCHEMA = "pv";

    /** URI query tag used to specify the application name */
    private static final String APP_QUERY_TAG = "app=";


    /** Create URI for a resource
     *
     *  <p>Takes the resource passed by the user,
     *  typically on the command line,
     *  and turns it into a URI.
     *
     *  <p>Supported are URI strings
     *  like "http:/..." or "file:...".
     *  Plain strings that do not start with a 'schema'
     *  are assumed to be files,
     *  and are resolved as a path in the local file system.
     *  Relative paths are resolved relative to the
     *  "current working directory",
     *  which may not be known when the application is running,
     *  so that it generally a bad idea.
     *
     *  <p>The file must NOT exist, the "file://.." URI
     *  will be created even if the file does not exist,
     *  because the caller may use the URI to then create
     *  the resource.
     *
     *  @param resource Resource text from user
     *  @return URI for the resource
     *  @throws Exception on error
     */
    public static URI createResourceURI(final String resource) throws Exception
    {
        try
        {
            final URI uri = URI.create(resource);
            // Received complete URL, starting with scheme?
            if (uri.getScheme() != null)
                return uri;
            else
            {
                // Treat it like a file resource
                //
                // By default the following is used by the default Paths.get(...) which should
                // be the directory from which the program was executed
                // FileSystem fs = FileSystems.getDefault();
                // System.out.println(fs.toString());
                final Path fileResource = Paths.get(resource);
                return fileResource.toUri();
            }
        }
        catch (Throwable ex)
        {
            throw new Exception("Cannot create URI for '" + resource + "'", ex);
        }
    }


    /** Get file for URI
     *  @return URI for the resource
     *  @return {@link File} if URL represents a file, otherwise <code>null</code>
     */
    public static File getFile(final URI resource) throws Exception
    {
        if (resource == null  ||  !resource.getScheme().equals("file"))
            return null;
        return new File(resource);
    }

    /** Get URI for file
     *  @param file {@link File}
     *  @return {@link URI} for that file
     */
    public static URI getURI(final File file)
    {
        return file.toURI();
    }

    /** Get list of PVs from a "pv://?PV1&PV2" type URL
     *
     *  <p>If the URI is simply no "pv:" URI, an empty list
     *  if returned.
     *  If it is a "pv:" URI, at least one PV is expected,
     *  otherwise an exception is thrown.
     *
     *  @param resource "pv://?PV1&PV2" type URL
     *  @return List of PVs parsed from the resource
     *  @throws Exception on error, including no PVs
     */
    public static List<String> parsePVs(final URI resource) throws Exception
    {
        if (! resource.getScheme().equals(PV_SCHEMA))
            return List.of();
        final List<String> pvs = getQueryStream(resource).filter(pv -> !pv.startsWith(APP_QUERY_TAG))
                                                         .collect(Collectors.toList());
        if (pvs.isEmpty())
            throw new Exception("No PVs found in '" + resource + "'");
        return pvs;
    }

    /** Get application name hint from resource
     *  @param resource URI that might contain "?...app=the_app_name"
     *  @return "the_app_name" or empty result.
     */
    public static Optional<String> getAppName(final URI resource)
    {
        return getQueryStream(resource).filter(q -> q.startsWith(APP_QUERY_TAG))
                                       .map(app_name -> app_name.substring(APP_QUERY_TAG.length()))
                                       .findFirst();
    }

    /** @param resource URI for resource
     *  @return Stream for all query items
     */
    private static Stream<String> getQueryStream(final URI resource)
    {
        final String query = resource.getQuery();
        if (query == null)
            return Stream.empty();
        return Arrays.stream(query.split("&"));
    }

    // ------------------------------------


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
     * Get file for URLSN=220416
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

    private static SimpleImmutableEntry<String, String> splitQueryParameter(String it) {
        final int idx = it.indexOf("=");
        final String key = idx > 0 ? it.substring(0, idx) : it;
        final String value = idx > 0 && it.length() > idx + 1 ? it.substring(idx + 1) : null;
        return new SimpleImmutableEntry<>(key, value);
    }
}

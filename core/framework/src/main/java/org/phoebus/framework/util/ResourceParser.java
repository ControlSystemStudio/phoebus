package org.phoebus.framework.util;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A utility class for parsing user defined resources
 *
 * @author Kunal Shroff
 * @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ResourceParser
{
    private static final String UTF_8 = "UTF-8";

    /** URI schema for PV names */
    public static final String PV_SCHEMA = "pv";

    /** URI query tag used to specify the application name */
    private static final String APP_QUERY_TAG = "app=";

    /** URI query tag used to specify the destination pane */
    private static final String TARGET_QUERY_TAG = "target=";


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
            try
            {
                return new File(resource).toURI();
            }
            catch (Exception file_ex)
            {
                throw new Exception("Cannot create URI for '" + resource + "'", ex);
            }
        }
    }

    /** Get file for URI
     *  @return URI for the resource
     *  @return {@link File} if URI represents a file, otherwise <code>null</code>
     */
    public static File getFile(final URI resource)
    {
        if (resource == null  ||  !resource.getScheme().equals("file"))
            return null;
        // URI might be file:/some/path/file.plt?MACRO1=Value1
        // Create file for just the path, not the query params.
        return new File(resource.getPath());
    }

    /** Get URI for file
     *  @param file {@link File}
     *  @return {@link URI} for that file
     */
    public static URI getURI(final File file)
    {
        return file.toURI();
    }

    /** Open a resource that can be read (file, web link)
     *  @param resource URI for a resource
     *  @return {@link InputStream} for the content of the resource
     *  @throws Exception on error: Not a URI that can be read
     */
    public static InputStream getContent(final URI resource) throws Exception
    {
        final URL url = resource.toURL();
        return url.openStream();
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
        final String scheme = resource.getScheme();
        if (! PV_SCHEMA.equals(scheme))
            return List.of();
        final List<String> pvs = getQueryStream(resource).filter(pv -> !pv.startsWith(APP_QUERY_TAG))
                                                         .filter(pv -> !pv.startsWith(TARGET_QUERY_TAG))
                                                         .collect(Collectors.toList());
        if (pvs.isEmpty())
            throw new Exception("No PVs found in '" + resource + "'");
        return pvs;
    }

    /** Get application name hint from resource
     *  @param resource URI that might contain "?...app=the_app_name"
     *  @return "the_app_name" or <code>null</code>
     */
    public static String getAppName(final URI resource)
    {
        return getQueryStream(resource).filter(q -> q.startsWith(APP_QUERY_TAG))
                                       .map(app_name -> app_name.substring(APP_QUERY_TAG.length()))
                                       .findFirst()
                                       .orElse(null);
    }

    /** Get target name hint from resource
     *  @param resource URI that might contain "?...target=the_pane_name"
     *  @return "the_pane_name" or <code>null</code>
     */
    public static String getTargetName(final URI resource)
    {
        return getQueryStream(resource).filter(q -> q.startsWith(TARGET_QUERY_TAG))
                                       .map(app_name -> app_name.substring(TARGET_QUERY_TAG.length()))
                                       .findFirst()
                                       .orElse(null);
    }

    /** Get stream of query items
     *
     *  <p>Filters the "app=.." and "target=.." items,
     *  passing only the remaining query items
     *
     *  @param resource Resource with optional query
     *  @return Stream of (name, null) or (name, value) entries
     */
    public static Stream<Map.Entry<String, String>> getQueryItemStream(final URI resource)
    {
        return getQueryStream(resource).filter(pv -> !pv.startsWith(APP_QUERY_TAG))
                                       .filter(pv -> !pv.startsWith(TARGET_QUERY_TAG))
                                       .map(ResourceParser::splitQueryParameter);
    }

    /** Get map of query items
    *
    *  <p>Filters the "app=.." and "target=.." items,
    *  passing only the remaining query items
    *
    *  @param resource Resource with optional query
    *  @return Map of query item names to list of values
    */
   public static Map<String, List<String>> parseQueryArgs(final URI resource)
   {
       return getQueryItemStream(resource)
               .collect(Collectors.groupingBy(Map.Entry::getKey,
                                              LinkedHashMap::new,
                                              Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
   }

    /** @param resource URI for resource
     *  @return Stream for all query items
     */
    private static Stream<String> getQueryStream(final URI resource)
    {
        final String query = resource.getRawQuery();
        if (query == null)
            return Stream.empty();
        return Arrays.stream(query.split("&"));
    }

    /** @param item Query item "name" or "name=value"
     *  @return Entry with (name, null) or (name, value)
     */
    private static Map.Entry<String, String> splitQueryParameter(final String item)
    {
        final int idx = item.indexOf("=");
        final String key = idx > 0 ? item.substring(0, idx) : item;
        final String value = idx > 0 && item.length() > idx + 1 ? item.substring(idx + 1) : "";
        return new SimpleImmutableEntry<String, String>(decode(key), decode(value));
    }

    /** Decode URI text
     *  @param text Text that may contain '+' or '%20' etc.
     *  @return Decoded text
     */
    private static String decode(final String text)
    {
        if (text == null)
            return null;
        try
        {
            return URLDecoder.decode(text, UTF_8);
        }
        catch (Exception ex)
        {
            Logger.getLogger(ResourceParser.class.getPackageName())
                  .log(Level.WARNING, "Error decoding '" + text + "'", ex);
            return text;
        }
    }

    /** Encode string to URI format
     *  @param text string
     *  @return Encoded URI formatted string
     */
    public static String encode(final String text)
    {
        if (text == null)
            return null;
        try
        {
            return URLEncoder.encode(text, UTF_8);
        }
        catch (Exception ex)
        {
            Logger.getLogger(ResourceParser.class.getPackageName())
                  .log(Level.WARNING, "Error encoding '" + text + "'", ex);
            return text;
        }
    }
}

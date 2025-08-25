/*******************************************************************************
 * Copyright (c) 2015-2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.util;

import static org.csstudio.display.builder.model.ModelPlugin.logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.ModelPlugin;
import org.csstudio.display.builder.model.Preferences;
import org.phoebus.framework.util.IOUtils;
import org.phoebus.framework.util.ResourceParser;

/** Helper for handling resources: File, web link.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ModelResourceUtil
{
    /** Schema used for the built-in display examples */
    public static final String EXAMPLES_SCHEMA = "examples";


    /** Cache for content read from a URL */
    private static final Cache<byte[]> url_cache = new Cache<>(Duration.ofSeconds(Preferences.cache_timeout));

    private static int timeout_ms = Preferences.read_timeout;

    // Many basic String operations since paths
    // may include " ", which URL won't handle,
    // or start with "https://", which File won't handle.

    private static boolean isURL(final String path)
    {
        return path.startsWith("http://")  ||
               path.startsWith("https://") ||
               path.startsWith("ftp://");
    }

    private static boolean isAbsolute(final String path)
    {
        return new File(path).isAbsolute() ||
               isURL(path);
    }

    /** Creates a relative path between two file path strings.
     *
     *  @param parent Parent file, for example "/one/of/my/directories/parent.bob"
     *  @param path Path to make relative, for example "/one/of/my/alternate_dirs/example.bob"
     *  @return Relative path, e.d. "../alternate_dirs/example.bob"
     */
    private static String relativizePaths(String parent, String path) {
        Path parentDirectory = Paths.get(parent).getParent();
        Path searchPath = Paths.get(path);
        String relativePath = parentDirectory.relativize(searchPath).toString();
        return normalize(relativePath);
    }

    /** Obtain a relative path for both filepaths or URLs. Also normalizes paths such that they always conform to unix.
     *
     *  <p>Returns original 'path' if it cannot be expressed
     *  relative to the 'parent'. (But normalized to unix)
     *  @param parent Parent file, for example "/directory/parent.bob" or "http://server/directory/common.bob"
     *  @param path Path to make relative, for example "/alternate_dirs/example.bob" or "http://server/alternate_dirs/example.bob"
     *  @return Relative path, e.g. "../alternate_dirs/example.bob"
     */
    public static String getRelativePath(final String parent, String path) {
        path = normalize(path);
        if (!isAbsolute(path)) {
            return path;
        }
        if(isURL(parent) != isURL(path)) {
            return path;
        }
        if(isURL(parent)) {
            String parentNoProtocol = parent.split("://")[1];
            String pathNoProtocol = path.split("://")[1];
            return relativizePaths(parentNoProtocol, pathNoProtocol);
        } else {
            return relativizePaths(parent, path);
        }
    }

    /** Normalize path
     *
     *  <p>Patch windows-type path with '\' into
     *  forward slashes,
     *  and collapse ".." up references.
     *
     *  @param path Path that may use Windows '\' or ".."
     *  @return Path with only '/' and up-references resolved
     */
    public static String normalize(String path)
    {
        String protocol = "";
        if(isURL(path)) {
            String[] splitPath = path.split("://");
            protocol = splitPath[0] + "://";
            path = splitPath[1];
        }

        path = path.replaceAll("\\\\(?!\\\\)", "/");

        // Collapse "something/../" into "something/"
        if(path.contains(":")){
            String[] pathsplit = path.split(":");
            String pathbefore = pathsplit[0];
            String pathafter = pathsplit[1];
            pathafter = Paths.get(pathafter).normalize().toString();
            path = pathbefore + ":" + pathafter;
        }else{
            path = Paths.get(path).normalize().toString();
        }

        // Pattern: '\(?!\)', i.e. backslash _not_ followed by another one.
        // Each \ is doubled as \\ to get one '\' into the string,
        // then doubled once more to tell regex that we want a '\'
        return protocol + path.replaceAll("\\\\(?!\\\\)", "/");
    }

    /** Obtain directory of file. For URL, this is the path up to the last element
     *
     *  <p>For a <code>null</code> path, the location will also be <code>null</code>.
     *
     *  @param path Complete path, i.e. "/some/location/resource"
     *  @return Location, i.e. "/some/location" without trailing "/", or "."
     */
    public static String getDirectory(String path)
    {
        if (path == null)
            return null;
        // Remove last segment from parent_display to get path
        path = normalize(path);
        int sep = path.lastIndexOf('/');
        if (sep >= 0)
            return path.substring(0, sep);
        return ".";
    }

    /** Obtain the local path for a resource
     *
     *  @param resource_name Resource that may be relative to workspace
     *  @return Location in local file system or <code>null</code>
     *  @deprecated There is no more "workspace", so no need to get local path
     *
     *  TODO Remove calls to getLocalPath
     */
    @Deprecated
    public static String getLocalPath(final String resource_name)
    {
        final File file = new File(resource_name);
        final File parent = file.getParentFile();
        if (parent != null  &&  parent.exists())
            return file.getAbsolutePath();

        return null;
    }

    /** Combine display paths
     *  @param parent_display Path to a 'parent' file, may be <code>null</code>
     *  @param display_path Display file. If relative, it is resolved relative to the parent display
     *  @return Combined path
     */
    public static String combineDisplayPaths(String parent_display, String display_path)
    {
        // Anything in the parent?
        if (parent_display == null  ||  parent_display.isEmpty())
            return display_path;

        display_path = normalize(display_path);

        // Is display already absolute?
        if (isAbsolute(display_path))
            return display_path;

        parent_display = normalize(parent_display);

        // Remove last segment from parent_display to get path
        String result = getDirectory(parent_display) + "/" + display_path;
        result = normalize(result);

        return result;
    }

    /** Attempt to resolve a resource relative to a display
     *
     *  <p>For *.opi files, checks if there is an updated .bob file.
     *
     *  @param model {@link DisplayModel}
     *  @param resource_name Resource path. If relative, it is resolved relative to the parent display
     *  @return Resolved file name. May also be the original name if no idea how to adjust it
     */
    public static String resolveResource(final DisplayModel model, final String resource_name)
    {
        final String parent_display = model.getUserData(DisplayModel.USER_DATA_INPUT_FILE);
        return resolveResource(parent_display, resource_name);
    }

    /** Attempt to resolve a resource relative to a display
     *
     *  <p>For *.opi files, checks if there is an updated .bob file.
     *
     *  @param parent_display Path to a 'parent' file, may be <code>null</code>
     *  @param resource_name Resource path. If relative, it is resolved relative to the parent display
     *  @return Resolved file name. May also be the original name if no idea how to adjust it
     */
    public static String resolveResource(final String parent_display, String resource_name)
    {
        logger.log(Level.FINE, "Resolving {0} relative to {1}", new Object[] { resource_name, parent_display });

        // Leave absolute file resources unchanged
        if (resource_name.startsWith("file:/"))
            return resource_name;

        // For relative file names, try to resolve
        if (resource_name.startsWith("file:"))
            resource_name = resource_name.substring(5);

        if (resource_name.endsWith("." + DisplayModel.LEGACY_FILE_EXTENSION))
        {   // Check if there is an updated file for a legacy resource
            final String updated_resource = resource_name.substring(0, resource_name.length()-3) + DisplayModel.FILE_EXTENSION;
            final String test = doResolveResource(parent_display, updated_resource, true);
            if (test != null)
            {
                logger.log(Level.FINE, "Using updated {0} instead of {1}", new Object[] { test, resource_name });
                return test;
            }
        }

        return doResolveResource(parent_display, resource_name, false);
    }

    private static String URLdecode(final String text)
    {
        try
        {
            return URLDecoder.decode(text, "UTF-8");
        }
        catch (UnsupportedEncodingException ex)
        {
            logger.log(Level.SEVERE, "Impossible to decode {0} because {1}", new Object[] { text, ex.getMessage() });
            return text;
        }
    }

    /** Attempt to resolve a resource relative to a display
     *
     *  <p>Checks for URL, including somewhat expensive access test,
     *  workspace resource,
     *  and finally plain file.
     *
     * @param parent_display
     * @param resource_name
     * @param check_if_exists whether to return null if the resource does not exist as a plain file
     * @return
     */
    private static String doResolveResource(final String parent_display, final String resource_name, boolean check_if_exists)
    {
        // Actual, existing URL?
        if (canOpenUrl(resource_name))
        {
            logger.log(Level.FINE, "Using URL {0}", resource_name);
            return resource_name;
        }

        // .. relative to parent?
        final String combined = combineDisplayPaths(parent_display, resource_name);
        if (canOpenUrl(combined))
        {
            logger.log(Level.FINE, "Using URL {0}", combined);
            return combined;
        }

        // Can display be opened as file?
        File file = new File(URLdecode(combined));
        if (check_if_exists == false || file.exists())
        {
            logger.log(Level.FINE, "Found file {0} relative to parent display", file);
            return file.getAbsolutePath();
        }

        // Give up
        logger.log(Level.WARNING, " {0} is not resolved ", new Object[] { resource_name});
        return null;
    }

    /** Check if a resource can be opened as a URL
     *  @param resource_name Path to resource, presumably "http://.."
     *  @return <code>true</code> if indeed an exiting URL
     */
    private static boolean canOpenUrl(final String resource_name)
    {
        final URL example = getExampleURL(resource_name);
        if (example != null)
        {
            try
            {
                example.openStream().close();
                return true;
            }
            catch (Exception ex)
            {
                logger.log(Level.SEVERE, "Impossible to open stream on {0} because of {1}", new Object[] { resource_name, ex.getMessage() });
                return false;
            }
        }

        if (! isURL(resource_name)) {
            return false;
        }

        final String escaped = resource_name.replace(" ", "%20");
        // Opening the URL stream to check availability seems expensive.
        // On success, we'll soon do this again to actually read.
        // But 'openURL' uses a cache, the next time around
        // we already have the content.
        // Attempts to "only check" without reading, for example
        //    new URL(...).openConnection().connect()
        // turned out to sometimes succeed even if the document doesn't exist
        // on the web server. Details might be related to the behavior
        // of site-specific intermediate proxies.
        // In any case, this seems the most reliable approach
        try (InputStream stream = openURL(escaped))
        {
            return true;
        }
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "Impossible to open connection on URL {0} because of {1}", new Object[] { resource_name, ex.getMessage() });
            return false;
        }
    }

    /** Check for "examples:.."
     *
     *  @param resource_name Path to file that may be based on "examples:.."
     *  @return URL for the example, or <code>null</code>
     */
    private static URL getExampleURL(final String resource_name)
    {
        if (resource_name.startsWith(EXAMPLES_SCHEMA + ":"))
        {
            String example = resource_name.substring(9);
            if (example.startsWith("/"))
                example = "/examples" + example;
            else
                example = "/examples/" + example;
            return ModelPlugin.class.getResource(example);
        }
        return null;
    }

    /** Open the file for a resource
     *
     *  <p>Handles "example:" and "file:" resources.
     *
     *  @param resource A resource for which there is a local file
     *  @return That {@link File}, otherwise <code>null</code>
     *  @throws Exception on error
     */
    public static File getFile(final URI resource) throws Exception
    {
        if (EXAMPLES_SCHEMA.equals(resource.getScheme()))
        {
            // While examples are in a local directory,
            // we can get the associated file
            final URL url = getExampleURL(resource.toString());
            try
            {
                return new File(url.toURI());
            }
            catch (Exception ex)
            {
                // .. but once examples are inside the jar,
                // we can only read them as a stream.
                // There is no File access.
                logger.log(Level.WARNING, "Cannot get `File` for " + url + " " + ex.getMessage());
                return null;
            }
        }
        // To get a file, strip query information,
        // because new File("file://xxxx?with_query") will throw exception
        return ResourceParser.getFile(new URI(resource.getScheme(), null, null, -1, resource.getPath(), null, null));
    }

    /** Open a file, web location, ..
     *
     *  <p>In addition, understands "examples:"
     *  to load a resource from the built-in examples.
     *
     *  @param resource_name Path to file, "examples:", "http:/.."
     *  @return {@link InputStream}
     *  @throws Exception on error
     */
    public static InputStream openResourceStream(final String resource_name) throws Exception
    {
//        {   // Artificial delay to simulate slow file access (1..5 seconds)
//            final long milli = Math.round(1000 + Math.random()*4000);
//            Thread.sleep(milli);
//        }
        if (resource_name.startsWith("http") || resource_name.startsWith("file:/"))
            return openURL(resource_name);

        // Handle legacy RCP URL
        // "platform:/plugin/name.of.plugin/path/within/plugin"
        if (resource_name.startsWith("platform:/plugin/"))
        {
            // Get path after plugin name
            String path = resource_name.substring("platform:/plugin/".length());
            final int sep = path.indexOf('/');
            if (sep > 0)
            {
                path = path.substring(sep);

                // There are no OSGi plugins.
                // As long as the phoebus components are jar files,
                // all merged into one large class path,
                // there is a good chance that this class loader can open the path.
                // If phoebus components are modules, we'll need
                // "requires name.of.other.module;
                // to access resources in other modules.
                final InputStream stream = ModelResourceUtil.class.getResourceAsStream(path);
                if (stream != null)
                    return stream;
                // Didn't work, so run into the FileInputStream exception which will
                // generate a stack trace that includes the resource name.
            }
        }

        final URL example = getExampleURL(resource_name);
        if (example != null)
        {
            try
            {
                return example.openStream();
            }
            catch (Exception ex)
            {
                throw new Exception("Cannot open example: '" + example + "'", ex);
            }
        }

        return new FileInputStream(resource_name);
    }

    /** Clear cached URLs
     *
     *  <p>Call to force a re-load right now without waiting for cache to expire
     */
    public static void clearURLCache()
    {
        url_cache.clear();
    }

    /** Open URL for "http", "https", "ftp", ..
     *  @param resource_name URL specification
     *  @return {@link InputStream}
     *  @throws Exception on error
     */
    public static InputStream openURL(final String resource_name) throws Exception
    {
        final byte[] content = url_cache.getCachedOrNew(resource_name, ModelResourceUtil::readUrl);
        return new ByteArrayInputStream(content);
    }

    private static final byte[] readUrl(final String url) throws Exception
    {
        final InputStream in = openURL(url, timeout_ms);
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        IOUtils.copy(in, buf);
        return buf.toByteArray();
    }

    /** Open URL for "http", "https", "ftp", ..
     *  @param resource_name URL specification
     *  @param timeout_ms Read timeout [milliseconds]
     *  @return {@link InputStream}
     *  @throws Exception on error
     */
    public static InputStream openURL(final String resource_name, final int timeout_ms) throws Exception
    {
        // Received name may be the result of resolving "some file.png"
        // relative to "http://server/displays/main.bob" as a string,
        // i.e. "http://server/displays/some file.png"
        // This would be acceptable for a file path, but URL() expects spaces to be escaped
        final String escaped = resource_name.replace(" ", "%20");
        return ResourceParser.getContent(new URL(escaped).toURI(), timeout_ms);
    }

    /** Write a resource.
     *
     *  <p>With RCP support, this resource is created or updated in the workspace.
     *  Otherwise, in the local file system.
     *
     *  @param resource_name Name of resource
     *  @return Stream for the resource
     *  @throws Exception on error
     */
    public static OutputStream writeResource(final String resource_name) throws Exception
    {
        return new FileOutputStream(resource_name);
    }

}

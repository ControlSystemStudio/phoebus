package org.phoebus.framework.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

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
     * @param url {@link URL}
     * @return {@link File} if URL represents a file, otherwise <code>null</code>
     */
    public static File getFile(final URL url) throws Exception
    {
        if (url == null  ||  !url.getProtocol().equals("file"))
            return null;
        return new File(url.toURI());
    }

    /**
     * @param file {@link File}
     * @return {@link URL} for that file
     */
    public static URL getURL(final File file)
    {
        return createValidURL(file.getAbsolutePath());
    }
}

package org.phoebus.framework.util;

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
}

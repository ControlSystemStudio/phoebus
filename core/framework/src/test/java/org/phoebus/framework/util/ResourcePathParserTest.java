package org.phoebus.framework.util;

import static org.phoebus.framework.util.ResourcePathParser.createValidURL;
import static org.junit.Assert.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;

/**
 * A utility class for parsing user defined resources
 * 
 * @author Kunal Shroff
 *
 */
public class ResourcePathParserTest {

    @Test
    public void checkResourcePathUtil() {
        // first check a valid http url
        String resource = "http://localhost:80";
        URL url = createValidURL(resource);
        try {
            assertEquals("failed to parse http url:", new URL(resource), url);
        } catch (MalformedURLException e) {
            fail(e.getLocalizedMessage());
            e.printStackTrace();
        }

        // Check a relative path
        resource = "src/test/resources/test.plt";
        url = createValidURL(resource);
        assertEquals("Failed to parse relative file path: ", url.getProtocol(), "file");
        assertTrue("Failed to parse relative file path: ", new File(url.getPath()).exists());

    }
}

package org.phoebus.framework.util;

import static org.phoebus.framework.util.ResourceParser.*;
import static org.junit.Assert.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * A utility class for parsing user defined resources
 * 
 * @author Kunal Shroff
 *
 */
public class ResourceParserTest {

    @Test
    public void checkResourcePathUtil() {
        // first check a valid http url
        String resource = "http://localhost:80";
        URL url = createResourceURL(resource);
        try {
            assertEquals("failed to parse http url:", new URL(resource), url);
        } catch (MalformedURLException e) {
            fail(e.getLocalizedMessage());
            e.printStackTrace();
        }

        // Check a relative path
        resource = "src/test/resources/test.plt";
        url = createResourceURL(resource);
        assertEquals("Failed to parse relative file path: ", url.getProtocol(), "file");
        assertTrue("Failed to parse relative file path: ", new File(url.getPath()).exists());

    }

    @Test
    public void checkAppNameParsing() {
        // Only the app name is provided
        String resourceURI = "probe";
        assertEquals("Failed to parse app launcher string expected: probe returned: " + parseAppName(resourceURI),
                "probe", parseAppName(resourceURI));

        // the app name and arguments are provided
        resourceURI = "probe?pv=sim://noise";

        assertEquals("Failed to parse app launcher string expected: probe returned: " + parseAppName(resourceURI),
                "probe", parseAppName(resourceURI));
        Map<String, List<String>> queryArgs = new HashMap<>();
        queryArgs.put("pv", Arrays.asList("sim://noise"));
        assertEquals(
                "Failed to parse app launcher string expected: " + queryArgs + " returned: "
                        + parseQueryArgs(createAppURI(resourceURI)),
                queryArgs, parseQueryArgs(createAppURI(resourceURI)));
        
        // the app name and arguments are provided
        resourceURI = "probe?pv=sim://noise&pv=sim://sine";
        queryArgs = new HashMap<>();
        queryArgs.put("pv", Arrays.asList("sim://noise","sim://sine"));
        assertEquals(
                "Failed to parse app launcher string expected: " + queryArgs + " returned: "
                        + parseQueryArgs(createAppURI(resourceURI)),
                queryArgs, parseQueryArgs(createAppURI(resourceURI)));
        
    }

}

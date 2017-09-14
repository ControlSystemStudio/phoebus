/**
 * 
 */
package org.phoebus.framework.workbench;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.util.ResourcePathParser;

/**
 * @author Kunal Shroff
 *
 */
public class ResourceHandlerService {

    private static final Logger log = Logger.getLogger(ResourceHandlerService.class.getCanonicalName());

    @SuppressWarnings("unused")
    private static ResourceHandlerService resourceHandlerService = new ResourceHandlerService();

    private ServiceLoader<AppResourceDescriptor> loader;

    private static Map<String, List<AppResourceDescriptor>> resourceMap;

    private ResourceHandlerService() {
        loader = ServiceLoader.load(AppResourceDescriptor.class);
        resourceMap = new HashMap<String, List<AppResourceDescriptor>>();
        for (AppResourceDescriptor appResourceDescriptor : loader.stream().map(Provider::get)
                .collect(Collectors.toList())) {
            appResourceDescriptor.start();
            for (String ext : appResourceDescriptor.supportedFileExtentions()) {
                if (!resourceMap.containsKey(ext)) {
                    resourceMap.put(ext, new ArrayList<AppResourceDescriptor>());
                }
                resourceMap.get(ext).add(appResourceDescriptor);
            }
        }
    }

    /**
     * Find applications for a resource
     * 
     * @param resource
     *            String resource
     * @return List of Applications that can open this resource
     */
    public static List<AppResourceDescriptor> getApplications(String resource) {
        return getApplications(ResourcePathParser.createValidURL(resource));
    }

    /**
     * Find applications for a resource
     * 
     * @param resource
     *            URI to resource
     * @return List of Applications that can open this resource
     */
    public static List<AppResourceDescriptor> getApplications(URL resource) {
        String path = resource.getPath();
        String ext = path.substring(path.lastIndexOf(".") + 1);
        if (resourceMap.containsKey(ext)) {
            return resourceMap.get(ext);
        } else {
            return Collections.emptyList();
        }
    }

}

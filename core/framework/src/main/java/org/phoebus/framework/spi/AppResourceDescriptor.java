package org.phoebus.framework.spi;

import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * The {@link AppResourceDescriptor} extends the {@link AppDescriptor} and
 * defines an interface needed to create Phoebus applications that can handle an
 * input resrouces (like a url,or it can be a list of pv names, or a
 * channelfinder query)
 *
 * <p>
 * The <code>open..</code> methods are called to create running instances of the
 * application.
 *
 * @author Kunal Shroff
 *
 */
public interface AppResourceDescriptor extends AppDescriptor {

    /**
     *
     * @return A list of supported file extensions (without 'dot')
     */
    public default List<String> supportedFileExtentions(){
        return Collections.emptyList();
    }

    /**
     * TODO
     *
     * Called to check if application can handle a resource.
     *
     * <p>
     * The application can implement more concrete checks on which resources can and
     * cannot be handled by this application. This could include checks on the
     * version numbers the resource was created with and the version of the current
     * applications.
     *
     * <p>
     * If the application indicates that it can handle a resource, the framework
     * will then invoke <code>open(resource)</code>.
     *
     * @param resource Resource to check
     * @return <code>true</code> if this application can open the resource
     * @deprecated Not used. Remove.
     */
    @Deprecated
    public default boolean canOpenResource(String resource) {
        return true;
    }

    /**
     * Create the application using the list of resources, the resource can be the
     * path or url to a configuration file like .bob or .plt or it can be a list of
     * pv names, or a channelfinder query
     */
    public AppInstance create(URI resource);

}

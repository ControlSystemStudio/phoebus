package org.phoebus.framework.spi;

import java.util.Map;

import org.phoebus.framework.selection.SelectionService;

/**
 * Basic interface for defining phoebus applications via java services
 *
 * <p>The framework creates one instance for each application.
 * The <code>start()</code> and <code>stop()</code>
 * methods allow an application to manage global resources.
 *
 * <p>The <code>open..</code> methods are called to create running
 * instances of the application.
 *
 * @author Kunal Shroff
 *
 */
public interface Application {

	/**
	 * Get the application name
	 *
	 * @return the name of the application
	 */
	public String getName();

	/**
	 * Create the resources (connects, load libraries,...) required by this
	 * particular application
	 */
	public default void start()
	{
        // Default does nothing
	}

	/**
	 * Called to check if application can handle a resource.
	 *
	 * <p>Implementation may check the file extension,
	 * or even open the file to check content.
	 *
	 * <p>If the application indicates that it can handle a resource,
	 * the framework will then invoke <code>open(resource)</code>.
	 *
	 * @param resource Resource to check
	 * @return <code>true</code> if this application can open the resource
	 */
	public default boolean canOpenResource(String resource)
	{
	    return false;
	}

	/**
	 * Open the application without any specific resources
	 */
	public void open();

	/**
	 * Open the application using the list of resources, the resource can be the
	 * path or url to a configuration file like .bob or .plt or it can be a list of
	 * pv names, or a channelfinder query
	 */
	default public void open(String... resources) {
		open();
	}

	/**
	 * Open the application using the a map of input parameters and a list of resources, the resource can be the
	 * path or url to a configuration file like .bob or .plt or it can be a list of
	 * pv names, or a channelfinder query
	 */
	default public void open(Map<String, String> inputParameters, String... resources) {
		open();
	}

	// TODO Remove
	// ContextMenuEntry already handles it,
	// AND ContextMenuEntry knows what type of selection it supports.
	// Would basically have to add all of ContextMenuEntry to Application.
	/**
	 * Open the application with the current selection. The selection should be
	 * recovered from the {@link SelectionService}
	 */
	default public void openWithSelection() {
		open();
	}

	/**
	 * Cleanup the resources used by this application, also perform the action of
	 * storing application state
	 */
	public default void stop()
	{
	       // Default does nothing

	}
}

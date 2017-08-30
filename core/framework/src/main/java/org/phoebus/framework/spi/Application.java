package org.phoebus.framework.spi;

import java.util.Map;

import org.phoebus.framework.selection.SelectionService;

/**
 * Basic interface for defining phoebus applications via java services
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
	public void start();

	/**
	 * Open the application
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
	public void stop();

}

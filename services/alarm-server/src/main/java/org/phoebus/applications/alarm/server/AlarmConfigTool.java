/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.client.AlarmConfigMonitor;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.xml.XmlModelReader;
import org.phoebus.applications.alarm.model.xml.XmlModelWriter;
import org.phoebus.util.time.SecondsParser;

/**
 * Writes Alarm System model to XML.
 * @author Evan Smith
 *
 */
@SuppressWarnings("nls")
public class AlarmConfigTool
{
    /** Time the model must be stable for. Unit is seconds. Default is 4 seconds. */
    private static final long STABILIZATION_SECS = 4;

    	// Export an alarm system model to an xml file.
    public void exportModel(String filename, String server, String config) throws Exception
	{
        final XmlModelWriter xmlWriter;

        // Write to stdout or to file.
        if (filename.equals("stdout"))
            xmlWriter = new XmlModelWriter(System.out);
        else
        {
            final File modelFile = new File(filename);
            if (modelFile.exists())
            {
                System.out.println("The file '" + filename + "' already exists. Remove it, then try again.");
                return;
            }
            final FileOutputStream fos = new FileOutputStream(modelFile);
            xmlWriter = new XmlModelWriter(fos);
        }

        final AlarmClient client = new AlarmClient(server, config);
        client.start();

        System.out.printf("Writing file after model is stable for %d seconds:\n", STABILIZATION_SECS);
        System.out.println("Monitoring changes...");

        final AlarmConfigMonitor updateMonitor = new AlarmConfigMonitor(STABILIZATION_SECS, client);
        updateMonitor.waitForPauseInUpdates(30);

        System.out.printf("Received no more updates for %d seconds, I think I have a stable configuration\n", STABILIZATION_SECS);
        System.out.println("Writing to file: " + filename);

        // Write the model.
        final long start = System.currentTimeMillis();
        xmlWriter.write(client.getRoot());
        xmlWriter.close();
        final long end = System.currentTimeMillis();

        System.out.println("Write time: " + SecondsParser.formatSeconds((end - start)/1000.0));
        System.out.printf("%d updates were received while writing model to file.\n", updateMonitor.getCount());

        updateMonitor.dispose();

        client.shutdown();
	}

	// Import an alarm system model from an xml file.
	public void importModel(final String filename, final String server, final String config) throws InterruptedException, Exception
	{
	    System.out.println("Reading new configuration from " + filename);
	    final long start = System.currentTimeMillis();
		final File file = new File(filename);
		final FileInputStream fileInputStream = new FileInputStream(file);

		final XmlModelReader xmlModelReader = new XmlModelReader();
		xmlModelReader.load(fileInputStream);

        final AlarmClientNode new_root = xmlModelReader.getRoot();
        // Check that the configs match.
        if (!config.equals(new_root.getName()))
        {
            System.out.printf("Expecting configuration for \"%s\" but file %s contains settings for \"%s\".\n", config, filename, new_root.getName());
            return;
        }
        final long got_xml = System.currentTimeMillis();

		// Connect to the server.
		final AlarmClient client = new AlarmClient(server, config);
        client.start();
        try
        {
            System.out.println("Fetching existing alarm configuration for \"" + config + "\", then waiting for it to remain stable for " + STABILIZATION_SECS + " seconds...");
            final AlarmConfigMonitor updateMonitor = new AlarmConfigMonitor(STABILIZATION_SECS, client);
            updateMonitor.waitForPauseInUpdates(30);
            updateMonitor.dispose();
            final long got_old_config = System.currentTimeMillis();

            System.out.println("Deleting existing " + new_root.getName() + " ...");

            // Get the server's root node for this config.
            final AlarmClientNode root = client.getRoot();

            // Delete the old model. Leave the root node.
            final List<AlarmTreeItem<?>> root_children = root.getChildren();
            for (final AlarmTreeItem<?> child : root_children)
            	client.removeComponent(child);
            final long deleted_old = System.currentTimeMillis();

            System.out.println("Loading new " + new_root.getName() + " ...");

            // For every child of the new root, add them and their descendants to the old root.
            final List<AlarmTreeItem<?>> new_root_children = new_root.getChildren();
            for (final AlarmTreeItem<?> child : new_root_children)
				addNodes(client, root, child);
            final long loaded_new = System.currentTimeMillis();

            System.out.println("Time to read XML                     : " + SecondsParser.formatSeconds((got_xml - start) / 1000.0));
            System.out.println("Time to fetch existing configuration : " + SecondsParser.formatSeconds((got_old_config - got_xml) / 1000.0));
            System.out.println("Time to delete existing configuration: " + SecondsParser.formatSeconds((deleted_old - got_old_config) / 1000.0));
            System.out.println("Time to load new configuration       : " + SecondsParser.formatSeconds((loaded_new - deleted_old) / 1000.0));
            System.out.println("Total                                : " + SecondsParser.formatSeconds((loaded_new - start) / 1000.0));
            System.out.println("Done.");
        }
        finally
        {
            client.shutdown();
        }
	}

	private void addNodes(final AlarmClient client, final AlarmTreeItem<?> parent, final AlarmTreeItem<?> tree_item) throws Exception
	{
		// Send the configuration for the newly created node.
		client.sendItemConfigurationUpdate(tree_item.getPathName(), tree_item);

		// Recurse over children.
		final List<AlarmTreeItem<?>> children = tree_item.getChildren();
		for (final AlarmTreeItem<?> child : children)
			addNodes(client, tree_item, child);
	}
}

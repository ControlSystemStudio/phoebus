/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientListener;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmTreeLeaf;
import org.phoebus.applications.alarm.model.xml.XmlModelReader;
import org.phoebus.applications.alarm.model.xml.XmlModelWriter;
import org.phoebus.framework.jobs.NamedThreadFactory;

/**
 * Writes Alarm System model to XML.
 * @author Evan Smith
 *
 */
@SuppressWarnings("nls")
public class AlarmConfigTool
{
	private class UpdateMonitor
	{
		// Time the model must be stable for. Unit is seconds. Default is 4 seconds.
		private long time = 4;

		private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Timer"));
	    private final CountDownLatch no_more_messages = new CountDownLatch(1);
	    private final Runnable signal_no_more_messages = () -> no_more_messages.countDown();
	    private final AtomicReference<ScheduledFuture<?>> timeout = new AtomicReference<>();
        private final AtomicInteger updates = new AtomicInteger();

	    // Sets the timeout member variable.
		private UpdateMonitor(final long new_time)
		{
			time = new_time;
			resetTimer();
		}

	    void resetTimer()
	    {
	        final ScheduledFuture<?> previous = timeout.getAndSet(timer.schedule(signal_no_more_messages, time, TimeUnit.SECONDS));
	        if (previous != null)
	            previous.cancel(false);
	    }

	    private final AlarmClientListener updateListener = new AlarmClientListener()
        {
            @Override
            public void itemAdded(final AlarmTreeItem<?> item)
            {
            	// Reset the timer when receiving update
                resetTimer();
        		updates.incrementAndGet();
            }

            @Override
            public void itemRemoved(final AlarmTreeItem<?> item)
            {
            	// Reset the timer when receiving update
                resetTimer();
        		updates.incrementAndGet();
            }

            @Override
            public void itemUpdated(final AlarmTreeItem<?> item)
            {
            	//NOP
            }
        };

        public void listen(AlarmClient client) throws InterruptedException, Exception
        {
        	client.addListener(updateListener);
        	 if (! no_more_messages.await(30, TimeUnit.SECONDS))
                 throw new Exception("30 seconds have passed, I give up waiting for updates to subside.");
        	// Reset the counter to count any updates received after we decide to continue.
        	updates.set(0);
        }

        public Integer getCount()
        {
        	return updates.get();
        }
	}

    private AlarmClient client = null;
    private UpdateMonitor updateMonitor = null;

	private final long time = 4;

	// Export an alarm system model to an xml file.
    public void exportModel(String filename, String server, String config) throws Exception
	{
        final XmlModelWriter xmlWriter;

        // Write to stdout or to file.
        if (filename.equals("stdout"))
        {
            xmlWriter = new XmlModelWriter(System.out);
        }
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

		client = new AlarmClient(server, config);
        client.start();

        System.out.printf("Writing file after model is stable for %d seconds:\n", time);

        System.out.println("Monitoring changes...");

        updateMonitor = new UpdateMonitor(time);

        updateMonitor.listen(client);

        System.out.printf("Received no more updates for %d seconds, I think I have a stable configuration\n", time);

        //Write the model.


        xmlWriter.getModelXML(client.getRoot());

        System.out.println("\nModel written to file: " + filename);
        System.out.printf("%d updates were received while writing model to file.\n", updateMonitor.getCount());

        client.shutdown();
	}

	// Import an alarm system model from an xml file.
	public void importModel(final String filename, final String server, final String config) throws InterruptedException, Exception
	{
		final File file = new File(filename);
		final FileInputStream fileInputStream = new FileInputStream(file);

		final XmlModelReader xmlModelReader = new XmlModelReader();
		xmlModelReader.load(fileInputStream);

		// Connect to the server.
		client = new AlarmClient(server, config);
        client.start();
        try
        {
            updateMonitor = new UpdateMonitor(time);

            System.out.println("Fetching server model. This could take some time ...");

            updateMonitor.listen(client);

            final AlarmClientNode new_root = xmlModelReader.getRoot();
            System.out.println("Importing " + new_root.getName() + " ...");

            // Get the server's root node for this config.
            final AlarmClientNode root = client.getRoot();

            // Check that the configs match.
            if (!config.equals(new_root.getName()))
            {
            	System.out.printf("The provided config \"%s\" does not match the config loaded from file \"%s\".\n", config, new_root.getName());
            	return;
            }

            // Delete the old model. Leave the root node.
            final List<AlarmTreeItem<?>> root_children = root.getChildren();
            for (final AlarmTreeItem<?> child : root_children)
            	client.removeComponent(child);

            // For every child of the new root, add them and their descendants to the old root.
            final List<AlarmTreeItem<?>> new_root_children = new_root.getChildren();
            for (final AlarmTreeItem<?> child : new_root_children)
				addNodes(root, child);
            System.out.println("Done.");
        }
        finally
        {
            client.shutdown();
        }
	}

	private void addNodes(AlarmTreeItem<?> parent, AlarmTreeItem<?> tree_item) throws Exception
	{
		// Determine if the item is a node or a leaf and add to the model appropriately.
		if (tree_item instanceof AlarmTreeLeaf)
			client.addPV(parent, tree_item.getName());
		else if (tree_item instanceof AlarmTreeItem)
			client.addComponent(parent, tree_item.getName());

		// Send the configuration for the newly created node.
		client.sendItemConfigurationUpdate(tree_item.getPathName(), tree_item);

		// Recurse over children.
		final List<AlarmTreeItem<?>> children = tree_item.getChildren();
		for (final AlarmTreeItem<?> child : children)
			addNodes(tree_item, child);
	}
}

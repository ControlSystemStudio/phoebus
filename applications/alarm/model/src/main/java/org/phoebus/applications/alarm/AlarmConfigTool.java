package org.phoebus.applications.alarm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
                 throw new Exception("30 seconds have passed, I give up waiting for updates to subside");
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

	// Prints help info about the program and then exits.
	private void help()
	{
		System.out.println("AlarmToolConfig help menu. Usage defined below.");
		System.out.println("\n\tThis program facilitates the importation and exportation of the Alarm System's model via XML files.\n");
		System.out.println("\tTo print this menu: java AlarmToolConfig --help\n");
		System.out.println("\tUsing '--export' the program will write the Alarm System's current model to an XML file.");
		System.out.println("\n\tThe 'wait_time' argument refers to the amount of time the model must have been stable before it will be written to file.\n");
		System.out.println("\tTo export model to a file:  java AlarmToolConfig --export output_filename wait_time");
		System.out.println("\tTo export model to console: java AlarmToolConfig --export stdout wait_time\n");
		System.out.println("\tUsing '--import' the program will read a user supplied XML file and import the model contained therein to the Alarm System server.");
		System.out.println("\n\tTo import model from a file: java AlarmToolConfig --import input_filename --server host_name --config config_name");

		System.exit(0);
	}

	private long time = 4;
	private void setTimeout(long new_time)
	{
		time = new_time;
	}
	// Export an alarm system model to an xml file.
	private void exportModel(String filename, String server, String config) throws Exception
	{

		client = new AlarmClient(server, config);
        client.start();

        System.out.printf("Writing file after model is stable for %d seconds:\n", time);

        System.out.println("Monitoring changes...");

        updateMonitor = new UpdateMonitor(time);

        updateMonitor.listen(client);

        System.out.printf("Received no more updates for %d seconds, I think I have a stable configuration\n", time);

        //Write the model.

        final File modelFile = new File(filename);
        final FileOutputStream fos = new FileOutputStream(modelFile);

        XmlModelWriter xmlWriter = null;

        // Write to stdout or to file.
        if (0 == filename.compareTo("stdout"))
        {
        	xmlWriter = new XmlModelWriter(System.out);
        }
        else
        {
        	xmlWriter = new XmlModelWriter(fos);
        }

        xmlWriter.getModelXML(client.getRoot());

        System.out.println("\nModel written to file: " + filename);
        System.out.printf("%d updates were recieved while writing model to file.\n", updateMonitor.getCount());

        client.shutdown();
	}

	// Import an alarm system model from an xml file.
	private void importModel(final String filename, final String hostname, final String config) throws InterruptedException, Exception
	{
		final File file = new File(filename);
		final FileInputStream fileInputStream = new FileInputStream(file);

		final XmlModelReader xmlModelReader = new XmlModelReader();

		try
		{
			xmlModelReader.load(fileInputStream);
		} catch (final Exception e)
		{
			e.printStackTrace();
		}


		// Connect to the server.
		client = new AlarmClient(hostname, config);
        client.start();

        updateMonitor = new UpdateMonitor(time);

        System.out.print("Fetching server model. This could take some time ...");

        updateMonitor.listen(client);

        final AlarmClientNode new_root = xmlModelReader.getRoot();

        // Get the server's root node for this config.
        final AlarmClientNode root = client.getRoot();

        // Check that the configs match.
        if (!config.equals(new_root.getName()))
        {
        	System.out.printf("The provided config \"%s\" does not match the config loaded from file \"%s\".\n", config, new_root.getName());
        	client.shutdown();
        	System.exit(1);
        }

        // Delete the old model. Leave the root node.
        final List<AlarmTreeItem<?>> root_children = root.getChildren();
        for (final AlarmTreeItem<?> child : root_children)
        	client.removeComponent(child);

        // For every child of the new root, add them and their descendants to the old root.
        final List<AlarmTreeItem<?>> new_root_children = new_root.getChildren();
        for (final AlarmTreeItem<?> child : new_root_children)
        {
	        try
			{
				addNodes(root, child);
			} catch (final Exception e1)
			{
				e1.printStackTrace();
			}
        }
	}

	private void addNodes(AlarmTreeItem<?> parent, AlarmTreeItem<?> tree_item) throws Exception
	{
		// Determine if the item is a node or a leaf and add to the model appropriately.
		if (tree_item instanceof AlarmTreeLeaf)
		{
			client.addPV(parent, tree_item.getName());
		}
		else if (tree_item instanceof AlarmTreeItem)
		{
			client.addComponent(parent, tree_item.getName());
		}

		// Send the configuration for the newly created node.
		client.sendItemConfigurationUpdate(tree_item.getPathName(), tree_item);

		// Recurse over children.
		final List<AlarmTreeItem<?>> children = tree_item.getChildren();
		for (final AlarmTreeItem<?> child : children)
		{
			addNodes(tree_item, child);
		}
	}

	// Constructor. Handles parsing of command lines and execution of command line options.
	private AlarmConfigTool(String[] args)
	{

		int wait_time = 0;
		for (int i = 0; i < args.length; i++)
		{
			if (0 == args[i].compareTo("--help"))
			{
				help();
			}
			if (0 == args[i].compareTo("--export"))
			{
				i++;
				if (i >= args.length)
				{
					System.out.println("ERROR: '--export' must be followed by an output file name and a wait time. Use --help for program usage info.");
					System.exit(1);
				}

				final String filename = args[i];
				i++;
				if (i >= args.length)
				{
					System.out.println("ERROR: --export' must be accompanied by an output file name and a wait time. Use --help for program usage info.");
					System.exit(1);
				}

				try
				{
					wait_time = Integer.parseInt(args[i]);
				}
				catch (final NumberFormatException e)
				{
					System.out.println("ERROR: Wait time must be an integer value. Use --help for program usage info.");
					System.exit(1);
				}

				setTimeout(wait_time);

				i++;
				if (i >= args.length || 0 != args[i].compareTo("--server"))
				{
					System.out.println("ERROR: '--import' must be followed by '--server'. Use --help for program usage info.");
					System.exit(1);
				}

				// Check that a host name was provided and didn't proceed to --config.
				i++;
				if (i >= args.length || 0 == args[i].compareTo("--config"))
				{
					System.out.println("ERROR: '--server' must be followed by a hostname. Use --help for program usage info.");
					System.exit(1);
				}

				final String server = args[i];

				i++;
				if (i >= args.length || 0 != args[i].compareTo("--config"))
				{
					System.out.println("ERROR: '--export' must be followed by '--config'. Use --help for program usage info.");
					System.exit(1);
				}
				i++;

				if (i >= args.length)
				{
					System.out.println("ERROR: '--config' must be followed by a config name. Use --hlp for program usage info.");
					System.exit(1);
				}

				final String config = args[i];

				try
				{
					exportModel(filename, server, config);
				}
				catch (final Exception e)
				{
					e.printStackTrace();
				}
			}
			else if (0 == args[i].compareTo("--import"))
			{
				i++;
				if (i >= args.length)
				{
					System.out.println("ERROR: '--import' must be accompanied by an input file name. Use --help for program usage info.");
					System.exit(1);
				}

				final String filename = args[i];

				i++;
				if (i >= args.length || 0 != args[i].compareTo("--server"))
				{
					System.out.println("ERROR: '--import' must be followed by '--server'. Use --help for program usage info.");
					System.exit(1);
				}

				// Check that a host name was provided and didn't proceed to --config.
				i++;
				if (i >= args.length || 0 == args[i].compareTo("--config"))
				{
					System.out.println("ERROR: '--server' must be followed by a hostname. Use --help for program usage info.");
					System.exit(1);
				}

				final String hostname = args[i];

				i++;
				if (i >= args.length || 0 != args[i].compareTo("--config"))
				{
					System.out.println("ERROR: '--import' must be followed by '--config'. Use --help for program usage info.");
					System.exit(1);
				}
				i++;
				if (i >= args.length)
				{
					System.out.println("ERROR: '--config' must be followed by a config name. Use --hlp for program usage info.");
					System.exit(1);
				}
				final String configname = args[i];

				try
				{
					importModel(filename, hostname, configname);
				} catch (final FileNotFoundException e)
				{
					System.out.println("Input file: \"" + filename + "\" not found.");
					System.exit(1);
				} catch (final InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (final Exception e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else
			{
				System.out.printf("ERROR: Unrecognized command line option: \"%s\". Use --help for program usage info.", args[i]);
				System.exit(1);
			}
		}
	}

	public static void main(String[] args)
	{
		@SuppressWarnings("unused")
		final AlarmConfigTool act = new AlarmConfigTool(args);
	}

}

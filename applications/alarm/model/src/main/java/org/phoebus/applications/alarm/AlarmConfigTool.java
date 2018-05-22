package org.phoebus.applications.alarm;

import java.io.File;
import java.io.FileOutputStream;

import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientListener;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.xml.XmlModelWriter;

public class AlarmConfigTool
{
	// Handles primary thread waiting.
	private final Object lock = new Object();

	// Time the model must be stable for.
	private long timeout = 4 * 1000;

	// Guard and update variable. Will be updated by multiple threads, must be guarded.
	private final Object update_guard = new Object();
	private boolean updated = false;

	// Prints help info about the program and then exits.
	private void help()
	{
		// TODO: Create help menu.
		System.out.println("AlarmToolConfig help menu. Usage defined below.\n");
		System.out.println("\n\tThis program facilitates the importation and exportation of the Alarm System's model.\n");
		System.out.println("\tTo print this menu: java AlarmToolConfig --help\n");
		System.out.println("\tUsing '--export' the program will write the Alarm System's current model to an XML file.");
		System.out.println("\n\tThe 'wait_time' argument refers to the amount of time the model must have been stable before it will be written to file.\n");
		System.out.println("\tTo export model to a file:  java AlarmToolConfig --export output_filename wait_time");
		System.out.println("\tTo export model to console: java AlarmToolConfig --export stdout wait_time\n");
		System.out.println("\tUsing '--import' the program will read a user supplied XML file and import the model contained therein to the Alarm System.");

		// TODO: Uncomment when import is implemented.
		//System.out.print("\tTo import model from a file: java AlarmToolConfig --import [import filename]");

		System.exit(0);
	}

	private void setTimeout(final long time)
	{
		timeout = time * 1000;
	}

	// Export an alarm system model to an xml file.
	private void exportModel(String filename) throws Exception
	{

		final AlarmClient client = new AlarmClient(AlarmDemoSettings.SERVERS, AlarmDemoSettings.ROOT);
        client.start();

        System.out.printf("Writing file after model is stable for %d seconds:\n", timeout/1000);

        System.out.println("Monitoring changes...");

        client.addListener(new AlarmClientListener()
        {
            @Override
            public void itemAdded(final AlarmTreeItem<?> item)
            {
            	// Mark as updated.
            	synchronized(update_guard)
            	{
            		updated = true;
            	}
            	// Notify the waiting thread an update has occurred.
            	synchronized (lock)
            	{
            		lock.notifyAll();
            	}
            }

            @Override
            public void itemRemoved(final AlarmTreeItem<?> item)
            {
            	// Mark as updated.
            	synchronized(update_guard)
            	{
            		updated = true;
            	}
            	// Notify the waiting thread an update has occurred.
            	synchronized (lock)
            	{
            		lock.notifyAll();

            	}
            }

            @Override
            public void itemUpdated(final AlarmTreeItem<?> item)
            {
            	// Mark as updated.
            	synchronized(update_guard)
            	{
            		updated = true;
            	}
            	// Notify the waiting thread an update has occurred.
            	synchronized (lock)
            	{
            		lock.notifyAll();
            	}
            }
        });

        while (true)
        {
        	// Wait for the model to be stable.
        	synchronized(lock)
        	{
        		lock.wait(timeout);
        	}
        	// If when the thread wakes the model has been updated, reset the variable.
        	// On next iteration wait again.
        	if (true == updated)
        	{
        		System.out.println("Model has been updated. Restarting wait for stability.");
        		synchronized (update_guard)
        		{
        			updated = false;
        		}
        	}
        	// If the model has not been updated break out of the loop.
        	else
        	{
        		break;
        	}
        }

        // Shutdown the client to stop the model from being changed again.
        client.shutdown();

        //Write the model.

        final File modelFile = new File(filename);
        final FileOutputStream fos = new FileOutputStream(modelFile);

        XmlModelWriter xmlWriter = null;

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
	}

	// Import an alarm system model from an xml file.
	@SuppressWarnings("unused")
	private void importModel(/* xml file? */)
	{
		// TODO: Code to import a model from an xml file.
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
					System.out.println("ERROR: '--export' must be accompanied by an output file name and a wait time. Use --help for program usage info.");
					System.exit(1);
				}

				final String filename = args[i];
				i++;
				if (i >= args.length)
				{
					System.out.println("'ERROR: --export' must be accompanied by an output file name and a wait time. Use --help for program usage info.");
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

				try
				{
					exportModel(filename);
				}
				catch (final Exception e)
				{
					e.printStackTrace();
				}
			}
			else if (0 == args[i].compareTo("--import"))
			{
				System.out.println("import not yet implemented.");
			}
			else
			{
				System.out.printf("Unrecognized command line option: %s\nUse --help for program usage info.", args[i]);
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

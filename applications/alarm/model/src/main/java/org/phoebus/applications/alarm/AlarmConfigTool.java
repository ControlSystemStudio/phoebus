package org.phoebus.applications.alarm;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientListener;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.xml.XmlModelWriter;

public class AlarmConfigTool
{
	// Prints help info about the program and then exits.
	private void help()
	{
		// TODO: Create help menu.
		System.out.println("AlarmToolConfig help menu. Usage defined below.\n");
		System.out.println("\tTo print this menu: java AlarmToolConfig --help");
		System.out.println("\n\tWhen using --export the 'wait time' argument refers to the amount of time the model must have been stable before it will be written to file.\n");
		System.out.println("\tTo export model to a file: java AlarmToolConfig --export [output filename] [wait time]");
		System.out.println("\tTo export model to output: java AlarmToolConfig --export stdout [wait time]");

		// TODO: Uncomment when import is implemented.
		//System.out.print("\tTo import model from a file: java AlarmToolConfig --import [import filename]");

		System.exit(0);
	}

	// Import an alarm system model from an xml file.
	@SuppressWarnings("unused")
	private void importModel(/* xml file? */)
	{
		// TODO: Code to import a model from an xml file.
	}

	// Handles primary thread waiting.
	private final Object lock = new Object();

	// Time the model must be stable for.
	private final long timeout = 4 * 1000;

	// Guard and update variable. Will be updated by multiple threads, must be guarded.
	private final Object update_guard = new Object();
	private boolean updated = false;

	// Export an alarm system model to an xml file.
	private void exportModel(String filename) throws Exception
	{
		// TODO: Decide on timing scheme for when to write snapshot of model to xml.
		// TODO: Create listener only for items added and removed. Not updated?
		final AlarmClient client = new AlarmClient(AlarmDemoSettings.SERVERS, AlarmDemoSettings.ROOT);
        client.start();
        TimeUnit.SECONDS.sleep(4);

        System.out.println("Snapshot after 4 seconds:");
        //ModelPrinter.print(client.getRoot());

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

	// Constructor. Handles parsing of command lines and execution of command line options.
	private AlarmConfigTool(String[] args)
	{
		// TODO: Parse command line arguments
		// TODO: Add command line argument for timeout
		final List<String> argList = Arrays.asList(args);
		int index = -1;
		if (argList.contains(new String("--help")))
		{
			help();
		}
		if (-1 != (index = argList.lastIndexOf(new String("--export"))))
		{

			// TODO: Handle the exception with appropriate error messages.
				try
				{
					exportModel(argList.get(index+1));
				}
				catch (final Exception e)
				{
					e.printStackTrace();
				}

		}

	}

	public static void main(String[] args)
	{
		@SuppressWarnings("unused")
		final AlarmConfigTool act = new AlarmConfigTool(args);
	}

}

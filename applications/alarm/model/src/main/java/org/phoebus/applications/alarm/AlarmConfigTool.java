package org.phoebus.applications.alarm;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientListener;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.print.ModelPrinter;

public class AlarmConfigTool
{
	// Prints help info about the program and then exits.
	private void help()
	{
		// TODO: Create help menu.
		System.out.println("AlarmToolConfig help menu. Usage defined below.\n");
		System.out.println("\tTo print this menu: java AlarmToolConfig --help");
		System.out.print("\tTo export model to a file: java AlarmToolConfig --export [output filename]");

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

	// Export an alarm system model to an xml file.
	private void exportModel(String filename) throws Exception
	{
		// TODO: Decide on timing scheme for when to write snapshot of model to xml.
		// TODO: Create listener only for items added and removed. Not updated?
		final AlarmClient client = new AlarmClient(AlarmDemoSettings.SERVERS, AlarmDemoSettings.ROOT);
        client.start();
        TimeUnit.SECONDS.sleep(4);

        System.out.println("Snapshot after 4 seconds:");
        ModelPrinter.print(client.getRoot());

        System.out.println("Monitoring changes...");

        client.addListener(new AlarmClientListener()
        {
            @Override
            public void itemAdded(final AlarmTreeItem<?> item)
            {
                // Reset timer.

            }

            @Override
            public void itemRemoved(final AlarmTreeItem<?> item)
            {
            	// Reset timer.
            }

            @Override
            public void itemUpdated(final AlarmTreeItem<?> item)
            {
            	// Reset timer.
            }
        });

        client.shutdown();
	}

	// Constructor. Handles parsing of command lines and execution of command line options.
	private AlarmConfigTool(String[] args)
	{
		// TODO: Parse command line arguments

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

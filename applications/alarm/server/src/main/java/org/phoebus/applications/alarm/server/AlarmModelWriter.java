package org.phoebus.applications.alarm.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.phoebus.applications.alarm.AlarmDemoSettings;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmTreeLeaf;
import org.phoebus.applications.alarm.model.TitleDetail;
import org.phoebus.framework.persistence.IndentingXMLStreamWriter;
import org.phoebus.framework.persistence.XMLUtil;

public class AlarmModelWriter
{
	private static XMLStreamWriter writer;

	private static void initWriter (final OutputStream stream) throws Exception
	{
		final XMLStreamWriter base =
	            XMLOutputFactory.newInstance().createXMLStreamWriter(stream, XMLUtil.ENCODING);
	        writer = new IndentingXMLStreamWriter(base);

	        writer.writeStartDocument(XMLUtil.ENCODING, "1.0");
	        // TODO: Does anything else need to be done to initialize the XML writer?

	}

	public static void getModelXML(final AlarmTreeItem<?> item) throws Exception
	{

        getModelXML(item, System.out);
    }

    public static void getModelXML(final AlarmTreeItem<?> item, final OutputStream out) throws Exception
    {
    	initWriter(out);
        getModelXML(item, out, 0);
    }

    private static void getModelXML(final AlarmTreeItem<?> item, final OutputStream out, final int level) throws Exception
    {

    	if (level == 0)
    	{
    		writer.writeStartElement("config");
        	writer.writeAttribute("name", item.getName());

        	getItemXML(item);

        	for (final AlarmTreeItem<?> child : item.getChildren())
                getModelXML(child, out, level+1);

        	writer.writeEndElement();
    	}
    	else if (item instanceof AlarmTreeLeaf)
        {
        	final AlarmTreeLeaf leaf = (AlarmTreeLeaf) item;

        	writer.writeStartElement("pv");
        	writer.writeAttribute("name", item.getName());

        	getLeafXML(leaf);

        	getItemXML(item);

        	writer.writeEndElement();
        }
        else
        {
        	writer.writeStartElement("component");
        	writer.writeAttribute("name", item.getName());

        	getItemXML(item);

        	for (final AlarmTreeItem<?> child : item.getChildren())
                getModelXML(child, out, level+1);

        	writer.writeEndElement();
        }


    }

    private static void getItemXML(final AlarmTreeItem<?> item) throws Exception
    {

    	// Write XML for Guidance
    	final List<TitleDetail> guidance = item.getGuidance();

    	if (!guidance.isEmpty())
    	{
    		writer.writeStartElement("guidance");
    		getTitleDetailListXML(guidance);
    		writer.writeEndElement();
    	}

    	// Write XML for Displays
    	final List<TitleDetail> displays = item.getDisplays();

    	if (!displays.isEmpty())
    	{
    		writer.writeStartElement("display");
    		getTitleDetailListXML(displays);
    		writer.writeEndElement();
    	}

    	// Write XML for Commands
    	final List<TitleDetail> commands = item.getCommands();

    	if (!commands.isEmpty())
    	{
    		writer.writeStartElement("command");
    		getTitleDetailListXML(commands);
    		writer.writeEndElement();
    	}

    	// Write XML for Actions
    	final List<TitleDetail> actions = item.getActions();

    	if (!actions.isEmpty())
    	{
    		writer.writeStartElement("action");
    		getTitleDetailListXML(actions);
    		writer.writeEndElement();
    	}
    }

    private static void getTitleDetailListXML(final List<TitleDetail> tdList) throws Exception
    {
    	for (final TitleDetail td : tdList)
		{
			// TODO: would a title element ever have empty or null title/detail?
			writer.writeStartElement("title");
			writer.writeCharacters(td.title);
			writer.writeEndElement();
			writer.writeStartElement("detail");
			writer.writeCharacters(td.detail);
			writer.writeEndElement();
		}
    }

    private static void getLeafXML(final AlarmTreeLeaf leaf) throws Exception
    {
    	final String description = leaf.getDescription();
    	if (description != null && !description.isEmpty())
    	{
    		writer.writeStartElement("description");
    		writer.writeCharacters(description);
    		writer.writeEndElement();
    	}

    	final String enabled = leaf.isEnabled() ? "true" : "false";

		writer.writeStartElement("enabled");
		writer.writeCharacters(enabled);
		writer.writeEndElement();

		final String latching = leaf.isLatching() ? "true" : "false";

		writer.writeStartElement("latching");
		writer.writeCharacters(latching);
		writer.writeEndElement();

		final String annunciating = leaf.isAnnunciating() ? "true" : "false";

		writer.writeStartElement("annunciating");
		writer.writeCharacters(annunciating);
		writer.writeEndElement();

		final int delay = leaf.getDelay();

		// A delay less than zero doesn't make sense but is technically possible.
		if (delay != 0)
		{
			writer.writeStartElement("delay");
			writer.writeCharacters(Integer.toString(delay));
			writer.writeEndElement();
		}

		final int count = leaf.getCount();

		// Count is unsigned so can be assumed greater than 0.
		if (count > 0)
		{
			writer.writeStartElement("count");
			writer.writeCharacters(Integer.toString(count));
			writer.writeEndElement();
		}

		final String filter = leaf.getFilter();
    	if (filter != null && !filter.isEmpty())
    	{
    		writer.writeStartElement("filter");
    		writer.writeCharacters(filter);
    		writer.writeEndElement();
    	}

    }

	public static void close() throws IOException
	{
        try
        {
            // End and close document
            writer.writeEndDocument();
            writer.flush();
            writer.close();
        }
        catch (final Exception ex)
        {
            throw new IOException("Failed to close XML", ex);
        }
    }

	// TODO: Do we need to handle exception better than simply throwing?
	public void testAlarmModelToXML(String args[]) throws Exception
	{
		final AlarmClient client = new AlarmClient(AlarmDemoSettings.SERVERS, AlarmDemoSettings.ROOT);
        client.start();
        TimeUnit.SECONDS.sleep(4);

        System.out.println("Snapshot after 4 seconds:");

        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        getModelXML(client.getRoot(), buf);
        close();

        System.out.println("File write finished.");
        final String xml = buf.toString();
        System.out.println(xml);
	}

}

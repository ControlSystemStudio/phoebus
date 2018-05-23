package org.phoebus.applications.alarm.model.xml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.model.TitleDetail;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlModelReader
{
	// Node Types
	public static final String TAG_CONFIG = "config";
	public static final String TAG_COMPONENT = "component";
	public static final String TAG_PV = "pv";

	// PV Specific
	public static final String TAG_DESCRIPTION = "description";
	public static final String TAG_ENABLED = "enabled";
	public static final String TAG_LATCHING = "latching";
	public static final String TAG_ANNUNCIATING = "annunciating";
	public static final String TAG_DELAY = "delay";
	public static final String TAG_COUNT = "count";
	public static final String TAG_FILTER = "filter";

	// PV and Component
	public static final String TAG_GUIDANCE = "guidance";
	public static final String TAG_DISPLAY = "display";
	public static final String TAG_COMMAND = "command";
	public static final String TAG_ACTIONS = "automated_action";

	// TitleDetail specific tags.
	public static final String TAG_TITLE = "title";
	public static final String TAG_DETAILS = "details";

	private AlarmClientNode root = null;

	public AlarmClientNode getRoot()
	{
		return root;
	}

	// Parse the xml stream and load the stream into a document.
	public void load(InputStream stream) throws Exception
	{
        final DocumentBuilder docBuilder =
                DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final Document doc = docBuilder.parse(stream);

        buildModel(doc);
	}

	private void buildModel(final Document doc) throws Exception
	{
		// Handle root node.

        doc.getDocumentElement().normalize();
        final Element root_node = doc.getDocumentElement();


        NodeList children = null;

        // Check if it's a <config/>. If it is collect the child nodes.
        if (!root_node.getNodeName().equals(TAG_CONFIG))
            throw new Exception("Expected " + TAG_CONFIG + " but got " + root_node.getNodeName());
        else
        	children = root_node.getChildNodes();

        // Create the root of the model. Parent is null and name must be config.
        root = new AlarmClientNode(null, root_node.getNodeName());

        if (children != null)
        {
        	// Process every child node.
        	for (int i = 0; i < children.getLength(); i++)
        	{
        		final Node child = children.item(i);
        		processNode(root /* parent */, child);
        	}

        }

	}

	private void processNode(AlarmClientNode parent, final Node nd)
	{
		final String name = nd.getNodeName();

		switch (name)
		{
		case TAG_COMPONENT:
			processComponent(parent, nd);
			break;
		case TAG_PV:
			processPV(parent, nd);
			break;
		default:
			// Unknown label. Ignore or throw?
			break;
		}

	}


	private void processComponent(AlarmClientNode parent, Node nd)
	{
		// Name of the new component node.
		String comp_node_name = null;
		final NamedNodeMap attrs = nd.getAttributes();

		if (attrs != null)
        {
			// Go through the attributes and find the component name.
			// This list should only ever hold the name attribute or else it will fail the schema.
			// Still best to treat it as if it could have multiple attributes.
            for (int idx = 0; idx < attrs.getLength(); idx++)
            {
                final Node attr = attrs.item(idx);
                final String attr_name = attr.getNodeName();

                if (attr_name.equals("name"))
                {
                	comp_node_name = attr.getNodeValue();
                }
            }
        }

		// New component node.
		final AlarmClientNode comp_node = new AlarmClientNode(parent, comp_node_name);

		final NodeList child_ndls = nd.getChildNodes();

		for (int idx = 0; idx < child_ndls.getLength(); idx++)
        {
				final Node child = child_ndls.item(idx);
				final String name = child.getNodeName();
            	if (name.equals(TAG_COMPONENT) || name.equals(TAG_PV))
            	{
            		processNode(comp_node, child);
            	}
            	else
            	{
            		// This does not refer to XML attributes but instead to the attributes of a model component node.
            		processCompAttr(comp_node, child);
            	}
        }

	}

	private void processCompAttr(AlarmClientNode comp_node, Node child)
	{
		final String name = child.getNodeName();
		if (name.equals(TAG_COMMAND))
		{
			// Get a copy of any previous guidance entries.
			final List<TitleDetail> c = comp_node.getCommands();
			final ArrayList<TitleDetail> commands = new ArrayList<>(c);
			// Add the new guidance entry.
			String title = "";
			String details = "";

			final NodeList children = child.getChildNodes();
			for (int i = 0; i < children.getLength(); i++)
			{
				final Node grandchild = children.item(i);
				if (grandchild.getNodeName().equals(TAG_TITLE))
					title = grandchild.getTextContent();
				else if (grandchild.getNodeName().equals(TAG_DETAILS))
					details = grandchild.getTextContent();
			}

			commands.add(new TitleDetail(title, details));

			comp_node.setCommands(commands);
		}
		else if (name.equals(TAG_GUIDANCE))
		{
			// Get a copy of any previous guidance entries.
			final List<TitleDetail> g = comp_node.getGuidance();
			final ArrayList<TitleDetail> guidance = new ArrayList<>(g);
			// Add the new guidance entry.
			String title = "";
			String details = "";

			final NodeList children = child.getChildNodes();
			for (int i = 0; i < children.getLength(); i++)
			{
				final Node grandchild = children.item(i);
				if (grandchild.getNodeName().equals(TAG_TITLE))
					title = grandchild.getTextContent();
				else if (grandchild.getNodeName().equals(TAG_DETAILS))
					details = grandchild.getTextContent();
			}

			guidance.add(new TitleDetail(title, details));

			comp_node.setGuidance(guidance);
		}
		else if (name.equals(TAG_DISPLAY))
		{
			// Get a copy of any previous guidance entries.
			final List<TitleDetail> d = comp_node.getDisplays();
			final ArrayList<TitleDetail> displays = new ArrayList<>(d);
			// Add the new display entry.
			String title = "";
			String details = "";

			final NodeList children = child.getChildNodes();
			for (int i = 0; i < children.getLength(); i++)
			{
				final Node grandchild = children.item(i);
				if (grandchild.getNodeName().equals(TAG_TITLE))
					title = grandchild.getTextContent();
				else if (grandchild.getNodeName().equals(TAG_DETAILS))
					details = grandchild.getTextContent();
			}

			displays.add(new TitleDetail(title, details));

			comp_node.setGuidance(displays);
		}
		else if (name.equals(TAG_ACTIONS))
		{
			// Get a copy of any previous guidance entries.
			final List<TitleDetail> a = comp_node.getActions();
			final ArrayList<TitleDetail> actions = new ArrayList<>(a);

			// Add the new actions entry.
			String title = "";
			String details = "";

			final NodeList children = child.getChildNodes();
			for (int i = 0; i < children.getLength(); i++)
			{
				final Node grandchild = children.item(i);
				if (grandchild.getNodeName().equals(TAG_TITLE))
					title = grandchild.getTextContent();
				else if (grandchild.getNodeName().equals(TAG_DETAILS))
					details = grandchild.getTextContent();
			}

			actions.add(new TitleDetail(title, details));

			comp_node.setActions(actions);
		}

	}

	private void processPV(AlarmClientNode parent, final Node nd)
	{
		String pv_node_name = null;
		final NamedNodeMap attrs = nd.getAttributes();

		if (attrs != null)
        {
			// Go through the attributes and find the component name.
			// This list should only ever hold the name attribute or else it will fail the schema.
			// Still best to treat it as if it could have multiple attributes.
            for (int idx = 0; idx < attrs.getLength(); idx++)
            {
                final Node attr = attrs.item(idx);
                final String attr_name = attr.getNodeName();

                if (attr_name.equals("name"))
                {
                	pv_node_name = attr.getNodeValue();
                }
            }
        }

		final AlarmClientLeaf pv = new AlarmClientLeaf(parent, pv_node_name);

		final NodeList child_ndls = nd.getChildNodes();

		for (int idx = 0; idx < child_ndls.getLength(); idx++)
        {
				final Node child = child_ndls.item(idx);
				final String name = child.getNodeName();

				if (name.equals(TAG_LATCHING))
				{
					final boolean value = child.getTextContent().equals("true") ? true : false;
					pv.setLatching(value);
				}
				else if (name.equals(TAG_ENABLED))
				{
					final boolean value = child.getTextContent().equals("true") ? true : false;
					pv.setEnabled(value);
				}
				else if (name.equals(TAG_ANNUNCIATING))
				{
					final boolean value = child.getTextContent().equals("true") ? true : false;
					pv.setAnnunciating(value);
				}
				else if (name.equals(TAG_DESCRIPTION))
				{
					final String value = child.getTextContent();
					pv.setDescription(value);
				}
				else if (name.equals(TAG_DELAY))
				{
					final int delay = Integer.parseInt(child.getTextContent());
					pv.setDelay(delay);
				}
				else if (name.equals(TAG_COUNT))
				{
					final int count = Integer.parseInt(child.getTextContent());
					pv.setCount(count);
				}
				else if (name.equals(TAG_FILTER))
				{
					final String value = child.getTextContent();
					pv.setFilter(value);
				}
				else if (name.equals(TAG_COMMAND))
				{
					// Get a copy of any previous guidance entries.
					final List<TitleDetail> c = pv.getCommands();
					final ArrayList<TitleDetail> commands = new ArrayList<>(c);
					// Add the new guidance entry.
					String title = "";
					String details = "";

					final NodeList children = child.getChildNodes();
					for (int i = 0; i < children.getLength(); i++)
					{
						final Node grandchild = children.item(i);
						if (grandchild.getNodeName().equals(TAG_TITLE))
							title = grandchild.getTextContent();
						else if (grandchild.getNodeName().equals(TAG_DETAILS))
							details = grandchild.getTextContent();
					}

					commands.add(new TitleDetail(title, details));

					pv.setCommands(commands);
				}
				else if (name.equals(TAG_GUIDANCE))
				{
					// Get a copy of any previous guidance entries.
					final List<TitleDetail> g = pv.getGuidance();
					final ArrayList<TitleDetail> guidance = new ArrayList<>(g);
					// Add the new guidance entry.
					String title = "";
					String details = "";

					final NodeList children = child.getChildNodes();
					for (int i = 0; i < children.getLength(); i++)
					{
						final Node grandchild = children.item(i);
						if (grandchild.getNodeName().equals(TAG_TITLE))
							title = grandchild.getTextContent();
						else if (grandchild.getNodeName().equals(TAG_DETAILS))
							details = grandchild.getTextContent();
					}

					guidance.add(new TitleDetail(title, details));

					pv.setGuidance(guidance);
				}
				else if (name.equals(TAG_DISPLAY))
				{
					// Get a copy of any previous guidance entries.
					final List<TitleDetail> d = pv.getDisplays();
					final ArrayList<TitleDetail> displays = new ArrayList<>(d);
					// Add the new display entry.
					String title = "";
					String details = "";

					final NodeList children = child.getChildNodes();
					for (int i = 0; i < children.getLength(); i++)
					{
						final Node grandchild = children.item(i);
						if (grandchild.getNodeName().equals(TAG_TITLE))
							title = grandchild.getTextContent();
						else if (grandchild.getNodeName().equals(TAG_DETAILS))
							details = grandchild.getTextContent();
					}

					displays.add(new TitleDetail(title, details));

					pv.setGuidance(displays);
				}
				else if (name.equals(TAG_ACTIONS))
				{
					// Get a copy of any previous guidance entries.
					final List<TitleDetail> a = pv.getActions();
					final ArrayList<TitleDetail> actions = new ArrayList<>(a);

					// Add the new actions entry.
					String title = "";
					String details = "";

					final NodeList children = child.getChildNodes();
					for (int i = 0; i < children.getLength(); i++)
					{
						final Node grandchild = children.item(i);
						if (grandchild.getNodeName().equals(TAG_TITLE))
							title = grandchild.getTextContent();
						else if (grandchild.getNodeName().equals(TAG_DETAILS))
							details = grandchild.getTextContent();
					}

					actions.add(new TitleDetail(title, details));

					pv.setActions(actions);
				}
        }
	}
}

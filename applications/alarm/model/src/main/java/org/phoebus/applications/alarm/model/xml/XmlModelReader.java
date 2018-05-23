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

		List<String> td = null;
		List<TitleDetail> tdl = null;
		ArrayList<TitleDetail> tdal = null;
		String title = new String();
		String detail = new String();

		switch (name)
		{
			case TAG_COMMAND:
				// Get a copy of any previous guidance entries.
				tdl = comp_node.getCommands();
				tdal = new ArrayList<>(tdl);

				// Add the new guidance entry.
				td = getTD(child);
				title = td.get(0);
				detail = td.get(1);
				tdal.add(new TitleDetail(title, detail));

				comp_node.setCommands(tdal);
				break;
			case TAG_GUIDANCE:
				// Get a copy of any previous guidance entries.
				tdl = comp_node.getGuidance();
				tdal = new ArrayList<>(tdl);

				// Add the new guidance entry.
				td = getTD(child);
				title = td.get(0);
				detail = td.get(1);
				tdal.add(new TitleDetail(title, detail));

				comp_node.setGuidance(tdal);
				break;
			case TAG_DISPLAY:
				// Get a copy of any previous guidance entries.
				tdl = comp_node.getDisplays();
				tdal = new ArrayList<>(tdl);

				// Add the new display entry.
				td = getTD(child);
				title = td.get(0);
				detail = td.get(1);
				tdal.add(new TitleDetail(title, detail));

				comp_node.setGuidance(tdal);
				break;
			case TAG_ACTIONS:
				// Get a copy of any previous guidance entries.
				tdl = comp_node.getActions();
				tdal = new ArrayList<>(tdl);

				// Add the new actions entry.
				td = getTD(child);
				title = td.get(0);
				detail = td.get(1);
				tdal.add(new TitleDetail(title, detail));

				comp_node.setActions(tdal);
				break;
			default:
				break;
				// ?
			} /* end of switch */

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

				boolean value = false;
				String str = new String();
				String title = new String();
				String detail = new String();
				List<TitleDetail> tdl = null;
				ArrayList<TitleDetail> tdal = null;
				List<String> td = null;

				switch(name)
				{
					case TAG_LATCHING:
						value = child.getTextContent().equals("true") ? true : false;
						pv.setLatching(value);
						break;
					case TAG_ENABLED:
						value = child.getTextContent().equals("true") ? true : false;
						pv.setEnabled(value);
						break;
					case TAG_ANNUNCIATING:
						value = child.getTextContent().equals("true") ? true : false;
						pv.setAnnunciating(value);
						break;
					case TAG_DESCRIPTION:
						str = child.getTextContent();
						pv.setDescription(str);
						break;
					case TAG_DELAY:
						final int delay = Integer.parseInt(child.getTextContent());
						pv.setDelay(delay);
						break;
					case TAG_COUNT:
						final int count = Integer.parseInt(child.getTextContent());
						pv.setCount(count);
						break;
					case TAG_FILTER:
						str = child.getTextContent();
						pv.setFilter(str);
						break;
					case TAG_COMMAND:
						// Get a copy of any previous guidance entries.
						tdl = pv.getCommands();
						tdal = new ArrayList<>(tdl);

						// Add the new guidance entry.
						td = getTD(child);
						title = td.get(0);
						detail = td.get(1);
						tdal.add(new TitleDetail(title, detail));

						pv.setCommands(tdal);
						break;
					case TAG_GUIDANCE:
						// Get a copy of any previous guidance entries.
						tdl = pv.getGuidance();
						tdal = new ArrayList<>(tdl);

						// Add the new guidance entry.
						td = getTD(child);
						title = td.get(0);
						detail = td.get(1);
						tdal.add(new TitleDetail(title, detail));

						pv.setGuidance(tdal);
						break;
					case TAG_DISPLAY:
						// Get a copy of any previous guidance entries.
						tdl = pv.getDisplays();
						tdal = new ArrayList<>(tdl);

						// Add the new display entry.
						td = getTD(child);
						title = td.get(0);
						detail = td.get(1);
						tdal.add(new TitleDetail(title, detail));

						pv.setGuidance(tdal);
						break;
					case TAG_ACTIONS:
						// Get a copy of any previous guidance entries.
						tdl = pv.getActions();
						tdal = new ArrayList<>(tdl);

						// Add the new actions entry.
						td = getTD(child);
						title = td.get(0);
						detail = td.get(1);
						tdal.add(new TitleDetail(title, detail));

						pv.setActions(tdal);
						break;
					default:
						break;
						// ?
				} /* end of switch */
        }
	}

	private List<String> getTD(Node nd)
	{
		String title = new String();
		String detail = new String();
		final NodeList children = nd.getChildNodes();
		for (int i = 0; i < children.getLength(); i++)
		{
			final Node grandchild = children.item(i);
			if (grandchild.getNodeName().equals(TAG_TITLE))
				title = grandchild.getTextContent();
			else if (grandchild.getNodeName().equals(TAG_DETAILS))
				detail = grandchild.getTextContent();
		}

		return List.of(title, detail);
	}
}

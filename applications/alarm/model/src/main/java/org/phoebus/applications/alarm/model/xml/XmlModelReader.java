package org.phoebus.applications.alarm.model.xml;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlModelReader
{
	public static final String TAG_CONFIG = "config";

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
        final AlarmClientNode root = new AlarmClientNode(null, root_node.getNodeName());

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
		case "component":
			processComponent(parent, nd);
			break;
		case "pv":
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

		System.out.println("created component " + comp_node_name + " parent=" + parent.getName());

		final NodeList child_ndls = nd.getChildNodes();

		for (int idx = 0; idx < child_ndls.getLength(); idx++)
        {
				final Node child = child_ndls.item(idx);
				final String name = child.getNodeName();
            	if (name.equals("component") || name.equals("pv"))
            	{
            		processNode(comp_node, child);
            	}
            	else
            	{
            		// This does not refer to XML attributes but instead to the attributes of a model component node.
            		//processCompAttr(comp_node, child);
            	}
        }

	}

	private void processCompAttr(AlarmClientNode comp_node, Node child)
	{
		final String attrName = child.getNodeName();


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
		System.out.println("created pv " + pv_node_name + " parent=" + parent.getName());

		final AlarmClientLeaf pv = new AlarmClientLeaf(parent, pv_node_name);

		final NodeList child_ndls = nd.getChildNodes();

		for (int idx = 0; idx < child_ndls.getLength(); idx++)
        {
				final Node child = child_ndls.item(idx);
				final String name = child.getNodeName();

				if (name.equals("latching"))
				{
					final boolean value = child.getTextContent().equals("true") ? true : false;
					pv.setLatching(value);
					System.out.println("\tpv " + pv_node_name + " latching: " + child.getTextContent());
				}
				else if (name.equals("enabled"))
				{
					final boolean value = child.getTextContent().equals("true") ? true : false;
					pv.setEnabled(value);
					System.out.println("\tpv " + pv_node_name + " enabled: " + child.getTextContent());
				}
				else if (name.equals("annunciating"))
				{
					final boolean value = child.getTextContent().equals("true") ? true : false;
					pv.setAnnunciating(value);
					System.out.println("\tpv " + pv_node_name + " annunciating: " + child.getTextContent());
				}
				else if (name.equals("description"))
				{
					final String value = child.getTextContent();
					pv.setDescription(value);
					System.out.println("\tpv " + pv_node_name + " description: " + value);
				}
				else if (name.equals("delay"))
				{
					final int delay = Integer.parseInt(child.getTextContent());
					pv.setDelay(delay);
					System.out.println("\tpv " + pv_node_name + " delay: " + child.getTextContent());
				}
				else if (name.equals("count"))
				{
					final int count = Integer.parseInt(child.getTextContent());
					pv.setCount(count);
					System.out.println("\tpv " + pv_node_name + " count: " + child.getTextContent());
				}
				else if (name.equals("filter"))
				{
					final String value = child.getTextContent();
					pv.setFilter(value);
					System.out.println("\tpv " + pv_node_name + " filter: " + value);
				}
        }
	}
}

/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.model.xml;

import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.model.TitleDetail;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
* Creates Alarm System model from XML.
* @author Evan Smith
*
*/

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

        // Check if it's a <config/>. If it is collect the child nodes.
        if (!root_node.getNodeName().equals(TAG_CONFIG))
            throw new Exception("Expected " + TAG_CONFIG + " but got " + root_node.getNodeName());

        // Create the root of the model. Parent is null and name must be config.
        root = new AlarmClientNode(null, root_node.getNodeName());

        for (final Node child : XMLUtil.getChildElements(root_node, TAG_COMPONENT))
        	processComponent(root /* parent */, child);

        for (final Element child : XMLUtil.getChildElements(root_node, TAG_PV))
        	processPV(root /* parent */, child);
	}

	private void processComponent(AlarmClientNode parent, Node nd) throws Exception
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

		// This does not refer to XML attributes but instead to the attributes of a model component node.
		processCompAttr(comp_node, nd);

        for (final Element child : XMLUtil.getChildElements(nd, TAG_COMPONENT))
        	processComponent(comp_node /* parent */, child);

        for (final Element child : XMLUtil.getChildElements(nd, TAG_PV))
        	processPV(comp_node/* parent */, child);

	}

	private void processCompAttr(AlarmClientNode comp_node, Node node)
	{
		ArrayList<TitleDetail> td = new ArrayList<>();

		for (final Element child : XMLUtil.getChildElements(node, TAG_GUIDANCE))
			td.add(getTD(child));

		if (td.size() > 0)
		{
			comp_node.setGuidance(td);
			td = new ArrayList<>();
		}
		for (final Element child : XMLUtil.getChildElements(node, TAG_DISPLAY))
			td.add(getTD(child));

		if (td.size() > 0)
		{
			comp_node.setDisplays(td);
			td = new ArrayList<>();
		}

		for (final Element child : XMLUtil.getChildElements(node, TAG_COMMAND))
			td.add(getTD(child));

		if (td.size() > 0)
		{
			comp_node.setCommands(td);
			td = new ArrayList<>();
		}

		for (final Element child : XMLUtil.getChildElements(node, TAG_ACTIONS))
			td.add(getTD(child));

		if (td.size() > 0)
		{
			comp_node.setActions(td);
			td = new ArrayList<>();
		}
	}

	private void processPV(AlarmClientNode parent, final Element nd) throws Exception
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

		XMLUtil.getChildBoolean(nd, TAG_ENABLED).ifPresent(pv::setEnabled);
		XMLUtil.getChildBoolean(nd, TAG_LATCHING).ifPresent(pv::setLatching);
		XMLUtil.getChildBoolean(nd, TAG_ANNUNCIATING).ifPresent(pv::setAnnunciating);
		XMLUtil.getChildString(nd, TAG_DESCRIPTION).ifPresent(pv::setDescription);
		XMLUtil.getChildInteger(nd, TAG_DELAY).ifPresent(pv::setDelay);
		XMLUtil.getChildInteger(nd, TAG_COUNT).ifPresent(pv::setCount);
		XMLUtil.getChildString(nd, TAG_FILTER).ifPresent(pv::setFilter);

		ArrayList<TitleDetail> td = new ArrayList<>();

		for (final Element child : XMLUtil.getChildElements(nd, TAG_GUIDANCE))
			td.add(getTD(child));

		if (td.size() > 0)
		{
			pv.setGuidance(td);
			td = new ArrayList<>();
		}

		for (final Element child : XMLUtil.getChildElements(nd, TAG_DISPLAY))
			td.add(getTD(child));

		if (td.size() > 0)
		{
			pv.setDisplays(td);
			td = new ArrayList<>();
		}

		for (final Element child : XMLUtil.getChildElements(nd, TAG_COMMAND))
			td.add(getTD(child));

		if (td.size() > 0)
		{
			pv.setCommands(td);
			td = new ArrayList<>();
		}

		for (final Element child : XMLUtil.getChildElements(nd, TAG_ACTIONS))
			td.add(getTD(child));

		if (td.size() > 0)
		{
			pv.setActions(td);
			td = new ArrayList<>();
		}

	}

	private TitleDetail getTD(Element node)
	{
		final String title = XMLUtil.getChildString(node, TAG_TITLE).orElse("");
		final String detail = XMLUtil.getChildString(node, TAG_DETAILS).orElse("");
		return new TitleDetail(title, detail);
	}
}

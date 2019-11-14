/*******************************************************************************
 * Copyright (c) 2018-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.model.xml;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.model.TitleDetail;
import org.phoebus.applications.alarm.model.TitleDetailDelay;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/** Creates Alarm System model from XML.
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class XmlModelReader
{
    // Node Types
    public static final String TAG_CONFIG = "config";
    public static final String TAG_COMPONENT = "component";
    public static final String TAG_PV = "pv";

    // Misc.
    public static final String TAG_NAME = "name";

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

    /** All known PV names and their path, used to check for duplicates */
    private Map<String, String> pv_names = new HashMap<>();

    private AlarmClientNode root = null;

    public AlarmClientNode getRoot()
    {
        return root;
    }

    // Parse the xml stream and load the stream into a document.
    public void load(final InputStream stream) throws Exception
    {
        final DocumentBuilder docBuilder =
                DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final Document doc = docBuilder.parse(stream);

        buildModel(doc);
        // Clear map used to check for duplicates
        pv_names.clear();
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
        root = new AlarmClientNode(null, root_node.getAttribute(TAG_NAME));

        // First add PVs at this level, ..
        for (final Element child : XMLUtil.getChildElements(root_node, TAG_PV))
            processPV(root /* parent */, child);

        // .. when sub-components which again have PVs.
        // This way, duplicate PVs will be detected and ignored at a nested level,
        // keeping those toward the root
        for (final Node child : XMLUtil.getChildElements(root_node, TAG_COMPONENT))
            processComponent(root /* parent */, child);
    }

    private void processComponent(final AlarmClientNode parent, final Node node) throws Exception
    {
        // Name of the new component node.
        String comp_node_name = null;
        final NamedNodeMap attrs = node.getAttributes();

        if (attrs != null)
        {
            // Go through the attributes and find the component name.
            // This list should only ever hold the name attribute or else it will fail the schema.
            // Still best to treat it as if it could have multiple attributes.
            for (int idx = 0; idx < attrs.getLength(); idx++)
            {
                final Node attr = attrs.item(idx);
                final String attr_name = attr.getNodeName();
                if (attr_name.equals(TAG_NAME))
                    comp_node_name = attr.getNodeValue();
            }
        }

        if (comp_node_name == null)
            throw new Exception("Component without name at " + parent.getPathName());

        if (parent.getChild(comp_node_name) != null)
            throw new Exception("Component with duplicate name " + comp_node_name + " at " + parent.getPathName());

        // New component node.
        final AlarmClientNode component = new AlarmClientNode(parent, comp_node_name);

        // This does not refer to XML attributes but instead to the attributes of a model component node.
        processCompAttr(component, node);

        // First add PVs at this level, then sub-components
        for (final Element child : XMLUtil.getChildElements(node, TAG_PV))
            processPV(component/* parent */, child);

        for (final Element child : XMLUtil.getChildElements(node, TAG_COMPONENT))
            processComponent(component /* parent */, child);
    }

    private void processCompAttr(final AlarmClientNode component, final Node node) throws Exception
    {
        ArrayList<TitleDetail> td = new ArrayList<>();

        for (final Element child : XMLUtil.getChildElements(node, TAG_GUIDANCE))
            td.add(getTD(child));

        if (td.size() > 0)
        {
            component.setGuidance(td);
            td = new ArrayList<>();
        }
        for (final Element child : XMLUtil.getChildElements(node, TAG_DISPLAY))
            td.add(getTD(child));

        if (td.size() > 0)
        {
            component.setDisplays(td);
            td = new ArrayList<>();
        }

        for (final Element child : XMLUtil.getChildElements(node, TAG_COMMAND))
            td.add(getTD(child));

        if (td.size() > 0)
        {
            component.setCommands(td);
            td = new ArrayList<>();
        }

        ArrayList<TitleDetailDelay> tdd = new ArrayList<>();
        for (final Element child : XMLUtil.getChildElements(node, TAG_ACTIONS))
            tdd.add(getTDD(child));

        if (tdd.size() > 0)
        {
            component.setActions(tdd);
            tdd = new ArrayList<>();
        }
    }

    private void processPV(final AlarmClientNode parent, final Element node) throws Exception
    {
        String pv_node_name = null;
        final NamedNodeMap attrs = node.getAttributes();

        if (attrs != null)
        {
            // Go through the attributes and find the component name.
            // This list should only ever hold the name attribute or else it will fail the schema.
            // Still best to treat it as if it could have multiple attributes.
            for (int idx = 0; idx < attrs.getLength(); idx++)
            {
                final Node attr = attrs.item(idx);
                final String attr_name = attr.getNodeName();
                if (attr_name.equals(TAG_NAME))
                    pv_node_name = attr.getNodeValue();
            }
        }

        if (pv_node_name == null)
            throw new Exception("PV without name at " + parent.getPathName());

        if (parent.getChild(pv_node_name) != null)
            throw new Exception("PV with duplicate name " + pv_node_name + " at " + parent.getPathName());

        // Check if PV is already handled
        final String duplicate_path = pv_names.get(pv_node_name);
        if (duplicate_path != null)
        {
            // PV is already handled elsewhere in the config,
            // so there will be alarms --> warn, but continue
            logger.log(Level.WARNING, "Ignoring duplicate PV " + parent.getPathName() + "/" + pv_node_name + ", already at " + duplicate_path);
            return;
        }
        // Remember to prevent duplicates
        pv_names.put(pv_node_name, parent.getPathName());

        final AlarmClientLeaf pv = new AlarmClientLeaf(parent, pv_node_name);

        // New XML export always writes these three tags.
        // Legacy XML file only wrote them if false, true, true,
        // i.e. missing tags meant true, false, false.
        pv.setEnabled(XMLUtil.getChildBoolean(node, TAG_ENABLED).orElse(true));
        pv.setLatching(XMLUtil.getChildBoolean(node, TAG_LATCHING).orElse(false));
        pv.setAnnunciating(XMLUtil.getChildBoolean(node, TAG_ANNUNCIATING).orElse(false));

        XMLUtil.getChildString(node, TAG_DESCRIPTION).ifPresent(pv::setDescription);

        final String delayStr = XMLUtil.getChildString(node, TAG_DELAY).orElse("");
        if (delayStr.equals(""))
            pv.setDelay(0);
        else
        {
            try
            {
                final Double tmp = Double.parseDouble(delayStr);
                final Integer delay = tmp.intValue();
                pv.setDelay(delay);
            }
            catch (final NumberFormatException e)
            {
                pv.setDelay(0);
            }
        }

        XMLUtil.getChildInteger(node, TAG_COUNT).ifPresent(pv::setCount);
        XMLUtil.getChildString(node, TAG_FILTER).ifPresent(pv::setFilter);

        ArrayList<TitleDetail> td = new ArrayList<>();

        for (final Element child : XMLUtil.getChildElements(node, TAG_GUIDANCE))
            td.add(getTD(child));

        if (td.size() > 0)
        {
            pv.setGuidance(td);
            td = new ArrayList<>();
        }

        for (final Element child : XMLUtil.getChildElements(node, TAG_DISPLAY))
            td.add(getTD(child));

        if (td.size() > 0)
        {
            pv.setDisplays(td);
            td = new ArrayList<>();
        }

        for (final Element child : XMLUtil.getChildElements(node, TAG_COMMAND))
            td.add(getTD(child));

        if (td.size() > 0)
        {
            pv.setCommands(td);
            td = new ArrayList<>();
        }

        ArrayList<TitleDetailDelay> tdd = new ArrayList<>();
        for (final Element child : XMLUtil.getChildElements(node, TAG_ACTIONS))
            tdd.add(getTDD(child));

        if (tdd.size() > 0)
        {
            pv.setActions(tdd);
            tdd = new ArrayList<>();
        }
    }

    private TitleDetail getTD(final Element node)
    {
        final String title = XMLUtil.getChildString(node, TAG_TITLE).orElse("");
        final String detail = XMLUtil.getChildString(node, TAG_DETAILS).orElse("");
        return new TitleDetail(title, detail);
    }

    private TitleDetailDelay getTDD(final Element node) throws Exception
    {
        final String  title  = XMLUtil.getChildString(node, TAG_TITLE).orElse("");
        final String  detail = XMLUtil.getChildString(node, TAG_DETAILS).orElse("");
        final int delay  = XMLUtil.getChildInteger(node, TAG_DELAY).orElse(0);
        return new TitleDetailDelay(title, detail, delay);
    }
}

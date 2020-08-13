/*******************************************************************************
 * Copyright (c) 2017-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtable.persistence;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.phoebus.applications.pvtable.Settings;
import org.phoebus.applications.pvtable.model.PVTableItem;
import org.phoebus.applications.pvtable.model.PVTableModel;
import org.phoebus.applications.pvtable.model.SavedArrayValue;
import org.phoebus.applications.pvtable.model.SavedScalarValue;
import org.phoebus.applications.pvtable.model.SavedValue;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Persist PVTableModel as XML file
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVTableXMLPersistence extends PVTablePersistence
{
    /** File extension used for XML files */
    final public  static String FILE_EXTENSION = "pvs";
    final private static String ROOT = "pvtable";
    final private static String TOLERANCE = "tolerance";
    final private static String PVLIST = "pvlist";
    final private static String PV = "pv";
    final private static String SELECTED = "selected";
    final private static String NAME = "name";
    final private static String SAVED_VALUE = "saved_value";
    final private static String SAVED_ARRAY = "saved_array_value";
    final private static String SAVED_TIME = "saved_value_timestamp";
    final private static String ITEM = "item";
    final private static String READBACK_NAME = "readback";
    final private static String READBACK_SAVED = "readback_value";
    final private static String COMPLETION = "completion";
    final private static String TIMEOUT = "timeout";
    final private static String ENABLE_SAVE_RESTORE = "enable_save_restore";

    /** {@inheritDoc} */
    @Override
    public String getFileExtension()
    {
        return FILE_EXTENSION;
    }

    /** {@inheritDoc} */
    @Override
    public void read(final PVTableModel model, final InputStream stream) throws Exception
    {
        final DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final Document doc = docBuilder.parse(stream);
        read(model, doc);
    }

    /** @param doc XML document
     *  @return PV table model
     *  @throws Exception on error
     */
    private void read(final PVTableModel model, final Document doc) throws Exception
    {
        // Check if it's a <pvtable/>.
        doc.getDocumentElement().normalize();
        final Element root_node = doc.getDocumentElement();
        final String root_name = root_node.getNodeName();

        if (!root_name.equals(ROOT))
            throw new Exception("Expected <" + ROOT + ">, found <" + root_name + ">");

        model.setSaveRestore(XMLUtil.parseBoolean(root_node.getAttribute(ENABLE_SAVE_RESTORE), true));

        // Get the default <tolerance> entry
        final double default_tolerance = XMLUtil.getChildDouble(root_node, TOLERANCE).orElse(Settings.tolerance);

        model.setCompletionTimeout(XMLUtil.getChildDouble(root_node, TIMEOUT).orElse(60.0).longValue());

        // Get the <pvlist> entry
        final Element pvlist = XMLUtil.getChildElement(root_node, PVLIST);

        if (pvlist != null)
            for (Element pv : XMLUtil.getChildElements(pvlist, PV))
            {
                String pv_name = XMLUtil.getChildString(pv, NAME).orElse("");
                if (pv_name.isEmpty())
                    continue;

                final double tolerance = XMLUtil.getChildDouble(pv, TOLERANCE).orElse(default_tolerance);
                final boolean selected = XMLUtil.getChildBoolean(pv, SELECTED).orElse(true);

                final String time_saved = model.isSaveRestoreEnabled() ? XMLUtil.getChildString(pv, SAVED_TIME).orElse("") : "";
                final SavedValue saved  = model.isSaveRestoreEnabled() ? readSavedValue(pv) : null;
                final PVTableItem pvItem = model.addItem(pv_name, tolerance, saved, time_saved);
                pvItem.setSelected(selected);
                if (model.isSaveRestoreEnabled())
                    pvItem.setUseCompletion(XMLUtil.getChildBoolean(pv, COMPLETION).orElse(false));

                // Legacy files may contain a separate readback PV and value for this entry
                XMLUtil.getChildString(pv, READBACK_NAME).ifPresent(readback ->
                {
                    // This legacy entry always saved a scalar
                    final SavedValue saved_readback = new SavedScalarValue(XMLUtil.getChildString(pv, READBACK_SAVED).orElse(""));

                    // If found, add as separate PV, not selected to be restored
                    model.addItem(readback, tolerance, saved_readback, "")
                         .setSelected(false);
                });
            }
    }

    /** @param pv PV element that might contain saved value, scalar or array
     *  @return {@link SavedValue} or <code>null</code>
     */
    private SavedValue readSavedValue(final Element pv)
    {
        final String saved_scalar = XMLUtil.getChildString(pv, SAVED_VALUE).orElse(null);
        if (saved_scalar != null)
            return new SavedScalarValue(saved_scalar);

        final Element saved_array = XMLUtil.getChildElement(pv, SAVED_ARRAY);
        if (saved_array != null)
        {
            final List<String> items = new ArrayList<>();
            for (Element item : XMLUtil.getChildElements(saved_array, ITEM))
                items.add(XMLUtil.getString(item));
            return new SavedArrayValue(items);
        }
        return null;
    }

    @Override
    public void write(final PVTableModel model, final OutputStream stream) throws Exception
    {
        Boolean saveRestore = model.isSaveRestoreEnabled();

        final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        final Element root = doc.createElement("pvtable");
        root.setAttribute("version", "3.0");
        root.setAttribute(ENABLE_SAVE_RESTORE, saveRestore.toString());
        doc.appendChild(root);

        root.appendChild(XMLUtil.createTextElement(doc, TIMEOUT, Double.toString(model.getCompletionTimeout())));

        final Element pvs = doc.createElement(PVLIST);
        for (PVTableItem item : model.getItems())
        {
            Element pv = doc.createElement(PV);

            pv.appendChild(XMLUtil.createTextElement(doc, SELECTED, Boolean.toString(item.isSelected())));
            pv.appendChild(XMLUtil.createTextElement(doc, NAME, item.getName()));
            pv.appendChild(XMLUtil.createTextElement(doc, TOLERANCE, Double.toString(item.getTolerance())));

            if (saveRestore)
            {
                pv.appendChild(XMLUtil.createTextElement(doc, SAVED_TIME, item.getTime_saved()));

                final SavedValue saved = item.getSavedValue().orElse(null);
                if (saved instanceof SavedScalarValue)
                    pv.appendChild(XMLUtil.createTextElement(doc, SAVED_VALUE, saved.toString()));
                else if (saved instanceof SavedArrayValue)
                {
                    final SavedArrayValue array = (SavedArrayValue) saved;
                    final Element el = doc.createElement(SAVED_ARRAY);

                    final int N = array.size();
                    for (int i=0; i<N; ++i)
                        el.appendChild(XMLUtil.createTextElement(doc, ITEM, array.get(i)));

                    pv.appendChild(el);

                }

                pv.appendChild(XMLUtil.createTextElement(doc, COMPLETION, Boolean.toString(item.isUsingCompletion())));
            }

            pvs.appendChild(pv);
        }
        root.appendChild(pvs);

        XMLUtil.writeDocument(doc, stream);
    }
}


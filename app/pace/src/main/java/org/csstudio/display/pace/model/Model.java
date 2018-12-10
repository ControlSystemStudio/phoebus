/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.pace.model;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** PACE Data model
 *  @author Delphy Nypaver Armstrong
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Model
{
    // XML Tags
    private static final String XML_ROOT = "paceconfig";
    private static final String XML_TITLE = "title";
    private static final String XML_COLUMNS = "columns";
    private static final String XML_COLUMN = "column";
    private static final String XML_INSTANCES = "instances";
    private static final String XML_INSTANCE = "instance";

    /** Overall title */
    final private String title;

    /** Column definitions */
    final private List<Column> columns;

    /** Instances, rows with cells */
    final private List<Instance> instances;

    /** Listener to be notified of model changes */
    final private CopyOnWriteArrayList<Consumer<Cell>> listeners = new CopyOnWriteArrayList<>();

    /** Has any cell been edited? */
    private volatile boolean dirty = false;

    /** Initialize model from XML file stream
     *  @param stream Stream for XML file
     *  @throws Exception on error: Missing XML elements, errors in macros,
     *          problems in PV creation
     */
    public Model(final InputStream stream) throws Exception
    {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        final DocumentBuilder db = dbf.newDocumentBuilder();
        final Document doc = db.parse(stream);

        // Check root element
        final Element root_node = doc.getDocumentElement();
        root_node.normalize();
        final String root_name = root_node.getNodeName();
        if (!root_name.equals(XML_ROOT))
            throw new Exception("Got " + root_name + " instead of 'paceconfig'");

        // Using org.csstudio.apputil.xml.DOMHelper plugin for parsing the XML file

        // Get Title
        title = XMLUtil.getChildString(root_node, XML_TITLE).orElse("Title");

        final List<Column> the_columns = new ArrayList<>();
        final List<Instance> the_instances = new ArrayList<>();

        // Read column definitions
        final Element cols_node = XMLUtil.getChildElement(root_node, XML_COLUMNS);
        if (cols_node != null)
        {
            // Loop over column definitions
            for (Element col_node : XMLUtil.getChildElements(cols_node, XML_COLUMN))
                the_columns.add(Column.fromDOM(col_node));

            // Locate instance definitions
            final Element insts_node = XMLUtil.getChildElement(root_node, XML_INSTANCES);
            if (insts_node != null)
                for (Element inst_node : XMLUtil.getChildElements(insts_node, XML_INSTANCE))
                {
                    final Instance instance = Instance.fromDOM(this, inst_node);
                    // Create cells, passing exceptions back up
                    instance.createCells(the_columns);
                    the_instances.add(instance);
                }
        }
        columns = Collections.unmodifiableList(the_columns);
        instances = Collections.unmodifiableList(the_instances);
    }

    /** @param listener Listener to add */
    public void addListener(final Consumer<Cell> listener)
    {
        listeners.add(listener);
    }

    /** @param listener Listener to remove */
    public void removeListener(final Consumer<Cell> listener)
    {
        listeners.remove(listener);
    }

    /** @return Overall title of the Model */
    public String getTitle()
    {
        return title;
    }

    /** @return {@link Column}s */
    public List<Column> getColumns()
    {
        return columns;
    }

    /** @return {@link Instance}s */
    public List<Instance> getInstances()
    {
        return instances;
    }

    /** Fast check if model is 'dirty'
     *  @return <code>true</code> if any cell has been edited
     */
    public boolean isEdited()
    {
        return dirty;
    }

    /** Slow check if any cell has been edited.
     *  <p>Used to update 'dirty' flag.
     *  @return <code>true</code> if any cell has been edited
     */
    private boolean isAnyCellEdited()
    {
        for (Instance instance : instances)
            for (int c = 0; c < columns.size(); c++)
                if (instance.getCell(c).isEdited())
                    return true;
        return false;
    }

    /** Start the PV connections of all cells in model
     *  @throws Exception on error
     */
    public void start() throws Exception
    {
        for (Instance instance : instances)
            for (int c = 0; c < columns.size(); c++)
                instance.getCell(c).start();
    }

    /** Stop the PV connections of all cells in model */
    public void stop()
    {
        for (Instance instance : instances)
            for (int c = 0; c < columns.size(); c++)
                instance.getCell(c).stop();
    }

    /** Save values entered by user to the PVs.
     *  Any cells with 'user' values meant to replace
     *  the original PV value get written to the PV.
     *  @param user_name Name of the user to be logged for cells with
     *                   a last user meta PV
     *  @throws Exception on error writing to the PV
     */
    public void saveUserValues(final String user_name) throws Exception
    {
        for (Instance instance : instances)
        {
            for (int c = 0; c < columns.size(); c++)
                instance.getCell(c).saveUserValue(user_name);
        }
    }

    /** Restore cells to state before trying to save user values
     *  @throws Exception on error writing to the PV
     */
    public void revertOriginalValues() throws Exception
    {
        dirty = false;
        for (Instance instance : instances)
        {
            for (int c = 0; c < columns.size(); c++)
                instance.getCell(c).revertOriginalValue();
        }
    }

    /** Clear all user values.
     *  <p>Meant to be called after user values have been saved,
     *  or to globally clear anything entered.
     */
    public void clearUserValues()
    {
        dirty = false;
        for (Instance instance : instances)
        {
            for (int c = 0; c < columns.size(); c++)
                instance.getCell(c).clearUserValue();
        }
    }

    /** Notify listeners of cell update
     *  @param cell Cell that changed
     */
    void fireCellUpdate(final Cell cell)
    {   // If this cell has been edited, the model is 'dirty'.
        // If this cell just cleared, need to check all other
        // cells to determine if model is still dirty or overall clear.
        // Could also turn 'dirty' into change counter,
        // but then need to assert that it's always properly
        // incremented/decremented...
        if (cell.isEdited())
            dirty = true;
        else
            dirty = isAnyCellEdited();
        for (Consumer<Cell> listener : listeners)
            listener.accept(cell);
    }

    /** @return Info string for debugging */
    @Override
    public String toString()
    {
        return "PACE Model '" + title + "'";
    }
}

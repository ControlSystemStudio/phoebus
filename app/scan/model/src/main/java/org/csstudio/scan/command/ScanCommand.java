/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The scan engine idea is based on the "ScanEngine" developed
 * by the Software Services Group (SSG),  Advanced Photon Source,
 * Argonne National Laboratory,
 * Copyright (c) 2011 , UChicago Argonne, LLC.
 *
 * This implementation, however, contains no SSG "ScanEngine" source code
 * and is not endorsed by the SSG authors.
 ******************************************************************************/
package org.csstudio.scan.command;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.csstudio.scan.device.DeviceInfo;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Description of a scan server command
 *
 *  <p>Used by the client to describe commands to the server,
 *  and returned by the server to describe elements of a Scan.
 *
 *  <p>This class offers generic property access
 *  based on introspection, assuming that the command has
 *  suitable 'getter' and 'setter' methods.
 *  A property with ID "some_property" must have associated "getSomeProperty"
 *  and "setSomeProperty" methods, i.e. using a CamelCase version
 *  of the property ID.
 *
 *  <p>The setter must accept an {@link Object} like {@link Double} or {@link Boolean},
 *  not a plain type like double or boolean.
 *
 *  <p>The command must allow concurrent access to its
 *  properties. The getter and setter should <code>synchronize</code>,
 *  or the property needs to be <code>volatile</code>.
 *
 *  <p>Supported property types:
 *  <ul>
 *  <li><code>Double</code> - Edited as number "5", "1e-20" etc.
 *  <li><code>String</code> - Edited as text
 *  <li><code>Boolean</code> - Edited as yes/no
 *  <li><code>String[]</code> - Edited as strings
 *  <li><code>DeviceInfo</code> - Edited as string, but editor suggests available device names
 *  <li><code>DeviceInfo[]</code> - Edited as strings, but editor suggests available device names
 *  <li><code>Enum</code> - Allows selection among the <code>toString()</code> values of the Enum
 *  <li><code>Object</code> - Edited as String, and if possible converted to Double
 *  </ul>
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
abstract public class ScanCommand
{
    /** Configurable properties of this command */
    final private List<ScanCommandProperty> properties;

    /** Address of this command within command sequence.
     *  <p>The {@link CommandSequence} assigns addresses 0, 1, 2, ...
     *  to all commands in the sequence to allow identification
     *  of each command while the sequence is executed.
     */
    private long address = -1;

    /** Error handler script name */
    private volatile String error_handler = "";

    /** Initialize */
    public ScanCommand()
    {
        final List<ScanCommandProperty> properties = new ArrayList<>();
        configureProperties(properties);
        this.properties = Collections.unmodifiableList(properties);
    }

    /** @return Name of the command, which is the base of the class name */
    final public String getCommandName()
    {
        final String name = getClass().getName();
        final int sep = name.lastIndexOf('.');
        return name.substring(sep + 1);
    }

    /** A command with implementing class "DoSomeThingCommand"
     *  has an ID of "do_some_thing"
     *  @return ID of this command
     */
    final public String getCommandID()
    {
        // Detected by Frederic Arnaud:
        // Java 7 will split "DoSomeThingCommand" into [ "", "Do", "Some", "Thing", "Command" ]
        // Java 8 will instead return  [  "Do", "Some", "Thing", "Command" ]
        final String[] sections = getCommandName().split("(?=[A-Z][a-z])");
        final int start = (sections.length > 0  &&  sections[0].isEmpty()) ? 1 : 0;
        final StringBuilder buf = new StringBuilder();
        final int N = sections.length;
        for (int i=start; i<N-1; ++i)
        {
            if (i > start)
                buf.append("_");
            buf.append(sections[i].toLowerCase());
        }
        return buf.toString();
    }

    /** Address of this command within the command sequence
     *
     *  <p>Addresses are only well-defined for the top-level
     *  command sequence.
     *  The <code>LoopCommand</code> will increment the addresses
     *  within its body, but the <code>IncludeCommand</code>
     *  will not be able to assign addresses for the included
     *  scan or possibly further sub-includes.
     *
     *  @return Address of this command or -1
     */
    final public long getAddress()
    {
        return address;
    }

    /** Set the address of this command.
     *
     *  <p>To be called by scan system, not end user code.
     *  Derived commands, i.e. custom commands that wrap a
     *  "body" of commands need to override and forward
     *  the address update to their embedded commands.
     *
     *  @param address Address of this command within command sequence
     *  @return Address of next command
     */
    public long setAddress(final long address)
    {
        this.address = address;
        return address+1;
    }

    /** Declare properties of this command
     *
     *  <p>Derived classes should add their properties and
     *  call the base implementation to declare inherited properties.
     *
     *  @param properties List to which to add properties
     */
    protected void configureProperties(final List<ScanCommandProperty> properties)
    {
        properties.add(
            new ScanCommandProperty("error_handler", "Error Handler", String.class));
    }

    /** @return Descriptions of Properties for this command */
    final public List<ScanCommandProperty> getProperties()
    {
        return properties;
    }

    /** @param property_id ID of a property
     *  @return Property description or <code>null</code> if property ID is not supported
     */
    final public ScanCommandProperty getPropertyDescription(final String property_id)
    {
        for (ScanCommandProperty property : getProperties())
            if (property.getID().equals(property_id))
                return property;
        return null;
    }

    /** Set a command's property
     *  @param property_id ID of the property to set
     *  @param value New value
     *  @throws UnknownScanCommandPropertyException when there is no property with that ID and value type
     */
    public void setProperty(final String property_id, final Object value) throws UnknownScanCommandPropertyException
    {
        for (ScanCommandProperty property : getProperties())
            if (property.getID().equals(property_id))
            {
                setProperty(property, value);
                return;
            }
        throw new UnknownScanCommandPropertyException("Unknown property ID " + property_id + " for " + getClass().getName());
    }

    /** Set a command's property
     *  @param property Property to set
     *  @param value New value
     *  @throws UnknownScanCommandPropertyException when there is no property with that ID and value type
     */
    public void setProperty(final ScanCommandProperty property, Object value) throws UnknownScanCommandPropertyException
    {
        final String meth_name = getMethodName("set", property.getID());

        // Type patching: DeviceInfo is read/set as String
        Class<?> type = property.getType();
        if (type == DeviceInfo.class)
            type = String.class;
        else if (type == DeviceInfo[].class)
            type = String[].class;

        // Try to adjust string if more specific type is required
        try
        {
            if (value instanceof String)
            {
                if (type == Double.class)
                    value = Double.parseDouble(value.toString());
                else if (type == Boolean.class)
                    value = Boolean.parseBoolean(value.toString());
                else if (type.isEnum())
                {
                    for (Object e : type.getEnumConstants())
                        if (e.toString().equals(value))
                        {
                            value = e;
                            break;
                        }
                }
            }
        }
        catch (Throwable ex)
        {
            throw new UnknownScanCommandPropertyException("Property ID " + property.getID() +
                    " requires value type " + type.getName() + " but received " + value.getClass().getName() + " for " + getClass().getName());
        }

        try
        {
            final Method method = getClass().getMethod(meth_name, type);
            method.invoke(this, value);
        }
        catch (Throwable ex)
        {   // Expect Exception or RuntimeException, but FindBugs complained about using Exception,
            // so using Throwable
            throw new UnknownScanCommandPropertyException("Unknown property ID " + property.getID() +
                    " with value type " + value.getClass().getName() + " for " + getClass().getName());
        }
    }

    /** Get a command's property
     *  @param property_id ID of the property to set
     *  @return Value
     *  @throws UnknownScanCommandPropertyException when there is no property with that ID and value type
     */
    public Object getProperty(final String property_id) throws UnknownScanCommandPropertyException
    {
        for (ScanCommandProperty property : getProperties())
            if (property.getID().equals(property_id))
                return getProperty(property);
        throw new UnknownScanCommandPropertyException("Unknown property ID " + property_id + " for " + getClass().getName());
    }

    /** Get a command's property
     *  @return Value
     *  @throws UnknownScanCommandPropertyException when there is no property with that ID and value type
     */
    public Object getProperty(final ScanCommandProperty property) throws UnknownScanCommandPropertyException
    {
        final String meth_name = getMethodName("get", property.getID());
        try
        {
            final Method method = getClass().getMethod(meth_name);
            return method.invoke(this);
        }
        catch (Exception ex)
        {
            throw new UnknownScanCommandPropertyException("Unknown property ID " + property.getID() + " for " + getClass().getName());
        }
    }

    /** Construct method name
     *  @param get_set Method prefix "get" or "set"
     *  @param property_id Property ID like "device_name"
     *  @return Method name like "setDeviceName"
     */
    private String getMethodName(final String get_set, final String property_id)
    {
        final String[] sections = property_id.split("_");
        final StringBuilder result = new StringBuilder(get_set);
        for (String sec : sections)
        {
            result.append(sec.substring(0, 1).toUpperCase());
            result.append(sec.substring(1));
        }
        return result.toString();
    }

    /** @return Name of error handler class */
    public String getErrorHandler()
    {
        return error_handler;
    }

    /** @param error_handler Name of the error handler class */
    public void setErrorHandler(final String error_handler)
    {
        this.error_handler = error_handler;
    }

    /** Write the command (and its sub-commands) to XML document.
     *
     *  <p>A command called AbcCommand writes itself as a tag "abc"
     *  so that the {@link XMLCommandReader} can later determine
     *  which class to use for reading the command back from XML.
     *
     *  <p>This method creates the overall XML element for the command
     *  and calls <code>addXMLElements()</code> for the content.
     *  Derived classes should update <code>addXMLElements()</code>.
     *
     *  @param dom {@link Document}
     *  @param root Where to add content for this command
     *  @see ScanCommand#addXMLElements(Document, Element)
     */
    final public void writeXML(final Document dom, final Element root)
    {
        final Element command_element = dom.createElement(getCommandID());
        root.appendChild(command_element);
        addXMLElements(dom, command_element);
    }

    /** Add XML elements for command (and its sub-commands) to document.
     *
     *  <p>Derived classes should call parent implementation.
     *
     *  @param dom {@link Document}
     *  @param command_element DOM {@link Element} for this command
     */
    public void addXMLElements(final Document dom, final Element command_element)
    {
        if (! error_handler.isEmpty())
        {
            final Element element = dom.createElement("error_handler");
            element.appendChild(dom.createTextNode(error_handler));
            command_element.appendChild(element);
        }
    }

    /** Read command parameters from XML element
     *
     *  <p>Derived classes must call base class implementation
     *  to read inherited properties.
     *
     *  @param element
     *  @throws Exception on error, for example missing essential data
     */
    public void readXML(final Element element) throws Exception
    {
        setErrorHandler(XMLUtil.getChildString(element, "error_handler").orElse(""));
    }

    /** @return Debug representation.
     *          Derived classes should provide their command name with properties
     */
    @Override
    public String toString()
    {
        return getClass().getName();
    }
}

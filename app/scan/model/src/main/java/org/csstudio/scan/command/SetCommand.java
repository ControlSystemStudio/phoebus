/*******************************************************************************
 * Copyright (c) 2011-2020 Oak Ridge National Laboratory.
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

import java.util.List;

import org.csstudio.scan.util.StringOrDouble;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Command that sets a device to a value
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SetCommand extends ScanCommand
{
    private volatile String device_name;
    private volatile Object value;
    private volatile String readback;
    private volatile Object readback_value;
    private volatile boolean completion;
    private volatile boolean wait;
    private volatile double tolerance;
    private volatile double timeout;

    /** Initialize empty set command */
    public SetCommand()
    {
        this("device", 0.0, false, "", "", true, 0.1, 0.0);
    }

    /** Initialize for readback with default tolerance and timeout
     *  @param device_name Name of device and readback
     *  @param value Value to write to the device
     */
    public SetCommand(final String device_name, final Object value)
    {
        this(device_name, value, false, device_name, "", true, 0.1, 0.0);
    }

    /** Initialize with default tolerance and timeout
     *  @param device_name Name of device and readback (if used)
     *  @param value Value to write to the device
     *  @param wait Wait for readback to match?
     */
    public SetCommand(final String device_name, final Object value, final boolean wait)
    {
        this(device_name, value, false, device_name, "", wait, 0.1, 0.0);
    }

    /** Initialize
     *  @param device_name Name of device
     *  @param value Value to write to the device
     *  @param readback Readback device
     */
    public SetCommand(final String device_name, final Object value,
            final String readback)
    {
        this(device_name, value, false, readback, "", true, 0.1, 0.0);
    }

    /** Initialize
     *  @param device_name Name of device
     *  @param value Value to write to the device
     *  @param completion Wait for write completion?
     *  @param readback Readback device
     *  @param readback_value Desired readback value ("" to use written value)
     *  @param wait Wait for readback to match?
     *  @param tolerance Numeric tolerance when checking value
     *  @param timeout Timeout in seconds, 0 as "forever"
     */
    public SetCommand(final String device_name, final Object value,
            final boolean completion,
            final String readback, final Object readback_value, final boolean wait,
            final double tolerance, final double timeout)
    {
        if (device_name == null)
            throw new NullPointerException();
        this.device_name = device_name;
        this.value = value;
        this.completion = completion;
        if (readback == null)
            throw new NullPointerException();
        this.readback = readback;
        this.readback_value = readback_value;
        this.wait = wait;
        this.tolerance = tolerance;
        this.timeout = timeout;
    }

    /** {@inheritDoc} */
    @Override
    protected void configureProperties(final List<ScanCommandProperty> properties)
    {
        properties.add(ScanCommandProperty.DEVICE_NAME);
        properties.add(new ScanCommandProperty("value", "Value", Object.class));
        properties.add(ScanCommandProperty.COMPLETION);
        properties.add(ScanCommandProperty.WAIT);
        properties.add(ScanCommandProperty.READBACK);
        properties.add(new ScanCommandProperty("readback_value", "Readback Value", Object.class));
        properties.add(ScanCommandProperty.TOLERANCE);
        properties.add(ScanCommandProperty.TIMEOUT);
        super.configureProperties(properties);
    }

    /** @return Name of device to set (may be "" but not <code>null</code>) */
    public String getDeviceName()
    {
        return device_name;
    }

    /** @param device_name Name of device */
    public void setDeviceName(final String device_name)
    {
        if (device_name == null)
            throw new NullPointerException();
        this.device_name = device_name;
    }

    /** @return Value to write to device */
    public Object getValue()
    {
        return value;
    }

    /** @param value Value to write to the device */
    public void setValue(final Object value)
    {
        this.value = value;
    }

    /** @return Wait for write completion? */
    public boolean getCompletion()
    {
        return completion;
    }

    /** @param wait Wait for write completion? */
    public void setCompletion(final Boolean completion)
    {
        this.completion = completion;
    }

    /** @return Wait for readback to match? */
    public boolean getWait()
    {
        return wait;
    }

    /** @param wait Wait for readback to match? */
    public void setWait(final Boolean wait)
    {
        this.wait = wait;
    }

    /** @return Name of readback device (may be "" but not <code>null</code>) */
    public String getReadback()
    {
        return readback;
    }

    /** @param readback Name of readback device */
    public void setReadback(final String readback)
    {
        if (device_name == null)
            throw new NullPointerException();
        this.readback = readback;
    }

    /** @return Value to check from readback device */
    public Object getReadbackValue()
    {
        return readback_value;
    }

    /** @param readback_value Value to check from readback device ("" for written value) */
    public void setReadbackValue(final Object readback_value)
    {
        this.readback_value = readback_value;
    }

    /** @return Tolerance */
    public double getTolerance()
    {
        return tolerance;
    }

    /** @param tolerance Tolerance */
    public void setTolerance(final Double tolerance)
    {
        this.tolerance = Math.max(0.0, tolerance);
    }

    /** @return Timeout in seconds */
    public double getTimeout()
    {
        return timeout;
    }

    /** @param timeout Time out in seconds */
    public void setTimeout(final Double timeout)
    {
        this.timeout = Math.max(0.0, timeout);
    }

    /** {@inheritDoc} */
    @Override
    public void addXMLElements(final Document dom, final Element command_element)
    {
        Element element = dom.createElement("device");
        element.appendChild(dom.createTextNode(device_name));
        command_element.appendChild(element);

        element = dom.createElement("value");
        if (value instanceof String)
            element.appendChild(dom.createTextNode('"' + (String)value + '"'));
        else
            element.appendChild(dom.createTextNode(value.toString()));
        command_element.appendChild(element);

        if (completion)
        {
            element = dom.createElement("completion");
            element.appendChild(dom.createTextNode(Boolean.toString(completion)));
            command_element.appendChild(element);
        }
        if (! wait)
        {
            element = dom.createElement("wait");
            element.appendChild(dom.createTextNode(Boolean.toString(wait)));
            command_element.appendChild(element);
        }
        else
        {
            if (! readback.isEmpty())
            {
                element = dom.createElement("readback");
                element.appendChild(dom.createTextNode(readback));
                command_element.appendChild(element);
            }

            if (readback_value instanceof String)
            {   // Skip when empty, otherwise persist as "text" ..
                if (((String)readback_value).length() > 0)
                {
                    element = dom.createElement("readback_value");
                    element.appendChild(dom.createTextNode('"' + (String)readback_value + '"'));
                    command_element.appendChild(element);
                }
            }
            else
            {   // .. or number
                element = dom.createElement("readback_value");
                element.appendChild(dom.createTextNode(readback_value.toString()));
                command_element.appendChild(element);
           }
        }
        if (tolerance > 0.0)
        {
            element = dom.createElement("tolerance");
            element.appendChild(dom.createTextNode(Double.toString(tolerance)));
            command_element.appendChild(element);
        }
        if (timeout > 0.0)
        {
            element = dom.createElement("timeout");
            element.appendChild(dom.createTextNode(Double.toString(timeout)));
            command_element.appendChild(element);
        }
        super.addXMLElements(dom, command_element);
    }

    /** {@inheritDoc} */
    @Override
    public void readXML(final Element element) throws Exception
    {
        setDeviceName(XMLUtil.getChildString(element, ScanCommandProperty.TAG_DEVICE).orElse(""));
        setValue(StringOrDouble.parse(XMLUtil.getChildString(element, ScanCommandProperty.TAG_VALUE).orElse("0")));
        setCompletion(XMLUtil.getChildBoolean(element, ScanCommandProperty.TAG_COMPLETION).orElse(false));
        setWait(XMLUtil.getChildBoolean(element, ScanCommandProperty.TAG_WAIT).orElse(true));
        setReadback(XMLUtil.getChildString(element, ScanCommandProperty.TAG_READBACK).orElse(getDeviceName()));
        setReadbackValue(StringOrDouble.parse(XMLUtil.getChildString(element, ScanCommandProperty.TAG_READBACK_VALUE).orElse("\"\"")));
        setTolerance(XMLUtil.getChildDouble(element, ScanCommandProperty.TAG_TOLERANCE).orElse(0.1));
        setTimeout(XMLUtil.getChildDouble(element, ScanCommandProperty.TAG_TIMEOUT).orElse(0.0));
        super.readXML(element);
    }

    /** @param buf If the set command uses a condition,
     *             information about it will be appended to string builder
     */
    public void appendConditionDetail(final StringBuilder buf)
    {
        if (completion)
        {
            buf.append(" with completion");
            if (timeout > 0)
                buf.append(" in ").append(timeout).append(" sec");
        }
        if (wait)
        {
            if (completion)
                buf.append(" (check for '");
            else
                buf.append(" (wait for '");
            if (readback.isEmpty())
                buf.append(device_name);
            else
                buf.append(readback);
            buf.append("'");

            if (! readback_value.toString().isEmpty())
                buf.append(" = ").append(StringOrDouble.quote(readback_value));
            if (tolerance > 0)
                buf.append(" +-").append(tolerance);
            if (timeout > 0  &&  !completion)
                buf.append(", ").append(timeout).append(" sec timeout");
            buf.append(")");
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append("Set '").append(device_name).append("' = ");
        buf.append(StringOrDouble.quote(value));
        appendConditionDetail(buf);
        return buf.toString();
    }
}

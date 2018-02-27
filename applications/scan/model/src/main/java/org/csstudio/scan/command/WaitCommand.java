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

import java.util.List;

import org.csstudio.scan.util.StringOrDouble;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Command that delays the scan until a device reaches a certain value
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WaitCommand extends ScanCommand
{
    private volatile String device_name;
    private volatile Comparison comparison;
    private volatile Object desired_value;
    private volatile double tolerance;
    private volatile double timeout;

    /** Initialize empty wait command */
    public WaitCommand()
    {
        this("device", Comparison.EQUALS, 0.0, 0.1, 0.0);
    }

    /** Initialize with default tolerance and no timeout
     *  @param device_name Name of device to check
     *  @param desired_value Desired value of the device for equality comparison
     */
    public WaitCommand(final String device_name,
            final Object desired_value)
    {
        this(device_name, Comparison.EQUALS, desired_value, 0.1, 0.0);
    }

    /** Initialize with default tolerance and no timeout
     *  @param device_name Name of device to check
     *  @param comparison Comparison to use
     *  @param desired_value Desired value of the device
     */
    public WaitCommand(final String device_name,
            final Comparison comparison, final Object desired_value)
    {
        this(device_name, comparison, desired_value, 0.1, 0.0);
    }

    /** Initialize
     *  @param device_name Name of device to check
     *  @param comparison Comparison to use
     *  @param desired_value Desired value of the device
     *  @param tolerance Numeric tolerance when checking value
     *  @param timeout Timeout in seconds, 0 as "forever"
     */
    public WaitCommand(final String device_name,
            final Comparison comparison, final Object desired_value,
            final double tolerance, final double timeout)
    {
        if (device_name == null)
            throw new NullPointerException();
        this.device_name = device_name;
        this.desired_value = desired_value;
        this.comparison = comparison;
        this.tolerance = tolerance;
        this.timeout = timeout;
    }

    /** {@inheritDoc} */
    @Override
    protected void configureProperties(final List<ScanCommandProperty> properties)
    {
        properties.add(ScanCommandProperty.DEVICE_NAME);
        properties.add(new ScanCommandProperty("comparison", "Comparison", Comparison.class));
        properties.add(new ScanCommandProperty("desired_value", "Desired Value", Object.class));
        properties.add(ScanCommandProperty.TOLERANCE);
        properties.add(ScanCommandProperty.TIMEOUT);
        super.configureProperties(properties);
    }

    /** @return Device name (may be "" but not <code>null</code>) */
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

    /** @return Desired value */
    public Object getDesiredValue()
    {
        return desired_value;
    }

    /** @param desired_value Desired value */
    public void setDesiredValue(final Object desired_value)
    {
        this.desired_value = desired_value;
    }

    /** @return Desired comparison */
    public Comparison getComparison()
    {
        return comparison;
    }

    /** @param comparison Desired comparison */
    public void setComparison(final Comparison comparison)
    {
        this.comparison = comparison;
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
        if (desired_value instanceof String)
            element.appendChild(dom.createTextNode('"' + (String)desired_value + '"'));
        else
            element.appendChild(dom.createTextNode(desired_value.toString()));
        command_element.appendChild(element);

        element = dom.createElement("comparison");
        element.appendChild(dom.createTextNode(comparison.name()));
        command_element.appendChild(element);

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
        setDesiredValue(StringOrDouble.parse(XMLUtil.getChildString(element, ScanCommandProperty.TAG_VALUE).orElse("0")));
        try
        {
            setComparison(Comparison.valueOf(XMLUtil.getChildString(element, "comparison").orElse(Comparison.EQUALS.toString())));
        }
        catch (Throwable ex)
        {
            setComparison(Comparison.EQUALS);
        }
        setTolerance(XMLUtil.getChildDouble(element, ScanCommandProperty.TAG_TOLERANCE).orElse(0.1));
        setTimeout(XMLUtil.getChildDouble(element, ScanCommandProperty.TAG_TIMEOUT).orElse(0.0));
        super.readXML(element);
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append("Wait for '").append(device_name).append("' ").append(comparison).append(" ");
        buf.append(StringOrDouble.quote(desired_value));
        if (comparison == Comparison.EQUALS)
            buf.append(" (+-").append(tolerance).append(")");
        if (timeout > 0)
            buf.append(" (").append(timeout).append(" sec timeout)");
        return buf.toString();
    }
}

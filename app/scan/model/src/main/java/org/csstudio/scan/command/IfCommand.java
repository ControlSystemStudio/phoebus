/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.csstudio.scan.util.StringOrDouble;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Command that conditionally executes a body
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class IfCommand extends ScanCommandWithBody
{
    private volatile String device_name;
    private volatile Comparison comparison;
    private volatile Object desired_value;
    private volatile double tolerance = 0.1;

    /** Initialize empty 'if' */
    public IfCommand()
    {
        this("device", Comparison.EQUALS, 0.0, Collections.emptyList());
    }

    /** Initialize
     *  @param device_name Name of device to check
     *  @param comparison Comparison to use
     *  @param desired_value Desired value of the device
     *  @param body Commands to execute when condition is met
     */
    public IfCommand(final String device_name,
                     final Comparison comparison, final Object desired_value,
                     final List<ScanCommand> body)
    {
        super(body);
        this.device_name = Objects.requireNonNull(device_name);
        this.desired_value = desired_value;
        this.comparison = comparison;
    }

    /** {@inheritDoc} */
    @Override
    protected void configureProperties(final List<ScanCommandProperty> properties)
    {
        properties.add(ScanCommandProperty.DEVICE_NAME);
        properties.add(new ScanCommandProperty("comparison", "Comparison", Comparison.class));
        properties.add(new ScanCommandProperty("desired_value", "Desired Value", Object.class));
        properties.add(ScanCommandProperty.TOLERANCE);
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
        super.addXMLElements(dom, command_element);
    }

    /** {@inheritDoc} */
    @Override
    public void readXML(final Element element) throws Exception
    {
        // Read body first, so we don't update other params if this fails
        super.readXML(element);

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
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append("If '").append(device_name).append("' ")
           .append(comparison).append(" ");
        buf.append(StringOrDouble.quote(desired_value));
        if (comparison == Comparison.EQUALS)
            buf.append(" (+-").append(tolerance).append(")");
        return buf.toString();
    }
}

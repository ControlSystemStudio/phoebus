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
import java.util.Objects;

import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Command that performs a loop
 *
 *  <p>The loop steps from a start to an end value
 *  by some step size, for example 1 to 5 by 1: 1, 2, 3, 4, 5.
 *
 *  <p>It stops at the end value, for example 1 to 6 by 2: 1, 3, 5
 *
 *  <p>When the start is larger than the end and the step size is negative,
 *  it will ramp down,
 *  for example from 5 to 1 by -1: 5, 4, 3, 2, 1.
 *
 *  <p>When the order of start and end does not match the step direction,
 *  for example the start is smaller than the end,
 *  but the step is negative,
 *  this enables a 'reverse' toggle:
 *  The direction of the loop will change every time it is executed.
 *
 *  <p>The loop checks if the device actually reaches the desired value
 *  with a timeout.
 *  By default, the read-back uses the device that the loop writes,
 *  but alternate read-back device can be configured.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class LoopCommand extends ScanCommandWithBody
{
    private volatile String device_name;
    private volatile double start;
    private volatile double end;
    private volatile double stepsize;
    private volatile String readback = "";
    private volatile boolean completion = false;
    private volatile boolean wait = true;
    private volatile double tolerance = 0.1;
    private volatile double timeout = 0.0;

    /** Initialize empty loop */
    public LoopCommand()
    {
        this("device", 0, 10, 1, new ScanCommand[0]);
    }

    /** Initialize with single command
     *
     *  <p>This constructor simplifies invocation from Matlab.
     *  In principle, the "ScanCommand... body" constructor
     *  handles loops with zero, one, many commands, i.e. all cases.
     *  Matlab, however, turns a single-element array into a scalar
     *  in a way incompatible with the var-length argument
     *  constructor.
     *
     *  @param device_name Device to update with the loop variable
     *  @param start Initial loop value
     *  @param end Final loop value
     *  @param stepsize Increment of the loop variable
     *  @param command Single-command body
     */
    public LoopCommand(final String device_name, final double start,
            final double end, final double stepsize,
            final ScanCommand command)
    {
        this(device_name, start, end, stepsize, new ScanCommand[] { command });
    }

    /** Initialize
     *  @param device_name Device to update with the loop variable
     *  @param start Initial loop value
     *  @param end Final loop value
     *  @param stepsize Increment of the loop variable
     *  @param body Optional loop body commands
     */
    public LoopCommand(final String device_name, final double start,
            final double end, final double stepsize,
            final ScanCommand... body)
    {
        this(device_name, start, end, stepsize, toList(body));
    }

    /** Initialize
     *  @param device_name Device to update with the loop variable
     *  @param start Initial loop value
     *  @param end Final loop value
     *  @param stepsize Increment of the loop variable
     *  @param body Loop body commands
     */
    public LoopCommand(final String device_name, final double start,
            final double end, final double stepsize,
            final List<ScanCommand> body)
    {
        super(body);
        this.device_name = Objects.requireNonNull(device_name);
        setStepSize(stepsize);
        this.start = start;
        this.end = end;
    }

    /** {@inheritDoc} */
    @Override
    protected void configureProperties(final List<ScanCommandProperty> properties)
    {
        properties.add(ScanCommandProperty.DEVICE_NAME);
        properties.add(new ScanCommandProperty("start", "Initial Value", Double.class));
        properties.add(new ScanCommandProperty("end", "Final Value", Double.class));
        properties.add(new ScanCommandProperty("step_size", "Step Size", Double.class));
        properties.add(ScanCommandProperty.COMPLETION);
        properties.add(ScanCommandProperty.WAIT);
        properties.add(ScanCommandProperty.READBACK);
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

    /** @return Loop start value */
    public double getStart()
    {
        return start;
    }

    /** @param start Initial loop value */
    public void setStart(final Double start)
    {
        this.start = start;
    }

    /** @return Loop end value */
    public double getEnd()
    {
        return end;
    }

    /** @param end Final loop value */
    public void setEnd(final Double end)
    {
        this.end = end;
    }

    /** @return Loop step size */
    public double getStepSize()
    {
        return stepsize;
    }

    /** @param stepsize Increment of the loop variable */
    public void setStepSize(final Double stepsize)
    {
        if (stepsize != 0.0)
            this.stepsize = stepsize;
        else
            this.stepsize = 1.0;
        // Use fraction of stepsize for tolerance
        tolerance = Math.abs(this.stepsize / 10.0);
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

    /** @return Name of readback device
     *          (may be "" to use the primary device of the loop,
     *           but not <code>null</code>)
     */
    public String getReadback()
    {
        return readback;
    }

    /** @param readback Name of readback device */
    public void setReadback(final String readback)
    {
        if (readback == null)
            throw new NullPointerException();
        this.readback = readback;
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

        element = dom.createElement("start");
        element.appendChild(dom.createTextNode(Double.toString(start)));
        command_element.appendChild(element);

        element = dom.createElement("end");
        element.appendChild(dom.createTextNode(Double.toString(end)));
        command_element.appendChild(element);

        element = dom.createElement("step");
        element.appendChild(dom.createTextNode(Double.toString(stepsize)));
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
        if (wait  &&  ! readback.isEmpty())
        {
            element = dom.createElement("readback");
            element.appendChild(dom.createTextNode(readback));
            command_element.appendChild(element);
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
        // Read body first, so we don't update other loop params if this fails
        super.readXML(element);

        setDeviceName(XMLUtil.getChildString(element, ScanCommandProperty.TAG_DEVICE).orElse(""));
        setStart(XMLUtil.getChildDouble(element, "start").orElse(0.0));
        setEnd(XMLUtil.getChildDouble(element, "end").orElse(10.0));
        setStepSize(XMLUtil.getChildDouble(element, "step").orElse(1.0));
        setCompletion(XMLUtil.getChildBoolean(element, ScanCommandProperty.TAG_COMPLETION).orElse(false));
        setWait(XMLUtil.getChildBoolean(element, ScanCommandProperty.TAG_WAIT).orElse(true));
        setReadback(XMLUtil.getChildString(element, ScanCommandProperty.TAG_READBACK).orElse(""));
        setTolerance(XMLUtil.getChildDouble(element, ScanCommandProperty.TAG_TOLERANCE).orElse(0.1));
        setTimeout(XMLUtil.getChildDouble(element, ScanCommandProperty.TAG_TIMEOUT).orElse(0.0));
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
            if (tolerance > 0)
                buf.append("' +-").append(tolerance);
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
        buf.append("Loop '").append(device_name).append("' = ")
            .append(start).append(" ... ").append(end).append(", step ").append(stepsize);
        appendConditionDetail(buf);
        return buf.toString();
    }
}

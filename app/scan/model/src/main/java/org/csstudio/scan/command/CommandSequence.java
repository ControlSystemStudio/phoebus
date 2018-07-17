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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** Helper for creating a sequence of commands
 *
 *  <p>Used by the client to build a sequence of commands
 *  which is then submitted to the server for execution as a "scan".
 *
 *  <p>Allows both <code>List&lt;Command></code> and plain <code>Command[]</code>.
 *  For Java code collections like <code>List</code>
 *  are most convenient. Jython nicely converts existing collections
 *  into a python list, but it is harder to create collections
 *  from a python list, so arrays are also accepted.
 *
 *  @author Kay Kasemir
 */
public class CommandSequence
{
    final private List<ScanCommand> commands;

    /** Initialize with a command.
     *
     *     <p>This constructor simplifies invocation from Matlab.
     *  In principle, the "ScanCommand..." constructor
     *  handles zero, one, many commands, i.e. all cases.
     *  Matlab, however, turns a single-element array into a scalar
     *  in a way incompatible with the var-length argument
     *  constructor.
     *
     *  @param commands Sequence of commands
     */
    public CommandSequence(final ScanCommand commands)
    {
        this.commands = Arrays.asList(commands);
    }

    /** Initialize with a given sequence of commands.
     *  @param commands Sequence of commands
     */
    public CommandSequence(final ScanCommand... commands)
    {
        this.commands = Arrays.asList(commands);
    }

    /** Initialize with a given sequence of commands.
     *  @param commands Sequence of commands
     */
    public CommandSequence(final Collection<ScanCommand> commands)
    {
        this.commands = new ArrayList<ScanCommand>();
        this.commands.addAll(commands);
    }

    /** Assign consecutive addresses
     *  @param commands Commands where addresses need to be set
     */
    public static void setAddresses(final List<ScanCommand> commands)
    {
        long next = 0;
        for (ScanCommand command : commands)
            next = command.setAddress(next);
    }

    // Note: This was called 'print' which causes warnings in a PyDev python
    //       editor because print is a reserved word in python
    /** Print current command sequence */
    public void dump()
    {
        try
        {
            XMLCommandWriter.write(System.out, getCommands());
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    /** @return List of commands in the sequence */
    public List<ScanCommand> getCommands()
    {
        setAddresses(commands);
        return commands;
    }

    /** @return Commands formatted as XML text
     *  @throws Exception on error
     */
    public String getXML() throws Exception
    {
        return XMLCommandWriter.toXMLString(getCommands());
    }

    /** @return Debug representation */
    @Override
    public String toString()
    {
        return commands.toString();
    }
}

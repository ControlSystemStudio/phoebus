/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.command;

/** Base class for Jython scripts that are executed by a {@link ScriptCommand}
 *
 *  <p>Example:
 *  <pre>
 *  from org.csstudio.scan.command import ScanScript
 *
 *  class MyScript(ScanScript):
 *      def __init__(self, args):
 *          pass
 *
 *      def getDeviceNames(self):
 *         return [ "result1", "result2" ]
 *
 *      def run(self, context):
 *          [x, y] = context.getData("xpos", "signal")
 *          context.write("result1", x[0])
 *          context.write("result2", y[0])
 *  </pre>
 *
 *  <p>The jython script that defines the class <code>MyScript</code> must
 *  be stored in file named <code>myscript.py</code>,
 *  i.e. using the lower case version of the class name.
 *
 *  <p>All script classes must derive from {@link ScanScript}.
 *  @author Kay Kasemir
 */
public class ScanScript
{
    /** Scan scripts may have arguments,
     *  and their constructor will be called with those arguments.
     *
     *  <p>The default implementation has no arguments.
     */
    public ScanScript()
    {
        // Nop
    }

    /** Scan scripts that intend to access devices
     *  need to list their names.
     *  This allows the scan system to connect to the
     *  required devices before starting the scan.
     *
     *  <p>The default implementation returns an
     *  empty list, meaning that the script cannot
     *  access any 'live' devices.
     *  Only data that the scan has logged by the time
     *  when the script is invoked will be available.
     *
     *  @return Device (alias) names used by the script
     */
    public String[] getDeviceNames()
    {
        return new String[0];
    }

    /** The <code>run</code> method is invoked by the scan server
     *  to execute the script
     *  @param context Access to logged data, devices, ...
     */
    public void run(final ScanScriptContext context)
    {
        // Default doesn't do anything
        // Script can read log data, perform computation,
        // write result to devices
    }
}

/*******************************************************************************
 * Copyright (c) 2013 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.command;

/** Base class for Jython scripts that handle scan errors
 *
 *  <p>Example:
 *  <pre>
 *  class MyErrorHandler(ScanErrorHandler):
 *   def handleError(self, command, error, context):
 *       return ScanErrorHandler.Result.Continue
 *  </pre>
 *
 *  <p>The jython script that defines the class <code>MyErrorHandler</code> must
 *  be stored in file named <code>myerrorhandler.py</code>,
 *  i.e. using the lower case version of the class name.
 *
 *  <p>All script classes must derive from {@link ScanErrorHandler}.
 *  @author Kay Kasemir
 */
public class ScanErrorHandler
{
    /** How to proceed when a command generates an error */
    public enum Result
    {
        /** Abort the scan (default behavior) */
        Abort,
        /** Continue with the next command, ignore the error */
        Continue,
        /** Re-try the command */
        Retry;
    }

    /** Invoked by the scan server when command generated an error
     *
     *  <p>Code within this method may inspect the command that generated
     *  an error as well as details of that error.
     *
     *  <p>It returns a code that instructs the scan server to either
     *  ignore the error or abort the scan.
     *
     *  @param command Command that created the error
     *  @param error Error created by command
     *  @param context Access to logged data, devices, ...
     */
    public Result handleError(final ScanCommand command, final Exception error, final ScanScriptContext context)
    {
        // Default will abort the scan.
        return Result.Abort;
    }
}

/*******************************************************************************
 * Copyright (c) 2015 Oak Ridge National Laboratory.
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

/** Command that executes commands it its body
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SequenceCommand extends ScanCommandWithBody
{
    /** Initialize with empty body */
    public SequenceCommand()
    {
        super(Collections.emptyList());
    }

    /** Initialize
     *  @param body Body commands, may be empty
     */
    public SequenceCommand(final ScanCommand... body)
    {
        super(toList(body));
    }

    /** Initialize
     *  @param body Body commands
     */
    public SequenceCommand(final List<ScanCommand> body)
    {
        super(body);
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return "Sequence";
    }
}

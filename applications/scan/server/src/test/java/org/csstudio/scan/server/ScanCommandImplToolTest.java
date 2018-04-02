/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
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
package org.csstudio.scan.server;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.csstudio.scan.command.CommentCommand;
import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.server.command.CommentCommandImpl;
import org.junit.Test;

@SuppressWarnings("nls")
public class ScanCommandImplToolTest
{
    @Test
    public void testImplementation() throws Exception
    {
        final ScanCommand cmd = new CommentCommand("Test");
        final ScanCommandImpl<ScanCommand> impl = ScanCommandImplTool.implement(cmd, null);
        System.out.println(impl);
        assertThat(impl, instanceOf(CommentCommandImpl.class));
    }
}

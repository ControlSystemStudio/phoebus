/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.workbench;

import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Test;

/** JUnit test/demo of {@link Locations}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class LocationsTest
{
    @Test
    public void testLocations()
    {
        Locations.initialize();

        File install = Locations.install();
        System.out.println("Install location: " + install);
        System.out.println("User location   : " + Locations.user());

        // During development in IDE, 'install' is the root dir of the source repository
        File check = new File(install, "core/framework/src");
        if (check.exists()  &&  check.isDirectory())
            System.out.println("==> Development setup, found core/framework/src");
        else
            fail("Cannot decode 'install' location " + install);
    }
}

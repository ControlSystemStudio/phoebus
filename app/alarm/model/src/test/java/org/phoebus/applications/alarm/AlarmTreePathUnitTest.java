/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.alarm;

import org.junit.jupiter.api.Test;
import org.phoebus.applications.alarm.model.AlarmTreePath;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/** JUnit test of AlarmTreePath
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmTreePathUnitTest
{
    @Test
    public void testPathCheck()
    {
        assertThat(AlarmTreePath.makePath(null, "root"), equalTo("/root"));
        assertThat(AlarmTreePath.makePath(null, "/root"), equalTo("/root"));
        assertThat(AlarmTreePath.makePath("/", "/root"), equalTo("/root"));
        assertThat(AlarmTreePath.isPath("/path/to/some/pv"), equalTo(true));
        assertThat(AlarmTreePath.getName("/path/to/some/pv"), equalTo("pv"));
        assertThat(AlarmTreePath.isPath("some_pv"), equalTo(false));
        assertThat(AlarmTreePath.isPath("sim:\\/\\/sine"), equalTo(false));
        assertThat(AlarmTreePath.getName("sim:\\/\\/sine"), equalTo("sim://sine"));
    }

    @Test
    public void testMakePath()
    {
        // Split
        String[] path = AlarmTreePath.splitPath("/path/to/some/pv");
        assertThat(path.length, equalTo(4));
        assertThat(path[1], equalTo("to"));

        path = AlarmTreePath.splitPath("///path//to///some//pv");
        assertThat(path.length, equalTo(4));
        assertThat(path[1], equalTo("to"));


        // Sub-path
        final String new_path = AlarmTreePath.makePath(path, 2);
        assertThat(new_path, equalTo("/path/to"));

        // New PV
        assertThat(AlarmTreePath.makePath(new_path, "another"),
                equalTo("/path/to/another"));
    }

    @Test
    public void testSpaces()
    {
        String path = AlarmTreePath.makePath("the path", "to");
        assertThat(path, equalTo("/the path/to"));

        path = AlarmTreePath.makePath(path, "an item");
        assertThat(path, equalTo("/the path/to/an item"));

        path = AlarmTreePath.makePath(path, "with / in it");
        assertThat(path, equalTo("/the path/to/an item/with \\/ in it"));

        // Split
        final String[] items = AlarmTreePath.splitPath(path);
        assertThat(items.length, equalTo(4));
        assertThat(items[0], equalTo("the path"));
        assertThat(items[1], equalTo("to"));
        assertThat(items[2], equalTo("an item"));
        assertThat(items[3], equalTo("with / in it"));

        // Re-assemble
        path = AlarmTreePath.makePath(items, items.length);
        assertThat(path, equalTo("/the path/to/an item/with \\/ in it"));
    }

    @Test
    public void testSpecialChars()
    {
        String path = AlarmTreePath.makePath("path", "to");
        assertThat(path, equalTo("/path/to"));

        // First element already contains '/'
        path = AlarmTreePath.makePath("/path", "to");
        assertThat(path, equalTo("/path/to"));

        path = AlarmTreePath.makePath(path, "sim://sine");
        // String is really "/path/to/sim:\/\/sine",
        // but to get the '\' into the string,
        // it itself needs to be escaped
        assertThat(path, equalTo("/path/to/sim:\\/\\/sine"));

        // Split
        final String[] items = AlarmTreePath.splitPath(path);
        assertThat(items.length, equalTo(3));
        assertThat(items[0], equalTo("path"));
        assertThat(items[1], equalTo("to"));
        assertThat(items[2], equalTo("sim://sine"));

        // Re-assemble
        path = AlarmTreePath.makePath(items, items.length);
        assertThat(path, equalTo("/path/to/sim:\\/\\/sine"));
    }

    @Test
    public void testPathUpdate()
    {
        String path = AlarmTreePath.makePath("path", "to");
        assertThat(path, equalTo("/path/to"));

        path = AlarmTreePath.update(path, "sub");
        assertThat(path, equalTo("/path/to/sub"));

        path = AlarmTreePath.update(path, "..");
        assertThat(path, equalTo("/path/to"));

        path = AlarmTreePath.update(path, "/new/path");
        assertThat(path, equalTo("/new/path"));

        path = AlarmTreePath.update(null, "/path");
        assertThat(path, equalTo("/path"));

        path = AlarmTreePath.update(null, null);
        assertThat(path, equalTo("/"));

        path = AlarmTreePath.update("/", "..");
        assertThat(path, equalTo("/"));

        path = AlarmTreePath.update("/", "path");
        assertThat(path, equalTo("/path"));

        path = AlarmTreePath.update("/", "path/to/sub");
        assertThat(path, equalTo("/path/to/sub"));

        path = AlarmTreePath.update("/", "path/to\\/sub");
        assertThat(path, equalTo("/path/to\\/sub"));
    }
}
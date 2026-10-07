/*******************************************************************************
 * Copyright (c) 2017-2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.util;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** JUnit test of the {@link ModelResourceUtil}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ModelResourceUtilTest
{
    @Test
    public void testExamples() throws Exception
    {
        final String parent_display = "examples:/01_main.bob";
        final String display_path = "monitors_textupdate.bob";
        String combined = ModelResourceUtil.combineDisplayPaths(parent_display, display_path);
        assertThat(combined, equalTo("examples:/monitors_textupdate.bob"));

        ModelResourceUtil.openResourceStream(parent_display).close();
        ModelResourceUtil.openResourceStream(combined).close();

        combined = ModelResourceUtil.resolveResource(parent_display, display_path);
        assertThat(combined, equalTo("examples:/monitors_textupdate.bob"));

        final File file = ModelResourceUtil.getFile(URI.create("examples:/monitors_textupdate.bob"));
        assertThat(file.canRead(), equalTo(true));
    }

    @Test
    public void testNormalizeUNC() throws Exception
    {
        // UNC path with forward slashes should be preserved
        String normalized = ModelResourceUtil.normalize("//wsl.localhost/AlmaLinux-9/home/user/display.bob");
        assertTrue(normalized.startsWith("//wsl.localhost/"),
                "Normalized UNC path should start with //wsl.localhost/: " + normalized);
        assertTrue(normalized.endsWith("/home/user/display.bob"),
                "Normalized UNC path should keep full path: " + normalized);

        // UNC path with backslashes (Windows-style) should convert to forward slashes
        // and preserve the // prefix
        normalized = ModelResourceUtil.normalize("\\\\wsl.localhost\\AlmaLinux-9\\home\\user\\display.bob");
        assertTrue(normalized.startsWith("//wsl.localhost/"),
                "Normalized Windows UNC path should start with //: " + normalized);
        assertTrue(normalized.contains("AlmaLinux-9/home/user/display.bob"),
                "Normalized Windows UNC path should convert backslashes: " + normalized);

        // UNC path with ".." should collapse parent references but keep //
        normalized = ModelResourceUtil.normalize("//server/share/dir/../file.bob");
        assertTrue(normalized.startsWith("//server/"),
                "Normalized UNC with .. should keep //: " + normalized);
        assertTrue(normalized.contains("/share/file.bob") || normalized.endsWith("/share/file.bob"),
                "Normalized UNC with .. should collapse ..: " + normalized);

        // Regular absolute path should not gain a // prefix
        normalized = ModelResourceUtil.normalize("/home/user/display.bob");
        assertThat(normalized, equalTo("/home/user/display.bob"));

        // Regular Windows path should not gain a // prefix
        normalized = ModelResourceUtil.normalize("C:\\Users\\test\\display.bob");
        assertThat(normalized, equalTo("C:/Users/test/display.bob"));

        // URL should not be affected by UNC handling
        normalized = ModelResourceUtil.normalize("http://server.example/path/display.bob");
        assertThat(normalized, equalTo("http://server.example/path/display.bob"));
    }

    @Test
    public void testCombineDisplayPathsUNC() throws Exception
    {
        // Relative display path resolved against UNC parent
        final String parent = "//wsl.localhost/AlmaLinux-9/home/user/displays/main.bob";
        String combined = ModelResourceUtil.combineDisplayPaths(parent, "child.bob");
        assertTrue(combined.startsWith("//wsl.localhost/"),
                "Combined with UNC parent should keep //: " + combined);
        assertTrue(combined.endsWith("/displays/child.bob"),
                "Combined should resolve relative child: " + combined);

        // Relative display path with subdirectory
        combined = ModelResourceUtil.combineDisplayPaths(parent, "subdir/other.bob");
        assertTrue(combined.startsWith("//wsl.localhost/"),
                "Combined with subdir should keep //: " + combined);
        assertTrue(combined.endsWith("/displays/subdir/other.bob"),
                "Combined should include subdir: " + combined);

        // Relative display path with parent reference (..)
        combined = ModelResourceUtil.combineDisplayPaths(parent, "../sibling/other.bob");
        assertTrue(combined.startsWith("//wsl.localhost/"),
                "Combined with .. should keep //: " + combined);
        assertTrue(combined.contains("/home/user/sibling/other.bob"),
                "Combined with .. should resolve correctly: " + combined);

        // Absolute display path should not be affected by parent
        combined = ModelResourceUtil.combineDisplayPaths(parent, "//other.server/share/abs.bob");
        assertThat(combined, equalTo("//other.server/share/abs.bob"));

        // Null parent should just return the display path
        combined = ModelResourceUtil.combineDisplayPaths(null, "//wsl.localhost/share/file.bob");
        assertThat(combined, equalTo("//wsl.localhost/share/file.bob"));
    }

    @Test
    public void testGetFileUNC() throws Exception
    {
        // getFile with UNC URI (host in authority) should produce File with host in path
        final URI unc_uri = new URI("file", "wsl.localhost", "/AlmaLinux-9/home/user/display.bob", null);
        final File unc_file = ModelResourceUtil.getFile(unc_uri);
        assertTrue(unc_file != null);
        assertTrue(unc_file.getPath().contains("wsl.localhost"),
                "getFile should preserve UNC host: " + unc_file.getPath());

        // getFile with UNC URI that also has a query parameter
        final URI unc_query = new URI("file", "server.example", "/share/displays/main.bob", "X=1&Y=2");
        final File query_file = ModelResourceUtil.getFile(unc_query);
        assertTrue(query_file != null);
        assertTrue(query_file.getPath().contains("server.example"),
                "getFile with query should preserve UNC host: " + query_file.getPath());
        // Query should be stripped from the file path
        assertTrue(!query_file.getPath().contains("X=1"),
                "getFile should strip query from path: " + query_file.getPath());
    }

    @Test
    public void testGetDirectoryUNC() throws Exception
    {
        // getDirectory on a UNC path should preserve the // prefix
        final String dir = ModelResourceUtil.getDirectory("//wsl.localhost/AlmaLinux-9/home/user/file.bob");
        assertTrue(dir.startsWith("//wsl.localhost/"),
                "getDirectory should keep UNC //: " + dir);
        assertTrue(dir.endsWith("/home/user"),
                "getDirectory should return parent directory: " + dir);
    }
}

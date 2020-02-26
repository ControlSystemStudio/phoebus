/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.junit.Test;
import org.phoebus.ui.javafx.PlatformInfo;

/** JUnit test of path handling
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PathTest
{
    @Test
    public void testNormalize() throws Exception
    {
        String path;

        path = ModelResourceUtil.normalize("C:\\some path\\subdir\\file.opi");
        assertThat(path, equalTo("C:/some path/subdir/file.opi"));

        path = ModelResourceUtil.normalize("C:\\some path\\subdir\\..\\file.opi");
        assertThat(path, equalTo("C:/some path/file.opi"));

        path = ModelResourceUtil.normalize("/old/../new/path/../dir/file.opi");
        assertThat(path, equalTo("/new/dir/file.opi"));

        path = ModelResourceUtil.normalize("https://old/../new/path/../dir/file.opi");
        assertThat(path, equalTo("https://new/dir/file.opi"));
    }

    @Test
    public void testDirectory() throws Exception
    {
        String loc;

        loc = ModelResourceUtil.getDirectory("https://webopi.sns.gov/webopi/opi/Instruments.opi");
        assertThat(loc, equalTo("https://webopi.sns.gov/webopi/opi"));

        loc = ModelResourceUtil.getDirectory("/usr/local/opi/Instruments.opi");
        assertThat(loc, equalTo("/usr/local/opi"));

        loc = ModelResourceUtil.getDirectory("Instruments.opi");
        assertThat(loc, equalTo("."));
    }

    @Test
    public void testCombineNotWindows() throws Exception
    {
        assumeThat(PlatformInfo.isWindows, equalTo(false));
        String path;

        path = ModelResourceUtil.combineDisplayPaths(null, "example.opi");
        assertThat(path, equalTo("example.opi"));

        path = ModelResourceUtil.combineDisplayPaths("examples/dummy.opi", "example.opi");
        assertThat(path, equalTo("examples/example.opi"));

        path = ModelResourceUtil.combineDisplayPaths("examples/dummy.opi", "scripts/test.py");
        assertThat(path, equalTo("examples/scripts/test.py"));

        path = ModelResourceUtil.combineDisplayPaths("https://webopi.sns.gov/webopi/opi/Instruments.bob", "../../share/opi/Motors/motor.opi");
        assertThat(path, equalTo("https://webopi.sns.gov/share/opi/Motors/motor.opi"));

        path = ModelResourceUtil.combineDisplayPaths("https://webopi.sns.gov/webopi/opi/Instruments.opi", "/home/beamline/main.bob");
        assertThat(path, equalTo("/home/beamline/main.bob"));
    }

    @Test
    public void testCombineWindows() throws Exception
    {
        assumeThat(PlatformInfo.isWindows, equalTo(true));
        String path;

        path = ModelResourceUtil.combineDisplayPaths(null, "example.opi");
        assertThat(path, equalTo("example.opi"));

        path = ModelResourceUtil.combineDisplayPaths("examples/dummy.opi", "example.opi");
        assertThat(path, equalTo("examples/example.opi"));

        path = ModelResourceUtil.combineDisplayPaths("examples/dummy.opi", "scripts/test.py");
        assertThat(path, equalTo("examples/scripts/test.py"));

        path = ModelResourceUtil.combineDisplayPaths("https://webopi.sns.gov/webopi/opi/Instruments.bob", "../../share/opi/Motors/motor.opi");
        assertThat(path, equalTo("https://webopi.sns.gov/share/opi/Motors/motor.opi"));

        path = ModelResourceUtil.combineDisplayPaths("https://webopi.sns.gov/webopi/opi/Instruments.opi", "C:\\home\\beamline\\main.bob");
        assertThat(path, equalTo("C:/home/beamline/main.bob"));
    }

    public void checkRelativePath(String parent, String path, String expectedResult) throws Exception
    {
        String resultingPath = ModelResourceUtil.getRelativePath(parent, path);
        assertThat(resultingPath, equalTo(expectedResult));
    }

    @Test
    public void testRelativeNotWindows() throws Exception
    {
        assumeThat(PlatformInfo.isWindows, equalTo(false));
        String parent = "/one/of/my/directories/parent.bob";

        // Same dir
        checkRelativePath(parent, "/one/of/my/directories/other.bob", "other.bob");

        // Other dirs, made relative
        checkRelativePath(parent, "/one/of/my/alternate_dirs/example.bob", "../alternate_dirs/example.bob");

        checkRelativePath(parent, "/one/main.bob","../../../main.bob");

        checkRelativePath(parent, "/other/of/my/directories/file.bob", "../../../../other/of/my/directories/file.bob");

        // Entered with "..", will be normalized and then made relative
        checkRelativePath(parent, "/elsewhere/b/../file.txt", "../../../../elsewhere/file.txt");

        // File to make relative is already relative
        checkRelativePath(parent, "file.bob", "file.bob");

        checkRelativePath(parent, "../c/d/file.bob", "../c/d/file.bob");

        // .. and in local file system
        checkRelativePath("/main/file.bob", "/share/common.bob", "../share/common.bob");

        // File contains spaces
        parent = "/path with spaces/parent.bob";
        checkRelativePath(parent, "/path with spaces/other.bob", "other.bob");

        checkRelativePath(parent, "/path with spaces/spaces file.bob", "spaces file.bob");

        // Mix of files and URLs give origina path
        checkRelativePath("/a/path/test.bob", "http://server/folder/main/other.bob", "http://server/folder/main/other.bob");

        checkRelativePath("http://server/folder/main/other.bob", "/a/path/test.bob", "/a/path/test.bob");

        checkRelativePath("http://server/folder/main/other.bob", "../relative_path/file.bob", "../relative_path/file.bob");
    }

    @Test
    public void testRelativeWindows() throws Exception
    {
        assumeThat(PlatformInfo.isWindows, equalTo(true));
        String parent = "C:\\one\\of\\my\\directories\\parent.bob";

        // Same dir
        checkRelativePath(parent, "C:\\one\\of\\my\\directories\\other.bob", "other.bob");

        // Other dirs, made relative
        checkRelativePath(parent, "C:\\one\\of\\my\\alternate_dirs\\example.bob",
                "../alternate_dirs/example.bob");

        checkRelativePath(parent, "C:\\one\\of\\my\\alternate_dirs\\example.bob",
                "../alternate_dirs/example.bob");

        checkRelativePath(parent, "C:\\one\\main.bob", "../../../main.bob");

        checkRelativePath(parent, "C:\\other\\of\\my\\directories\\file.bob",
                "../../../../other/of/my/directories/file.bob");

        // Entered with "..", will be normalized and then made relative
        checkRelativePath(parent, "C:\\elsewhere\\b\\..\\file.txt",
                "../../../../elsewhere/file.txt");

        // File to make relative is already relative
        checkRelativePath(parent, "file.bob", "file.bob");

        checkRelativePath(parent, "..\\c\\d\\file.bob", "../c/d/file.bob");

        // .. and in local file system
        checkRelativePath("C:\\main\\file.bob", "C:\\share\\common.bob", "../share/common.bob");

        // File contains spaces
        parent = "C:\\path with spaces\\parent.bob";
        checkRelativePath(parent, "C:\\path with spaces\\other.bob", "other.bob");

        checkRelativePath(parent, "C:\\path with spaces\\spaces file.bob", "spaces file.bob");

        // Mix of files and URLs give origina path
        checkRelativePath("C:\\a\\path\\test.bob", "http://server/folder/main/other.bob", "http://server/folder/main/other.bob");

        checkRelativePath("http://server/folder/main/other.bob", "C:\\a\\path\\test.bob", "C:/a/path/test.bob");

        checkRelativePath("http://server/folder/main/other.bob", "..\\relative_path\\file.bob", "../relative_path/file.bob");

    }

    @Test
    public void testRelativePathForURL() throws Exception {
        // Same relative layout of file.bob and other.bob, via http ..
        String parent = "http://server/folder/main/file.bob";
        checkRelativePath(parent, "http://server/folder/main/other.bob", "other.bob");

        checkRelativePath(parent, "http://server/folder/share/common.bob", "../share/common.bob");

        checkRelativePath(parent, "http://server/folder/main/path+with+plus.bob", "path+with+plus.bob");

        checkRelativePath(parent, "http://server/folder/main/path with spaces.bob", "path with spaces.bob");
    }
}

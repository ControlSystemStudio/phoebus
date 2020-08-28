/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

import java.io.File;

public class FileExtensionUtilTest {

    @Test
    public void testFile() throws Exception
    {
        File bob = new File("/some/path/file.bob");

        // Change from no file extension
        File file = FileExtensionUtil.enforceFileExtension(new File("/some/path/file"), "bob");
        assertThat(file, equalTo(bob));

        // Change from other file extension
        file = FileExtensionUtil.enforceFileExtension(new File("/some/path/file.abc"), "bob");
        assertThat(file, equalTo(bob));

        // Leave matching extension
        file = FileExtensionUtil.enforceFileExtension(new File("/some/path/file.bob"), "bob");
        assertThat(file, equalTo(bob));
    }
}

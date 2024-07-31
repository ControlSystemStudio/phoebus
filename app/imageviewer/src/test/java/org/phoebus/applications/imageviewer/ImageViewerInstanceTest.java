/*
 * Copyright (C) 2023 European Spallation Source ERIC.
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
 *
 */

package org.phoebus.applications.imageviewer;

import org.junit.jupiter.api.Test;

import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;

public class ImageViewerInstanceTest {

    @Test
    public void testUriSanitizer() throws Exception{
        String uriString = "file:/foo/bar";
        URI uri = new URI(uriString);
        String sanitized = ImageViewerInstance.sanitizeUri(uri);

        assertEquals(sanitized, uriString);

        String uriWithQuestionMark = "file:/foo/bar?";
        sanitized = ImageViewerInstance.sanitizeUri(uri);

        assertEquals(sanitized, uriString);

        String uriWithQueryParams = "file:/foo/bar?foo=bar";
        sanitized = ImageViewerInstance.sanitizeUri(uri);

        assertEquals(sanitized, uriString);
    }

}

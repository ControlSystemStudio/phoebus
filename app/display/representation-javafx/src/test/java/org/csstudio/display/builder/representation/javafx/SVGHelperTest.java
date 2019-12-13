/*
 * Copyright (C) 2019 European Spallation Source ERIC.
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

package org.csstudio.display.builder.representation.javafx;

import javafx.scene.image.Image;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.InputStream;

public class SVGHelperTest {

    /**
     * Verifies that the SVG file can be loaded and that the created {@link Image} has
     * non-zero width and height.
     */
    @Test
    public void testSVGHelper(){
        InputStream is = getClass().getClassLoader().getResourceAsStream("interlock.svg");

        Image image = SVGHelper.loadSVG(is);

        assertTrue(image.getHeight() > 0);
        assertTrue(image.getWidth() > 0);
    }
}

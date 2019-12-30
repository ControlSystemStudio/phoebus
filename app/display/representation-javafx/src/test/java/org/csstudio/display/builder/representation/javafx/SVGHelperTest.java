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

        try {
            Image image = SVGHelper.loadSVG(is, 400d, 400d);

            assertTrue(image.getHeight() > 0);
            assertTrue(image.getWidth() > 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Verifies that {@link Exception} is thrown when loading png file.
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testSVGHelperPngFile() throws Exception{
        InputStream is = getClass().getClassLoader().getResourceAsStream("interlock.png");
        SVGHelper.loadSVG(is, 400d, 400d);
    }

    /**
     * Verifies that {@link Exception} is thrown when loading jpg file.
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testSVGHelperJpgFile() throws Exception{
        InputStream is = getClass().getClassLoader().getResourceAsStream("interlock.jpg");
        SVGHelper.loadSVG(is, 400d, 400d);
    }

    /**
     * Verifies that {@link Exception} is thrown when loading gif file.
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testSVGHelperGifFile() throws Exception{
        InputStream is = getClass().getClassLoader().getResourceAsStream("interlock.gif");
        SVGHelper.loadSVG(is, 400d, 400d);
    }

    /**
     * Verifies that {@link Exception} is thrown when loading tiff file.
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testSVGHelperTiffFile() throws Exception{
        InputStream is = getClass().getClassLoader().getResourceAsStream("interlock.tiff");
        SVGHelper.loadSVG(is, 400d, 400d);
    }
}

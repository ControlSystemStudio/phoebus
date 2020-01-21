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

import java.io.File;
import java.net.URL;

import static org.junit.Assert.*;

public class SVGHelperTest {

    /**
     * Verifies that the SVG file can be loaded and that the created {@link Image} has
     * non-zero width and height.
     */
    @Test
    public void testSVGHelper(){

        try {
            Image image = SVGHelper.loadSVG(getPath("interlock.svg"), 400d, 400d);
            assertTrue(image.getHeight() > 0);
            assertTrue(image.getWidth() > 0);
        } catch (Exception e) {
           fail(e.getMessage());
        }
    }


    /**
     * Verifies that <code>null</code> is returned when loading png file.
     */
    @Test
    public void testSVGHelperPngFile(){
        String path = null;
        try {
            path = getPath("interlock.png");
        } catch (Exception e) {
            fail(e.getMessage());
            return;
        }
        assertNull(SVGHelper.loadSVG(path, 400d, 400d));
    }

    /**
     * Verifies that <code>null</code> is returned when loading jpg file.
     */
    @Test
    public void testSVGHelperJpgFile(){

        String path = null;
        try {
            path = getPath("interlock.jpg");
        } catch (Exception e) {
            fail(e.getMessage());
            return;
        }
        assertNull(SVGHelper.loadSVG(path, 400d, 400d));
    }

    /**
     * Verifies that <code>null</code> is returned when loading gif file.
     */
    @Test
    public void testSVGHelperGifFile() throws Exception{
        String path = null;
        try {
            path = getPath("interlock.gif");
        } catch (Exception e) {
            fail(e.getMessage());
            return;
        }
        assertNull(SVGHelper.loadSVG(path, 400d, 400d));
    }

    /**
     * Verifies that <code>null</code> is returned when loading tiff file.
     */
    @Test
    public void testSVGHelperTiffFile() throws Exception{

        String path = null;
        try {
            path = getPath("interlock.tiff");
        } catch (Exception e) {
            fail(e.getMessage());
            return;
        }
        assertNull(SVGHelper.loadSVG(path, 400d, 400d));
    }

    private String getPath(String resource) throws Exception{
        URL url = getClass().getClassLoader().getResource(resource);
        File file = new File(url.toURI());
        return file.getAbsolutePath();
    }
}

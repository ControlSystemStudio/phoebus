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

package org.csstudio.javafx.rtplot.util;

import javafx.scene.paint.Color;
import org.csstudio.javafx.rtplot.internal.util.GraphicsUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GraphicsUtilsTest {

    @Test
    public void convertColor1(){
        javafx.scene.paint.Color in = new Color(1.0, 1.0, 1.0, 1.0);
        java.awt.Color out = GraphicsUtils.convert(in);
        assertEquals(255, out.getGreen());
        assertEquals(255, out.getBlue());
        assertEquals(255, out.getRed());
        assertEquals(255, out.getAlpha());
    }

    @Test
    public void convertColor2(){
        javafx.scene.paint.Color in = new Color(1.0, 1.0, 1.0, 0.0);
        java.awt.Color out = GraphicsUtils.convert(in);
        assertEquals(255, out.getGreen());
        assertEquals(255, out.getBlue());
        assertEquals(255, out.getRed());
        assertEquals(0, out.getAlpha());
    }

    @Test
    public void convertColor3(){
        javafx.scene.paint.Color in = new Color(1.0, 1.0, 1.0, 0.0);
        java.awt.Color out = GraphicsUtils.convert(in, 255);
        assertEquals(255, out.getGreen());
        assertEquals(255, out.getBlue());
        assertEquals(255, out.getRed());
        assertEquals(255, out.getAlpha());
    }
}

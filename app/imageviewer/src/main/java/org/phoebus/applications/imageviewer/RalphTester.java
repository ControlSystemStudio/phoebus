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
 *
 */

package org.phoebus.applications.imageviewer;

import org.phoebus.ui.javafx.svg.SVGTranscoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RalphTester {

    public static void main(String[] args){
        new RalphTester().doIt();
    }

    int i = 0;

    public void doIt(){
        Path path = Path.of("/Users/georgweiss/projects/org.csstudio.iter/products/org.csstudio.iter.css.product/SymbolLibrary_quad_hd/SVG");
        try {
            Files.find(path, 10, (p, a) -> p.toFile().getAbsolutePath().endsWith("svg")).forEach(p -> tryOpen(p.toFile()));
        } catch (IOException e) {
           e.printStackTrace();
        }

        System.out.println(i);
    }

    private void tryOpen(File f){
        i++;
        System.out.println(i + " " + f.getAbsolutePath());
        try {
            SVGTranscoder.loadSVG(new FileInputStream(f), 0, 0);
        } catch (Exception e) {
           e.printStackTrace();
        }
    }



}

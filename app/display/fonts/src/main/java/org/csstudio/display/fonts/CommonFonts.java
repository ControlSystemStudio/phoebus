/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.fonts;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.text.Font;

/** Installer for Liberation fonts
 *
 *  <p>These fonts could be installed on the OS level,
 *  but by including them in the display builder
 *  end users do not need to install them manually.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class CommonFonts
{
    private final static String[] fonts = new String[]
    {
        "LiberationMono-Regular.ttf",
        "LiberationSerif-Bold.ttf",
        "LiberationSans-Bold.ttf",
        "LiberationSerif-BoldItalic.ttf",
        "LiberationMono-Bold.ttf",
        "LiberationSans-BoldItalic.ttf",
        "LiberationSerif-Italic.ttf",
        "LiberationMono-BoldItalic.ttf",
        "LiberationSans-Italic.ttf",
        "LiberationSerif-Regular.ttf",
        "LiberationMono-Italic.ttf",
        "LiberationSans-Regular.ttf"
    };

    /** Install common fonts */
    public static void install()
    {
        final Logger logger = Logger.getLogger(CommonFonts.class.getPackageName());
        for (String name : fonts)
        {
            try
            (
                final InputStream stream = CommonFonts.class.getResourceAsStream("/fonts/" + name);
            )
            {
                Font.loadFont(stream, 0);
                logger.log(Level.FINE, "Loading font {0}", name);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot install font " + name, ex);
            }
        }
    }

    /** Standalone demo */
//    public static void main(String[] args) throws Exception
//    {
//        install();
//        for (String family : Font.getFamilies())
//            System.out.println(family);
//    }
}

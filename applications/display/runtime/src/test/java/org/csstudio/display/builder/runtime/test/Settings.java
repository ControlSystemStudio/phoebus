/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.test;

import java.io.FileInputStream;
import java.util.logging.LogManager;

import org.csstudio.display.builder.model.persist.WidgetColorService;

/** Demo settings
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Settings
{
    // Default display, can be changed via command line
    public static String display_path =
          //"../org.csstudio.display.builder.model/examples/graphics_picture.opi";
           "../org.csstudio.display.builder.model/examples/01_main.bob";
//        "example.opi";
//        "legacy.opi";
//        "legacy_embed.opi";
//        "https://webopi.sns.gov/webopi/opi/Instruments.opi";
//        "image.opi";
//          "embedded_script.opi";
//        "main.opi";
//        "demo.opi";

    public static void setup() throws Exception
    {
        LogManager.getLogManager().readConfiguration(new FileInputStream("../org.csstudio.display.builder.runtime.test/examples/logging.properties"));

        final String addr_list = "127.0.0.1 webopi.sns.gov:5066 160.91.228.17";
        System.setProperty("com.cosylab.epics.caj.CAJContext.addr_list", addr_list);
        System.setProperty("gov.aps.jca.jni.JNIContext.addr_list", addr_list);

        final String max_array_bytes = Long.toString(1000000L * 8);
        System.setProperty("com.cosylab.epics.caj.CAJContext.max_array_bytes", max_array_bytes);
        System.setProperty("gov.aps.jca.jni.JNIContext.max_array_bytes", max_array_bytes);

        final String color_file = "../org.csstudio.display.builder.model/examples/color.def";
        WidgetColorService.loadColors(new String[] { color_file } , file -> new FileInputStream(file));
    }
}
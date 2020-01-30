/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import java.util.logging.Logger;

import org.csstudio.display.builder.model.persist.WidgetClassesService;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.persist.WidgetFontService;
import org.csstudio.display.builder.model.util.ModelResourceUtil;

/** Plugin information.
 *  @author Kay Kasemir
 */
public class ModelPlugin
{
    public final static Logger logger = Logger.getLogger(ModelPlugin.class.getPackageName());

    /** Trigger re-load of configuration files
     *
     *  <p>Loads widget classes, fonts, colors.
     *
     *  <p>When asking e.g. WidgetClassesService
     *  for information right away, beware
     *  that it will delay until done loading the file.
     */
    public static void reloadConfigurationFiles()
    {
        final String colors[] = Preferences.color_files;
        WidgetColorService.loadColors(colors, file -> ModelResourceUtil.openResourceStream(file));

        final String fonts[] = Preferences.font_files;
        WidgetFontService.loadFonts(fonts, file -> ModelResourceUtil.openResourceStream(file));

        final String class_files[] = Preferences.class_files;
        WidgetClassesService.loadWidgetClasses(class_files, file -> ModelResourceUtil.openResourceStream(file));
    }
}

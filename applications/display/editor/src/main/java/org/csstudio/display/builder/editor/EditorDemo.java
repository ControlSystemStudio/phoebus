/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.LogManager;

import org.csstudio.display.builder.model.persist.WidgetClassesService;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.persist.WidgetFontService;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

@SuppressWarnings("nls")
public class EditorDemo extends Application
{
    // private static String display_file = "../org.csstudio.display.builder.runtime.test/examples/my_rules_example.opi";
    private static String display_file = "../org.csstudio.display.builder.model/examples/01_main.bob";
    private EditorDemoGUI editor;

    /** JavaFX main
     *  @throws Exception
     */
    public static void main(final String[] args) throws Exception
    {
        if (args.length == 1)
            display_file = args[0];

        LogManager.getLogManager().readConfiguration(new FileInputStream("../org.csstudio.display.builder.runtime.test/examples/logging.properties"));

        launch(args);
    }

    /** JavaFX Start */
    @Override
    public void start(final Stage stage)
    {
        final File classes_file = new File("../org.csstudio.display.builder.model/examples/classes.bcf");
        WidgetClassesService.loadWidgetClasses(new String[] { classes_file.getPath() }, file -> new FileInputStream(file));

        final File color_file = new File("../org.csstudio.display.builder.model/examples/color.def");
        WidgetColorService.loadColors(new String[] { color_file.getPath() }, file -> new FileInputStream(file));

        final File font_file = new File("../org.csstudio.display.builder.model/examples/font.def");
        WidgetFontService.loadFonts(new String[] { font_file.getPath() }, file -> new FileInputStream(file));

        editor = new EditorDemoGUI(stage);
        editor.loadModel(new File(display_file));
        stage.setOnCloseRequest((WindowEvent event) -> editor.dispose());
    }
}

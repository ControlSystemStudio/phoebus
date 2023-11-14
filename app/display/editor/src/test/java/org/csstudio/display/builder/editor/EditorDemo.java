/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor;

import static org.csstudio.display.builder.editor.Plugin.logger;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;

import org.csstudio.display.builder.editor.actions.ActionDescription;
import org.csstudio.display.builder.model.ModelPlugin;
import org.phoebus.ui.javafx.ApplicationWrapper;
import org.phoebus.ui.javafx.ImageCache;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

@SuppressWarnings("nls")
public class EditorDemo extends ApplicationWrapper
{
    private static String display_file = "../model/src/main/resources/examples/01_main.bob";
    private EditorGUI editor;
    private volatile File file = null;

    /** @return Currently edited file */
    public File getFile()
    {
        return file;
    }

    /** JavaFX Start */
    @Override
    public void start(final Stage stage)
    {
        // Call ModelPlugin to trigger its static loading of config file..
        ModelPlugin.logger.fine("Load configuration files");


        editor = new EditorGUI();

        final ObservableList<Node> toolbar = editor.getDisplayEditor().getToolBar().getItems();
        toolbar.add(0, createButton(new LoadModelAction(editor)));
        toolbar.add(1, createButton(new SaveModelAction(editor)));
        toolbar.add(2, new Separator());

        stage.setTitle("Editor");
        stage.setWidth(1200);
        stage.setHeight(600);
        final Scene scene = new Scene(editor.getParentNode(), 1200, 600);
        stage.setScene(scene);
        EditorUtil.setSceneStyle(scene);

        // If ScenicView.jar is added to classpath, open it here
        //ScenicView.show(scene);

        stage.show();



        // .. before the model is loaded which may then use predefined colors etc.
        editor.loadModel(new File(display_file));
        stage.setOnCloseRequest((WindowEvent event) -> editor.dispose());
    }

    private Button createButton(final ActionDescription action)
    {
        final Button button = new Button();
        try
        {
            button.setGraphic(ImageCache.getImageView(action.getIconResourcePath()));
        }
        catch (final Exception ex)
        {
            logger.log(Level.WARNING, "Cannot load action icon", ex);
        }
        button.setTooltip(new Tooltip(action.getToolTip()));
        button.setOnAction(event -> action.run(editor.getDisplayEditor()));
        return button;
    }

    /** JavaFX main
     *  @throws Exception
     */
    public static void main(final String[] args) throws Exception
    {
        if (args.length == 1)
            display_file = args[0];

        LogManager.getLogManager().readConfiguration(new FileInputStream("../../../core/launcher/src/main/resources/logging.properties"));

        launch(EditorDemo.class, args);
    }
}

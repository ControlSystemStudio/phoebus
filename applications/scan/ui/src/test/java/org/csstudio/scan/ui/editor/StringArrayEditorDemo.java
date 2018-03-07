/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor;

import org.csstudio.scan.ui.editor.properties.StringArrayEditor;

import com.sun.tools.javac.util.List;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Demo of {@link StringArrayEditor}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class StringArrayEditorDemo extends Application
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        final StringArrayEditor editor = new StringArrayEditor();
        editor.setValues(List.of("One", "", "Two", "Three"));
        editor.setValueHandler(System.out::println);
        stage.setScene(new Scene(editor, 200, 100));
        stage.show();
    }

    public static void main(final String[] args)
    {
        launch(args);
    }
}

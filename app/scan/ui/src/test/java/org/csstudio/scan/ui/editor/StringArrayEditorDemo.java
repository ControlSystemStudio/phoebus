/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor;

import java.util.List;

import org.csstudio.scan.ui.editor.properties.StringArrayEditor;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Scene;
import javafx.stage.Stage;

/** Demo of {@link StringArrayEditor}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class StringArrayEditorDemo extends ApplicationWrapper
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
        launch(StringArrayEditorDemo.class, args);
    }
}

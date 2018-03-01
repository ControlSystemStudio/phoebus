/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor;

import java.util.List;

import org.csstudio.scan.command.CommandSequence;
import org.csstudio.scan.command.CommentCommand;
import org.csstudio.scan.command.DelayCommand;
import org.csstudio.scan.command.LoopCommand;
import org.csstudio.scan.command.SetCommand;
import org.csstudio.scan.command.WaitCommand;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

@SuppressWarnings("nls")
public class EditorDemo extends Application
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        final CommandSequence cmds = new CommandSequence(
                new CommentCommand("Demo"),
                new SetCommand("device", 3.14),
                new WaitCommand("abc", 2.0),
                new LoopCommand("pos", 0, 10, 1, List.of(
                    new SetCommand("run", 1),
                    new DelayCommand(1.0),
                    new SetCommand("run", 0)))
            );


        final ScanEditor editor = new ScanEditor();

        final Scene scene = new Scene(editor, 800, 600);
        stage.setScene(scene);
        stage.show();

        editor.setScan(cmds.getCommands());
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}

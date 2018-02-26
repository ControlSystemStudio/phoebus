package org.csstudio.scan.ui.editor;

import org.csstudio.scan.command.CommandSequence;
import org.csstudio.scan.command.CommentCommand;
import org.csstudio.scan.command.DelayCommand;
import org.csstudio.scan.command.LoopCommand;
import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.command.SetCommand;
import org.csstudio.scan.command.WaitCommand;

import com.sun.tools.javac.util.List;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
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

        final TreeItem<ScanCommand> root = new TreeItem<>(null);
        final TreeView<ScanCommand> tree = new TreeView<>(root);
        tree.setShowRoot(false);
        tree.setCellFactory(tree_view ->  new ScanCommandTreeCell());

        final Scene scene = new Scene(tree, 800, 600);
        stage.setScene(scene);
        stage.show();

        for (ScanCommand cmd : cmds.getCommands())
            root.getChildren().add(new TreeItem<>(cmd));
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}

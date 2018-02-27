package org.csstudio.scan.ui.editor.actions;

import java.util.List;

import org.csstudio.scan.command.ScanCommand;
import org.phoebus.ui.undo.UndoableAction;

public class AddCommands extends UndoableAction
{
    private final ScanCommand target;
    private final List<ScanCommand> commands;
    private final boolean after;

    public AddCommands(final ScanCommand target, final List<ScanCommand> commands,
                       final boolean after)
    {
        super("Add commands");
        this.target = target;
        this.commands = commands;
        this.after = after;
    }

    @Override
    public void run()
    {
        // TODO Add commands
        System.out.println((after ? "Add after " : "Add before ") + target + ": " + commands);
    }

    @Override
    public void undo()
    {
        // TODO Remove commands
    }
}

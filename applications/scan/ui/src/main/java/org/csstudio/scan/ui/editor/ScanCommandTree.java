/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor;

import java.util.ArrayList;
import java.util.List;

import org.csstudio.scan.command.CommandSequence;
import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.command.ScanCommandWithBody;
import org.csstudio.scan.ui.editor.actions.AddCommands;
import org.csstudio.scan.ui.editor.actions.RemoveCommands;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;

/** {@link TreeView} for {@link ScanCommand}s
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanCommandTree extends TreeView<ScanCommand>
{
    private final Model model;
    private final UndoableActionManager undo;
    final TreeItem<ScanCommand> root = new TreeItem<>(null);

    private final ModelListener listener = new ModelListener()
    {
        @Override
        public void commandsChanged()
        {
            System.out.println("Commands changed");
            // Convert scan into tree items
            root.getChildren().clear();
            addCommands(root, model.getCommands());
            updateAddresses();

            // Expand complete tree
            expand(root);
        }

        @Override
        public void commandAdded(final ScanCommandWithBody parent, final ScanCommand command)
        {
            System.out.println("Added " + command + " to " + parent);
            // TODO Optimize
            commandsChanged();


            updateAddresses();
        }

        @Override
        public void commandRemoved(final ScanCommand command)
        {
            remove(root.getChildren(), command);
            updateAddresses();
        }

        private boolean remove(final List<TreeItem<ScanCommand>> items,
                               final ScanCommand command)
        {
            for (TreeItem<ScanCommand> item : items)
                if (item.getValue() == command)
                {
                    System.out.println("Removed " + command);
                    items.remove(item);
                    return true;
                }
                else if (remove(item.getChildren(), command))
                    return true;
            return false;
        }

        private void updateAddresses()
        {
            final List<ScanCommand> commands = model.getCommands();
            CommandSequence.setAddresses(commands);
        }
    };

    // TODO TextFieldTreeCell?

    ScanCommandTree(final Model model, final UndoableActionManager undo)
    {
        this.model = model;
        this.undo = undo;
        setShowRoot(false);
        setCellFactory(tree_view ->  new ScanCommandTreeCell(undo, model));
        setRoot(root);
        getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        model.addListener(listener);

        createContextMenu();
        handleKeys();
    }

    private static void addCommands(final TreeItem<ScanCommand> item,
                                    final List<ScanCommand> commands)
    {
        final List<TreeItem<ScanCommand>> children = item.getChildren();
        for (ScanCommand cmd : commands)
        {
            final TreeItem<ScanCommand> cmd_item = new TreeItem<>(cmd);
            children.add(cmd_item);
            if (cmd instanceof ScanCommandWithBody)
                addCommands(cmd_item, ((ScanCommandWithBody)cmd).getBody());
        }
    }

    private static void expand(final TreeItem<ScanCommand> item)
    {
        item.setExpanded(true);
        for (TreeItem<ScanCommand> sub : item.getChildren())
            expand(sub);
    }

    private List<ScanCommand> getSelectedCommands()
    {
        // Assume a tree with Loop, and the body of the loop is expanded.
        // When selecting all that, the selected items will include the loop
        // and the commands in the body.
        // In the result, we only include the loop command (which contains its body commands),
        // without adding the body commands once more
        final List<ScanCommand> included_via_body = new ArrayList<>();
        final List<ScanCommand> commands = new ArrayList<>();
        for (TreeItem<ScanCommand> item : getSelectionModel().getSelectedItems())
        {
            final ScanCommand cmd = item.getValue();
            checkBodyCommand(cmd, included_via_body);
            if (! included_via_body.contains(cmd))
                commands.add(cmd);
        }
        return commands;
    }

    private static void checkBodyCommand(final ScanCommand cmd,
                                         final List<ScanCommand> included_via_body)
    {
        if (cmd instanceof ScanCommandWithBody)
            for (ScanCommand bdy : ((ScanCommandWithBody)cmd).getBody())
            {
                included_via_body.add(bdy);
                checkBodyCommand(bdy, included_via_body);
            }
    }

    private List<ScanCommand> copyToClipboard()
    {
        final Clipboard clip = Clipboard.getSystemClipboard();
        final List<ScanCommand> copied = getSelectedCommands();
        clip.setContent(ScanCommandDragDrop.createClipboardContent(copied));
        return copied;
    }

    private void cutToClipboard()
    {
        final List<ScanCommand> to_remove = copyToClipboard();
        if (! to_remove.isEmpty())
            undo.execute(new RemoveCommands(model, to_remove));
    }

    private void pasteFromClipboard()
    {
        final Clipboard clip = Clipboard.getSystemClipboard();
        if (ScanCommandDragDrop.hasCommands(clip))
        {
            final List<ScanCommand> commands = ScanCommandDragDrop.getCommands(clip);

            final TreeItem<ScanCommand> item = getSelectionModel().getSelectedItem();
            final ScanCommand location = item != null ? item.getValue() : null;
            undo.execute(new AddCommands(model, location, commands, true));
        }
    }

    private void createContextMenu()
    {
        final MenuItem copy = new MenuItem("Copy",
                                           ImageCache.getImageView(ImageCache.class, "/icons/copy.png"));
        copy.setOnAction(event -> copyToClipboard());

        final MenuItem paste = new MenuItem("Paste",
                                            ImageCache.getImageView(ImageCache.class, "/icons/paste.png"));
        paste.setOnAction(event -> pasteFromClipboard());

        final MenuItem delete = new MenuItem("Delete",
                                             ImageCache.getImageView(ImageCache.class, "/icons/delete.png"));
        delete.setOnAction(event -> cutToClipboard());

        final ContextMenu menu = new ContextMenu(copy, paste, delete);
        setOnContextMenuRequested(event ->
        {
            menu.show(getScene().getWindow(), event.getScreenX(), event.getScreenY());
        });
    }

    private void handleKeys()
    {
        setOnKeyPressed(event ->
        {
            final KeyCode code = event.getCode();
            if (code == KeyCode.DELETE)
                cutToClipboard();
            if (event.isShortcutDown())
            {
                switch (code)
                {
                case Z:
                    undo.undoLast();
                    break;
                case Y:
                    undo.redoLast();
                    break;
                case X:
                    cutToClipboard();
                    break;
                case C:
                    copyToClipboard();
                    break;
                case V:
                    pasteFromClipboard();
                    break;
                default:
                    break;
                }
            }
        });
    }
}

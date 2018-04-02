/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor;

import static org.csstudio.scan.ScanSystem.logger;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.logging.Level;

import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.command.ScanCommandWithBody;
import org.csstudio.scan.ui.editor.actions.AddCommands;
import org.csstudio.scan.ui.editor.actions.RemoveCommands;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.javafx.TreeHelper;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;

/** {@link TreeView} for {@link ScanCommand}s
 *
 *  <p>Opens and displays a scan with ~30k commands within a second,
 *  on a computer where RCP version takes more than 30 seconds.
 *
 *  <p>No problem with copy/paste of a screen full of commands, i.e. about 40 commands,
 *  within a scan of ~30k commands.
 *  Selecting 'all' (Ctrl-A) with ~30k commands takes about 10 seconds.
 *  Then pasting those ~30k commands to the end again takes about 5 seconds.
 *  ==> Could be better with a multithreaded model (ScanCommandWithBody having CopyOnWriteArrayList for body, ..),
 *  but for now considered good enough.
 *
 *  @author Kay Kasemir
 */
public class ScanCommandTree extends TreeView<ScanCommand>
{
    private final Model model;
    private final UndoableActionManager undo;
    final TreeItem<ScanCommand> root = new TreeItem<>(null);

    private volatile TreeItem<ScanCommand> active_item;

    private boolean reveal_active_item = false;

    private final ModelListener listener = new ModelListener()
    {
        @Override
        public void commandsChanged()
        {
            // Convert scan into tree items
            JobManager.schedule("Create Scan Tree", monitor ->
            {
                final TreeItem<ScanCommand> new_root = new TreeItem<>(null);
                addCommands(new_root, model.getCommands());

                Platform.runLater(() ->
                {
                    // Clearing the selection results in faster tree update
                    final int sel = getSelectionModel().getSelectedIndex();
                    getSelectionModel().clearSelection();

                    root.getChildren().setAll(new_root.getChildren());

                    if (sel >= 0)
                        getSelectionModel().select(sel);

                    // Expand complete tree
                    expand(root);
                });
            });
        }

        @Override
        public void commandAdded(final ScanCommandWithBody parent, final ScanCommand command)
        {
            // TreeItem for new command and parent
            final TreeItem<ScanCommand> new_item = createTreeItem(command);
            final TreeItem<ScanCommand> parent_item = findItem(parent);

            // Determine position of command within parent
            final ObservableList<TreeItem<ScanCommand>> items = parent_item.getChildren();
            final List<ScanCommand> parent_commands = parent == null ? model.getCommands() : parent.getBody();
            final int pos = parent_commands.indexOf(command);

            // Add new tree item at matching position
            items.add(pos, new_item);

            // Select the new item
            getSelectionModel().clearSelection();
            getSelectionModel().select(new_item);
        }

        @Override
        public void commandRemoved(final ScanCommand command)
        {
            remove(root.getChildren(), command);
        }

        /** @param command Command that's shown in tree
         *  @return {@link TreeItem} for that command
         */
        private TreeItem<ScanCommand> findItem(final ScanCommand command)
        {
            if (command == null)
                return root;
            return findItem(root.getChildren(), command);
        }

        /** @param items Model's root items or body of command
         *  @param command Command that's shown in tree
         *  @return {@link TreeItem} for that command or <code>null</code> if not in items
         */
        private TreeItem<ScanCommand> findItem(final List<TreeItem<ScanCommand>> items,
                                               final ScanCommand command)
        {
            for (TreeItem<ScanCommand> item : items)
                if (item.getValue() == command)
                    return item;
                else
                {
                    final TreeItem<ScanCommand> found = findItem(item.getChildren(), command);
                    if (found != null)
                        return found;
                }
            return null;
        }

        /** @param items Model's root items or body of command
         *  @param command Command to remove
         *  @return <code>true</code> if command was found and tree item was removed
         */
        private boolean remove(final List<TreeItem<ScanCommand>> items,
                               final ScanCommand command)
        {
            for (TreeItem<ScanCommand> item : items)
                if (item.getValue() == command)
                {
                    items.remove(item);
                    return true;
                }
                else if (remove(item.getChildren(), command))
                    return true;
            return false;
        }
    };

    ScanCommandTree(final Model model, final UndoableActionManager undo)
    {
        this.model = model;
        this.undo = undo;
        setShowRoot(false);
        setCellFactory(tree_view ->  new ScanCommandTreeCell(undo, model));
        setRoot(root);
        getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        model.addListener(listener);

        handleKeys();
        hookDrag();
    }

    void revealActiveItem(final boolean reveal)
    {
        reveal_active_item = reveal;
    }

    private static TreeItem<ScanCommand> createTreeItem(final ScanCommand command)
    {
        final TreeItem<ScanCommand> cmd_item = new TreeItem<>(command);
        if (command instanceof ScanCommandWithBody)
            addCommands(cmd_item, ((ScanCommandWithBody)command).getBody());
        return cmd_item;
    }

    private static void addCommands(final TreeItem<ScanCommand> item,
                                    final List<ScanCommand> commands)
    {
        final List<TreeItem<ScanCommand>> children = item.getChildren();
        for (ScanCommand cmd : commands)
        {
            final TreeItem<ScanCommand> cmd_item = createTreeItem(cmd);
            children.add(cmd_item);
        }
    }

    private static void expand(final TreeItem<ScanCommand> item)
    {
        item.setExpanded(true);
        for (TreeItem<ScanCommand> sub : item.getChildren())
            expand(sub);
    }

    /** @return {@link ScanCommand}s that are selected in tree */
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

    /** Copy selected commands to clipboard */
    List<ScanCommand> copyToClipboard()
    {
        final Clipboard clip = Clipboard.getSystemClipboard();
        final List<ScanCommand> copied = getSelectedCommands();
        clip.setContent(ScanCommandDragDrop.createClipboardContent(copied));
        return copied;
    }

    /** Cut selected commands to clipboard */
    void cutToClipboard()
    {
        final List<ScanCommand> to_remove = copyToClipboard();
        if (! to_remove.isEmpty())
            undo.execute(new RemoveCommands(model, to_remove));
    }

    /** Paste commands from clipboard, append after currently selected item */
    void pasteFromClipboard()
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

    private void handleKeys()
    {
        setOnKeyPressed(event ->
        {
            final KeyCode code = event.getCode();
            if (code == KeyCode.DELETE)
            {
                cutToClipboard();
                event.consume();
            }
            else if (event.isShortcutDown())
            {
                switch (code)
                {
                case Z:
                    undo.undoLast();
                    event.consume();
                    break;
                case Y:
                    undo.redoLast();
                    event.consume();
                    break;
                case X:
                    cutToClipboard();
                    event.consume();
                    break;
                case C:
                    copyToClipboard();
                    event.consume();
                    break;
                case V:
                    pasteFromClipboard();
                    event.consume();
                    break;
                default:
                    break;
                }
            }
        });
    }

    private void hookDrag()
    {
        final List<ScanCommand> commands = new ArrayList<>();
        setOnDragDetected(event ->
        {
            // Only start drag on left (primary) button,
            // not also on right button which opens the context menu,
            // or the apple 'Command' button which is again for the context menu
            if (event.isPrimaryButtonDown() &&  !event.isMetaDown())
            {
                commands.clear();
                commands.addAll(getSelectedCommands());
                if (! commands.isEmpty())
                {
                    // Would like to default to move,
                    // allowing key modifiers for copy.
                    // TransferMode.COPY_OR_MOVE will default to copy,
                    // i.e. wrong order.
                    // --> Deciding based on key modifier at start of drag
                    // https://stackoverflow.com/questions/38699306/how-to-adjust-or-deviate-from-the-default-javafx-transfer-mode-behavior
                    final Dragboard db = startDragAndDrop(event.isShortcutDown()
                                                          ? TransferMode.COPY
                                                          : TransferMode.MOVE);
                    db.setContent(ScanCommandDragDrop.createClipboardContent(commands));
                }
            }
            event.consume();
        });

        setOnDragDone(event ->
        {
            if (event.getTransferMode() == TransferMode.MOVE)
                undo.execute(new RemoveCommands(model, commands));
            commands.clear();
        });
    }

    /** Set active command, which highlights and optionally reveals it
     *  @param address Address of active command
     */
    void setActiveCommand(final long address)
    {
        model.setActiveAddress(address);

        // Locate tree node for active command
        final TreeItem<ScanCommand> previous = active_item;
        active_item = findTreeItem(address);

        if (previous == active_item)
            return;
        Platform.runLater(() ->
        {
            // In principle, need to redraw the previously active item
            // and the one that's now active.
            // Test, however, shows that the complete visible tree is always redrawn,
            // so can skip refreshing `previous`...
            if (previous != null)
                TreeHelper.triggerTreeItemRefresh(previous);
            if (active_item != null)
            {
                TreeHelper.triggerTreeItemRefresh(active_item);
                if (reveal_active_item)
                    reveal(active_item);
            }
        });
    }

    private void reveal(final TreeItem<ScanCommand> item)
    {
        // Expand tree up to parent because 'getRow' will
        // only find items that are expanded
        TreeItem<ScanCommand> parent = item.getParent();
        while (parent != null)
        {
            if (! parent.isExpanded())
                parent.setExpanded(true);
            parent = parent.getParent();
        }

        // Scroll to the active command
        final int row = getRow(active_item);
        if (row >= 0)
        {
            // Try to show one command above the desired one to get some context
            if (row > 1)
                scrollTo(row-1);
            else
                scrollTo(row);
        }
    }

    private TreeItem<ScanCommand> findTreeItem(final long address)
    {
        if (address < 0)
            return null;
        while (true)
        {
            try
            {
                return findTreeItem(root.getChildren(), address);
            }
            catch (ConcurrentModificationException ex)
            {
                // XXX Avoid ConcurrentModificationException instead of catching and trying again?
                logger.log(Level.WARNING, "Scan tree needs to re-try lookup of command", ex);
            }
        }
    }

    /** Find tree item for a command's address
     *  @param items
     *  @param address
     *  @return {@link TreeItem} or <code>null</code>
     */
    private TreeItem<ScanCommand> findTreeItem(final ObservableList<TreeItem<ScanCommand>> items, final long address)
    {
        // Oh no, linear search!
        // But running off the UI thread...

        // XXX Sort-a binary search because items are ordered by address
        for (TreeItem<ScanCommand> item : items)
        {
            if (item.getValue().getAddress() == address)
                return item;
            final TreeItem<ScanCommand> sub = findTreeItem(item.getChildren(), address);
            if (sub != null)
                return sub;
        }
        return null;
    }
}

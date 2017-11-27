/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.docking;

import static org.phoebus.ui.application.PhoebusApplication.logger;

import java.io.File;
import java.net.URI;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.jobs.JobRunnable;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.ui.application.Messages;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.dialog.SaveAsDialog;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.stage.FileChooser.ExtensionFilter;

/** Item for a {@link DockPane} that has an 'input' file or URI.
 *
 *  <p>While technically a {@link Tab},
 *  only the methods declared in here and
 *  in {@link DockItem} should be called
 *  to assert compatibility with future updates.
 *
 *  <p>Tracks the current 'input' and the 'dirty' state.
 *  When the item becomes 'dirty', 'Save' or 'Save As'
 *  are supported via the provided list of file extensions
 *  and a 'save_handler'.
 *  User will be asked to save a dirty tab when the tab is closed.
 *  Saving can also be initiated from the 'File' menu.
 *  If the 'input' is <code>null</code>, 'Save' automatically
 *  invokes 'Save As' to prompt for a file name.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DockItemWithInput extends DockItem
{
    private static final String DIRTY = "* ";

    private AtomicBoolean is_dirty = new AtomicBoolean(false);

    /** The one item that should always be included in 'file_extensions' */
    public static final ExtensionFilter ALL_FILES = new ExtensionFilter("All", "*.*");

    private final ExtensionFilter[] file_extensions;

    private final JobRunnable save_handler;

    private volatile URI input;

    /** Create dock item
     *
     *  <p>The 'save_handler' will be called to save the content.
     *  It will be called in a background job, because writing files
     *  might be slow.
     *
     *  <p>When 'save_handler' is called, the 'input' will be set to a file-based URI.
     *  On success, or if for some reason there is nothing to save,
     *  the 'save_handler' returns.
     *  On error, the 'save_handler' throws an exception.
     *
     *  @param application {@link AppInstance}
     *  @param content Initial content
     *  @param input URI for the input. May be <code>null</code>
     *  @param file_extensions File extensions for "Save As". May be <code>null</code> if never calling <code>setDirty(true)</code>
     *  @param save_handler Will be called to 'save' the content. May be <code>null</code> if never calling <code>setDirty(true)</code>
     */
    public DockItemWithInput(final AppInstance application, final Node content, final URI input,
                             final ExtensionFilter[] file_extensions,
                             final JobRunnable save_handler)
    {
        super(application, content);
        this.file_extensions =  file_extensions;
        this.save_handler = save_handler;
        setInput(input);

        addCloseCheck(this::okToClose);
    }

    // Override to include 'dirty' tab
    @Override
    public void setLabel(final String label)
    {
        name = label;
        if (isDirty())
            name_tab.setText(DIRTY + label);
        else
            name_tab.setText(label);
    }

    /** @param input Input */
    public void setInput(final URI input)
    {
        this.input = input;
        if (input == null)
            name_tab.setTooltip(new Tooltip("<Not saved to file>"));
        else
            name_tab.setTooltip(new Tooltip(input.toString()));
    }

    /** @return Input, which may be <code>null</code> (OK to call from any thread) */
    public URI getInput()
    {
        return input;
    }

    /** @return Current 'dirty' state */
    public boolean isDirty()
    {
        return is_dirty.get();
    }

    /** Update 'dirty' state.
     *
     *  <p>May be called from any thread
     *  @param dirty Updated 'dirty' state
     */
    public void setDirty(final boolean dirty)
    {
        if (is_dirty.getAndSet(dirty) == dirty)
            return;
        // Dirty state changed. Update label on UI thread
        Platform.runLater(() -> setLabel(name));
    }

    /** @return Is "Save As" supported, i.e. have file extensions and a save handler? */
    public boolean isSaveAsSupported()
    {
        return file_extensions != null  &&  save_handler != null;
    }

    /** Called when user tries to close the tab
     *
     *  <p>Derived class may override.
     *
     *  @return Should the tab close? Otherwise it stays open.
     */
    protected boolean okToClose()
    {
        if (! isDirty())
            return true;

        final String text = MessageFormat.format("The {0} has been modified.\n\nSave before closing?", getLabel());
        final Alert prompt = new Alert(AlertType.NONE,
                                      text,
                                      ButtonType.NO, ButtonType.CANCEL, ButtonType.YES);
        prompt.setTitle("Save File");
        DialogHelper.positionDialog(prompt, getTabPane(), -200, -100);
        final ButtonType result = prompt.showAndWait().orElse(ButtonType.CANCEL);

        // Cancel the close request
        if (result == ButtonType.CANCEL)
            return false;

        // Close without saving?
        if (result == ButtonType.NO)
            return true;

        // Save in background job ...
        JobManager.schedule(Messages.Save, monitor ->
        {
            if (save(monitor))
                Platform.runLater(this::close);
        });
        // .. and leave the tab open..
        return false;
    }

    /** Save the content of the item to its current 'input'
     *
     *  <p>Called by the framework when user invokes the 'Save*'
     *  menu items or when a 'dirty' tab is closed.
     *
     *  <p>Will never be called when the item remains clean,
     *  i.e. never called {@link #setDirty(true)}.
     *
     *  @param monitor {@link JobMonitor} for reporting progress
     *  @return <code>true</code> on success
     */
    public final boolean save(final JobMonitor monitor)
    {
        // 'final' because any save customization should be possible
        // inside the save_handler
        monitor.beginTask(MessageFormat.format("Saving {0}...", input));

        try
        {   // If there is no file (input is null or for example http:),
            // call save_as to prompt for file
            File file = ResourceParser.getFile(getInput());
            if (file == null)
                return save_as(monitor);

            if (save_handler == null)
                throw new Exception("No save_handler provided for 'dirty' " + toString());
            save_handler.run(monitor);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Save error", ex);
            Platform.runLater(() ->
                ExceptionDetailsErrorDialog.openError("Save error",
                                                      "Error saving " + getLabel(), ex));
            return false;
        }

        // Successfully saved the file
        setDirty(false);
        return true;
    }

    /** Prompt for new file, then save the content of the item that file.
     *
     *  <p>Called by the framework when user invokes the 'Save As'
     *  menu item.
     *
     *  <p>Will never be called when the item does not report
     *  {@link #isSaveAsSupported()}.
     *
     *  @param monitor {@link JobMonitor} for reporting progress
     *  @return <code>true</code> on success
     */
    public final boolean save_as(final JobMonitor monitor)
    {
        // 'final' because any save customization should be possible
        // inside the save_handler
        try
        {
            // Prompt for file
            File file = ResourceParser.getFile(getInput());
            file = new SaveAsDialog().promptForFile(getTabPane().getScene().getWindow(),
                                                    Messages.SaveAs, file, file_extensions);
            if (file == null)
                return false;

            // Update input
            setInput(ResourceParser.getURI(file));
            // Save in that file
            return save(monitor);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Save-As error", ex);
            Platform.runLater(() ->
                ExceptionDetailsErrorDialog.openError("Save-As error",
                                                      "Error saving " + getLabel(), ex));
        }
        return false;
    }

    @Override
    public String toString()
    {
        return "DockItemWithInput(\"" + getLabel() + "\", " + getInput() + ")";
    }
}
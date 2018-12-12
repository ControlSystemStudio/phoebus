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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.stream.Collectors;

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
import javafx.event.Event;
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
    public static final ExtensionFilter ALL_FILES = new ExtensionFilter(Messages.DockAll, "*.*");

    private final ExtensionFilter[] file_extensions;

    private JobRunnable save_handler;

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

    // Add 'input'
    @Override
    protected void fillInformation(final StringBuilder info)
    {
        super.fillInformation(info);
        info.append("\n");
        info.append(Messages.DockInput).append(getInput());
    }

    private static String extract_name(String path)
    {
        if (path == null)
            return null;

        // Get basename
        int sep = path.lastIndexOf('/');
        if (sep < 0)
            sep = path.lastIndexOf('\\');
        if (sep >= 0)
            path = path.substring(sep+1);

        // Remove extension
        sep = path.lastIndexOf('.');
        if (sep < 0)
            return path;
        return path.substring(0, sep);
    }

    /** Set input
     *
     *  <p>Registers the input to be persisted and restored.
     *  The tab tooltip indicates complete input,
     *  while tab label will be set to basic name (sans path and extension).
     *  For custom name, call <code>setLabel</code> after updating input
     *  in <code>Platform.runLater()</code>
     *
     *  @param input Input
     *  @see DockItemWithInput#setLabel(String)
     */
    public void setInput(final URI input)
    {
        this.input = input;
        final String name = input == null ? null : extract_name(input.getPath());

        // Tooltip update must be on UI thread
        Platform.runLater(() ->
        {
            if (input == null)
                name_tab.setTooltip(new Tooltip(Messages.DockNotSaved));
            else
            {
                name_tab.setTooltip(new Tooltip(input.toString()));
                setLabel(name);
            }
        });
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

        final String text = MessageFormat.format(Messages.DockAlertMsg, getLabel());
        final Alert prompt = new Alert(AlertType.NONE,
                                       text,
                                       ButtonType.NO, ButtonType.CANCEL, ButtonType.YES);
        prompt.setTitle(Messages.DockAlertTitle);
        prompt.getDialogPane().setMinSize(300, 100);
        prompt.setResizable(true);
        DialogHelper.positionDialog(prompt, getTabPane(), -200, -100);
        final ButtonType result = prompt.showAndWait().orElse(ButtonType.CANCEL);

        // Cancel the close request
        if (result == ButtonType.CANCEL)
            return false;

        // Close without saving?
        if (result == ButtonType.NO)
            return true;

        // Save in background job ...
        JobManager.schedule(Messages.Save, monitor -> save(monitor));
        // .. and leave the tab open, so user can then try to close again
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
        monitor.beginTask(MessageFormat.format(Messages.Saving , input));

        try
        {   // If there is no file (input is null or for example http:),
            // call save_as to prompt for file
            File file = ResourceParser.getFile(getInput());
            if (file == null)
                return save_as(monitor);


            if (file.exists()  &&  !file.canWrite())
            {   // Warn on UI thread that file is read-only
                final CompletableFuture<ButtonType> response = new CompletableFuture<>();
                Platform.runLater(() ->
                {
                    final Alert prompt = new Alert(AlertType.CONFIRMATION);
                    prompt.setTitle(Messages.SavingAlertTitle);
                    prompt.setResizable(true);
                    prompt.setHeaderText(MessageFormat.format(Messages.SavingAlert , file.toString()));
                    DialogHelper.positionDialog(prompt, getTabPane(), -200, -200);
                    response.complete(prompt.showAndWait().orElse(ButtonType.CANCEL));

                });

                // If user doesn't want to overwrite, abort the save
                if (response.get() == ButtonType.OK)
                    return save_as(monitor);
                return false;
            }

            if (save_handler == null)
                throw new Exception("No save_handler provided for 'dirty' " + toString());
            save_handler.run(monitor);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Save error", ex);
            Platform.runLater(() ->
                ExceptionDetailsErrorDialog.openError(Messages.SavingHdr,
                                                      Messages.SavingErr + getLabel(), ex));
            return false;
        }

        // Successfully saved the file
        setDirty(false);
        return true;
    }

    /** @param file_extensions {@link ExtensionFilter}s
     *  @return List of valid file extensions, ignoring "*.*"
     */
    private static List<String> getValidExtensions(final ExtensionFilter[] file_extensions)
    {
        final List<String> valid = new ArrayList<>();
        for (ExtensionFilter filter : file_extensions)
            for (String ext : filter.getExtensions())
                if (! ext.equals("*.*"))
                {
                    final int sep = ext.lastIndexOf('.');
                    if (sep > 0)
                        valid.add(ext.substring(sep+1));
                }
        return valid;
    }

    /** @param file File
     *  @param valid List of valid file extensions
     *  @return <code>true</code> if file has one of the valid extensions
     */
    private static boolean checkFileExtension(final File file, final List<String> valid)
    {
        final String path = file.getPath();
        final int sep = path.lastIndexOf('.');
        if (sep < 0)
            return false;
        final String ext = path.substring(sep+1);
        return valid.contains(ext);
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
            final File initial = ResourceParser.getFile(getInput());
            final File file = new SaveAsDialog().promptForFile(getTabPane().getScene().getWindow(),
                                                    Messages.SaveAs, initial, file_extensions);
            if (file == null)
                return false;

            // Enforce one of the file extensions
            final List<String> valid = getValidExtensions(file_extensions);
            if (! checkFileExtension(file, valid))
            {
                // Prompt on UI thread
                final String prompt = MessageFormat.format(Messages.SaveAsPrompt, file, valid.stream().collect(Collectors.joining(", ")));

                final CompletableFuture<Boolean> go_on = new CompletableFuture<>();
                Platform.runLater(() ->
                {
                    final Alert dialog = new Alert(AlertType.CONFIRMATION);
                    dialog.setTitle(Messages.SaveAs);
                    dialog.setHeaderText(Messages.SaveAsHdr);
                    dialog.setContentText(prompt);
                    dialog.setResizable(true);
                    DialogHelper.positionDialog(dialog, getTabPane(), -100, -200);
                    go_on.complete(dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK);
                });
                // In background thread, wait for the result
                if (! go_on.get())
                    return false;
            }

            // Update input
            setInput(ResourceParser.getURI(file));
            // Save in that file
            return save(monitor);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Save-As error", ex);
            Platform.runLater(() ->
                ExceptionDetailsErrorDialog.openError(Messages.SaveAsErrHdr,
                                                      Messages.SaveAsErrMsg + getLabel(), ex));
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    protected void handleClosed(final Event event)
    {
        // Do the same as in the parent class, DockItem.handleClosed, but clean up save_handler.
        super.handleClosed(event);

        save_handler = null;
    }

    @Override
    public String toString()
    {
        return "DockItemWithInput(\"" + getLabel() + "\", " + getInput() + ")";
    }
}
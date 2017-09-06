/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.docking;

import java.io.File;
import java.net.URL;
import java.text.MessageFormat;

import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.jobs.JobManager;
import org.phoebus.ui.jobs.JobMonitor;
import org.phoebus.ui.jobs.JobRunnable;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;

/** Item for a {@link DockPane} that has an 'input' file or URL.
 *
 *  <p>While technically a {@link Tab},
 *  only the methods declared in here and
 *  in {@link DockItem} should be called
 *  to assert compatibility with future updates.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DockItemWithInput extends DockItem
{
    private static final String DIRTY = "* ";

    private boolean is_dirty = false;

    private final JobRunnable save_handler;

    private volatile URL input;

    /** Create dock item
     *
     *  <p>The 'save_handler' will be called to save the content.
     *  It will be called in a background job, because writing files
     *  might be slow.
     *  Implementation would typically use the current input.
     *  If <code>null</code>, it might prompt for another input,
     *  for which it might have to return to the UI thread.
     *  On success, or if for some reason there is nothing to save,
     *  the 'save_handler' returns.
     *  On error, the 'save_handler' throws an exception.
     *
     *  @param label Initial label
     *  @param content Initial content
     *  @param input URL for the input. May be <code>null</code>
     *  @param save_handler Will be called to 'save' the content
     */
    public DockItemWithInput(final String label, final Node content, final URL input, final JobRunnable save_handler)
    {
        super(label, content);
        this.save_handler = save_handler;
        setInput(input);

        setOnCloseRequest(event ->
        {
            if (! okToClose())
                event.consume();
        });
    }

    /** @param input Input */
    public void setInput(final URL input)
    {
        this.input = input;
        if (input == null)
            tab_name.setTooltip(new Tooltip("<Not saved to file>"));
        else
            tab_name.setTooltip(new Tooltip(input.toString()));
    }

    /** @param file File-based input */
    public void setInputFile(final File file) throws Exception
    {
        setInput(new URL("file", "", file.getCanonicalPath()));
    }

    /** @return Input, which may be <code>null</code> (OK to call from any thread) */
    public URL getInput()
    {
        return input;
    }

    /** @return File for file-based input, which may be <code>null</code> (OK to call from any thread) */
    public File getInputFile() throws Exception
    {
        final URL url = getInput();
        if (url == null  ||  !url.getProtocol().equals("file"))
            return null;
        return new File(url.toURI());
    }

    /** @param dirty Updated 'dirty' state */
    public void setDirty(final boolean dirty)
    {
        if (dirty == is_dirty)
            return;
        is_dirty = dirty;

        final String text = getLabel();
        if (dirty)
        {
            if (!text.startsWith(DIRTY))
                setLabel(DIRTY + text);
        }
        else
        {
            if (text.startsWith(DIRTY))
                setLabel(text.substring(2));
        }
    }

    /** Called when user tries to close the tab
     *
     *  <p>Derived class may override.
     *
     *  @return Should the tab close? Otherwise it stays open.
     */
    protected boolean okToClose()
    {
        if (! is_dirty)
            return true;

        final String text = MessageFormat.format("The {0} has been modified.\n\nSave before closing?", getLabel());
        final Alert prompt = new Alert(AlertType.NONE,
                                      text,
                                      ButtonType.NO, ButtonType.CANCEL, ButtonType.YES);
        prompt.setTitle("Save File");
        final ButtonType result = prompt.showAndWait().orElse(ButtonType.CANCEL);

        // Cancel the close request
        if (result == ButtonType.CANCEL)
            return false;

        // Close without saving?
        if (result == ButtonType.NO)
            return true;

        // Save in background job ...
        JobManager.schedule(MessageFormat.format("Saving {0}...", input), this::do_save);
        // .. and leave the tab open..
        return false;
    }

    private void do_save(final JobMonitor monitor)
    {
        monitor.beginTask("Saving...");

        try
        {
            save_handler.run(monitor);
        }
        catch (Exception ex)
        {
            Platform.runLater(() -> ExceptionDetailsErrorDialog.openError("Save error",
                    "Error saving " + getLabel(), ex));
            return;
        }

        // Successfully saved the file
        is_dirty = false;
        Platform.runLater(this::close);
    }

    @Override
    public String toString()
    {
        return "DockItem(\"" + getLabel() + "\")";
    }
}
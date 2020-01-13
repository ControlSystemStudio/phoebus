/*******************************************************************************
 * Copyright (c) 2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.autocomplete;

import static org.phoebus.ui.autocomplete.AutocompleteMenu.logger;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.util.logging.Level;

import org.phoebus.ui.javafx.PlatformInfo;

import javafx.event.EventHandler;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;

/** X11 Paste Buffer Support
 *
 *  <p>The Linux/Unix X11 window system supports a 'Paste Buffer',
 *  aka 'Selection' buffer, in addition to the 'Clipboard' that's
 *  commonly associated with Copy/Paste operations.
 *
 *  <p>Older EPICS display tools like MEDM, EDM, .. allow fetching
 *  the PV name by middle-clicking on a widget, placing it in the
 *  paste buffer, and _only_ placing it there, not on the clipboard.
 *
 *  <p>On many X11 programs, that PV name can then be pasted via
 *  another middle-button click, but JavaFX text widgets only support
 *  a "Ctrl-V" paste from the normal clipboard.
 *  Transferring a PV name from the older EPICS tools is thus cumbersome:
 *  1) Middle click in older tool to copy into paste buffer
 *  2) Paste that into some text editor, select, Ctrl-C to copy to clipboard
 *  3) Ctrl-V from clipboard into JavaFX text widget
 *
 *  This class adds support for a middle-click-paste from the X11 buffer
 *  when running on Linux.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class XPasteBuffer
{
    public static final boolean is_supported = PlatformInfo.is_linux || PlatformInfo.isUnix;

    /** Lazily initialized
     *  All calls must be on UI thread, so no synchronization added
     */
    private static Clipboard clip = null;

    /** On middle button click, insert text from X paste buffer */
    private static final EventHandler<MouseEvent> middle_click = event ->
    {
        if (event.isMiddleButtonDown())
        {
            final TextField field = (TextField) event.getSource();
            field.insertText(field.getCaretPosition(), XPasteBuffer.getText());
        }
    };

    /** @return Text from X11 Paste Buffer */
    public static String getText()
    {
        try
        {
            if (clip == null)
                clip = Toolkit.getDefaultToolkit().getSystemSelection();
            if (clip.isDataFlavorAvailable(DataFlavor.stringFlavor))
                return clip.getData(DataFlavor.stringFlavor).toString();
        }
        catch (Throwable ex)
        {
            logger.log(Level.WARNING, "Cannot read X 'pastebuffer'", ex);
        }
        return "";
    }

    /** @param field Text field to which middle-button-paste will be added */
    public static void addMiddleClickPaste(final TextInputControl field)
    {
        if (XPasteBuffer.is_supported)
            field.addEventFilter(MouseEvent.MOUSE_PRESSED, middle_click);

    }

    /** @param field Text field from which middle-button-paste will be removed */
    public static void removeMiddleClickPaste(final TextInputControl field)
    {
        if (XPasteBuffer.is_supported)
            field.removeEventFilter(MouseEvent.MOUSE_PRESSED, middle_click);
    }
}

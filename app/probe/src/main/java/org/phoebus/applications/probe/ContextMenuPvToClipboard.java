/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.probe;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.Selection;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.ContextMenuEntry;

import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

// Not strictly related to probe, but seemed too small to start
// its own application, so added it to the 'simplest' appliation
// which deals with PVs and already makes context menu contributions.

/** Context menu entry for PV that copies PV name to clipboard
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ContextMenuPvToClipboard implements ContextMenuEntry
{
    private static final Class<?> supportedTypes = ProcessVariable.class;

    private static final Image icon = ImageCache.getImage(ImageCache.class, "/icons/copy.png");

    @Override
    public String getName()
    {
        return Messages.Copy;
    }

    @Override
    public Image getIcon()
    {
        return icon;
    }

    @Override
    public Class<?> getSupportedType()
    {
        return supportedTypes;
    }

    @Override
    public void call(final Selection selection) throws Exception
    {
        final List<ProcessVariable> pvs = selection.getSelections();

        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(pvs.stream().map(ProcessVariable::getName).collect(Collectors.joining(" ")));
        clipboard.setContent(content);
    }
}

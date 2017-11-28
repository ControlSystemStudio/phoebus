/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.runtime.Messages;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.MenuItem;

/** Open display in editor
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class OpenInEditorAction extends MenuItem
{
    public OpenInEditorAction(final AppResourceDescriptor editor,
                              final DisplayInfo info)
    {
        super(Messages.OpenInEditor,
              ImageCache.getImageView(DisplayModel.class, "/icons/display.png"));
        setOnAction(event -> editor.create(info.toURI()));
    }
}

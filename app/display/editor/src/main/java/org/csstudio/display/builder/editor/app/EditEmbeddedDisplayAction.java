/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.app;

import java.net.URI;

import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.MenuItem;

/** Action that opens the body of an embedded display widget in the editor
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class EditEmbeddedDisplayAction extends MenuItem
{
    public EditEmbeddedDisplayAction(final AppResourceDescriptor app, final Widget widget, String file)
    {
        super(Messages.EditEmbededDisplay,
              ImageCache.getImageView(DisplayModel.class, "/icons/embedded.png"));
        setOnAction(event ->
        {
            String embedded;
            try
            {
                embedded = ModelResourceUtil.resolveResource(widget.getDisplayModel(), file);
            }
            catch (Exception ex)
            {
                embedded = file;
            }
            try
            {
                final URI resource = ResourceParser.createResourceURI(embedded);
                app.create(resource);
            }
            catch (Exception ex)
            {
                ExceptionDetailsErrorDialog.openError("Error", "Cannot open editor for\n" + embedded, ex);
            }
        });
    }
}

/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.model.widgets.EmbeddedDisplayWidget;
import org.csstudio.display.builder.runtime.Messages;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;

/** Open display in editor
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class OpenInEditorAction extends WeakRefWidgetAction
{
    public OpenInEditorAction(final AppResourceDescriptor editor,
                              final Widget the_widget)
    {
        super(Messages.OpenInEditor,
              ImageCache.getImageView(DisplayModel.class, "/icons/display.png"),
              the_widget);

        setOnAction(event ->
        {
            try
            {
                final Widget widget = getWidget();
                final DisplayModel model = widget.getDisplayModel();
                final String path;
                // Options:
                if (widget instanceof EmbeddedDisplayWidget)
                {   // c) Widget is an embedded widget.
                    //    -> User probably wanted to edit the _body_ of that embedded widget
                    final EmbeddedDisplayWidget embedded = (EmbeddedDisplayWidget) widget;
                    path = ModelResourceUtil.resolveResource(model, embedded.propFile().getValue());
                }
                else
                {   // b) Widget is one of the widgets in the body of an embedded widget:
                    //    -> Get the body display, _not_ the top-level display
                    // a) Widget is in the top-level display, or the display itself:
                    //    -> Use that that
                    path = model.getUserData(DisplayModel.USER_DATA_INPUT_FILE);
                }
                editor.create(ResourceParser.createResourceURI(path));
            }
            catch (Exception ex)
            {
                ExceptionDetailsErrorDialog.openError("Error", "Cannot open editor", ex);
            }
        });
    }
}

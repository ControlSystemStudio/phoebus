/*******************************************************************************
 * Copyright (c) 2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.internal;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.representation.javafx.widgets.JFXBaseRepresentation;
import org.csstudio.display.builder.runtime.RuntimeAction;
import org.phoebus.ui.javafx.PrintAction;

import javafx.scene.Node;

/** Action for runtime of widgets that should print individually
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PrintWidgetAction extends RuntimeAction
{
    private final Widget widget;

    /** @param widget Widget to print
     *  @param title Title of action
     */
    public PrintWidgetAction(final Widget widget, final String title)
    {
        super(title, "/icons/print_edit.png");
        this.widget = widget;
    }

    @Override
    public void run()
    {
        final Node node = JFXBaseRepresentation.getJFXNode(widget);
        PrintAction.print(node);
    }
}

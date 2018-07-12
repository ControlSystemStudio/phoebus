/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.editor.undo;

import java.util.ArrayList;

import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.widgets.GroupWidget;

/** Action to un-group widgets
 *  @author Kay Kasemir
 */
public class UnGroupWidgetsAction extends GroupWidgetsAction
{
    public UnGroupWidgetsAction(final GroupWidget group)
    {
        super(Messages.RemoveGroup,
              ChildrenProperty.getParentsChildren(group),
              group,
              // Create copy since underlying list of group's children changes
              new ArrayList<>(group.runtimeChildren().getValue()),
              group.propX().getValue() + group.runtimePropInsets().getValue()[0],
              group.propY().getValue() + group.runtimePropInsets().getValue()[1]);
    }

    // Implemented as reversal of GroupWidgetsAction
    @Override
    public void run()
    {
        super.undo();
    }

    @Override
    public void undo()
    {
        super.run();
    }
}

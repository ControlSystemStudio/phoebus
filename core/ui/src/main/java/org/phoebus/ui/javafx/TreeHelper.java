/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.javafx;

import javafx.event.Event;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeItem.TreeModificationEvent;

/** Helper for TreeView
 *  @author Kay Kasemir
 */
public class TreeHelper
{
    /** Trigger a redraw of a tree item
     *
     *  <p>Call when the model item's representation changed,
     *  so tree item with existing value object needs to be
     *  redrawn.
     *
     *  @param item {@link TreeItem}
     */
    public static <TV> void triggerTreeItemRefresh(final TreeItem<TV> item)
    {
        // TreeView or TreeItem has no 'refresh()', update or redraw method.
        // 'setValue' only triggers a refresh of the item if value is different
        //
        // final TV value = item.getValue();
        // item.setValue(null);
        // item.setValue(value);

        // The API does expose the valueChangedEvent(), so send that
        Event.fireEvent(item, new TreeModificationEvent<TV>(TreeItem.<TV>valueChangedEvent(), item, item.getValue()));
    }
}

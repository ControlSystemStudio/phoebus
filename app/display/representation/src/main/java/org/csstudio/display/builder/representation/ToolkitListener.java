/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ActionInfo;

/** Listener to a widget representation
 *
 *  <p>Provides notification of events (action invoked, ..)
 *  independent from the underlying toolkit (JavaFX, ..)
 *
 *  @author Kay Kasemir
 */
public interface ToolkitListener
{
    /** User invoked an action
     *
     *  @param widget {@link Widget} on which user invoked the action
     *  @param action Information about the action that user wants to be executed
     */
    default public void handleAction(Widget widget, ActionInfo action) {};

    /** User clicked (selected) a widget
     *  @param widget Widget that was clicked
     *  @param with_control Is 'control' key held?
     */
    default public void handleClick(Widget widget, boolean with_control) {};

    /** User requested context menu for a widget
     *  @param widget Widget on which context menu was invoked
     *  @param screen_x X coordinate of mouse when menu was invoked
     *  @param screen_y Y coordinate of mouse when menu was invoked
     */
    default public void handleContextMenu(Widget widget, int screen_x, int screen_y) {};

    /** User provided a new value that should be written to PV
     *  @param widget Widget that provided the value; Widget's (primary) PV should be written
     *  @param value The value
     */
    default public void handleWrite(Widget widget, Object value) {};
}

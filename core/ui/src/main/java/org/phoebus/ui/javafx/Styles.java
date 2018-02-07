/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.javafx;

import javafx.scene.Node;
import javafx.scene.Scene;

/** Helper for dealing with styles
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Styles
{
    /** Style used to cause enabled/disabled appearance */
    public final static String NOT_ENABLED = "not_enabled";

    /** @param scene Scene where style sheet is added */
    public static void setSceneStyle(final Scene scene)
    {
        final String css = Styles.class.getResource("csstudio.css").toExternalForm();
        set(scene, css);
    }

    /** Apply style sheet to scene (but only once)
     *
     *  @param scene Scene where style sheet is added
     *  @param css Style sheet to apply
     */
    public static void set(final Scene scene, final String css)
    {
        if (! scene.getStylesheets().contains(css))
            scene.getStylesheets().add(css);
    }

    /** Set a style
     *
     *  <p>Adds that style to the node, unless already set.
     *  @param jfx_node Node
     *  @param style Style to include in styles
     */
    public static void set(final Node jfx_node, final String style)
    {
        // Simply calling 'add' would add even if it's already there
        if (! jfx_node.getStyleClass().contains(style))
            jfx_node.getStyleClass().add(style);
    }

    /** Clear a style
     *
     *  <p>Removes style from the node
     *  @param jfx_node Node
     *  @param style Style to include in styles
     */
    public static void clear(final Node jfx_node, final String style)
    {
        // If added more than once, remove all
        jfx_node.getStyleClass().removeAll(style);
    }

    /** Set or clear a style
     *
     *  <p>Adds that style to the node, unless already set,
     *  or removes is
     *
     *  @param jfx_node Node
     *  @param style Style to include or not
     *  @param set <code>true</code> to set the style, otherwise clear
     */
    public static void update(final Node jfx_node, final String style, final boolean set)
    {
        if (set)
            set(jfx_node, style);
        else
            clear(jfx_node, style);
    }
}

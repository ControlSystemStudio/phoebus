/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.javafx;

import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** Helper for Toolbar
 *  @author Kay Kasemir
 */
public class ToolbarHelper
{
    // See http://www.oracle.com/technetwork/articles/java/javafxbest2-1634274.html

    /** @return 'Spring', spacer that fills available space */
    public static Node createSpring()
    {
        final Region sep = new Region();
        HBox.setHgrow(sep, Priority.ALWAYS);
        return sep;
    }

    /** @return 'Strut', a small fixed-width spacer */
    public static Node createStrut()
    {
        return createStrut(10);
    }

    /** @param width Desired width
     *  @return 'Strut' that occupies requested width
     */
    public static Node createStrut(int width)
    {
        final Region sep = new Region();
        sep.setPrefWidth(width);
        sep.setMinWidth(Region.USE_PREF_SIZE);
        sep.setMaxWidth(Region.USE_PREF_SIZE);
        return sep;
    }
}

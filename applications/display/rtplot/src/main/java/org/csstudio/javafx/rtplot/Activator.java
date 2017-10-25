/*******************************************************************************
 * Copyright (c) 2014-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

import java.util.logging.Logger;

import javafx.scene.image.Image;

/** Not an actual Plugin Activator, but providing plugin-related helpers
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Activator
{
    final public static Logger logger = Logger.getLogger("org.csstudio.javafx.rtplot");

    public static Image getIcon(final String base_name) throws Exception
    {
        return new Image(Activator.class.getResource("/icons/" + base_name + ".png").toExternalForm());
    }
}

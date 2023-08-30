/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.errlog;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;

/** LineLog preference settings
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Preferences
{
    @Preference public static int max_lines;

    static
    {
    	AnnotatedPreferences.initialize(Preferences.class, "/errlog_preferences.properties");
    }
}

/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.formula;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;

/** Preferences for {@link FormulaPV}s
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class FormulaPVPreferences
{
    @Preference public static int throttle_ms;

    static
    {
    	AnnotatedPreferences.initialize(FormulaPVFactory.class, "/pv_formula_preferences.properties");
    }
}

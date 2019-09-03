/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.formula;

import org.phoebus.framework.preferences.PreferencesReader;

/** Preferences for {@link FormulaPV}s
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class FormulaPVPreferences
{
    public static final int throttle_ms;

    static
    {
        final PreferencesReader prefs = new PreferencesReader(FormulaPVFactory.class, "/pv_formula_preferences.properties");
        throttle_ms = prefs.getInt("throttle_ms");
    }
}

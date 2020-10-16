/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor;

import org.junit.Test;
import static org.junit.Assert.*;

/** JUnit test of editor preferences
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PreferencesTest
{
    @Test
    public void testPrefs()
    {
        System.out.println("Hidden widget types: " + Preferences.hidden_widget_types);
    }

    @Test
    public void testUndoStackSize(){
        assertEquals(50, Preferences.undoStackSize);
    }
}

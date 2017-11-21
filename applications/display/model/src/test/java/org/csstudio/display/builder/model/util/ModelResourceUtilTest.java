/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.util;

import org.junit.Test;
import static org.junit.Assert.assertThat;

import static org.hamcrest.CoreMatchers.equalTo;

/** JUnit test of the {@link ModelResourceUtil}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ModelResourceUtilTest
{
    @Test
    public void testExamples() throws Exception
    {
        final String parent_display = "examples:/01_main.bob";
        final String display_path = "monitors_textupdate.bob";
        String combined = ModelResourceUtil.combineDisplayPaths(parent_display, display_path);
        assertThat(combined, equalTo("examples:/monitors_textupdate.bob"));
        
        ModelResourceUtil.openResourceStream(parent_display).close();
        ModelResourceUtil.openResourceStream(combined).close();
        
        combined = ModelResourceUtil.resolveResource(parent_display, display_path);
        assertThat(combined, equalTo("examples:/monitors_textupdate.bob"));
    }
}

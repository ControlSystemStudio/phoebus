/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.junit.Test;

/** JUnit test of JFXUtil
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class JFXUtilTest
{
    @Test
    public void testRGB()
    {
        assertThat(JFXUtil.webRGB(new WidgetColor(15, 255, 0)), equalTo("#0FFF00"));
        assertThat(JFXUtil.webRGB(new WidgetColor(0, 16, 255)), equalTo("#0010FF"));
    }
}
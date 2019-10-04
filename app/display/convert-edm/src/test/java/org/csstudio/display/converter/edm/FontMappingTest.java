/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/** Font mapping test
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FontMappingTest extends TestHelper
{
    @Test
    public void testParser() throws Exception
    {
        assertThat(ConverterPreferences.mapFont("helvetica"), equalTo("Liberation Sans"));
        assertThat(ConverterPreferences.mapFont("courier"), equalTo("Liberation Mono"));
        assertThat(ConverterPreferences.mapFont("times"), equalTo("Liberation Serif"));
        assertThat(ConverterPreferences.mapFont("anything_else"), equalTo("Liberation Sans"));
    }
}
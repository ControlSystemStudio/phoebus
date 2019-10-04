/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.framework.preferences;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;

import org.junit.Test;

/** JUnit test of the PreferencesReader
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PreferencesReaderTest
{
    @Test
    public void testProperties()
    {
        System.out.println(PreferencesReader.replaceProperties("Running on Java $(java.version) in $(java.home)"));

        System.out.println("Expect warning about undefined property");
        assertThat(PreferencesReader.replaceProperties("$(VeryUnlikelyToExist)"), equalTo("$(VeryUnlikelyToExist)"));

        // Replace one property
        System.setProperty("test", "OK");
        assertThat(PreferencesReader.replaceProperties("$(test)"), equalTo("OK"));

        // Replace one property
        assertThat(PreferencesReader.replaceProperties("This is $(test)"), equalTo("This is OK"));
    }

    @Test
    public void testListSettings() throws Exception
    {
        try
        (
            final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        )
        {
            PropertyPreferenceWriter.save(buf);
            System.out.print(buf.toString());
        }
        System.out.println("Done");
    }
}

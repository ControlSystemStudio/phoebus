/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;

/** Unit test of {@link DroppedPVNameParser}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DroppedPVNameParserTest
{
    @Test
    public void testSingle() throws Exception
    {
        // Just one PV name seems to be the easiest case..
        List<String> names = DroppedPVNameParser.parseDroppedPVs("SomePVName");
        assertThat(names, equalTo(List.of("SomePVName")));

        // .. but it depends on all the other cases being handled,
        // since the PV may contain commas or spaces
        // that need to be ignored when inside quoted text or argument lists
        names = DroppedPVNameParser.parseDroppedPVs("loc://array(1, 2, 3)");
        assertThat(names, equalTo(List.of("loc://array(1, 2, 3)")));

        // loc://info("Text (with comma, spaces; and semicolon)")
        names = DroppedPVNameParser.parseDroppedPVs("loc://info(\"Text (with comma, spaces; and semicolon)\")");
        assertThat(names, equalTo(List.of("loc://info(\"Text (with comma, spaces; and semicolon)\")")));

        // loc://options<VEnum>(2, "A", "B (2)", "C")
        names = DroppedPVNameParser.parseDroppedPVs("loc://options<VEnum>(2, \"A\", \"B (2)\", \"C\")");
        assertThat(names, equalTo(List.of("loc://options<VEnum>(2, \"A\", \"B (2)\", \"C\")")));
   }

    @Test
    public void testNewline() throws Exception
    {
        // Plain newline
        List<String> names = DroppedPVNameParser.parseDroppedPVs("pv1\npv2");
        assertThat(names, equalTo(List.of("pv1", "pv2")));

        // Windows line ending
        names = DroppedPVNameParser.parseDroppedPVs("pv1\r\npv2");
        assertThat(names, equalTo(List.of("pv1", "pv2")));

        // Old Mac
        names = DroppedPVNameParser.parseDroppedPVs("pv1\rpv2");
        assertThat(names, equalTo(List.of("pv1", "pv2")));

        // Wrapped into 'array' brackets..
        names = DroppedPVNameParser.parseDroppedPVs("[ pv1\npv2 ] ");
        assertThat(names, equalTo(List.of("pv1", "pv2")));
    }

    @Test
    public void testTab() throws Exception
    {
        List<String> names = DroppedPVNameParser.parseDroppedPVs("pv1\tpv2");
        assertThat(names, equalTo(List.of("pv1", "pv2")));

        names = DroppedPVNameParser.parseDroppedPVs(" pv1 \t  pv2 \t pv3");
        assertThat(names, equalTo(List.of("pv1", "pv2", "pv3")));
    }

    @Test
    public void testComma() throws Exception
    {
        List<String> names = DroppedPVNameParser.parseDroppedPVs("pv1,pv2");
        assertThat(names, equalTo(List.of("pv1", "pv2")));

        names = DroppedPVNameParser.parseDroppedPVs(" pv1,   pv2 , pv3");
        assertThat(names, equalTo(List.of("pv1", "pv2", "pv3")));

        // Ignore commata inside quoted text
        names = DroppedPVNameParser.parseDroppedPVs(" pv1,   loc://txt(\"a, b, c\") , pv3");
        assertThat(names, equalTo(List.of("pv1", "loc://txt(\"a, b, c\")", "pv3")));

        // Ignore commata inside round brackets
        names = DroppedPVNameParser.parseDroppedPVs(" pv1,   sim://sine(1, 2, 3) , pv3");
        assertThat(names, equalTo(List.of("pv1", "sim://sine(1, 2, 3)", "pv3")));

        // Ignore closing brace inside quotes
        names = DroppedPVNameParser.parseDroppedPVs(" pv1,   loc://names(\"Fred\", \"Ed (3rd)\") , pv3");
        assertThat(names, equalTo(List.of("pv1", "loc://names(\"Fred\", \"Ed (3rd)\")", "pv3")));

        // Also allow semicolons
        names = DroppedPVNameParser.parseDroppedPVs(" pv1;   pv2 ; pv3");
        assertThat(names, equalTo(List.of("pv1", "pv2", "pv3")));
    }

    @Test
    public void testSpace() throws Exception
    {
        List<String> names = DroppedPVNameParser.parseDroppedPVs("pv1   pv2");
        assertThat(names, equalTo(List.of("pv1", "pv2")));

        // Ignore spaces in brackets
        names = DroppedPVNameParser.parseDroppedPVs("pv1   sim://sine(1, 2, 3)");
        assertThat(names, equalTo(List.of("pv1", "sim://sine(1, 2, 3)")));

        // Ignore spaces in quotes
        names = DroppedPVNameParser.parseDroppedPVs("  pv1   loc://tag(\"You're it!\")    ");
        assertThat(names, equalTo(List.of("pv1", "loc://tag(\"You're it!\")")));
    }
}

/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui;

import javafx.util.Pair;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/** Unit test of {@link DroppedPVNameParser}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DroppedPVNameParserTest
{
    private static Pair<String, String> duplicate(String string) {
        return new Pair(string, string);
    }

    @Test
    public void testSingle() throws Exception
    {
        // Just one PV name seems to be the easiest case..
        List<Pair<String, String>> names = DroppedPVNameParser.parseDroppedPVs("SomePVName");
        assertThat(names, equalTo(List.of(duplicate("SomePVName"))));

        // .. but it depends on all the other cases being handled,
        // since the PV may contain commas or spaces
        // that need to be ignored when inside quoted text or argument lists
        names = DroppedPVNameParser.parseDroppedPVs("loc://array(1, 2, 3)");
        assertThat(names, equalTo(List.of(duplicate("loc://array(1, 2, 3)"))));

        // loc://info("Text (with comma, spaces; and semicolon)")
        names = DroppedPVNameParser.parseDroppedPVs("loc://info(\"Text (with comma, spaces; and semicolon)\")");
        assertThat(names, equalTo(List.of(duplicate("loc://info(\"Text (with comma, spaces; and semicolon)\")"))));

        // loc://options<VEnum>(2, "A", "B (2)", "C")
        names = DroppedPVNameParser.parseDroppedPVs("loc://options<VEnum>(2, \"A\", \"B (2)\", \"C\")");
        assertThat(names, equalTo(List.of(duplicate("loc://options<VEnum>(2, \"A\", \"B (2)\", \"C\")"))));
   }

    @Test
    public void testNewline() throws Exception
    {
        // Plain newline
        List<Pair<String, String>> names = DroppedPVNameParser.parseDroppedPVs("pv1\npv2");
        assertThat(names, equalTo(List.of(duplicate("pv1"), duplicate("pv2"))));

        // Windows line ending
        names = DroppedPVNameParser.parseDroppedPVs("pv1\r\npv2");
        assertThat(names, equalTo(List.of(duplicate("pv1"), duplicate("pv2"))));

        // Old Mac
        names = DroppedPVNameParser.parseDroppedPVs("pv1\rpv2");
        assertThat(names, equalTo(List.of(duplicate("pv1"), duplicate("pv2"))));

        // Wrapped into 'array' brackets..
        names = DroppedPVNameParser.parseDroppedPVs("[ pv1\npv2 ] ");
        assertThat(names, equalTo(List.of(duplicate("pv1"), duplicate("pv2"))));
    }
}

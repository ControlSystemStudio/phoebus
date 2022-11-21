/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** JUnit test of JFXUtil
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class JFXUtilTest
{

    @Test
    public void testHex() {
        assertThat(JFXUtil.webHex(new WidgetColor(15, 255, 0)), equalTo("#0FFF00"));
        assertThat(JFXUtil.webHex(new WidgetColor(0, 16, 255)), equalTo("#0010FF"));
        assertThat(JFXUtil.webHex(new WidgetColor(0, 0, 0)), equalTo("#000000"));
        assertThat(JFXUtil.webHex(new WidgetColor(255, 255, 255)), equalTo("#FFFFFF"));
        assertThat(JFXUtil.webHex(null), equalTo(""));
    }

    @Test
    public void testRGB()
    {
        assertThat(JFXUtil.webRgbOrHex(new WidgetColor(15, 255, 0)), equalTo("#0FFF00"));
        assertThat(JFXUtil.webRgbOrHex(new WidgetColor(0, 16, 255)), equalTo("#0010FF"));
        assertThat(JFXUtil.webRgbOrHex(new WidgetColor(0, 16, 255, 50)), equalTo("rgba(0,16,255,0.19607843)"));
    }

    @Test
    public void tetCssFont() {

        // Given a font and prefix
        String prefix = "foobar";
        Font font = Font.font("serif", FontWeight.BOLD, FontPosture.ITALIC,37);

        // When converted
        String actual = JFXUtil.cssFont(prefix, font);

        // Then it matches as expected, note the system lookup finds Serif instead of serif
        // And the individual pieces are broken out instead of on one line
        String expected = "foobar-size: 37px;foobar-family: \"Serif\";foobar-weight: bold;foobar-style: italic;";
        assertEquals(expected, actual);

    }

    @Test
    public void testCssFontShorthand() {

        // Given a font and prefix
        String prefix = "foobar";
        Font font = Font.font("serif", FontWeight.BOLD, FontPosture.ITALIC,37);

        // When converted
        String actual = JFXUtil.cssFontShorthand(prefix, font);

        // Then it matches as expected, note the system lookup finds Serif instead of serif
        String expected = "foobar: bold italic 37px 'Serif';";
        assertEquals(expected, actual);

    }

    @Test
    public void testCssFontShorthandNonexistentFont() {

        // Given a font and prefix
        String prefix = "foobar";
        Font font = Font.font("My Fancy Font That Doesnt Exist", FontWeight.BOLD, FontPosture.ITALIC,37);

        // When converted
        String actual = JFXUtil.cssFontShorthand(prefix, font);

        // Then it matches as expected
        String expected = "foobar: bold italic 37px 'System';";
        assertEquals(expected, actual);

    }

}
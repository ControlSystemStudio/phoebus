/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.text;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import org.phoebus.util.text.StringUtil;
import static org.hamcrest.Matchers.*;

/**
 * Test simulated pv function names parsing
 *
 * @author carcassi
 */
public class StringUtilTest {

    public StringUtilTest() {
    }

    @Test
    public void unescapeString1() {
        assertThat(StringUtil.unescapeString("\\\""), equalTo("\""));
        assertThat(StringUtil.unescapeString("\\\"hello\\\""), equalTo("\"hello\""));
    }

    @Test
    public void unescapeString2() {
        assertThat(StringUtil.unescapeString("\\\\"), equalTo("\\"));
        assertThat(StringUtil.unescapeString("path\\\\to\\\\file"), equalTo("path\\to\\file"));
    }

    @Test
    public void unescapeString3() {
        assertThat(StringUtil.unescapeString("\\\'"), equalTo("\'"));
        assertThat(StringUtil.unescapeString("That\\\'s right!"), equalTo("That\'s right!"));
    }

    @Test
    public void unescapeString4() {
        assertThat(StringUtil.unescapeString("\\r"), equalTo("\r"));
        assertThat(StringUtil.unescapeString("This\\rThat"), equalTo("This\rThat"));
    }

    @Test
    public void unescapeString5() {
        assertThat(StringUtil.unescapeString("\\n"), equalTo("\n"));
        assertThat(StringUtil.unescapeString("This\\nThat"), equalTo("This\nThat"));
    }

    @Test
    public void unescapeString6() {
        assertThat(StringUtil.unescapeString("\\b"), equalTo("\b"));
        assertThat(StringUtil.unescapeString("Back\\bspace"), equalTo("Back\bspace"));
    }

    @Test
    public void unescapeString7() {
        assertThat(StringUtil.unescapeString("\\t"), equalTo("\t"));
        assertThat(StringUtil.unescapeString("Column one\\tColumn two"), equalTo("Column one\tColumn two"));
    }

    @Test
    public void unescapeString8() {
        assertThat(StringUtil.unescapeString("\\u0061"), equalTo("\u0061"));
        assertThat(StringUtil.unescapeString("Th\\u0061t is w\\u006fnderfu\\u006C!"), equalTo("That is wonderful!"));
    }

    @Test
    public void unescapeString9() {
        assertThat(StringUtil.unescapeString("\\141"), equalTo("\141"));
        assertThat(StringUtil.unescapeString("L\\141st \\612"), equalTo("Last 12"));
    }

    @Test
    public void unquoteString1() {
        assertThat(StringUtil.unquote("\"I said \\\"Hi\\\"\""), equalTo("I said \"Hi\""));
    }

    @Test
    public void parseCSVLine1() {
        String line = "\"a\" 1 2.3 \"b\"";
        List<Object> tokens = StringUtil.parseCSVLine(line, "\\s*");
        assertThat(tokens, equalTo(Arrays.<Object>asList("a", 1.0, 2.3, "b")));
    }

    @Test
    public void parseCSVLine2() {
        String line = "\"This is a test\" \"Another test\"  \"No spaces\"\"Between these two\"";
        List<Object> tokens = StringUtil.parseCSVLine(line, "\\s*");
        assertThat(tokens, equalTo(Arrays.<Object>asList("This is a test", "Another test", "No spaces", "Between these two")));
    }

    @Test
    public void parseCSVLine3() {
        String line = "\"And he asked:\\\"Does quoting works?\\\"\"";
        List<Object> tokens = StringUtil.parseCSVLine(line, "\\s*");
        assertThat(tokens, equalTo(Arrays.<Object>asList("And he asked:\"Does quoting works?\"")));
    }
}
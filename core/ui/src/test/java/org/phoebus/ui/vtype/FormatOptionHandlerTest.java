/*******************************************************************************
 * Copyright (c) 2015-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.vtype;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;

import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ArrayInteger;
import org.epics.util.array.ListNumber;
import org.epics.util.stats.Range;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.EnumDisplay;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VEnum;
import org.epics.vtype.VIntArray;
import org.epics.vtype.VLong;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VType;
import org.junit.Before;
import org.junit.Test;

/** JUnit test of {@link FormatOptionHandler}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FormatOptionHandlerTest
{
    final NumberFormat fmt = DecimalFormat.getNumberInstance(FormatOptionHandler.LOCALE);
    final Display display = Display.of(Range.of(-10, 10),
            Range.of(-9, 9),
            Range.of(-8, 8),
            Range.of(-10, 10), "V", fmt);

    @Before
    public void setup()
    {
        // To run test in a specific locale, set via
        //   java -Duser.country=SE -Duser.language=en
        // or in code:
        //   Locale.setDefault(new Locale("en", "SE"));
    }

    @Test
    public void testNaNInf() throws Exception
    {
        VType number = VDouble.of(Double.NaN, Alarm.none(), Time.now(), display);
        String text = FormatOptionHandler.format(number, FormatOption.DEFAULT, -1, true);
        System.out.println(text);
        assertThat(text, equalTo("NaN V"));

        number = VDouble.of(Double.POSITIVE_INFINITY, Alarm.none(), Time.now(), display);
        text = FormatOptionHandler.format(number, FormatOption.DEFAULT, -1, true);
        System.out.println(text);
        assertThat(text, equalTo("Infinity V"));

        number = VDouble.of(Double.NEGATIVE_INFINITY, Alarm.none(), Time.now(), display);
        text = FormatOptionHandler.format(number, FormatOption.DEFAULT, -1, true);
        System.out.println(text);
        assertThat(text, equalTo("-Infinity V"));
    }

    @Test
    public void testDecimal() throws Exception
    {
        VType number = VDouble.of(3.16, Alarm.none(), Time.now(), display);

        assertThat(fmt.format(3.16), equalTo("3.16"));

        String text = FormatOptionHandler.format(number, FormatOption.DEFAULT, -1, true);
        System.out.println(text);
        assertThat(text, equalTo("3.160 V"));

        text = FormatOptionHandler.format(number, FormatOption.DEFAULT, -1, false);
        System.out.println(text);
        assertThat(text, equalTo("3.160"));

        text = FormatOptionHandler.format(number, FormatOption.DECIMAL, 4, true);
        System.out.println(text);
        assertThat(text, equalTo("3.1600 V"));

        text = FormatOptionHandler.format(number, FormatOption.DECIMAL, 1, true);
        System.out.println(text);
        assertThat(text, equalTo("3.2 V"));

        // For running in debugger: Repeated use of precision 4 should use cached format
        text = FormatOptionHandler.format(number, FormatOption.DECIMAL, 4, true);
        System.out.println(text);
        assertThat(text, equalTo("3.1600 V"));
    }

    @Test
    public void testEnum() throws Exception
    {
        final VEnum value = VEnum.of(1, EnumDisplay.of("One", "Two"), Alarm.none(), Time.now());
        String text = FormatOptionHandler.format(value, FormatOption.DECIMAL, 4, true);
        System.out.println(text);
        assertThat(text, equalTo("1"));

        text = FormatOptionHandler.format(value, FormatOption.DEFAULT, -4, true);
        System.out.println(text);
        assertThat(text, equalTo("Two"));
    }

    @Test
    public void testExponential() throws Exception
    {
        VType number = VDouble.of(3.16, Alarm.none(), Time.now(), display);

        String text = FormatOptionHandler.format(number, FormatOption.DEFAULT, -1, true);
        System.out.println(text);
        assertThat(text, equalTo("3.160 V"));

        text = FormatOptionHandler.format(number, FormatOption.EXPONENTIAL, 3, true);
        System.out.println(text);
        assertThat(text, equalTo("3.160E0 V"));

        text = FormatOptionHandler.format(number, FormatOption.EXPONENTIAL, 1, true);
        System.out.println(text);
        assertThat(text, equalTo("3.2E0 V"));
    }

    @Test
    public void testEngineering() throws Exception
    {
        VType number = VDouble.of(0.0316, Alarm.none(), Time.now(), display);

        // 1 'significant digits': 31.6
        String text = FormatOptionHandler.format(number, FormatOption.ENGINEERING, 1, true);
        System.out.println(text);
        assertThat(text, equalTo("31.6E-3 V"));

        // 3 digits
        text = FormatOptionHandler.format(number, FormatOption.ENGINEERING, 3, false);
        System.out.println(text);
        assertThat(text, equalTo("31.600E-3"));

        // 4 'significant digits': 31.6000
        text = FormatOptionHandler.format(number, FormatOption.ENGINEERING, 4, true);
        System.out.println(text);
        assertThat(text, equalTo("31.6000E-3 V"));

        number = VDouble.of(12345678.0, Alarm.none(), Time.now(), display);
        text = FormatOptionHandler.format(number, FormatOption.ENGINEERING, 2, true);
        System.out.println(text);
        assertThat(text, equalTo("12.35E6 V"));

        text = FormatOptionHandler.format(number, FormatOption.ENGINEERING, 3, true);
        System.out.println(text);
        assertThat(text, equalTo("12.346E6 V"));

        // Can't use that to compute more digits for pi
        number = VDouble.of(3.14, Alarm.none(), Time.now(), display);
        text = FormatOptionHandler.format(number, FormatOption.ENGINEERING, 10, true);
        System.out.println(text);
        assertThat(text, equalTo("3.1400000000E0 V"));
    }

    @Test
    public void testHexFormat() throws Exception
    {
        VType number = VDouble.of(65535.0, Alarm.none(), Time.now(), display);

        String text = FormatOptionHandler.format(number, FormatOption.HEX, 4, true);
        System.out.println(text);
        assertThat(text, equalTo("0xFFFF V"));

        text = FormatOptionHandler.format(number, FormatOption.HEX, 8, true);
        System.out.println(text);
        assertThat(text, equalTo("0x0000FFFF V"));

        text = FormatOptionHandler.format(number, FormatOption.HEX, 16, true);
        System.out.println(text);
        assertThat(text, equalTo("0x000000000000FFFF V"));
    }

    @Test
    public void testHexParse() throws Exception
    {
        final VType number = VDouble.of(65535.0, Alarm.none(), Time.now(), display);

        // Parse hex as hex
        Object parsed = FormatOptionHandler.parse(number, "0xFF", FormatOption.HEX);
        System.out.println(parsed);
        assertThat(parsed, instanceOf(Number.class));
        assertThat(((Number)parsed).intValue(), equalTo(255));

        // Parse hex when using default format
        parsed = FormatOptionHandler.parse(number, "0x7777", FormatOption.DEFAULT);
        System.out.println(parsed);
        assertThat(parsed, instanceOf(Number.class));
        assertThat(((Number)parsed).intValue(), equalTo(30583));

        // Parse hex without '0x' identifier when using hex format
        parsed = FormatOptionHandler.parse(number, "0xFFFF", FormatOption.HEX);
        System.out.println(parsed);
        assertThat(parsed, instanceOf(Number.class));
        assertThat(((Number)parsed).intValue(), equalTo(65535));
    }

    @Test
    public void testString() throws Exception
    {   // Actual String
        VType value = VString.of("Test1", Alarm.none(), Time.now());
        String text = FormatOptionHandler.format(value, FormatOption.DEFAULT, -1, true);
        System.out.println(text);
        assertThat(text, equalTo("Test1"));

        text = FormatOptionHandler.format(value, FormatOption.STRING, -1, true);
        System.out.println(text);
        assertThat(text, equalTo("Test1"));

        text = FormatOptionHandler.format(value, FormatOption.EXPONENTIAL, -1, true);
        System.out.println(text);
        assertThat(text, equalTo("Test1"));

        // Number interpreted as char
        value = VDouble.of(65.0, Alarm.none(), Time.now(), display);
        text = FormatOptionHandler.format(value, FormatOption.STRING, -1, true);
        System.out.println(text);
        assertThat(text, equalTo("A V"));

        // Number array interpreted as long string
        ListNumber data = ArrayInteger.of(72, 101, 108, 108, 111); // UTF-8 for 'Hello'
        value = VIntArray.of(data, Alarm.none(), Time.now(), display);
        System.out.println(value);
        text = FormatOptionHandler.format(value, FormatOption.STRING, -1, true);
        System.out.println(text);
        assertThat(text, equalTo("Hello"));

        data = ArrayInteger.of(/* Dollar */ 0x24,  /* Euro */ 0xE2, 0x82, 0xAC);
        value = VIntArray.of(data, Alarm.none(), Time.now(), display);
        System.out.println(value);
        text = FormatOptionHandler.format(value, FormatOption.STRING, -1, true);
        System.out.println(text);
        // For this to work, Eclipse IDE Preferences -> General -> Workspace -> Text file encoding
        // must be set to UTF-8, which is the default on Linux but not necessarily Windows
        assertThat(text, equalTo("$€"));
    }

    @Test
    public void testCompact() throws Exception
    {
        String text = FormatOptionHandler.format(VDouble.of(65.43, Alarm.none(), Time.now(), display), FormatOption.COMPACT, 2, true);
        System.out.println(text);
        assertThat(text, equalTo("65.43 V"));

        text = FormatOptionHandler.format(VDouble.of(0.00006543, Alarm.none(), Time.now(), display), FormatOption.COMPACT, 2, true);
        System.out.println(text);
        assertThat(text, equalTo("6.54E-5 V"));

        text = FormatOptionHandler.format(VDouble.of(65430000.0, Alarm.none(), Time.now(), display), FormatOption.COMPACT, 2, true);
        System.out.println(text);
        assertThat(text, equalTo("6.54E7 V"));
    }

    @Test
    public void testBinary() throws Exception
    {
        String text = FormatOptionHandler.format(VLong.of(0b101010, Alarm.none(), Time.now(), display), FormatOption.BINARY, 10, true);
        System.out.println(text);
        assertThat(text, equalTo("0b0000101010 V"));

        text = FormatOptionHandler.format(VLong.of(0b101010, Alarm.none(), Time.now(), display), FormatOption.BINARY, 0, false);
        System.out.println(text);
        assertThat(text, equalTo("0b101010"));
    }

    @Test
    public void testBinaryParse() throws Exception
    {
        final VType number = VLong.of(0b101010, Alarm.none(), Time.now(), display);

        // Parse binary as binary
        Object parsed = FormatOptionHandler.parse(number, "0b0000101010", FormatOption.BINARY);
        System.out.println(parsed);
        assertThat(parsed, instanceOf(Number.class));
        assertThat(((Number)parsed).intValue(), equalTo(42));

        // Parse binary when using default format
        parsed = FormatOptionHandler.parse(number, "0b1111111111111111", FormatOption.DEFAULT);
        System.out.println(parsed);
        assertThat(parsed, instanceOf(Number.class));
        assertThat(((Number)parsed).intValue(), equalTo(65535));

        // Parse binary without '0b' identifier when using hex format
        parsed = FormatOptionHandler.parse(number, "0000101010", FormatOption.BINARY);
        System.out.println(parsed);
        assertThat(parsed, instanceOf(Number.class));
        assertThat(((Number)parsed).intValue(), equalTo(42));
    }

    @Test
    public void testArray() throws Exception
    {
        final ListNumber data = ArrayDouble.of(1.0, 2.0, 3.0, 4.0);
        VType value = VNumberArray.of(data, Alarm.none(), Time.now(), display);
        System.out.println(value);
        String text = FormatOptionHandler.format(value, FormatOption.DEFAULT, 0, true);
        System.out.println(text);
        assertThat(text, equalTo("[1, 2, 3, 4] V"));

        text = FormatOptionHandler.format(value, FormatOption.DECIMAL, 2, true);
        System.out.println(text);
        assertThat(text, equalTo("[1.00, 2.00, 3.00, 4.00] V"));
    }

    @Test
    public void testSexagesimalFormat() throws Exception
    {
        final VType sexaPositiveValue = VDouble.of(12.5824414, Alarm.none(), Time.now(), display),
                    sexaNegativeValue = VDouble.of(-12.5824414, Alarm.none(), Time.now(), display),
                    sexaRoundedValue = VDouble.of(12.9999999, Alarm.none(), Time.now(), display);
        assertThat(FormatOptionHandler.format(sexaPositiveValue, FormatOption.SEXAGESIMAL, 7, false), equalTo("12:34:56.789"));
        assertThat(FormatOptionHandler.format(sexaPositiveValue, FormatOption.SEXAGESIMAL, 2, false), equalTo("12:35"));
        assertThat(FormatOptionHandler.format(sexaPositiveValue, FormatOption.SEXAGESIMAL, 4, false), equalTo("12:34:57"));
        assertThat(FormatOptionHandler.format(sexaNegativeValue, FormatOption.SEXAGESIMAL, 7, false), equalTo("-12:34:56.789"));
        assertThat(FormatOptionHandler.format(sexaRoundedValue, FormatOption.SEXAGESIMAL, 7, false), equalTo("13:00:00.000"));
        assertThat(FormatOptionHandler.format(sexaRoundedValue, FormatOption.SEXAGESIMAL, 8, false), equalTo("12:59:59.9996"));

        assertThat(FormatOptionHandler.format(VDouble.of(2*Math.PI, Alarm.none(), Time.now(), display), FormatOption.SEXAGESIMAL_HMS, 7, false), equalTo("24:00:00.000"));
        assertThat(FormatOptionHandler.format(sexaPositiveValue, FormatOption.SEXAGESIMAL_HMS, 7, false), equalTo("48:03:40.989"));
        assertThat(FormatOptionHandler.format(sexaNegativeValue, FormatOption.SEXAGESIMAL_HMS, 7, false), equalTo("-48:03:40.989"));

        assertThat(FormatOptionHandler.format(VDouble.of(2*Math.PI, Alarm.none(), Time.now(), display), FormatOption.SEXAGESIMAL_DMS, 7, false), equalTo("360:00:00.000"));
        assertThat(FormatOptionHandler.format(sexaPositiveValue, FormatOption.SEXAGESIMAL_DMS, 7, false), equalTo("720:55:14.837"));
        assertThat(FormatOptionHandler.format(sexaNegativeValue, FormatOption.SEXAGESIMAL_DMS, 7, false), equalTo("-720:55:14.837"));
        assertThat(FormatOptionHandler.format(sexaRoundedValue, FormatOption.SEXAGESIMAL_DMS, 7, false), equalTo("744:50:42.461"));
    }

    @Test
    public void testSexagesimalParser() throws Exception
    {
        final VType number = VDouble.of(0.0, Alarm.none(), Time.now(), display);
        assertEquals(12.5824414, (Double)FormatOptionHandler.parse(number, "12:34:56.789", FormatOption.SEXAGESIMAL), 0.0000001);
        assertEquals(Math.PI, (Double)FormatOptionHandler.parse(number, "12:00:00.000", FormatOption.SEXAGESIMAL_HMS), 0.0000001);
        assertEquals(12.5824414, (Double)FormatOptionHandler.parse(number, "48:03:40.989", FormatOption.SEXAGESIMAL_HMS), 0.0000001);
        assertEquals(Math.PI, (Double)FormatOptionHandler.parse(number, "180:00:00.000", FormatOption.SEXAGESIMAL_DMS), 0.0000001);
        assertEquals(12.5824414, (Double)FormatOptionHandler.parse(number, "720:55:14.837", FormatOption.SEXAGESIMAL_DMS), 0.0000001);
    }

    @Test
    public void testNumberParsing() throws Exception
    {
        VType value = VDouble.of(3.16, Alarm.none(), Time.now(), display);

        Object parsed = FormatOptionHandler.parse(value, "42.5 Socks", FormatOption.DEFAULT);
        assertThat(parsed, instanceOf(Number.class));
        assertThat(((Number)parsed).doubleValue(), equalTo(42.5));
    }

    @Test
    public void testNumberArrayParsing() throws Exception
    {
        final ListNumber data = ArrayDouble.of(1.0, 2.0, 3.0, 4.0);
        final VType value = VNumberArray.of(data, Alarm.none(), Time.now(), display);

        Object parsed = FormatOptionHandler.parse(value, " [  1, 2.5  ,  3 ] ", FormatOption.DEFAULT);
        assertThat(parsed, instanceOf(double[].class));
        final double[] numbers = (double[]) parsed;
        assertThat(numbers, equalTo(new double[] { 1.0, 2.5, 3.0 }));
    }

    @Test
    public void testStringArrayParsing() throws Exception
    {
        final VType value = VStringArray.of(Arrays.asList("Flintstone, \"Al\" Fred", "Jane"), Alarm.none(), Time.now());

        final String text = FormatOptionHandler.format(value, FormatOption.DEFAULT, 0, true);
        System.out.println(text);

        Object parsed = FormatOptionHandler.parse(value, text, FormatOption.DEFAULT);
        System.out.println(Arrays.toString((String[])parsed));
        assertThat(parsed, equalTo(new String[] { "Flintstone, \"Al\" Fred", "Jane" }));

        parsed = FormatOptionHandler.parse(value, "[ \"Freddy\", \"Janet\" ] ", FormatOption.DEFAULT);
        assertThat(parsed, instanceOf(String[].class));
        assertThat(parsed, equalTo(new String[] { "Freddy", "Janet" }));

        parsed = FormatOptionHandler.parse(value, "[ Freddy, Janet ] ", FormatOption.DEFAULT);
        assertThat(parsed, equalTo(new String[] { "Freddy", "Janet" }));

        parsed = FormatOptionHandler.parse(value, "Freddy, Janet", FormatOption.DEFAULT);
        assertThat(parsed, equalTo(new String[] { "Freddy", "Janet" }));

        parsed = FormatOptionHandler.parse(value, " [ \"Flintstone, Fred\", Janet", FormatOption.DEFAULT);
        assertThat(parsed, equalTo(new String[] { "Flintstone, Fred", "Janet" }));

        parsed = FormatOptionHandler.parse(value, " \"Al \\\"Ed\\\" Stone\", Jane", FormatOption.DEFAULT);
        System.out.println(Arrays.toString((String[])parsed));
        assertThat(parsed, equalTo(new String[] { "Al \"Ed\" Stone", "Jane" }));
    }
}

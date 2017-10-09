/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.text;

import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import org.phoebus.util.array.ArrayDouble;
import org.phoebus.util.array.ListNumber;
import org.junit.Test;
import static org.junit.Assert.*;
import org.phoebus.util.text.CsvParserResult;
import static org.hamcrest.Matchers.*;

/**
 * Tests CSV parser
 *
 * @author carcassi
 */
public class CsvParserTest {

    @Test
    public void parseCSVLine1() {
        String line = "\"a\" 1 2.3 \"b\"";
        List<Object> tokens = CsvParser.parseCSVLine(line, " ");
        assertThat(tokens, equalTo(Arrays.<Object>asList("a", 1.0, 2.3, "b")));
    }

    @Test
    public void parseCSVLine2() {
        String line = "\"This is a test\" \"Another test\" \"No spaces\" \"Between these two\"";
        List<Object> tokens = CsvParser.parseCSVLine(line, " ");
        assertThat(tokens, equalTo(Arrays.<Object>asList("This is a test", "Another test", "No spaces", "Between these two")));
    }

    @Test
    public void parseCSVLine3() {
        String line = "\"And he asked:\"\"Does quoting works?\"\"\"";
        List<Object> tokens = CsvParser.parseCSVLine(line, " ");
        assertThat(tokens, equalTo(Arrays.<Object>asList("And he asked:\"Does quoting works?\"")));
    }

    @Test
    public void parseCSVLine4() {
        String line = "1 2 3 4";
        List<Object> tokens = CsvParser.parseCSVLine(line, " ");
        assertThat(tokens, equalTo(Arrays.<Object>asList(1.0, 2.0, 3.0, 4.0)));
    }

    @Test
    public void parseCSVLine5() {
        String line = "\"Name\" \"Value\" \"Index\"\n";
        List<Object> tokens = CsvParser.parseCSVLine(line, " ");
        assertThat(tokens, equalTo(Arrays.<Object>asList("Name", "Value", "Index")));
    }

    @Test
    public void parseCSVLine6() {
        String line = "\"A\" 0.234 1";
        List<Object> tokens = CsvParser.parseCSVLine(line, " ");
        assertThat(tokens, equalTo(Arrays.<Object>asList("A", 0.234, 1.0)));
    }

    @Test
    public void parseCSVLine7() {
        String line = "\"One \nTwo\nThree\"";
        List<Object> tokens = CsvParser.parseCSVLine(line, " ");
        assertThat(tokens, equalTo(Arrays.<Object>asList("One \nTwo\nThree")));
    }

    @Test
    public void parseTable1CSV() throws Exception {
        CsvParserResult result = CsvParser.AUTOMATIC.parse(new InputStreamReader(getClass().getResource("table1.csv").openStream()));
        assertThat(result.getColumnNames().size(), equalTo(3));
        assertThat(result.getColumnNames().get(0), equalTo("Name"));
        assertThat(result.getColumnNames().get(1), equalTo("Value"));
        assertThat(result.getColumnNames().get(2), equalTo("Index"));
        assertThat((Object) result.getColumnTypes().get(0), equalTo((Object) String.class));
        assertThat((Object) result.getColumnTypes().get(1), equalTo((Object) double.class));
        assertThat((Object) result.getColumnTypes().get(2), equalTo((Object) double.class));
        assertThat(result.getColumnValues().get(0), equalTo((Object) Arrays.asList("A\"", "B", "C", "D", "E")));
        assertThat(result.getColumnValues().get(1), equalTo((Object) new ArrayDouble(0.234, 1.456, 234567891234.0, 0.000000123, 123)));
        assertThat(result.getColumnValues().get(2), equalTo((Object) new ArrayDouble(1,2,3,4,5)));
    }

    @Test
    public void parseFileTable2CSV() throws Exception {
        CsvParserResult result = CsvParser.AUTOMATIC.parse(new InputStreamReader(getClass().getResource("table2.csv").openStream()));
        assertThat(result.getColumnNames().size(), equalTo(3));
        assertThat(result.getColumnNames().get(0), equalTo("Name"));
        assertThat(result.getColumnNames().get(1), equalTo("Value"));
        assertThat(result.getColumnNames().get(2), equalTo("Index"));
        assertThat((Object) result.getColumnTypes().get(0), equalTo((Object) String.class));
        assertThat((Object) result.getColumnTypes().get(1), equalTo((Object) double.class));
        assertThat((Object) result.getColumnTypes().get(2), equalTo((Object) double.class));
        assertThat(result.getColumnValues().get(0), equalTo((Object) Arrays.asList("A", "B", "C", "D", "E")));
        assertThat(result.getColumnValues().get(1), equalTo((Object) new ArrayDouble(0.234, 1.456, 234567891234.0, 0.000000123, 123)));
        assertThat(result.getColumnValues().get(2), equalTo((Object) new ArrayDouble(1,2,3,4,5)));
    }

    @Test
    public void parseFileTable3CSV() throws Exception {
        CsvParserResult result = CsvParser.AUTOMATIC.parse(new InputStreamReader(getClass().getResource("table3.csv").openStream()));
        assertThat(result.getColumnNames().size(), equalTo(3));
        assertThat(result.getColumnNames().get(0), equalTo("Author"));
        assertThat(result.getColumnNames().get(1), equalTo("Time"));
        assertThat(result.getColumnNames().get(2), equalTo("Message"));
        assertThat((Object) result.getColumnTypes().get(0), equalTo((Object) String.class));
        assertThat((Object) result.getColumnTypes().get(1), equalTo((Object) double.class));
        assertThat((Object) result.getColumnTypes().get(2), equalTo((Object) String.class));
        assertThat(result.getColumnValues().get(0), equalTo((Object) Arrays.asList("Gabriele Carcassi", "Kunal Shroff", "Eric Berryman")));
        assertThat(result.getColumnValues().get(1), equalTo((Object) new ArrayDouble(201401281140.0, 201401281150.0, 201401281160.0)));
        assertThat(result.getColumnValues().get(2), equalTo((Object) Arrays.asList("This is my message:\nI said \"Hi!\" to everybody",
                "I am busy", "Shopping list:\n* potatoes\n* carrots")));
    }

    @Test
    public void parseFileTable4CSV() throws Exception {
        CsvParserResult result = CsvParser.AUTOMATIC.parse(new InputStreamReader(getClass().getResource("table4.csv").openStream()));
        assertThat(result.getColumnNames().size(), equalTo(13));
        assertThat(result.getColumnNames().get(0), equalTo("timestamp"));
        assertThat(result.getColumnNames().get(1), equalTo("rta_MIN"));
        assertThat(result.getColumnNames().get(2), equalTo("rta_MAX"));
        assertThat((Object) result.getColumnTypes().get(0), equalTo((Object) double.class));
        assertThat((Object) result.getColumnTypes().get(1), equalTo((Object) double.class));
        assertThat((Object) result.getColumnTypes().get(2), equalTo((Object) double.class));
        assertThat(((ListNumber) result.getColumnValues().get(0)).getDouble(0), equalTo(1390913220.0));
        assertThat(((ListNumber) result.getColumnValues().get(1)).getDouble(1), equalTo(0.28083333333));
        assertThat(((ListNumber) result.getColumnValues().get(2)).getDouble(2), equalTo(0.266825));
    }

    @Test
    public void parseFileTable5CSV() throws Exception {
        CsvParserResult result = CsvParser.AUTOMATIC.parse(new InputStreamReader(getClass().getResource("table5.csv").openStream()));
        assertThat(result.getColumnNames().size(), equalTo(3));
        assertThat(result.getColumnNames().get(0), equalTo("A"));
        assertThat(result.getColumnNames().get(1), equalTo("B"));
        assertThat(result.getColumnNames().get(2), equalTo("C"));
        assertThat((Object) result.getColumnTypes().get(0), equalTo((Object) String.class));
        assertThat((Object) result.getColumnTypes().get(1), equalTo((Object) double.class));
        assertThat((Object) result.getColumnTypes().get(2), equalTo((Object) double.class));
        assertThat(result.getColumnValues().get(0), equalTo((Object) Arrays.asList("A", "B", "C", "D", "E")));
        assertThat(result.getColumnValues().get(1), equalTo((Object) new ArrayDouble(0.234, 1.456, 234567891234.0, 0.000000123, 123)));
        assertThat(result.getColumnValues().get(2), equalTo((Object) new ArrayDouble(1,2,3,4,5)));
    }

    @Test
    public void parseFileTable5CSVbis() throws Exception {
        CsvParserResult result = CsvParser.AUTOMATIC.withHeader(CsvParser.Header.FIRST_LINE).parse(new InputStreamReader(getClass().getResource("table5.csv").openStream()));
        assertThat(result.getColumnNames().size(), equalTo(3));
        assertThat(result.getColumnNames().get(0), equalTo("A"));
        assertThat(result.getColumnNames().get(1), equalTo("0.234"));
        assertThat(result.getColumnNames().get(2), equalTo("1"));
        assertThat((Object) result.getColumnTypes().get(0), equalTo((Object) String.class));
        assertThat((Object) result.getColumnTypes().get(1), equalTo((Object) double.class));
        assertThat((Object) result.getColumnTypes().get(2), equalTo((Object) double.class));
        assertThat(result.getColumnValues().get(0), equalTo((Object) Arrays.asList("B", "C", "D", "E")));
        assertThat(result.getColumnValues().get(1), equalTo((Object) new ArrayDouble(1.456, 234567891234.0, 0.000000123, 123)));
        assertThat(result.getColumnValues().get(2), equalTo((Object) new ArrayDouble(2,3,4,5)));
    }

    @Test
    public void parseFileTable6CSV() throws Exception {
        CsvParserResult result = CsvParser.AUTOMATIC.parse(new InputStreamReader(getClass().getResource("table6.csv").openStream()));
        assertThat(result.getColumnNames().size(), equalTo(3));
        assertThat(result.getColumnNames().get(0), equalTo("Name"));
        assertThat(result.getColumnNames().get(1), equalTo("Surname"));
        assertThat(result.getColumnNames().get(2), equalTo("Address"));
        assertThat((Object) result.getColumnTypes().get(0), equalTo((Object) String.class));
        assertThat((Object) result.getColumnTypes().get(1), equalTo((Object) String.class));
        assertThat((Object) result.getColumnTypes().get(2), equalTo((Object) String.class));
        assertThat(result.getColumnValues().get(0), equalTo((Object) Arrays.asList("Gabriele", "Kunal")));
        assertThat(result.getColumnValues().get(1), equalTo((Object) Arrays.asList("Carcassi", "Shroff")));
        assertThat(result.getColumnValues().get(2), equalTo((Object) Arrays.asList("Happytown", "Politeville")));
    }

    @Test
    public void parseFileTable6CSVbis() throws Exception {
        CsvParserResult result = CsvParser.AUTOMATIC.withHeader(CsvParser.Header.NONE).parse(new InputStreamReader(getClass().getResource("table6.csv").openStream()));
        assertThat(result.getColumnNames().size(), equalTo(3));
        assertThat(result.getColumnNames().get(0), equalTo("A"));
        assertThat(result.getColumnNames().get(1), equalTo("B"));
        assertThat(result.getColumnNames().get(2), equalTo("C"));
        assertThat((Object) result.getColumnTypes().get(0), equalTo((Object) String.class));
        assertThat((Object) result.getColumnTypes().get(1), equalTo((Object) String.class));
        assertThat((Object) result.getColumnTypes().get(2), equalTo((Object) String.class));
        assertThat(result.getColumnValues().get(0), equalTo((Object) Arrays.asList("Name", "Gabriele", "Kunal")));
        assertThat(result.getColumnValues().get(1), equalTo((Object) Arrays.asList("Surname", "Carcassi", "Shroff")));
        assertThat(result.getColumnValues().get(2), equalTo((Object) Arrays.asList("Address", "Happytown", "Politeville")));
    }

    @Test
    public void parseFileTable7CSV() throws Exception {
        CsvParserResult result = CsvParser.AUTOMATIC.withSeparators("~").parse(new InputStreamReader(getClass().getResource("table7.csv").openStream()));
        assertThat(result.getColumnNames().size(), equalTo(3));
        assertThat(result.getColumnNames().get(0), equalTo("Name"));
        assertThat(result.getColumnNames().get(1), equalTo("Value"));
        assertThat(result.getColumnNames().get(2), equalTo("Index"));
        assertThat((Object) result.getColumnTypes().get(0), equalTo((Object) String.class));
        assertThat((Object) result.getColumnTypes().get(1), equalTo((Object) double.class));
        assertThat((Object) result.getColumnTypes().get(2), equalTo((Object) double.class));
        assertThat(result.getColumnValues().get(0), equalTo((Object) Arrays.asList("A", "B", "C", "D", "E")));
        assertThat(result.getColumnValues().get(1), equalTo((Object) new ArrayDouble(0.234, 1.456, 234567891234.0, 0.000000123, 123)));
        assertThat(result.getColumnValues().get(2), equalTo((Object) new ArrayDouble(1,2,3,4,5)));
    }

    @Test
    public void parseTable8CSV() throws Exception {
        CsvParserResult result = CsvParser.AUTOMATIC.parse(new InputStreamReader(getClass().getResource("table8.csv").openStream()));
        assertThat(result.getColumnNames().size(), equalTo(3));
        assertThat(result.getColumnNames().get(0), equalTo("Name"));
        assertThat(result.getColumnNames().get(1), equalTo("Value"));
        assertThat(result.getColumnNames().get(2), equalTo("Index"));
        assertThat((Object) result.getColumnTypes().get(0), equalTo((Object) String.class));
        assertThat((Object) result.getColumnTypes().get(1), equalTo((Object) double.class));
        assertThat((Object) result.getColumnTypes().get(2), equalTo((Object) double.class));
        assertThat(result.getColumnValues().get(0), equalTo((Object) Arrays.asList("", "B", "C", "D", "E")));
        assertThat(result.getColumnValues().get(1), equalTo((Object) new ArrayDouble(Double.NaN, 1.456, 234567891234.0, 0.000000123, 0.0)));
        assertThat(result.getColumnValues().get(2), equalTo((Object) new ArrayDouble(Double.NaN,2,3,4,5)));
    }

    @Test
    public void parseTable9CSV() throws Exception {
        CsvParserResult result = CsvParser.AUTOMATIC.parse(new InputStreamReader(getClass().getResource("table9.csv").openStream()));
        assertThat(result.getColumnNames().size(), equalTo(3));
        assertThat(result.getColumnNames().get(0), equalTo("A"));
        assertThat(result.getColumnNames().get(1), equalTo("B"));
        assertThat(result.getColumnNames().get(2), equalTo("C"));
        assertThat((Object) result.getColumnTypes().get(0), equalTo((Object) String.class));
        assertThat((Object) result.getColumnTypes().get(1), equalTo((Object) double.class));
        assertThat((Object) result.getColumnTypes().get(2), equalTo((Object) double.class));
        assertThat(result.getColumnValues().get(0), equalTo((Object) Arrays.asList("a", "b", "c", "", "e")));
        assertThat(result.getColumnValues().get(1), equalTo((Object) new ArrayDouble(1, Double.NaN, Double.NaN, 4, Double.NaN)));
        assertThat(result.getColumnValues().get(2), equalTo((Object) new ArrayDouble(1,2,3, Double.NaN, Double.NaN)));
    }
}
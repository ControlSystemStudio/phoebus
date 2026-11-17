/*******************************************************************************
 * Copyright (c) 2024 Fermi National Accelerator Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.command;

import org.csstudio.scan.util.PVReference;
import org.csstudio.scan.util.StringOrDouble;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the PVReference feature in scan commands.
 *
 * <p>Covers three layers:
 * <ol>
 *   <li>{@link StringOrDouble} parse/quote round-trip</li>
 *   <li>{@link IfCommand}, {@link WaitCommand}, {@link WhileCommand},
 *       {@link LoopCommand} model XML round-trip</li>
 *   <li>Verification that PVReference is preserved (not silently coerced to String or Double)</li>
 * </ol>
 *
 * <p>To run:  mvn test -pl scan-app-fnal/model -Dtest=PVReferenceTest
 */
@SuppressWarnings("nls")
public class PVReferenceTest
{
    // -------------------------------------------------------------------------
    // 1. StringOrDouble parser
    // -------------------------------------------------------------------------

    @Test
    public void testParseDouble()
    {
        Object v = StringOrDouble.parse("5.3");
        assertInstanceOf(Double.class, v, "Plain number should parse to Double");
        assertEquals(5.3, (Double) v, 1e-9);
    }

    @Test
    public void testParseNegativeDouble()
    {
        Object v = StringOrDouble.parse("-12.0");
        assertInstanceOf(Double.class, v);
        assertEquals(-12.0, (Double) v, 1e-9);
    }

    @Test
    public void testParseQuotedString()
    {
        Object v = StringOrDouble.parse("\"hello world\"");
        assertInstanceOf(String.class, v, "Quoted text should parse to String literal");
        assertEquals("hello world", (String) v);
    }

    @Test
    public void testParseUnquotedNonNumeric_becomesPVReference()
    {
        Object v = StringOrDouble.parse("some:ref:pv");
        assertInstanceOf(PVReference.class, v,
                "Unquoted non-numeric text should parse as PVReference, not String");
        assertEquals("some:ref:pv", ((PVReference) v).getPVName());
    }

    @Test
    public void testParseEmpty()
    {
        Object v = StringOrDouble.parse("");
        assertInstanceOf(Double.class, v);
        assertEquals(0.0, (Double) v, 1e-9);
    }

    @Test
    public void testQuoteDouble()
    {
        assertEquals("5.3", StringOrDouble.quote(5.3));
    }

    @Test
    public void testQuoteString()
    {
        assertEquals("\"hello\"", StringOrDouble.quote("hello"));
    }

    @Test
    public void testQuotePVReference()
    {
        // PVReference must serialise without quotes so it round-trips back as PVReference
        assertEquals("some:ref:pv", StringOrDouble.quote(new PVReference("some:ref:pv")));
    }

    @Test
    public void testRoundTrip_PVReference()
    {
        final String pv = "my:threshold:pv";
        final String quoted = StringOrDouble.quote(new PVReference(pv));
        final Object parsed = StringOrDouble.parse(quoted);
        assertInstanceOf(PVReference.class, parsed);
        assertEquals(pv, ((PVReference) parsed).getPVName());
    }

    // -------------------------------------------------------------------------
    // 2. IfCommand XML round-trip
    // -------------------------------------------------------------------------

    @Test
    public void testIfCommand_numericValue_roundTrip() throws Exception
    {
        final IfCommand cmd = new IfCommand("my:device", Comparison.ABOVE, 42.0,
                                            Collections.emptyList());
        final String xml = XMLCommandWriter.toXMLString(List.of(cmd));
        System.out.println("IfCommand (numeric) XML:\n" + xml);

        final List<ScanCommand> readBack = XMLCommandReader.readXMLString(xml);
        assertEquals(1, readBack.size());
        final IfCommand result = (IfCommand) readBack.get(0);
        assertInstanceOf(Double.class, result.getDesiredValue());
        assertEquals(42.0, (Double) result.getDesiredValue(), 1e-9);
    }

    @Test
    public void testIfCommand_stringLiteral_roundTrip() throws Exception
    {
        final IfCommand cmd = new IfCommand("my:device", Comparison.EQUALS, "OPEN",
                                            Collections.emptyList());
        final String xml = XMLCommandWriter.toXMLString(List.of(cmd));
        System.out.println("IfCommand (string) XML:\n" + xml);

        final List<ScanCommand> readBack = XMLCommandReader.readXMLString(xml);
        final IfCommand result = (IfCommand) readBack.get(0);
        assertInstanceOf(String.class, result.getDesiredValue(),
                "Quoted string should stay a String literal");
        assertEquals("OPEN", result.getDesiredValue());
    }

    @Test
    public void testIfCommand_pvReference_roundTrip() throws Exception
    {
        // desired_value is a PVReference — compare 'my:device' against live value of 'ref:threshold'
        final IfCommand cmd = new IfCommand("my:device", Comparison.ABOVE,
                                            new PVReference("ref:threshold"),
                                            Collections.emptyList());
        final String xml = XMLCommandWriter.toXMLString(List.of(cmd));
        System.out.println("IfCommand (PVRef) XML:\n" + xml);

        // XML must contain the bare PV name WITHOUT quotes
        assertTrue(xml.contains("<value>ref:threshold</value>"),
                "PVReference should be written as bare PV name, got:\n" + xml);

        final List<ScanCommand> readBack = XMLCommandReader.readXMLString(xml);
        final IfCommand result = (IfCommand) readBack.get(0);
        assertInstanceOf(PVReference.class, result.getDesiredValue(),
                "Unquoted non-numeric value should round-trip back as PVReference");
        assertEquals("ref:threshold", ((PVReference) result.getDesiredValue()).getPVName());
    }

    // -------------------------------------------------------------------------
    // 3. WaitCommand XML round-trip
    // -------------------------------------------------------------------------

    @Test
    public void testWaitCommand_pvReference_roundTrip() throws Exception
    {
        final WaitCommand cmd = new WaitCommand("my:device", Comparison.ABOVE,
                                                new PVReference("ref:threshold"), 0.1, 10.0);
        final String xml = XMLCommandWriter.toXMLString(List.of(cmd));
        System.out.println("WaitCommand (PVRef) XML:\n" + xml);

        assertTrue(xml.contains("<value>ref:threshold</value>"),
                "PVReference should be written as bare PV name");

        final List<ScanCommand> readBack = XMLCommandReader.readXMLString(xml);
        final WaitCommand result = (WaitCommand) readBack.get(0);
        assertInstanceOf(PVReference.class, result.getDesiredValue());
        assertEquals("ref:threshold", ((PVReference) result.getDesiredValue()).getPVName());
        assertEquals("my:device", result.getDeviceName());
        assertEquals(Comparison.ABOVE, result.getComparison());
    }

    // -------------------------------------------------------------------------
    // 4. WhileCommand XML round-trip
    // -------------------------------------------------------------------------

    @Test
    public void testWhileCommand_pvReference_roundTrip() throws Exception
    {
        final WhileCommand cmd = new WhileCommand("loop:device", Comparison.ABOVE,
                                                  new PVReference("loop:limit"),
                                                  Collections.emptyList());
        final String xml = XMLCommandWriter.toXMLString(List.of(cmd));
        System.out.println("WhileCommand (PVRef) XML:\n" + xml);

        assertTrue(xml.contains("<value>loop:limit</value>"),
                "PVReference should be written as bare PV name");

        final List<ScanCommand> readBack = XMLCommandReader.readXMLString(xml);
        final WhileCommand result = (WhileCommand) readBack.get(0);
        assertInstanceOf(PVReference.class, result.getDesiredValue());
        assertEquals("loop:limit", ((PVReference) result.getDesiredValue()).getPVName());
    }

    // -------------------------------------------------------------------------
    // 5. LoopCommand (for) XML round-trip — start / end / step as PVReference
    // -------------------------------------------------------------------------

    @Test
    public void testLoopCommand_allLiteral_roundTrip() throws Exception
    {
        final LoopCommand cmd = new LoopCommand("motor:position", 0.0, 10.0, 2.0);
        final String xml = XMLCommandWriter.toXMLString(List.of(cmd));
        System.out.println("LoopCommand (literals) XML:\n" + xml);

        final List<ScanCommand> readBack = XMLCommandReader.readXMLString(xml);
        final LoopCommand result = (LoopCommand) readBack.get(0);
        assertInstanceOf(Double.class, result.getStart());
        assertInstanceOf(Double.class, result.getEnd());
        assertInstanceOf(Double.class, result.getStepSize());
        assertEquals(0.0,  (Double) result.getStart(),    1e-9);
        assertEquals(10.0, (Double) result.getEnd(),      1e-9);
        assertEquals(2.0,  (Double) result.getStepSize(), 1e-9);
    }

    @Test
    public void testLoopCommand_startAsPVReference_roundTrip() throws Exception
    {
        final LoopCommand cmd = new LoopCommand("motor:position", 0.0, 10.0, 1.0);
        cmd.setStart(new PVReference("scan:start:pv"));
        final String xml = XMLCommandWriter.toXMLString(List.of(cmd));
        System.out.println("LoopCommand (start=PVRef) XML:\n" + xml);

        assertTrue(xml.contains("<start>scan:start:pv</start>"),
                "PVReference start should be written as bare PV name, got:\n" + xml);

        final List<ScanCommand> readBack = XMLCommandReader.readXMLString(xml);
        final LoopCommand result = (LoopCommand) readBack.get(0);
        assertInstanceOf(PVReference.class, result.getStart());
        assertEquals("scan:start:pv", ((PVReference) result.getStart()).getPVName());
        // end and step remain literals
        assertInstanceOf(Double.class, result.getEnd());
        assertInstanceOf(Double.class, result.getStepSize());
    }

    @Test
    public void testLoopCommand_allAsPVReference_roundTrip() throws Exception
    {
        final LoopCommand cmd = new LoopCommand("motor:position", 0.0, 10.0, 1.0);
        cmd.setStart(new PVReference("scan:start:pv"));
        cmd.setEnd(new PVReference("scan:end:pv"));
        cmd.setStepSize(new PVReference("scan:step:pv"));

        final String xml = XMLCommandWriter.toXMLString(List.of(cmd));
        System.out.println("LoopCommand (all PVRef) XML:\n" + xml);

        assertTrue(xml.contains("<start>scan:start:pv</start>"));
        assertTrue(xml.contains("<end>scan:end:pv</end>"));
        assertTrue(xml.contains("<step>scan:step:pv</step>"));

        final List<ScanCommand> readBack = XMLCommandReader.readXMLString(xml);
        final LoopCommand result = (LoopCommand) readBack.get(0);
        assertInstanceOf(PVReference.class, result.getStart());
        assertInstanceOf(PVReference.class, result.getEnd());
        assertInstanceOf(PVReference.class, result.getStepSize());
        assertEquals("scan:start:pv", ((PVReference) result.getStart()).getPVName());
        assertEquals("scan:end:pv",   ((PVReference) result.getEnd()).getPVName());
        assertEquals("scan:step:pv",  ((PVReference) result.getStepSize()).getPVName());
    }

    // -------------------------------------------------------------------------
    // 6. Regression: old literal-String behaviour is now PVReference
    //    (previously unquoted non-numeric text was kept as String)
    // -------------------------------------------------------------------------

    @Test
    public void testLegacyUnquotedTextIsNowPVReference()
    {
        // Before this change: StringOrDouble.parse("some_text") → String
        // After  this change: StringOrDouble.parse("some_text") → PVReference
        final Object v = StringOrDouble.parse("some_text");
        assertInstanceOf(PVReference.class, v,
                "Previously unquoted non-numeric text silently became a String literal. " +
                "It should now be treated as a PV reference.");
    }
}

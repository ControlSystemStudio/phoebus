/*******************************************************************************
 * Copyright (c) 2015-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.macros;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

/** JUnit test of macro handling
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MacrosUnitTest
{
    /** Test check of macro name */
    @Test
    public void testNames() throws Exception
    {
        assertThat(Macros.checkMacroName("ExampleMacro"), nullValue());
        assertThat(Macros.checkMacroName("My_Macro"), nullValue());
        assertThat(Macros.checkMacroName("My-Macro"), nullValue());
        assertThat(Macros.checkMacroName("Macro2"), nullValue());

        assertThat(Macros.checkMacroName("2MustStartWithCharacter"), not(nullValue()));
        assertThat(Macros.checkMacroName("-CannotHaveNon-CharacterAtStart"), not(nullValue()));
        assertThat(Macros.checkMacroName("No Spaces"), not(nullValue()));
    }

    /** Test check for unresolved macros
     *  @throws Exception on error
     */
    @Test
    public void testCheck() throws Exception
    {
        assertThat(MacroHandler.containsMacros("Plain Text"), equalTo(false));
        assertThat(MacroHandler.containsMacros("${S}"), equalTo(true));
        assertThat(MacroHandler.containsMacros("This is $(S)"), equalTo(true));
        assertThat(MacroHandler.containsMacros("$(MACRO)"), equalTo(true));
        assertThat(MacroHandler.containsMacros("$(${MACRO})"), equalTo(true));
        assertThat(MacroHandler.containsMacros("Escaped \\$(S)"), equalTo(true));
        assertThat(MacroHandler.containsMacros("Escaped \\$(S) Used $(S)"), equalTo(true));
    }

    @Test
    public void testBraces() throws Exception
    {
        assertThat(MacroHandler.findClosingBrace("${X\\(XX}", 1), equalTo(7));
        assertThat(MacroHandler.findClosingBrace("$(XXX)", 1), equalTo(5));
        assertThat(MacroHandler.findClosingBrace("$(X(X)X)", 1), equalTo(7));
        assertThat(MacroHandler.findClosingBrace("$(X{X}X)", 1), equalTo(7));
        assertThat(MacroHandler.findClosingBrace("$(X(X\\))X)", 1), equalTo(9));
        assertThat(MacroHandler.findClosingBrace("${XXX}", 1), equalTo(5));
        assertThat(MacroHandler.findClosingBrace("${XXX\\}}", 1), equalTo(7));
        assertThat(MacroHandler.findClosingBrace("${XXX)", 1), equalTo(-1));
        assertThat(MacroHandler.findClosingBrace("$(XXX}", 1), equalTo(-1));
    }

    /** Test recursive macro error
     *  @throws Exception on error
     */
    @Test
    public void testSimpleSpec() throws Exception
    {
        // Plain  NAME=VALUE with some spaces
        Macros macros = Macros.fromSimpleSpec("A=1,  B = 2");
        assertThat(macros.getNames().size(), equalTo(2));
        assertThat(macros.getValue("A"), equalTo("1"));
        assertThat(macros.getValue("B"), equalTo("2"));

        // Quoted value with spaces and comma
        macros = Macros.fromSimpleSpec("MSG = \"Hello, Dolly\" , B=2");
        assertThat(macros.getNames().size(), equalTo(2));
        assertThat(macros.getValue("MSG"), equalTo("Hello, Dolly"));
        assertThat(macros.getValue("B"), equalTo("2"));

        // Value with escaped quote
        macros = Macros.fromSimpleSpec("MSG = \"This is a \\\"Message\\\" .. \" , B=2");
        assertThat(macros.getNames().size(), equalTo(2));
        assertThat(macros.getValue("MSG"), equalTo("This is a \"Message\" .. "));
        assertThat(macros.getValue("B"), equalTo("2"));
    }

    /** Test basic macro=value
     *  @throws Exception on error
     */
    @Test
    public void testMacros() throws Exception
    {
        final Macros macros = new Macros();
        macros.add("S", "BL7");
        macros.add("NAME", "Flint, Eugene");
        macros.add("TAB", "    ");
        macros.add("MACRO", "S");
        macros.add("traces[0].y_pv", "TheValueWaveform");

        assertThat(MacroHandler.replace(macros, "Plain Text"), equalTo("Plain Text"));
        assertThat(MacroHandler.replace(macros, "${S}"), equalTo("BL7"));
        assertThat(MacroHandler.replace(macros, "This is $(S)"), equalTo("This is BL7"));
        // Plain macro
        assertThat(MacroHandler.replace(macros, "$(MACRO)"), equalTo("S"));
        // $($(MACRO)) ->  $(S) -> BL7
        assertThat(MacroHandler.replace(macros, "$(${MACRO})"), equalTo("BL7"));
        assertThat(MacroHandler.replace(macros, "$(TAB)$(NAME)$(TAB)"), equalTo("    Flint, Eugene    "));
        assertThat(MacroHandler.replace(macros, "$(traces[0].y_pv)"), equalTo("TheValueWaveform"));

        // Escaped macros leave the $(..), i.e. remove the escape char
        assertThat(MacroHandler.replace(macros, "Escaped \\$(S)"), equalTo("Escaped $(S)"));
        assertThat(MacroHandler.replace(macros, "Escaped \\$(S) Used $(S)"), equalTo("Escaped $(S) Used BL7"));

        // If you want to keep a '\$' in the output, escape the escape
        assertThat(MacroHandler.replace(macros, "Escaped \\\\$(S)"), equalTo("Escaped \\$(S)"));
    }

    /** Test special cases
     *  @throws Exception on error
     */
    @Test
    public void testSpecials() throws Exception
    {
        final MacroValueProvider macros = new Macros();
        System.out.println(macros);
        assertThat(macros.toString(), equalTo("[]"));

        assertThat(MacroHandler.replace(macros, "Plain Text"), equalTo("Plain Text"));
        assertThat(MacroHandler.replace(macros, "Nothing for ${S} <-- this one"), equalTo("Nothing for ${S} <-- this one"));
        assertThat(MacroHandler.replace(macros, "${NOT_CLOSED"), equalTo("${NOT_CLOSED"));
    }

    /** Test macros with default values
     *  @throws Exception on error
     */
    @Test
    public void testDefaults() throws Exception
    {
        Macros macros = new Macros();
        macros.add("A", "a");
        macros.add("B", "b");
        System.out.println(macros);

        assertThat(MacroHandler.replace(macros, "Empty default ${S=} value"), equalTo("Empty default  value"));
        // Name cannot include spaces
        assertThat(MacroHandler.replace(macros, "Invalid-name default ${ S=X}"), equalTo("Invalid-name default ${ S=X}"));
        assertThat(MacroHandler.replace(macros, "Default ${S=X} value"), equalTo("Default X value"));
        assertThat(MacroHandler.replace(macros, "Default ${S=X + Y = Z} value"), equalTo("Default X + Y = Z value"));
        assertThat(MacroHandler.replace(macros, "Doesn't use default: ${S=$(A=z)}"), equalTo("Doesn't use default: a"));

        // Default value itself can be a macro
        assertThat(MacroHandler.replace(macros, "$(A=$(B))"), equalTo("a"));

        macros = new Macros();
        macros.add("DERIVED", "$(MAIN=default)");
        System.out.println(macros);

        assertThat(MacroHandler.replace(macros, "$(DERIVED)"), equalTo("default"));
        assertThat(MacroHandler.replace(macros, "/$(DERIVED)/$(DERIVED)/"), equalTo("/default/default/"));

        macros.add("MAIN", "main");
        System.out.println(macros);

        assertThat(MacroHandler.replace(macros, "$(DERIVED)"), equalTo("main"));
        assertThat(MacroHandler.replace(macros, "/$(DERIVED)/$(DERIVED)/"), equalTo("/main/main/"));
        assertThat(MacroHandler.replace(macros, "/$(MAIN=default)/$(DERIVED)/"), equalTo("/main/main/"));

        assertThat(MacroHandler.replace(macros, "${MACRO=Use A (alpha) or B}"), equalTo("Use A (alpha) or B"));
        // Not handled, because there is no counting of balanced brackets within the "=..." default
        // assertThat(MacroHandler.replace(macros, "$(MACRO=Use A (alpha) or B)"), equalTo("Use A (alpha) or B"));
    }

    /** Test macros with default values
     *  @throws Exception on error
     */
    @Test
    public void testDefaults2() throws Exception
    {
        final Macros macros = new Macros();
        macros.add("FINAL", "$(MAY_NOT_BE_SET=default_value)");
        System.out.println(macros);

        assertThat(MacroHandler.replace(macros, "${FINAL}"), equalTo("default_value"));
        assertThat(MacroHandler.replace(macros, "${FINAL}/${FINAL}"), equalTo("default_value/default_value"));
    }

    /** Test recursive macro error
     *  @throws Exception on error
     */
    @Test
    public void testRecursion() throws Exception
    {
        final Macros macros = new Macros();
        macros.add("S", "$(S)");
        try
        {
            MacroHandler.replace(macros, "Never ending $(S)");
            fail("Didn't detect recursive macro");
        }
        catch (Exception ex)
        {
            assertThat(ex.getMessage(), containsString(/* [Rr] */ "ecursive"));
        }

        try
        {
            MacroHandler.replace(macros, "Recursive $(S=a) default");
        }
        catch (Exception ex)
        {
            assertThat(ex.getMessage(), containsString(/* [Rr] */ "ecursive"));
        }
    }
}

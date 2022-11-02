/*******************************************************************************
 * Copyright (c) 2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.framework.preferences;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** JUnit test of {@link AnnotatedPreferences}
 *  @author Kay Kasemir
 */
public class AnnotatedPreferencesTest
{
	// Basic examples for reading int, String, double, File, ...,
	// using same name for the variable and the preference tag.
	
	// Will be set from 'example_int=42' in preference file
	@Preference
	public static int example_int;
	
	@Preference
	public static String example_string;

	@Preference
	public static double example_double;

	@Preference
	public static boolean example_bool;
	
	@Preference
	public static int[] example_rgb;

	@Preference
	public static String[] example_strings;

	@Preference
	public static File example_file;

	// Name in preference file that differs from the field name used in here
	@Preference(name="example_named_bar")
	public static String example_foo;

	// Not annotated, not set from preferences
	public static int not_set_from_preferences = -1;

	
	// Example for more complex preference that's first read as a string
	// (or other basic supported type) and then parsed.
	// Keeping the 'raw' value private
	
	@Preference(name="example_first_last")
	private static String first_last_spec;
	
	public static String[] example_first_last;
	
	
	enum ExampleEnum { RED, GREEN, BLUE }
	
	@Preference
	public static ExampleEnum example_enum;
		
	static
	{
		AnnotatedPreferences.initialize(AnnotatedPreferencesTest.class, "/anno_prefs_test.properties");
	}
	
	
	@Test
	public void testAnnotatedPreference()
	{
		assertEquals(42, example_int);
		assertEquals("Fred Flintstone", example_string);
		assertEquals(3.14, example_double, 0.1);
		assertTrue(example_bool);
		assertArrayEquals(new int[] { 128, 50, 255}, example_rgb);
		assertArrayEquals(new String[] { "One", "Two", "Three" }, example_strings);
		assertEquals(new File("/tmp/example.dat"), example_file);
		assertEquals("Value of foo", example_foo);
		
		// Not reading 13 from preference file
		assertEquals(-1, not_set_from_preferences);
	
		// Reading 'raw' value
		assertEquals("Fred Jason, Miller", first_last_spec);
		example_first_last = first_last_spec.split("\\s*,\\s*");
		assertEquals("Fred Jason", example_first_last[0]);
		assertEquals("Miller", example_first_last[1]);

		assertEquals(ExampleEnum.BLUE, example_enum);
	}
}

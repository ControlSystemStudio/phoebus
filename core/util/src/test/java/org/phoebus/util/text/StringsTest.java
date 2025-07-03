/*
 * Copyright (C) 2010 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.phoebus.util.text;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for {@link Strings}.
 *
 * Based on the tests in google guava @author Kevin Bourrillion
 */
public class StringsTest {


  @Test
  public void testIsNullOrEmpty() {
    assertTrue(Strings.isNullOrEmpty(null));
    assertTrue(Strings.isNullOrEmpty(""));
    assertFalse(Strings.isNullOrEmpty("a"));
  }

  @Test
  void testParseStringList() {
    assertEquals(
            List.of("A", "B", "C"),
            Strings.parseStringList("A, B, C")
    );
    assertEquals(
            List.of("An apple", "an orange", "banana"),
            Strings.parseStringList("An apple, an orange, banana")
    );

    assertEquals(
            List.of("An apple", "an orange", "banana"),
            Strings.parseStringList("'An apple', an orange, banana")
    );

    assertEquals(
            List.of("INVALID", "ERROR"),
            Strings.parseStringList("\"INVALID\", \"ERROR\"")
    );

    assertEquals(
            List.of("    item with leading and trailing spaces that are not removed    "),
            Strings.parseStringList("'    item with leading and trailing spaces that are not removed    '")
    );

    assertEquals(
            List.of("Hello, Dolly", "Goodbye, cruel world"),
            Strings.parseStringList("\"Hello, Dolly\", \"Goodbye, cruel world\"")
    );
  }
}
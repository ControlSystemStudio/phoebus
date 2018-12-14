package org.phoebus.util.text;

import java.util.Comparator;

/** 'Natural' comparator for text with numbers
 *
 *  <p>A plain string comparator will sort "Sys10:V" before "Sys1:V"
 *  because '0' is less than ':' in the ASCII table.
 *  A human on the other hand might prefer to sort "Sys1:V" before "Sys10:V"
 *  because 1 is smaller than 10.
 *
 *  <p>This comparator switches to a numeric comparison
 *  when locating a number in the string.
 *
 *  <p>Based on Olivier Oudot example in
 *  https://stackoverflow.com/questions/104599/sort-on-a-string-that-may-contain-a-number
 */
public class CompareNatural
{
    public static final Comparator<String> INSTANCE = (a, b) -> compareTo(a, b);

    public static int compareTo(final String s1, final String s2)
    {
       // Skip all identical characters
       final int len1 = s1.length();
       final int len2 = s2.length();
       int i = 0;
       char c1 = 0, c2 = 0;
       while ((i < len1) &&
              (i < len2) &&
              (c1 = s1.charAt(i)) == (c2 = s2.charAt(i)))
            ++i;

       // Check end of string
       if (c1 == c2)
          return len1 - len2;

       // Check digit in first string
       if (Character.isDigit(c1))
       {
          // Check digit only in first string
          if (!Character.isDigit(c2))
             return 1;

          // Scan all integer digits
          int x1 = i + 1;
          while ((x1 < len1) && Character.isDigit(s1.charAt(x1)))
              ++x1;
          int x2 = i + 1;
          while ((x2 < len2) && Character.isDigit(s2.charAt(x2)))
              ++x2;

          // Longer integer wins, first digit otherwise
          return x2 == x1 ? c1 - c2 : x1 - x2;
       }

       // Check digit only in second string
       if (Character.isDigit(c2))
          return -1;

       // No digits
       return c1 - c2;
    }
}

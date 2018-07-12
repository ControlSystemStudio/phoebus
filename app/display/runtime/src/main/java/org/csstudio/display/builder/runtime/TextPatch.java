/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Patch for text
 *
 *  <p>Pre-compiled pattern and the replacement.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TextPatch
{
    final private Pattern pattern;
    final private String replacement;

    /** @param pattern Regular expression pattern, may contain groups "(..)"
     *  @param replacement Regular expression replacement, may contain references "$1"
     *  @throws PatternSyntaxException on error in pattern
     */
    public TextPatch(final String pattern, final String replacement) throws PatternSyntaxException
    {
        this.pattern = Pattern.compile(pattern);
        this.replacement = replacement.replace("[@]", "@");
    }

    /** @param text Original text
     *  @return Patched text
     */
    public String patch(final String text)
    {
        return pattern.matcher(text).replaceAll(replacement);
    }

    @Override
    public String toString()
    {
        return "TextPatch '" + pattern.pattern() + "' -> '" + replacement + "'";
    }
}

/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.rules;

import static org.csstudio.display.builder.model.ModelPlugin.logger;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.properties.Point;
import org.csstudio.display.builder.model.properties.Points;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.csstudio.display.builder.model.rules.RuleInfo.ExpressionInfo;
import org.phoebus.framework.macros.MacroHandler;
import org.phoebus.framework.macros.Macros;

/** Transform rules into scripts
 *
 *  <p>Rules produce scripts attached to widgets
 *  rules execute in response to changes in triggering
 *  PVs
 *
 *  @author Megan Grodowitz
 */
@SuppressWarnings("nls")
public class RuleToScript
{
    /** @param pvCount Number of script/rule input PVs
     *  @return Map of script variables that refer to those PVs and PVUtil calls to create them
     */
    private static Map<String,String> pvNameOptions(int pvCount)
    {   // LinkedHashMap to preserve order of PVs
        // so it looks good in generated script
        final Map<String,String> pvm = new LinkedHashMap<>();
        for (int idx = 0; idx < pvCount; idx++)
        {
            final String istr = Integer.toString(idx);
            pvm.put("pv" + istr, "PVUtil.getDouble(pvs["+istr+"])"  );
            pvm.put("pvInt" + istr, "PVUtil.getLong(pvs["+istr+"])"  );
            pvm.put("pvStr" + istr, "PVUtil.getString(pvs["+istr+"])"  );
            pvm.put("pvSev" + istr, "PVUtil.getSeverity(pvs["+istr+"])"  );
            pvm.put("pvLegacySev" + istr, "PVUtil.getLegacySeverity(pvs["+istr+"])"  );
        }
        return pvm;
    }

    private enum PropFormat
    {
        NUMERIC, BOOLEAN, STRING, COLOR, FONT, POINTS
    }

    /** Appends text that represents value of the property as python literal
     *  @param builder StringBuilder
     *  @param prop Property
     *  @param pform Format
     *  @return The passed in StringBuilder
     */
    private static StringBuilder formatPropVal(StringBuilder builder, final WidgetProperty<?> prop, final PropFormat pform)
    {
        final Object value = prop.getValue();
        switch(pform)
        {
        case BOOLEAN:
	    // Property should be boolean, but in case it's not,
            // convert property to string, parse as boolean,
            // then return the 'python' version of true/false
            // for evaluation in script
            return builder.append(Boolean.parseBoolean(value.toString()) ? "True" : "False");
        case STRING:
            return builder.append("u\"").append(escapeString(value.toString())).append("\"");
        case COLOR:
            return createWidgetColor(builder, (WidgetColor) value);
        case FONT:
            return createWidgetFont(builder, (WidgetFont) value);
        case POINTS:
            return createPoints(builder, (Points) value);
        case NUMERIC:
            // Set enum to its ordinal
            if (value instanceof Enum<?>)
                return builder.append(Integer.toString(((Enum<?>)value).ordinal()));
            // else: Format number as string
        default:
            return builder.append(String.valueOf(value));
        }
    }

    /** @param text Text
     *  @return Text where quotes and whitespace is escaped
     */
    private static String escapeString(final String text)
    {
        return text.replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\t", "\\t");
    }

    /** Patch logic
     *
     *  <p>Replaces 'true' with 'True', '&&' with 'and' and so on.
     *
     *  @param text Text with logical expression
     *  @return Javascript type logic somewhat updated to Python
     */
    protected static String javascriptToPythonLogic(final String text)
    {
        final int len = text.length();
        final StringBuilder result = new StringBuilder(len);
        for (int i=0; i<len; ++i)
        {
            // Skip quoted text, ignoring escaped quotes
            if (text.charAt(i) == '"'  &&  (i == 0  ||  text.charAt(i-1) != '\\'))
            {
                result.append(text.charAt(i));
                ++i;
                while (text.charAt(i) != '"' || text.charAt(i-1) == '\\')
                {
                    result.append(text.charAt(i));
                    ++i;
                    // Unmatched quotes
                    if (i >= len)
                        return text;
                }
                result.append(text.charAt(i));
            } // Same for single quotes
            else if (text.charAt(i) == '\''  &&  (i == 0  ||  text.charAt(i-1) != '\\'))
            {
                result.append(text.charAt(i));
                ++i;
                while (text.charAt(i) != '\'' || text.charAt(i-1) == '\\')
                {
                    result.append(text.charAt(i));
                    ++i;
                    // Unmatched quotes
                    if (i >= len)
                        return text;
                }
                result.append(text.charAt(i));
            }
            else if (matches(text, i, "true"))
            {
                result.append("True");
                i += 3;
            }
            else if (matches(text, i, "false"))
            {
                result.append("False");
                i += 4;
            }
            else if (matches(text, i, "&&"))
            {
                result.append(" and ");
                i += 1;
            }
            else if (matches(text, i, "||"))
            {
                result.append(" or ");
                i += 1;
            }
            else if (matches(text, i, "!="))
            {
                result.append("!=");
                i += 1;
            }
            else if (text.charAt(i) == '!')  // and not "!=" since we just checked for that
                result.append(" not ");
            else if (matches(text, i, "==")) // and not "!="
            {
                result.append("==");
                i += 1;
            }
            else if (text.charAt(i) == '=' && i > 0 && !(text.charAt(i-1)=='<' || text.charAt(i-1)=='>')) // and neither "==" nor "!=" nor "<=", ">="
                result.append("==");
	    else if (matches(text, i, "Math."))
            {
		i += 4;
            }
            else
                result.append(text.charAt(i));
        }
        return result.toString();
    }

    private static boolean matches(final String text, final int pos, final String literal)
    {
        final int ll = literal.length();
        return text.length() >= pos + ll  &&  text.substring(pos, pos+ll).equals(literal);
    }

    private static StringBuilder createWidgetColor(StringBuilder script, final WidgetColor col)
    {
        script.append("WidgetColor(").append(col.getRed()).append(", ")
                                     .append(col.getGreen()).append(", ")
                                     .append(col.getBlue()).append(", ")
                                     .append(col.getAlpha()).append(")");
        return script;
    }

    private static StringBuilder createWidgetFont(StringBuilder script, final WidgetFont fon)
    {
        script.append("WidgetFont(\"").append(fon.getFamily()).append("\", WidgetFontStyle.")
                                      .append(fon.getStyle().name()).append(", ")
                                      .append(fon.getSize()).append(")");
        return script;
    }

    private static StringBuilder createPoints(StringBuilder script, final Points points)
    {
        script.append("Points([");

        boolean first = true;
        for (Point p : points)
        {
            if (!first)
                script.append(", ");
            script.append(p.getX()).append(", ").append(p.getY());
            first = false;
        }

        script.append("])");

        return script;
    }

    public static String generatePy(final Widget attached_widget, final RuleInfo rule)
    {
        final WidgetProperty<?> prop = attached_widget.getProperty(rule.getPropID());

        final PropFormat pform;
        if (prop.getDefaultValue() instanceof Number)
            pform = PropFormat.NUMERIC;
        else if (prop.getDefaultValue() instanceof Boolean)
            pform = PropFormat.BOOLEAN;
        else if (prop.getDefaultValue() instanceof Enum<?>)
             pform = PropFormat.NUMERIC;
        else if (prop.getDefaultValue() instanceof WidgetColor)
            pform = PropFormat.COLOR;
        else if (prop.getDefaultValue() instanceof WidgetFont)
            pform = PropFormat.FONT;
        else if (prop.getDefaultValue() instanceof Points)
            pform = PropFormat.POINTS;
        else
            pform = PropFormat.STRING;

        final StringBuilder script = new StringBuilder();
        script.append("## Script for Rule: ").append(rule.getName()).append("\n\n");
        script.append("from org.csstudio.display.builder.runtime.script import PVUtil\n");
        script.append("from java import lang\n");
        if (pform == PropFormat.COLOR)
            script.append("from ").append(WidgetColor.class.getPackageName()).append(" import WidgetColor\n");
        else if (pform == PropFormat.FONT)
            script.append("from ").append(WidgetFont.class.getPackageName()).append(" import WidgetFont, WidgetFontStyle\n");
        else if (pform == PropFormat.POINTS)
            script.append("from ").append(Points.class.getPackageName()).append(" import Points\n");

        script.append("\n## Process variable extraction\n");
        script.append("## Use any of the following valid variable names in an expression:\n");

        final Map<String,String> pvm = pvNameOptions(rule.getPVs().size());

        for (Map.Entry<String, String> entry : pvm.entrySet())
        {
            script.append("##     ").append(entry.getKey());
            if (entry.getKey().contains("Legacy"))
                script.append("  [DEPRECATED]");
            script.append("\n");
        }
        script.append("\n");

        // Check which pv* variables are actually used
        final Map<String,String> output_pvm = new HashMap<>();
        for (ExpressionInfo<?> expr : rule.getExpressions())
        {
            // Check the boolean expressions.
            // In principle, should parse an expression like
            //   pv0 > 10
            // to see if it refers to the variable "pv0".
            // Instead of implementing a full parser, we
            // just check for "pv0" anywhere in the expression.
            // This will erroneously detect a variable reference in
            //   len("Text with pv0")>4
            // which doesn't actually reference "pv0" as a variable,
            // but it doesn't matter if the script creates some
            // extra variable "pv0" which is then left unused.
            String expr_to_check = expr.getBoolExp();
            // If properties are also expressions, check those by
            // simply including them in the string to check
            if (rule.getPropAsExprFlag())
                expr_to_check += " " + expr.getPropVal().toString();
            for (Map.Entry<String, String> entry : pvm.entrySet())
            {
                final String varname = entry.getKey();
                /*
                 * PVUtil.getDouble() (used for pv0, pv1, etc),
                 * getSeverity() and getLegacySeverity() do not throw
                 * an exception when the PV is disconnected:
                 * Let's make sure that we have a pvInt for every PV
                 */
                if (expr_to_check.contains(varname) || varname.contains("pvInt"))
                    output_pvm.put(varname, entry.getValue());
            }
        }

        script.append("\ntry:\n");
        final String try_indent = "    ";
        // Generate code that reads the required pv* variables from PVs
        for (Map.Entry<String, String> entry : output_pvm.entrySet())
            script.append(try_indent).append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");

        script.append("\n## Script Body\n");
        final String indent = try_indent + "    ";

        final String setPropStr = "widget.setPropertyValue('" + rule.getPropID() + "', ";
        int idx = 0;

        final Macros macros = attached_widget.getEffectiveMacros();
        for (ExpressionInfo<?> expr : rule.getExpressions())
        {
            script.append(try_indent).append((idx == 0) ? "if" : "elif");

            String expanded_expression;
            try
            {
                expanded_expression = MacroHandler.replace(macros, expr.getBoolExp());
            }
            catch (Exception ex)
            {
                expanded_expression = expr.getBoolExp();
                logger.log(Level.WARNING, "Cannot expand macro in " + expanded_expression, ex);
            }

            script.append(" ").append(javascriptToPythonLogic(expanded_expression)).append(":\n");
            script.append(indent).append(setPropStr);
            if (rule.getPropAsExprFlag())
                script.append(javascriptToPythonLogic(expr.getPropVal().toString())).append(")\n");
            else
                formatPropVal(script, (WidgetProperty<?>) expr.getPropVal(), pform).append(")\n");
            idx++;
        }

        if (idx > 0)
        {
            script.append(try_indent).append("else:\n");
            script.append(indent);
        }
        else
            script.append(try_indent);

        script.append(setPropStr);
        formatPropVal(script, prop, pform).append(")\n");

        script.append("\nexcept (Exception, lang.Exception) as e:\n");
        script.append(try_indent).append(setPropStr);
        formatPropVal(script, prop, pform).append(")\n");
        script.append(try_indent).append("if not isinstance(e, PVUtil.PVHasNoValueException):\n");
        script.append(indent).append("raise e\n");

        return script.toString();
    }

    /** Add line numbers to script
     *  @param script Script text
     *  @return Same text with line numbers
     */
    public static String addLineNumbers(final String script)
    {
        final String[] lines = script.split("\r\n|\r|\n");
        // Reserve buffer for script, then on each line add "1234: "
        final StringBuilder ret = new StringBuilder(script.length() + lines.length*6);
        for (int ldx = 0; ldx < lines.length; ldx++)
            ret.append(String.format("%4d", ldx+1)).append(": ").append(lines[ldx]).append("\n");
        return ret.toString();
    }
}

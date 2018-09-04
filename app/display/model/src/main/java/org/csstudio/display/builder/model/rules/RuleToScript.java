/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.rules;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.rules.RuleInfo.ExpressionInfo;

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
        NUMERIC, BOOLEAN, STRING, COLOR
    }

    /** @param prop Property
     *  @param exprIDX Index of expression
     *  @param pform Format
     *  @return Text that represents value of the property as python literal
     */
    private static String formatPropVal(final WidgetProperty<?> prop, final int exprIDX, final PropFormat pform)
    {
        switch(pform)
        {
        case BOOLEAN:
            return (Boolean) prop.getValue() ? "True" : "False";
        case STRING:
            return "\"" + escapeString(prop.getValue().toString()) + "\"";
        case COLOR:
            if (exprIDX >= 0)
                return "colorVal" + String.valueOf(exprIDX);
            else
                return "colorCurrent";
        case NUMERIC:
            // Set enum to its ordinal
            if (prop.getValue() instanceof Enum<?>)
                return Integer.toString(((Enum<?>)prop.getValue()).ordinal());
            // else: Format number as string
        default:
            return String.valueOf(prop.getValue());
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
        else
            pform = PropFormat.STRING;

        final StringBuilder script = new StringBuilder();
        script.append("## Script for Rule: ").append(rule.getName()).append("\n\n");
        script.append("from org.csstudio.display.builder.runtime.script import PVUtil\n");
        if (pform == PropFormat.COLOR)
            script.append("from org.csstudio.display.builder.model.properties import WidgetColor\n");

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
                if (expr_to_check.contains(varname))
                    output_pvm.put(varname, entry.getValue());
            }
        }
        // Generate code that reads the required pv* variables from PVs
        for (Map.Entry<String, String> entry : output_pvm.entrySet())
            script.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");

        if (pform == PropFormat.COLOR)
        {   // If property is a color, create variables for all the used colors
            script.append("\n## Define Colors\n");
            WidgetColor col = (WidgetColor) prop.getValue();
            script.append("colorCurrent = ")
                  .append("WidgetColor(").append(col.getRed()).append(", ")
                                         .append(col.getGreen()).append(", ")
                                         .append(col.getBlue()).append(")\n");

            if (!rule.getPropAsExprFlag())
            {
                int idx = 0;
                for (ExpressionInfo<?> expr : rule.getExpressions())
                {
                    if (expr.getPropVal() instanceof WidgetProperty<?>)
                    {
                        final Object value = (( WidgetProperty<?>)expr.getPropVal()).getValue();
                        if (value instanceof WidgetColor)
                        {
                            col = (WidgetColor) value;
                            script.append("colorVal").append(idx).append(" = ")
                                  .append("WidgetColor(").append(col.getRed()).append(", ")
                                  .append(col.getGreen()).append(", ")
                                  .append(col.getBlue()).append(")\n");
                        }
                    }
                    idx++;
                }
            }
        }

        script.append("\n## Script Body\n");
        String indent = "    ";

        final String setPropStr = "widget.setPropertyValue('" + rule.getPropID() + "', ";
        int idx = 0;
        for (ExpressionInfo<?> expr : rule.getExpressions())
        {
            script.append((idx == 0) ? "if" : "elif");
            script.append(" ").append(javascriptToPythonLogic(expr.getBoolExp())).append(":\n");
            script.append(indent).append(setPropStr);
            if (rule.getPropAsExprFlag())
                script.append(javascriptToPythonLogic(expr.getPropVal().toString())).append(")\n");
            else
                script.append(formatPropVal((WidgetProperty<?>) expr.getPropVal(), idx, pform)).append(")\n");
            idx++;
        }

        if (idx > 0)
        {
            script.append("else:\n");
            script.append(indent).append(setPropStr).append(formatPropVal(prop, -1, pform)).append(")\n");
        }
        else
            script.append(setPropStr).append(formatPropVal(prop, -1, pform)).append(")\n");

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

/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import static org.csstudio.display.converter.edm.Converter.logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.NamedWidgetColor;
import org.csstudio.display.builder.model.properties.ScriptInfo;
import org.csstudio.display.builder.model.properties.ScriptPV;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.csstudio.display.builder.model.properties.WidgetFontStyle;
import org.csstudio.display.builder.model.rules.RuleInfo;
import org.csstudio.display.builder.model.rules.RuleInfo.ExpressionInfo;
import org.csstudio.display.converter.edm.ConverterPreferences;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.StringSplitter;
import org.csstudio.opibuilder.converter.model.EdmAttribute;
import org.csstudio.opibuilder.converter.model.EdmColor;
import org.csstudio.opibuilder.converter.model.EdmFont;
import org.csstudio.opibuilder.converter.model.EdmModel;
import org.csstudio.opibuilder.converter.model.EdmWidget;

/** Base for each converter
 *
 *  <p>Constructing a converter will convert an EDM
 *  widget into a corresponding Display Builder widget.
 *
 *  <p>Base class handles common properties like X, Y, Width, Height.
 *
 *  @author Kay Kasemir
 *  @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 *
 *  @param <W> Display Manager {@link Widget} type
 */
@SuppressWarnings("nls")
public abstract class ConverterBase<W extends Widget>
{
    protected final W widget;

    public ConverterBase(final EdmConverter converter, final Widget parent, final EdmWidget t)
    {
        widget = createWidget(t);
        widget.propName().setValue(t.getType());

        // Correct offset of parent widget
        widget.propX().setValue(t.getX() - converter.getOffsetX());
        widget.propY().setValue(t.getY() - converter.getOffsetY());
        widget.propWidth().setValue(t.getW());
        widget.propHeight().setValue(t.getH());

        // Does widget support visibility, and there's a 'visPv'?
        final Optional<WidgetProperty<Boolean>> visibility = widget.checkProperty(CommonWidgetProperties.propVisible);
        final EdmAttribute visPvAttr = t.getAttribute("visPv");
        if (visibility.isPresent()  &&  visPvAttr != null  &&  visPvAttr.isExistInEDL())
        {
            final WidgetProperty<Boolean> vis_prop = visibility.get();
            final List<RuleInfo> rules = new ArrayList<>(widget.propRules().getValue());
            final List<ScriptPV> pvs = List.of(new ScriptPV(convertPVName(t.getVisPv())));

            // If pv0 >= min  && < max:  show (unless inverted)
            final List<ExpressionInfo<?>> exprs = new ArrayList<>();
            final String expression = "pv0>=" + t.getVisMin() + " && pv0<" + t.getVisMax();
            WidgetProperty<Boolean> val = vis_prop.clone();
            val.setValue(!t.isVisInvert());
            exprs.add(new RuleInfo.ExprInfoValue<>(expression, val));

            // Else: Hide (unless inverted)
            val = vis_prop.clone();
            val.setValue(t.isVisInvert());
            exprs.add(new RuleInfo.ExprInfoValue<>("true", val));

            rules.add(new RuleInfo(vis_prop.getName(), vis_prop.getName(), false, exprs, pvs));
            widget.propRules().setValue(rules);
        }

        final ChildrenProperty parent_children = ChildrenProperty.getChildren(parent);
        if (parent_children == null)
            throw new IllegalStateException("Cannot add as child to " + parent);
        parent_children.addChild(widget);
    }

    /** Create Display Builder widget
     *
     *  <p>In most cases, the called converter knows which widget
     *  to create, but in special cases it can check the EDM widget
     *  configuration to decide how to map.
     *
     *  @param edm EDM widget
     *  @return Display Builder widget
     */
    protected abstract W createWidget(EdmWidget edm);

    /** @param edm EDM Color
     *  @param prop Display builder color property to set from EDM color
     */
    public static void convertColor(final EdmColor edm,
            final WidgetProperty<WidgetColor> prop)
    {
        convertColor(edm,  null, prop);
    }

    /** @param edm EDM Color
     *  @param color_pv Color PV for dynamic color or <code>null</code>
     *  @param prop Display builder color property to set from EDM color
     */
    public static void convertColor(final EdmColor edm,
                                    final String color_pv,
                                    final WidgetProperty<WidgetColor> prop)
    {
        if (edm.isDynamic() && color_pv != null)
        {
            final Widget widget = prop.getWidget();
            final List<RuleInfo> rules = new ArrayList<>(widget.propRules().getValue());
            final List<ScriptPV> pvs = List.of(new ScriptPV(convertPVName(color_pv)));
            final List<ExpressionInfo<?>> exprs = new ArrayList<>();

            for (Entry<String, String> entry : edm.getRuleMap().entrySet())
            {
                final String expression = convertColorRuleExpression(entry.getKey());
                final EdmColor edm_color = EdmModel.getColorsList().getColor(entry.getValue());
                if (edm_color == null)
                {
                    logger.log(Level.WARNING, "Dynamic color uses unknown color " + entry.getValue());
                    return;
                }
                final WidgetColor color = convertStaticColor(edm_color);
                final WidgetProperty<WidgetColor> prop_col = prop.clone();
                prop_col.setValue(color);
                exprs.add(new RuleInfo.ExprInfoValue<>(expression, prop_col));
            }

            final String name = edm.getName() == null ? "color" : edm.getName();
            rules.add(new RuleInfo(name, prop.getName(), false, exprs, pvs));
            widget.propRules().setValue(rules);

            return;
        }
        if (edm.isBlinking())
        {
            logger.log(Level.WARNING, "Can only handle static colors");
            return;
        }

        prop.setValue(convertStaticColor(edm));
    }

    /** Find a '=' that is neither preceded by '<', '>', '=' nor followed by '=' */
    private static final Pattern expand_equal = Pattern.compile("(?<![<>=])=(?!=)");

    /** @param expression EDM color rule expression like ">=5 && <10"
     *  @return Display Builder rule expression like "pv0>=5 && pv0<10"
     */
    static String convertColorRuleExpression(String expression)
    {
        if (expression.equals("default"))
            return "true";

        // Expand all single '=' into 'pv0=='
        expression = expand_equal.matcher(expression).replaceAll("pv0==");

        // Add variable to '>', '<' which includes '>=', '<='
        expression = expression.replace(">", "pv0>");
        expression = expression.replace("<", "pv0<");

        return expression;
    }

    /** Make color alarm-sensitive
     *  @param alarm_pv Alarm PV where severity is obtained
     *  @param prop Property to set to the named OK, MINOR, ... color
     */
    public static void createAlarmColor(final String alarm_pv, final WidgetProperty<WidgetColor> prop)
    {
        final String alarm_script =
            "from org.csstudio.display.builder.runtime.script import PVUtil\n" +
            "from org.csstudio.display.builder.model.persist import WidgetColorService\n" +
            "sevr = PVUtil.getSeverity(pvs[0])\n" +
            "cn = ( 'OK', 'MINOR', 'MAYOR', 'INVALID', 'DISCONNECTED' )[sevr]\n" +
            "c = WidgetColorService.getColor(cn)\n" +
            "widget.setPropertyValue('" + prop.getName() + "', c)";

        final String pv = convertPVName(alarm_pv);
        final Widget widget = prop.getWidget();
        final List<ScriptInfo> scripts = new ArrayList<>(widget.propScripts().getValue());
        scripts.add(new ScriptInfo(ScriptInfo.EMBEDDED_PYTHON, alarm_script, true, List.of(new ScriptPV(pv))));
        widget.propScripts().setValue(scripts);
    }

    /** @param edm Static EDM color
     *  @return {@link WidgetColor}
     */
    private static WidgetColor convertStaticColor(final EdmColor edm)
    {
        // EDM uses 16 bit color values
        final int red   = edm.getRed()   >> 8,
                  green = edm.getGreen() >> 8,
                  blue  = edm.getBlue()  >> 8;
        final String name = edm.getName();
        if (name != null  &&  !name.isBlank())
            return new NamedWidgetColor(name, red, green, blue);
        return new WidgetColor(red, green, blue);
    }

    /** @param edm EDM font
     *  @param prop Display builder font property to set from EDM font
     */
    public static void convertFont(final EdmFont edm,
                                   final WidgetProperty<WidgetFont> prop)
    {
        final String family = ConverterPreferences.mapFont(edm.getName());

        final WidgetFontStyle style;
        if (edm.isBold() && edm.isItalic())
            style = WidgetFontStyle.BOLD_ITALIC;
        else if (edm.isBold())
            style = WidgetFontStyle.BOLD;
        else if (edm.isItalic())
            style = WidgetFontStyle.ITALIC;
        else
            style = WidgetFontStyle.REGULAR;

        final double size = edm.getSize();
        prop.setValue(new WidgetFont(family, style, size));
    }

    /**
     * If pvName is a LOC or CALC EDM PV, attempt to convert it to a syntax
     * understood by CSS.
     *
     * If conversion fails or it is a regular PV, return the unchanged PV name.
     * @param pvName PV name to convert
     * @return converted PV name
     */
    public static String convertPVName(final String pvName)
    {
        if (pvName.startsWith("LOC"))
            return parseLocPV(pvName);
        else if (pvName.startsWith("CALC"))
            return convertCalcPV(pvName);
        return pvName;
    }

    /** @param pvName "CALC\{A+2}(SomePVName)"
     *  @return "=`SomePVName`+2"
     */
    private static String convertCalcPV(final String pvName)
    {
        // Remove "CALC\" and other escape backslashes
        String cvt = pvName.replace("CALC\\", "")
                           .replace("\\", "");

        // "{expression}(PVA, PVB, ...)"
        final Pattern pattern = Pattern.compile("\\{(.*)\\}\\((.*)\\)");
        final Matcher matcher = pattern.matcher(cvt);
        if (!matcher.matches())
        {
            logger.log(Level.WARNING, "Cannot handle '" + pvName + "', expected CALC\\{expression_with_A_B_C}(pva, pvb, pvc, ..)");
            return pvName;
        }
        // Extract expression
        cvt = "=" + matcher.group(1);

        // Get PVs
        final List<String> pvs = new ArrayList<>();
        for (String pv : matcher.group(2).split(","))
            pvs.add(pv.strip());

        // Replace PVs in expression
        int i=0;
        for (String pv : pvs)
        {
            final String variable = String.valueOf((char) ('A' + i));
            cvt = cvt.replace(variable, "`" + pv + "`");
            ++i;
        }
        return cvt;
    }

    /**
     * Convert an EDM local PV into a CSS local PV.
     * @param pvName local EDM PV name
     * @return local CSS PV name
     */
    private static String parseLocPV(String pvName)
    {
        if (pvName.startsWith("LOC\\"))
        {
            try
            {
                String newName = pvName.replace("$(!W)", "$(DID)");
                newName = newName.replaceAll("\\x24\\x28\\x21[A-Z]{1}\\x29", "\\$(DID)");
                String[] parts = StringSplitter.splitIgnoreInQuotes(newName, '=', true);
                StringBuilder sb = new StringBuilder("loc://");
                sb.append(parts[0].substring(5));
                if (parts.length > 1)
                {
                    String type = "";
                    String initValue = parts[1];
                    if (parts[1].startsWith("d:"))
                    {
                        type = "<VDouble>";
                        initValue = parts[1].substring(2);
                    }
                    else if (parts[1].startsWith("i:"))
                    {
//                        type = "<VDouble>";
                        initValue = parts[1].substring(2);
                    }
                    else if (parts[1].startsWith("s:"))
                    {
//                          type = "<VString>";
                        initValue = "\""+parts[1].substring(2)+"\"";
                    }
                    else if (parts[1].startsWith("e:"))
                    {   // Enumerated pv
                        // cannot be
                        // converted.
                        // TODO loc://xxx<VEnum> is now supported
                        return pvName;
                    }
                    //doesn't append type yet to support utility pv.
                    sb.append("(").append(initValue).append(")");
                }
                return sb.toString();
            }
            catch (Exception e)
            {
                // Ignore
            }
        }
        return pvName;
    }
}

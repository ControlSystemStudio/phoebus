/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.rules;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.properties.Points;
import org.csstudio.display.builder.model.properties.ScriptPV;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.csstudio.display.builder.model.properties.WidgetFontStyle;
import org.csstudio.display.builder.model.rules.RuleInfo;
import org.csstudio.display.builder.model.rules.RuleToScript;
import org.csstudio.display.builder.model.widgets.LabelWidget;
import org.csstudio.display.builder.model.widgets.PolylineWidget;
import org.csstudio.display.builder.model.widgets.plots.ImageWidget;
import org.junit.Test;

/** JUnit test of Rules
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RulesTest
{
    /** Rule that checks pv0>10 and picks a certain value for that */
    @Test
    public void testValueForCondition() throws Exception
    {
        final ImageWidget widget = new ImageWidget();

        final WidgetProperty<Integer> width = widget.propDataWidth().clone();
        width.setValue(47);

        final RuleInfo rule = new RuleInfo("WidthBasedOnPV", "data_width", false,
                Arrays.asList(new RuleInfo.ExprInfoValue<Integer>("pv0>10", width)),
                Arrays.asList(new ScriptPV("XSize")));

        System.out.println(rule);
        final String script = RuleToScript.generatePy(widget, rule);
        System.out.println(script);
        // Script must read the PV
        assertThat(script, containsString("pv0 = PVUtil.getDouble(pvs[0])"));
    }

    /** Rules that uses the value of a PV within an expression */
    @Test
    public void testValueAsExpression() throws Exception
    {
        final Widget widget = new ImageWidget();
        final RuleInfo rule = new RuleInfo("WidthFromPV", "data_width", true,
                Arrays.asList(new RuleInfo.ExprInfoString("true", "pv0")),
                Arrays.asList(new ScriptPV("XSize")));

        System.out.println(rule);
        final String script = RuleToScript.generatePy(widget, rule);
        System.out.println(script);
        // Script must read the PV
        assertThat(script, containsString("PVUtil.get"));
    }

    /** Rule that uses color */
    @Test
    public void testColorRule() throws Exception
    {
        final LabelWidget widget = new LabelWidget();

        final WidgetProperty<WidgetColor> color = widget.propForegroundColor().clone();
        color.setValue(new WidgetColor(1, 2, 3, 4));

        final RuleInfo rule = new RuleInfo("Color", "foreground_color", false,
                Arrays.asList(new RuleInfo.ExprInfoValue<WidgetColor>("pv0 > 10", color)),
                Arrays.asList(new ScriptPV("Whatever")));

        System.out.println(rule);
        final String script = RuleToScript.generatePy(widget, rule);
        System.out.println(script);
        // Script must create WidgetColor for colors
        assertThat(script, containsString("WidgetColor(1, 2, 3, 4)"));
    }

    /** Rule that uses font */
    @Test
    public void testFontRule() throws Exception
    {
        final LabelWidget widget = new LabelWidget();

        final WidgetProperty<WidgetFont> font = widget.propFont().clone();
        font.setValue(new WidgetFont("Liberation Sans", WidgetFontStyle.ITALIC, 18.0));

        final RuleInfo rule = new RuleInfo("Font", "font", false,
                Arrays.asList(new RuleInfo.ExprInfoValue<WidgetFont>("pv0 > 10", font)),
                Arrays.asList(new ScriptPV("Whatever")));

        System.out.println(rule);
        final String script = RuleToScript.generatePy(widget, rule);
        System.out.println(script);
        // Script must create WidgetFont for fonts
        assertThat(script, containsString("WidgetFont(\"" + font.getValue().getFamily() + "\", WidgetFontStyle." + font.getValue().getStyle().name() + ", " + font.getValue().getSize() + ")"));
    }

    /** Rule that uses points */
    @Test
    public void testPointsRule() throws Exception
    {
        final PolylineWidget widget = new PolylineWidget();

        final WidgetProperty<Points> points = widget.propPoints().clone();
        points.setValue(new Points(0.0, 0.0, 42.0, 42.0));

        final RuleInfo rule = new RuleInfo("Points", "points", false,
                Arrays.asList(new RuleInfo.ExprInfoValue<Points>("pv0 > 10", points)),
                Arrays.asList(new ScriptPV("Whatever")));

        System.out.println(rule);
        final String script = RuleToScript.generatePy(widget, rule);
        System.out.println(script);
        // Script must create Points for points
        assertThat(script, containsString("Points([0.0, 0.0, 42.0, 42.0])"));
    }
}

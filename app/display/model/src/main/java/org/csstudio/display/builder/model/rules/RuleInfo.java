/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.properties.ActionsWidgetProperty;
import org.csstudio.display.builder.model.properties.MacrosWidgetProperty;
import org.csstudio.display.builder.model.properties.RulesWidgetProperty;
import org.csstudio.display.builder.model.properties.ScriptsWidgetProperty;

/** Information about a rule
 *
 *
 *  <p>PVs will be created for each input/output.
 *  The rule is executed whenever one or
 *  more of the 'triggering' inputs receive
 *  a new value.
 *
 *  @author Megan Grodowitz
 */
@SuppressWarnings("nls")
public class RuleInfo
{


    /** Instantiate Expression with bool_exp string and widget property value
     *
     * @param bool_exp String for the boolean expression, e.g. (pv0 == 9)
     * @param prop_val Widget Property to set if the boolean expression evaluates true
     *
     * Do NOT pass in a widget property object that belongs to a widget!
     * The expression will alter the property value. This needs to be a property
     * object created for this expression, probably by the containing rule.
     */
    public static class ExpressionInfo<T>
    {
        private final String exp;
        private final boolean boolean_exp;
        private final T prop_val;

        public ExpressionInfo(final String exp, final boolean boolean_exp, final T prop_val)
        {
            this.exp = exp;
            this.boolean_exp = boolean_exp;
            this.prop_val = prop_val;
        }

        public String getExp()
        {
            return exp;
        }

        public T getPropVal()
        {
            return prop_val;
        }

        public boolean isBooleanExp() {
            return boolean_exp;
        }

        @Override
        public String toString()
        {
            // TODO shroffk fix this 
            return "(" + this.exp + ") ? " + prop_val;
        }
    };

    public static class PropInfo
    {
        private final WidgetProperty<?> prop;
        private final String prop_id;

        public PropInfo(final Widget attached_widget, final String prop_id_str)
        {
            prop_id = prop_id_str;
            WidgetProperty<?> check;
            try
            {
                check = attached_widget.getProperty(prop_id_str);
            }
            catch (IllegalArgumentException ex)
            {
                check = null;
            }
            prop = check;
        }

        public WidgetProperty<?> getProp() {
            return prop;
        }

        public String getPropID() {
            return prop_id;
        }

        @Override
        public String toString()
        {
            if (prop == null)
                return "INVALID: " + prop_id;
            return prop_id + ", [" + prop.getName() + "=" + prop.getValue() + "]";
        }
    }

    private final List<ExpressionInfo<?>> expressions;
    private final String name;
    private final String prop_id;
    private final boolean prop_as_expr_flag;

    // TODO: no more creating expressions externally to rules. All the control of adding/changing expressions
    // needs to go live inside the rule so that we always make sure expressions get a new property object
    /** @param name Name of rule
     *  @param prop_id property that this rule applies to
     *  @param prop_as_expr_flag Set to true if expressions output expressions, false if output values
     *  @param exprs Pairs of (boolean expression , output), where output is either a value or another expression
     */
    public RuleInfo(final String name,
            final String prop_id,
            final boolean prop_as_expr_flag,
            final List<ExpressionInfo<?>> exprs)
    {
        this.name = name;
        this.prop_as_expr_flag = prop_as_expr_flag;
        this.prop_id = prop_id;
        this.expressions = Collections.unmodifiableList(Objects.requireNonNull(exprs));
    }

    /** Some properties cannot be the target of rules.
     *  This function takes a widget and returns a list of
     *  valid targets for expressions
     *  @param attached_widget
     *  @return List of all properties of a widget that a rule can target
     */
    static public List<PropInfo> getTargettableProperties (final Widget attached_widget)
    {
        final List<PropInfo> propls = new ArrayList<>();
        for (WidgetProperty<?> prop : attached_widget.getProperties())
        {
            // Do not include RUNTIME properties
            if (prop.getCategory() == WidgetPropertyCategory.RUNTIME)
                continue;
            // Used to exclude WIDGET properties (type, name)
            // until key properties like "text", "pv_name" became WIDGET props.
            // Now only skipping read-only properties
            if (prop.isReadonly())
                continue;
            // Do not include properties that are not supported in scripting
            if ( (prop instanceof MacrosWidgetProperty)  ||
                 (prop instanceof ActionsWidgetProperty) ||
                 (prop instanceof ScriptsWidgetProperty) ||
                 (prop instanceof RulesWidgetProperty) )
                continue;

            for (String name : Widget.expandPropertyNames(prop))
                propls.add(new PropInfo(attached_widget, name));
        };

        return propls;
    }


    /** @return Expressions consisting of (bool expression, target) pairs
     */
    public List<ExpressionInfo<?>> getExpressions()
    {
        return expressions;
    }

    public String getName()
    {
        return name;
    }

    public String getPropID()
    {
        return prop_id;
    }

    public boolean getPropAsExprFlag()
    {
        return prop_as_expr_flag;
    }

    @Override
    public String toString()
    {
        return "RuleInfo('" + name + ": " + expressions + ")";
    }
}

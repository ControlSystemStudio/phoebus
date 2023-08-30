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
 *  A rule is comprised of one or more logical expressions which are evaluated to dynamically set the property of a widget
 *  @author Megan Grodowitz
 */
@SuppressWarnings("nls")
public class RuleInfo
{
    /**
     * An object describing an expression associated with a widget property.
     * The expressions can be of 2 types.
     * 1. boolean expressions, when true the predefined output value is set to the widget property
     * 2. value expression, the output of the expression itself is set to the widget property
     *
     * @author Kunal Shroff
     */
    public static class ExpressionInfo<T>
    {
        private final String exp;
        private final boolean boolean_exp;
        private final T prop_val;

        /**
         * Constructor for creating an Expression
         * @param exp the expression to be evaluated
         * @param boolean_exp a flag indicating if this expression is a boolean expression or a value expression
         * @param prop_val the value to be set if a boolean expression is evaluated true
         */
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

        /** @return Value to use when expression is met */
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
            if (isBooleanExp())
            {
                return "if " + this.exp +"==true : " + prop_val;
            }
            else {
                return this.exp;
            }
        }
    };

    /** Information about a property */
    public static class PropInfo
    {
        private final WidgetProperty<?> prop;
        private final String prop_id;

        /** @param attached_widget Widget
         *  @param prop_id_str Property ID
         */
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

        /** @return WidgetProperty */
        public WidgetProperty<?> getProp() {
            return prop;
        }

        /** @return ID of widget property */
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
     *  @param attached_widget Widget
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

    /** @return Name of rule */

    public String getName()
    {
        return name;
    }

    /** @return Property that is updated by this rule */
    public String getPropID()
    {
        return prop_id;
    }

    /** @return Get value or expression? */
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

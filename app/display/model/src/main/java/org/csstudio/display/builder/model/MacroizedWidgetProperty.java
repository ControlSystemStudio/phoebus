/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import static org.csstudio.display.builder.model.ModelPlugin.logger;

import java.util.logging.Level;

import org.csstudio.display.builder.model.properties.IntegerWidgetProperty;
import org.phoebus.framework.macros.MacroHandler;
import org.phoebus.framework.macros.MacroValueProvider;

/** Base for Property that supports macros.
 *
 *  <p>Properties are typed.
 *  For example, the {@link IntegerWidgetProperty} has
 *  a value of type Integer.
 *
 *  <p>Macro-based properties have an additional 'specification',
 *  a text that may contain macros, for example "$(SOME_MACRO)".
 *
 *  <p>A model editor presents the specification to the user,
 *  and macro based properties persist the specification.
 *
 *  <p>At runtime, the specification is evaluated by
 *  replacing macros, setting the actual value.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
abstract public class MacroizedWidgetProperty<T> extends WidgetProperty<T>
{
// Also allow entering dynamic value description
// (PVManager formula "=`pv1`*2" ?)?
// Runtime then establishes subscription and updates value?

    /** Specification of the value, may contain macros that need to be expanded */
    protected volatile String specification;

    /** Constructor
     *  @param descriptor Property descriptor
     *  @param widget Widget that holds the property and handles listeners
     *  @param default_value Default and initial value
     */
    public MacroizedWidgetProperty(
            final WidgetPropertyDescriptor<T> descriptor,
            final Widget widget,
            final T default_value)
    {
        super(descriptor, widget, default_value);
        // XXX Should null become "null" or ""?
        specification = computeSpecification(default_value);
        // If specification contains macro,
        // clear value to force evaluation of macro on first value request.
        // Can't evaluate now because macros may not be available.
        if (MacroHandler.containsMacros(specification))
            value = null;
    }

    /** @return Value specification. Text that may contain macros */
    public String getSpecification()
    {
        return specification;
    }

    /** Update the specification.
     *
     *  <p>If specification contains macros,
     *  this invalidates the typed value of the property,
     *  which is re-calculated when fetched the next time.
     *
     *  @param specification Specification of the value. Text that may contain macros
     */
    public void setSpecification(final String specification)
    {
        // If specification contains macros,
        // clear value so that it can be evaluated
        // later, presumably once the macro values
        // have been provided.
        if (MacroHandler.containsMacros(specification))
        {
            this.specification = specification;
            value = null;
            firePropertyChange(this, null, null);
            return;
        }
        // Specification contains no macros
        // -> Try to parse & restrict it
        try
        {
            final T old = value;
            value = restrictValue(parseExpandedSpecification(specification));
            this.specification = computeSpecification(value);
            firePropertyChange(this, old, value);
        }
        catch (Exception ex)
        {   // Set "as is".
            // When getValue() is called once macro values have
            // been provided, parsing might actually succeed because
            // of macro values,
            // or the same 'ex' will be reported if the problem
            // still persists.
            this.specification = specification;
            value = null;
            firePropertyChange(this, null, null);
        }
    }

    /** Determine specification for a value
     *  @param value Value
     *  @return Specification for that value
     */
    protected String computeSpecification(T value)
    {
        return String.valueOf(value);
    }

    /** Macro-based properties implement this to parse
     *  a specification text where all macros have been
     *  evaluated into the typed value.
     *
     *  <p>If implementation throws an exception,
     *  the default value of the property is used.
     *
     *  @param text Specification text, all known macros have been resolved
     *  @return Typed value
     *  @throws Exception on parse error.
     */
    abstract protected T parseExpandedSpecification(String text) throws Exception;

    /** Evaluates value based on specification
     *  @return Current value of the property
     */
    @Override
    public synchronized T getValue()
    {
        if (value == null)
        {
            final MacroValueProvider macros = widget.getMacrosOrProperties();
            String expanded;
            try
            {
                expanded = MacroHandler.replace(macros, specification);
            }
            catch (StackOverflowError ex)
            {
                expanded = "Recursive " + specification.replace("$", "");
            }
            catch (final Exception ex)
            {
                logger.log(Level.WARNING, widget + " property " + getName() + " cannot expand macros for '" + specification + "'", ex);
                expanded = specification;
            }

            // Warn if expanded text still contains macros.
            // .. unless specification contained escaped macros,
            // which have been un-escaped.
            // Note that this ignores remaining macros as soon
            // as there is just one escaped macro, as in "$(MISSING_AND_IGNORED) \\$(ESCAPED)"
            if (MacroHandler.containsMacros(expanded)  &&  ! specification.contains("\\$"))
                logger.log(Level.WARNING, widget + " '" + getName() + "' is not fully resolved: '" + expanded + "'");

            try
            {
                // Do NOT notify listeners.
                // Otherwise, if property.getValue() is called within a listener
                // and the listener is registered to fire when setValue() is called
                // then listener -> getValue() -> setValue() -> call listener again ..
                doSetValue(parseExpandedSpecification(expanded), false);
            }
            catch (final Exception ex)
            {
                logger.log(Level.WARNING, widget + " property " + getName() + " cannot evaluate '" + expanded + "'", ex);
                value = default_value;
            }
        }
        return value;
    }

    /** @return <code>true</code> if current value matches the default value */
    @Override
    public boolean isDefaultValue()
    {
        return !use_class  &&  specification.equals(computeSpecification(default_value));
    }

    /** Sets property to a typed value.
     *
     *  <p>Updates the specification to string representation
     *  of the value.
     *
     *  @param value New typed value of the property
     */
    @Override
    public void setValue(final T value)
    {
        specification = computeSpecification(value);
        super.setValue(value);
    }

    /** @return Debug representation */
    @Override
    public String toString()
    {
        final T safe_copy = value;
        if (safe_copy == null)
            return "'" + getName() + "' = " + specification;
        else
            return "'" + getName() + "' = " + value;
    }
}

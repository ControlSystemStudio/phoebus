/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import static org.csstudio.display.builder.model.ModelPlugin.logger;

import java.lang.reflect.Constructor;
import java.util.Objects;
import java.util.logging.Level;

import javax.xml.stream.XMLStreamWriter;

import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.properties.PropertyChangeHandler;
import org.w3c.dom.Element;

/**
 * Base class for all widget properties.
 *
 * <p>
 * The property name identifies a property inside the model. A separate
 * description, which can be localized, is meant for user interfaces that
 * present the property to humans.
 *
 * @author Kay Kasemir
 *
 * @param <T> Type of the property's value
 */
@SuppressWarnings("nls")
public abstract class WidgetProperty<T extends Object> extends PropertyChangeHandler<T>
{
    /** 'Parent', widget that holds this property */
    protected final Widget widget;

    /** Property descriptor */
    protected final WidgetPropertyDescriptor<T> descriptor;

    /** Default value
     *
     *  <p>Initial value, can also be used as fallback
     *  when receiving an invalid new value.
     */
    protected final T default_value;

    /** Does property follow the value suggested by class? */
    protected volatile boolean use_class = false;

    /** Current value of the property */
    protected volatile T value;

    /** Constructor
     *  @param descriptor Property descriptor
     *  @param widget Widget that holds the property and handles listeners
     *  @param default_value Default and initial value
     */
    protected WidgetProperty(
            final WidgetPropertyDescriptor<T> descriptor,
            final Widget widget,
            final T default_value)
    {
        this.widget = widget;
        this.descriptor = Objects.requireNonNull(descriptor);
        this.default_value = default_value;
        this.value = this.default_value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public WidgetProperty<T> clone()
    {
        WidgetProperty<T> ret = null;

        //ret = this.getClass().getDeclaredConstructor(cArg).newInstance(descriptor, widget, default_value);
        Constructor<?>[] constructors = this.getClass().getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            Class<?>[] params = constructor.getParameterTypes();
            if ( (params.length == 3) &&
                    params[0].isInstance(descriptor) &&
                    params[1].isInstance(widget) &&
                    params[2].isInstance(default_value) )
            {
                try
                {
                    ret = (WidgetProperty<T>) constructor.newInstance(descriptor, widget, default_value);
                    if (ret instanceof MacroizedWidgetProperty)
                        ((MacroizedWidgetProperty<T>) ret).setSpecification(((MacroizedWidgetProperty<T>) this).getSpecification());
                    else
                        ret.setValue( this.getValue() );
                }
                catch (Exception ex)
                {
                    logger.log(Level.SEVERE, "Cannot clone " + this, ex);
                }
                return ret;
            }
        }
        return ret;
    }

    /** @return Widget that has this property */
    public Widget getWidget()
    {
        return widget;
    }

    /** @return {@link WidgetPropertyCategory} of this property */
    public WidgetPropertyCategory getCategory()
    {
        return descriptor.getCategory();
    }

    /** @return Name that identifies the property within the model API */
    public String getName()
    {
        return descriptor.getName();
    }

    /** Get full path to property
     *
     *  <p>A widget property 'visible' might be the basic
     *  widget's 'visible' property, or the 'visible' property
     *  of an element in a structure within an array,
     *  like ImageWidget's 'color_bar.visible'.
     *
     *  <p>The path is seldom needed, so properties do not
     *  carry a 'parent' reference for fast determination
     *  of the path.
     *  Instead, the path is determined at runtime by
     *  searching down from the top-level widget properties.
     *
     *  @return The full path to property.
     */
    public String getPath()
    {
        final StringBuilder buf = new StringBuilder();
        for (WidgetProperty<?> prop : widget.getProperties())
            if (locateProperty(buf, prop, false))
                break;
        if (buf.length() <= 0)
            logger.log(Level.WARNING, "Cannot determine path to " + getName() + " for " + widget);
        return buf.toString();
    }

    /** Recursively build path to property
     *  @param buf Buffer where path is assembled
     *  @param property Property to locate
     *  @param in_array Is recursion level within an array?
     *  @return Recursively growing path to property
     */
    private boolean locateProperty(final StringBuilder buf, final WidgetProperty<?> property, final boolean in_array)
    {
        if (property == this)
        {
            buf.append(getName());
            return true;
        }
        if (property instanceof ArrayWidgetProperty<?>)
        {
            final ArrayWidgetProperty<?> array = (ArrayWidgetProperty<?>) property;
            for (int i=0; i<array.size(); ++i)
                if (locateProperty(buf, array.getElement(i), true))
                {
                    buf.insert(0, array.getName() + "[" + i + "]");
                    return true;
                }
        }
        else if (property instanceof StructuredWidgetProperty)
        {
            final StructuredWidgetProperty struct = (StructuredWidgetProperty) property;
            for (int i=0; i<struct.size(); ++i)
                if (locateProperty(buf, struct.getElement(i), false))
                {
                    buf.insert(0, '.');
                    // In an array, we omit the structure name.
                    // "traces[1].name" instead of "traces[1].trace.name"
                    if (! in_array)
                        buf.insert(0, struct.getName());
                    return true;
                }
        }
        return false;
    }

    /** @return Human-readable description of the property */
    public String getDescription()
    {
        return descriptor.getDescription();
    }

    /** @return <code>true</code> if this property prohibits write access */
    public boolean isReadonly()
    {
        return descriptor.isReadonly();
    }

    /** @return Default value of the property */
    public T getDefaultValue()
    {
        return default_value;
    }

    /** @return Current value of the property */
    public T getValue()
    {
        return value;
    }

    /** @return <code>true</code> if current value matches the default value */
    public boolean isDefaultValue()
    {
        // In class editor, even 'default' values need
        // to be written if they're marked as 'use_class'.
        // In display editor, if the value came from the
        // class but happens to match the default it's
        // still written - no big deal.
        return !use_class  &&  Objects.equals(value, default_value);
    }

    /** @param use_class Should value of this property follow
     *                   the suggestion from the class support?
     */
    public void useWidgetClass(final boolean use_class)
    {
        if (this.use_class == use_class)
            return;

        this.use_class = use_class;

        // Editor for class model needs update, runtime doesn't
        final DisplayModel model = getWidget().checkDisplayModel();
        if (model != null  &&  model.isClassModel())
            firePropertyChange(null, null);
    }

    /** @return Is value of this property following
     *          the suggestion from the class support?
     */
    public boolean isUsingWidgetClass()
    {
        return use_class;
    }

    /** Restrict value.
     *
     *  <p>Called when setting a new value to transform the provided
     *  value into one that fits the permitted value range.
     *
     *  <p>Derived class may override to limit the range of
     *  permitted values.
     *  It may return the requested value,
     *  or an adjusted value.
     *  To refuse the requested value,
     *  return the current value of the property.
     *
     *  @param requested_value Suggested value
     *  @return Allowed value. Must not be null.
     */
    protected T restrictValue(final T requested_value)
    {
        return requested_value;
    }

    /** @param value New value of the property */
    public void setValue(final T value)
    {
        doSetValue(value, true);
    }

    /** @param value New value of the property
     *  @param notify_listeners Send notification to listeners?
     */
    protected void doSetValue(final T value, final boolean notify_listeners)
    {
        if (isReadonly())
            return;

        final T old_value = this.value;
        // Check value
        final T new_value = restrictValue(value);
        this.value = new_value;
        if (notify_listeners)
            firePropertyChange(this, old_value, new_value);
    }

    /** Set value from Object.
     *
     *  <p>Type-safe access via <code>setValue()</code> is preferred,Helper for implementing Runtime properties
     *  but if property type is not known, this method allows setting
     *  the property value from an Object.
     *
     *  @param value New value of the property
     *  @throws Exception if value type is not applicable to this property
     */
    abstract public void setValueFromObject(final Object value) throws Exception;

    /** Persist value to XML
     *
     *  <p>Writer will be positioned inside the property.
     *  Implementation needs to write the property's value.
     *  @param model_writer {@link ModelWriter}
     *  @param writer Stream writer
     *  @throws Exception on error
     */
    abstract public void writeToXML(final ModelWriter model_writer, final XMLStreamWriter writer) throws Exception;

    /** Read value from persisted XML
     *  @param model_reader {@link ModelReader}
     *  @param property_xml XML element
     *  @throws Exception on error
     */
    abstract public void readFromXML(final ModelReader model_reader, final Element property_xml) throws Exception;

    /** Notify listeners of property change.
     *
     *  <p>New value usually matches <code>property.getValue()</code>,
     *  but in multi-threaded context value might already have changed
     *  _again_ by the time this executes.
     *
     *  <p>Suppresses notifications where old_value equals new_value,
     *  unless the values are null, treating that as a "notify anyway"
     *  case.
     *
     *  @param old_value Original value
     *  @param new_value New value
     */
    protected void firePropertyChange(final T old_value, final T new_value)
    {
        firePropertyChange(this, old_value, new_value);
    }

    /** @return Debug representation */
    @Override
    public String toString()
    {
        return "'" + getName() + "' = " + value;
    }
}

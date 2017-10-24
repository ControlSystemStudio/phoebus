/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import java.util.Objects;

/** Widget property descriptor.
 *
 *  <p>The property name identifies a property inside the model.
 *  A separate description, which can be localized, is meant for
 *  user interfaces that present the property to humans.
 *
 *  @author Kay Kasemir
 *
 *  @param <T> Type of the property's value
 */
@SuppressWarnings("nls")
public abstract class WidgetPropertyDescriptor<T extends Object>
{
    final private WidgetPropertyCategory category;

    final private String name;

    final private String description;

    final private boolean readonly;

    /** Constructor
     *  @param category Category
     *  @param name Internal name of the property
     *  @param description Human-readable description
     *  @param readonly <code>true</code> if property is read-only
     */
    protected WidgetPropertyDescriptor(
            final WidgetPropertyCategory category,
            final String name, final String description,
            final boolean readonly)
    {
        this.category = Objects.requireNonNull(category);
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.readonly = readonly;
    }

    /** Constructor for writable property
     *  @param category Category
     *  @param name Internal name of the property
     *  @param description Human-readable description
     */
    protected WidgetPropertyDescriptor(
            final WidgetPropertyCategory category,
            final String name, final String description)
    {
        this(category, name, description, false);
    }

    /** @return {@link WidgetPropertyCategory} of this property */
    public WidgetPropertyCategory getCategory()
    {
        return category;
    }

    /** @return Name that identifies the property within the model API */
    public String getName()
    {
        return name;
    }

    /** @return Human-readable description of the property */
    public String getDescription()
    {
        return description;
    }

    /** @return <code>true</code> if this property prohibits write access */
    public boolean isReadonly()
    {
        return readonly;
    }

    /** Create property.
     *  @param widget Widget that holds the property and handles listeners
     *  @param default_value Default and initial value
     *  @return {@link WidgetProperty}
     */
    abstract public WidgetProperty<T> createProperty(final Widget widget,
            final T default_value);

    /** @return Debug representation */
    @Override
    public String toString()
    {
        return "Widget Property " + name;
    }
}

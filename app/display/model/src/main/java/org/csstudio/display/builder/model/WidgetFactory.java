/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.csstudio.display.builder.model.spi.WidgetsService;

/** Factory that creates widgets based on type
 *
 *  <p>Locates widgets via the SPI {@link WidgetsService}
 *
 *  <p>Widgets register with their 'primary' type,
 *  which needs to be unique.
 *  In addition, they can provide alternate types
 *  to list the legacy types which they handle.
 *  More than one widget can register for the same
 *  alternate type.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WidgetFactory
{
    /** Exception that indicates an unknown widget type */
    public static class WidgetTypeException extends Exception
    {
        private static final long serialVersionUID = 1L;
        private final String type;

        public WidgetTypeException(final String type, final String message)
        {
            super(message);
            this.type = type;
        }

        /** @return Widget type that's not known */
        public String getType()
        {
            return type;
        }
    }

    /** Singleton instance */
    private static final WidgetFactory instance = new WidgetFactory();

    /** List of widget types.
     *
     *  <p>Sorted by widget category, then type
     */
    private final SortedSet<WidgetDescriptor> descriptors =
        new TreeSet<>(
            Comparator.comparing(WidgetDescriptor::getCategory)
                      .thenComparing(WidgetDescriptor::getType));

    /** Map of primary type IDs to {@link WidgetDescriptor} */
    private final Map<String, WidgetDescriptor> descriptor_by_type = new ConcurrentHashMap<>();

    /** Map of alternate type IDs to {@link WidgetDescriptor}s */
    private final Map<String, List<WidgetDescriptor>> alternates_by_type = new ConcurrentHashMap<>();

    // Prevent instantiation
    private WidgetFactory()
    {
        // Load widgets from services
        for (WidgetsService service : ServiceLoader.load(WidgetsService.class))
            for (WidgetDescriptor descriptor : service.getWidgetDescriptors())
                addWidgetType(descriptor);
    }

    /** @return Singleton instance */
    public static WidgetFactory getInstance()
    {
        return instance;
    }

    /** Inform factory about a widget type.
     *
     *  <p>This is meant to be called during plugin initialization,
     *  based on information from the registry.
     *
     *  @param descriptor {@link WidgetDescriptor}
     */
    public void addWidgetType(final WidgetDescriptor descriptor)
    {
        // Primary type must be unique
        if (descriptor_by_type.putIfAbsent(descriptor.getType(), descriptor) != null)
            throw new Error(descriptor + " already defined");

        // descriptors sorts by category and type
        descriptors.add(descriptor);

        for (final String alternate : descriptor.getAlternateTypes())
            alternates_by_type.computeIfAbsent(alternate, k -> new CopyOnWriteArrayList<>())
                              .add(descriptor);
    }

    /** @return Descriptions of all currently known widget types */
    public Set<WidgetDescriptor> getWidgetDescriptions()
    {
        return Collections.unmodifiableSortedSet(descriptors);
    }

    /** Check if type is defined.
     *  @param type Widget type ID
     *  @return WidgetDescriptor
     */
    public WidgetDescriptor getWidgetDescriptor(final String type)
    {
        return Objects.requireNonNull(descriptor_by_type.get(type));
    }

    /** Get all widget descriptors
     *  @param type Type ID of the widget or alternate type
     *  @return {@link WidgetDescriptor}s, starting with primary followed by possible alternates
     *  @throws WidgetTypeException when widget type is not known
     */
    public List<WidgetDescriptor> getAllWidgetDescriptors(final String type) throws WidgetTypeException
    {
        final List<WidgetDescriptor> descs = new ArrayList<>();
        final WidgetDescriptor descriptor = descriptor_by_type.get(type);
        if (descriptor != null)
            descs.add(descriptor);
        final List<WidgetDescriptor> alt = alternates_by_type.get(type);
        if (alt != null)
            descs.addAll(alt);
        if (descs.isEmpty())
            throw new WidgetTypeException(type, "Unknown widget type '" + type + "'");
        return descs;
    }
}

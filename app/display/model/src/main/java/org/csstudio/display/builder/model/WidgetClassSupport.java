/*******************************************************************************
 * Copyright (c) 2015-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import static org.csstudio.display.builder.model.ModelPlugin.logger;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.logging.Level;

import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.widgets.TabsWidget;
import org.csstudio.display.builder.model.widgets.TabsWidget.TabItemProperty;

/** Widget 'class' support
 *
 *  <p>Maintains 'classes' for each widget type,
 *  for example a 'TITLE' class for 'label' widgets.
 *  A class consists of properties and values
 *  which can then be applied to a widget.
 *
 *  <p>Each property of a widget can be configured
 *  to _not_ use the class settings.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WidgetClassSupport
{
    /** File extension used for widget class definition files */
    // 'bob class file'
    public static final String FILE_EXTENSION = "bcf";

    /** Name of the default class
     *
     *  <p>Is always present, using the default value
     *  of each property.
     *  Can be overwritten in widget class file.
     */
    public static final String DEFAULT = "DEFAULT";

    /** Alphabetical sort with exception that 'DEFAULT' is on top */
    private static final Comparator<String> classname_sort = (a, b) ->
    {
        final int cmp = a.compareTo(b);
        if (cmp == 0)
            return 0;
        if (DEFAULT.equals(a))
            return -1;
        if (DEFAULT.equals(b))
            return 1;
        return cmp;
    };

    /** Value for a property
     *
     *  <p>Keeps either the specification (for macro-based)
     *  or the class-based value of property
     *  and supports applying it to a widget's property.
     */
    public static class PropertyValue
    {
        private final String specification;
        private final Object value;

        public PropertyValue(final WidgetProperty<?> property)
        {
            if (property instanceof MacroizedWidgetProperty)
            {
                specification = ((MacroizedWidgetProperty<?>) property).getSpecification();
                value = null;
            }
            else
            {
                specification = null;
                value = property.getValue();
            }
        }

        public void apply(final WidgetProperty<?> property)
        {
            if (specification != null)
            {
                if (property instanceof MacroizedWidgetProperty)
                    ((MacroizedWidgetProperty<?>)property).setSpecification(specification);
                else
                {
                    final Widget widget = property.getWidget();
                    logger.log(Level.WARNING, "Expected macroized property to apply class value for " + widget.getWidgetClass() +
                            " to property " + property.getName() + " of " + widget);
                }
            }
            else
                try
                {
                    property.setValueFromObject(value);
                }
                catch (Exception ex)
                {
                    final Widget widget = property.getWidget();
                    logger.log(Level.WARNING, "Cannot apply class value for " + widget.getWidgetClass() +
                                              " to property " + property.getName() + " of " + widget, ex);
                }
        }

        @Override
        public String toString()
        {
            if (specification != null)
                return specification;
            return Objects.toString(value);
        }
    }

    /** Map:
     *  widget type to classes-for-type,
     *  class name to properties,
     *  property name to class value
     */
    // TreeMap  -> sorted; may help when dumping, debugging
    private final Map<String,
                      Map<String,
                          Map<String, PropertyValue>>> widget_types = new TreeMap<>();

    /** Default model */
    public WidgetClassSupport()
    {
        registerDefaultWidgets();
    }

    /** Load widget classes
     *
     *  @param stream Stream for a widget class file
     *  @throws Exception on error
     */
    public WidgetClassSupport(final InputStream stream) throws Exception
    {
        registerDefaultWidgets();
        // Register widget classes from class definition file
        loadClasses(stream);
    }

    /** Load widget classes
     *  @param stream Stream for a widget class file
     *  @throws Exception on error
     */
    public void loadClasses(final InputStream stream) throws Exception
    {
        final DisplayModel model = new ModelReader(stream).readModel();
        model.propName().setValue(DEFAULT);
        registerClass(model);
        for (Widget widget : model.getChildren())
            registerClass(widget);
    }

    /** Register all DEFAULT widgets, including model itself */
    private void registerDefaultWidgets()
    {
        final DisplayModel model = new DisplayModel();
        model.propName().setValue(DEFAULT);
        registerClass(model);
        for (WidgetDescriptor descr : WidgetFactory.getInstance().getWidgetDescriptions())
        {
            final Widget widget = descr.createWidget();
            widget.propName().setValue(DEFAULT);
            registerClass(widget);
        }
    }

    /** @param widget Widget to register, using its class and properties */
    public void registerClass(final Widget widget)
    {
        final String type = widget.getType();
        // For the class definition file, the widget 'name' sets the class to define.
        // The 'class' is later used to pick the class to apply to a widget,
        // it's ignored when defining classes.
        final String widget_class = widget.getName();

        // Empty -> Not meant to define a class
        if (widget_class.isEmpty())
            return;
        final Map<String, Map<String, PropertyValue>> widget_classes = widget_types.computeIfAbsent(type, t -> new TreeMap<>(classname_sort));

        // New class for this widget?
        // OK to re-define 'DEFAULT', warn for others
        Map<String, PropertyValue> class_properties = widget_classes.get(widget_class);
        if (class_properties == null)
        {
            class_properties = new TreeMap<>();
            widget_classes.put(widget_class, class_properties);
        }
        else if (! DEFAULT.equals(widget_class))
            logger.log(Level.WARNING, "Widget type '" + type + "' class '" + widget_class + "' is defined more than once");

        for (WidgetProperty<?> property : widget.getProperties())
            if (property.isUsingWidgetClass())
                class_properties.put(property.getName(), new PropertyValue(property));
    }

    /** Get known widget classes
     *  @param widget_type Widget type
     *  @return Widget class names for that type
     */
    public Collection<String> getWidgetClasses(final String widget_type)
    {
        final Map<String, Map<String, PropertyValue>> classes = widget_types.get(widget_type);
        if (classes == null)
            return Arrays.asList(DEFAULT);
        return classes.keySet();
    }

    /** Get class-based properties
     *
     *  @param widget Widget for which to get the class info
     *  @return Properties and values for that widget type and class, or <code>null</code>
     */
    private Map<String, PropertyValue> getClassSettings(final Widget widget)
    {
        final Map<String, Map<String, PropertyValue>> widget_classes = widget_types.get(widget.getType());
        if (widget_classes == null)
        {
            logger.log(Level.WARNING, "No class support for unknown widget type " + widget);
            return null;
        }

        // Empty class name indicates: Do not apply any class, not even DEFAULT
        final String class_name = widget.getWidgetClass();
        if (class_name.isEmpty())
            return null;

        final Map<String, PropertyValue> result = widget_classes.get(class_name);
        if (result == null)
            logger.log(Level.WARNING, "Undefined widget type " + widget.getType() +
                                      " and class " + class_name);
        return result;
    }

    /** Apply class-based property values to widget (and child widgets)
     *
     *  @param widget Widget to update based on its current 'class'
     */
    public void apply(final Widget widget)
    {
        final Map<String, PropertyValue> class_settings = getClassSettings(widget);
        for (WidgetProperty<?> prop : widget.getProperties())
            apply(class_settings, prop);
        // Apply to child widgets
        ChildrenProperty children = ChildrenProperty.getChildren(widget);
        if (children != null)
        {
            for (Widget child : children.getValue())
                apply(child);
        }
        else if (widget instanceof TabsWidget)
        {   // Apply to child widgets in all tabs
            final List<TabItemProperty> tabs = ((TabsWidget)widget).propTabs().getValue();
            for (TabItemProperty tab : tabs)
            {
                children = tab.children();
                for (Widget child : children.getValue())
                    apply(child);
            }
        }
    }

    /** Apply class-based property values to a property
     *
     *  @param class_settings Settings for a property
     *  @param property Property to update
     */
    private void apply(final Map<String, PropertyValue> class_settings, final WidgetProperty<?> property)
    {
        if (class_settings == null  ||
            (property instanceof RuntimeWidgetProperty))
        {
            property.useWidgetClass(false);
            return;
        }

        final PropertyValue class_setting = class_settings.get(property.getName());
        if (class_setting == null)
            property.useWidgetClass(false);
        else
        {
            property.useWidgetClass(true);
            class_setting.apply(property);
        }
    }

    /** Apply class-based setting to a property
     *  @param property Property to which to apply settings for its widget's class
     */
    public void apply(final WidgetProperty<?> property)
    {
        final Map<String, PropertyValue> class_settings = getClassSettings(property.getWidget());
        if (class_settings != null)
            apply(class_settings, property);
    }

    /** @return Debug representation */
    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();

        for (String type : widget_types.keySet())
        {
            buf.append(type).append(":\n");

            final Map<String, Map<String, PropertyValue>> widget_classes = widget_types.get(type);
            for (String clazz : widget_classes.keySet())
            {
                buf.append("  ").append(clazz).append("\n");
                final Map<String, PropertyValue> class_props = widget_classes.get(clazz);
                for (String prop : class_props.keySet())
                    buf.append("    ").append(prop).append(" = ").append(class_props.get(prop)).append("\n");
            }
        }

        return buf.toString();
    }
}

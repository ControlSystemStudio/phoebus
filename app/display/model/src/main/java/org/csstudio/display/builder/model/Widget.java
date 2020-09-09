/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import static org.csstudio.display.builder.model.ModelPlugin.logger;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propActions;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propHeight;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propName;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propRules;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propScripts;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propType;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propWidgetClass;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propWidth;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propX;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.csstudio.display.builder.model.macros.MacroOrPropertyProvider;
import org.csstudio.display.builder.model.properties.ActionInfos;
import org.csstudio.display.builder.model.properties.ScriptInfo;
import org.csstudio.display.builder.model.rules.RuleInfo;
import org.csstudio.display.builder.model.widgets.EmbeddedDisplayWidget;
import org.phoebus.framework.macros.MacroValueProvider;
import org.phoebus.framework.macros.Macros;

/** Base class for all widgets.
 *
 *  <p>A Widget has properties, supporting read access, subscription
 *  and for most properties also write access.
 *
 *  <p>Properties can be accessed in a most generic way based on the
 *  property name:
 *  <pre>
 *  getPropertyValue("text")
 *  setPropertyValue("text", "Hello")
 *  getPropertyValue("x")
 *  setPropertyValue("x", 60)
 *  </pre>
 *
 *  <p>While this is ideal for access from scripts,
 *  Java code that deals with a specific widget can access
 *  properties in the type-safe way:
 *  <pre>
 *  LabelWidget label;
 *  label.positionX().getValue();
 *  label.positionX().setValue(60);
 *  </pre>
 *
 *  <p>Widgets are part of a hierarchy.
 *  Their parent is either the {@link DisplayModel} or another
 *  widget with a {@link ChildrenProperty}
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Widget
{
    /** Initial version for all Display Builder widgets.
     *
     *  <p>Legacy used 1.0.0 for most widgets,
     *  so 2.0.0 indicates an update.
     *  Selected legacy widgets had incremented to a higher version,
     *  which needs to be handled for each such widget.
     */
    public static final Version BASE_WIDGET_VERSION = new Version(2, 0, 0);

    // These user data keys are reserved for internal use
    // of the framework.
    //
    // On the API level, the Model (DisplayModel, Widgets)
    // is independent from the Representation (ToolkitRepresentation)
    // and Runtime (WidgetRuntime).
    //
    // The representation and runtime implementations, however,
    // need to associate certain pieces of data with model elements,
    // which is done via the following reserved user data keys.
    //
    // They all start with an underscore to indicate that they
    // are meant to be private, not to be used as API.

    /** Reserved widget user data key for storing the representation.
     *
     *  <p>The WidgetRepresentation for each {@link Widget}
     *  is stored under this key.
     */
    public static final String USER_DATA_REPRESENTATION = "_representation";

    /** Reserved user data key for Widget that has 'children', i.e. is a parent,
     *  to store the toolkit parent item
     */
    public static final String USER_DATA_TOOLKIT_PARENT = "_toolkit_parent";

    /** Reserved widget user data key for storing the runtime.
     *
     *  <p>The WidgetRuntime for each {@link Widget}
     *  is stored under this key.
     */
    public static final String USER_DATA_RUNTIME = "_runtime";

    /** Reserved widget user data key for storing script support.
     *
     *  <p>ScriptSupport is attached to the top-level root
     *  of the widget tree, i.e. the {@link DisplayModel}
     *  that is obtained by traversing up via {@link EmbeddedDisplayWidget}s
     *  to the top-level model.
     */
    public static final String USER_DATA_SCRIPT_SUPPORT = "_script_support";

    /** Parent widget */
    private volatile Widget parent = null;

    /** All properties, ordered by category, then sequence of definition */
    protected final Set<WidgetProperty<?>> properties;

    // Design decision:
    //
    // Widget holds a map of properties: "name" -> CommonWidgetProperties.widgetName
    //
    // Could also depend on each widget defining getters/setters getName()/setName()
    // and then use beam-type introspection, but this would not allow determining
    // if a property has a default value, is a runtime-only property etc.
    //
    // A property accessor nameProperty() as used for JavaFX bindable properties
    // would work as long as it preserves the order of properties.
    //
    // The implementation might later change from a property_map to
    // introspection-based lookup of xxxProperty() accessors.
    // The API for widget users would remain the same:
    // getProperties(), getProperty(), getPropertyValue(), setPropertyValue()

    /** Map of property names to properties */
    // Map is final, all properties are collected in widget constructor.
    // Values of properties can change, but the list of properties itself
    // is thread safe
    protected final Map<String, WidgetProperty<?>> property_map;

    // Actual properties
    private WidgetProperty<String> type;
    private WidgetProperty<String> name;
    private WidgetProperty<String> widget_class;
    private WidgetProperty<Integer> x;
    private WidgetProperty<Integer> y;
    private WidgetProperty<Integer> width;
    private WidgetProperty<Integer> height;
    private WidgetProperty<ActionInfos> actions;
    private WidgetProperty<List<RuleInfo>> rules;
    private WidgetProperty<List<ScriptInfo>> scripts;

    /** Map of user data */
    protected final Map<String, Object> user_data = new ConcurrentHashMap<>(4); // Reserve room for "representation", "runtime"

    /** Loaded without errors? */
    protected volatile Boolean clean;

    /** Widget constructor.
     *  @param type Widget type
     */
    public Widget(final String type)
    {
        this(type, 100, 20);
    }

    /** Widget constructor.
     *  @param type Widget type
     *  @param default_width Default width
     *  @param default_height .. and height
     */
    public Widget(final String type, final int default_width, final int default_height)
    {
        // Collect properties
        final List<WidgetProperty<?>> prelim_properties = new ArrayList<>();

        // -- Mandatory properties --
        prelim_properties.add(this.type = propType.createProperty(this, type));
        prelim_properties.add(name = propName.createProperty(this, ""));
        prelim_properties.add(widget_class = propWidgetClass.createProperty(this, WidgetClassSupport.DEFAULT));
        prelim_properties.add(x = propX.createProperty(this, 0));
        prelim_properties.add(y = propY.createProperty(this, 0));
        prelim_properties.add(width = propWidth.createProperty(this, default_width));
        prelim_properties.add(height = propHeight.createProperty(this, default_height));
        prelim_properties.add(actions = propActions.createProperty(this, ActionInfos.EMPTY));
        prelim_properties.add(rules = propRules.createProperty(this, Collections.emptyList()));
        prelim_properties.add(scripts = propScripts.createProperty(this, Collections.emptyList()));

        // -- Widget-specific properties --
        defineProperties(prelim_properties);
        if (prelim_properties.contains(null))
            throw new IllegalStateException("Null properties");

        // Sort by category, then order of definition.
        // Prelim_properties has the original order of definition,
        // which we want to preserve as a secondary sorting criteria
        // after property category.
        final List<WidgetProperty<?>> sorted = new ArrayList<>(prelim_properties.size());
        sorted.addAll(prelim_properties);

        final Comparator<WidgetProperty<?>> byCategory =
                Comparator.comparing(WidgetProperty::getCategory);
        final Comparator<WidgetProperty<?>> byOrder =
                Comparator.comparingInt(p -> prelim_properties.indexOf(p));
        Collections.sort(sorted, byCategory.thenComparing(byOrder));
        // Capture as constant sorted set
        properties = Collections.unmodifiableSet(new LinkedHashSet<>(sorted));

        // Map for faster lookup by property name
        property_map = properties.stream().collect(
                Collectors.toMap(WidgetProperty::getName, Function.identity()));
    }

    /** Unique runtime identifier of a widget
     *
     *  <p>At runtime, this ID can be used to construct
     *  PVs that are unique and specific to this instance
     *  of a widget.
     *  Even if the same display is opened multiple times
     *  within the same JVM, the widget is very likely
     *  to receive a new, unique identifier.
     *
     *  @return Unique Runtime Identifier for widget
     */
    public final String getID()
    {   // Base on ID hash code
        final int id = System.identityHashCode(this);
        return "WD" + Integer.toHexString(id);
    }

    /** @return Widget version number */
    public Version getVersion()
    {
        return BASE_WIDGET_VERSION;
    }

    /** @return Widget Type */
    public final String getType()
    {
        return type.getValue();
    }

    /** @return Widget Name */
    public final String getName()
    {
        return name.getValue();
    }

    /** @return Widget class to use for updating properties that use the class */
    public final String getWidgetClass()
    {
        return widget_class.getValue();
    }

    /** @return Parent widget in Widget tree */
    public final Optional<Widget> getParent()
    {
        return Optional.ofNullable(parent);
    }

    /** Invoked by the parent widget
     *  @param parent Parent widget
     */
    protected void setParent(final Widget parent)
    {
        if (parent == this)
            throw new IllegalArgumentException();
        this.parent = parent;
    }

    /** Locate display model, i.e. root of widget tree
     *
     *  <p>Note that for embedded displays, this would
     *  return the embedded model, not the top-level
     *  model of the window.
     *  Compare <code>getTopDisplayModel()</code>
     *
     *  @return {@link DisplayModel} for widget
     *  @throws Exception if widget is not part of a model
     */
    public final DisplayModel getDisplayModel() throws Exception
    {
        final DisplayModel model = checkDisplayModel();
        if (model == null)
            throw new Exception("Missing DisplayModel for " + this);
        return model;
    }

    /** Locate display model, i.e. root of widget tree
     *
     *  @return {@link DisplayModel} for widget or <code>null</code>
     *  @see #getDisplayModel() version that throws exception
     */
    public final DisplayModel checkDisplayModel()
    {
        Widget candidate = this;
        while (candidate.getParent().isPresent())
            candidate = candidate.getParent().get();
        if (candidate instanceof DisplayModel)
            return (DisplayModel) candidate;
        return null;
    }

    /** Locate top display model.
     *
     *  <p>For embedded displays, <code>getDisplayModel</code>
     *  only provides the embedded model.
     *  This method traverses up via the {@link EmbeddedDisplayWidget}
     *  to the top-level display model.
     *
     *  @return Top-level {@link DisplayModel} for widget
     *  @throws Exception if widget is not part of a model
     */
    public final DisplayModel getTopDisplayModel() throws Exception
    {
        DisplayModel model = getDisplayModel();
        while (true)
        {
           final Widget embedder = model.getUserData(DisplayModel.USER_DATA_EMBEDDING_WIDGET);
           if (embedder == null)
               return model;
           model = embedder.getTopDisplayModel();
       }
    }

    public final void setConfiguratorResult(final WidgetConfigurator configurator)
    {
        if (this.clean != null)
            throw new RuntimeException("Cannot change cleanliness of Widget");

        // Only set it if not clean; DisplayModel needs to update it when all the children are loaded
        if (configurator.isClean() == false)
            this.clean = Boolean.valueOf(configurator.isClean());
    }

    /** @return <code>true</code> if this widget was loaded without errors,
     *          <code>false</code> if there were errors
     */
    public boolean isClean()
    {
        Boolean safe = clean;

        if (safe != null && ! safe.booleanValue())
            return false;

        java.util.Optional<WidgetProperty<DisplayModel>> child_dm_prop = checkProperty(EmbeddedDisplayWidget.runtimeModel.getName());
        if (child_dm_prop.isPresent())
        {
            final DisplayModel child_dm = child_dm_prop.get().getValue();
            if (child_dm != null && child_dm.isClean() == false)
            {
                clean = Boolean.valueOf(false);
                return false;
            }
        }

        return true;
    }

    /** Called on construction to define widget's properties.
     *
     *  <p>Mandatory properties have already been defined.
     *  Derived class overrides to add its own properties.
     *
     *  @param properties List to which properties must be added
     */
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        // Derived class should invoke
        //    super.defineProperties(properties)
        // and may then add its own properties.
    }

    // Accessors to properties are not strictly needed
    // because of generic getProperty(..),
    // but are useful in IDE when dealing with
    // known widget type

    /** @return 'name' property */
    public final WidgetProperty<String> propName()
    {
        return name;
    }

    /** @return 'class' property */
    public final WidgetProperty<String> propClass()
    {
        return widget_class;
    }

    /** @return 'x' property */
    public final WidgetProperty<Integer> propX()
    {
        return x;
    }

    /** @return 'y' property */
    public final WidgetProperty<Integer> propY()
    {
        return y;
    }

    /** @return 'width' property */
    public final WidgetProperty<Integer> propWidth()
    {
        return width;
    }

    /** @return 'height' property */
    public final WidgetProperty<Integer> propHeight()
    {
        return height;
    }

    /** @return 'actions' property */
    public final WidgetProperty<ActionInfos> propActions()
    {
        return actions;
    }

    /** @return 'rules' property */
    public final WidgetProperty<List<RuleInfo>> propRules()
    {
        return rules;
    }

    /** @return 'scripts' property */
    public final WidgetProperty<List<ScriptInfo>> propScripts()
    {
        return scripts;
    }

    /** Obtain configurator.
     *
     *  <p>While typically using the default {@link WidgetConfigurator},
     *  widget may provide a different configurator for reading older
     *  persisted date.
     *  @param persisted_version Version of the persisted data.
     *  @return Widget configurator for that version
     *  @throws Exception if persisted version cannot be handled
     */
    public WidgetConfigurator getConfigurator(final Version persisted_version)
            throws Exception
    {
        // if (persisted_version.getMajor() < 1)
        //    throw new Exception("Can only handle version 1.0.0 and higher");
        return new WidgetConfigurator(persisted_version);
    }

    /** Get all properties of the widget.
     *
     *  <p>Properties are ordered by category and sequence of definition.
     *  @return Unmodifiable set
     */
    public final Set<WidgetProperty<?>> getProperties()
    {
        return properties;
    }

    /** Helper for obtaining the complete property name 'paths'
     *
     *  <p>For a scalar property, this method simply returns that property name.
     *
     *  <p>For arrays or structures, it returns names for each array resp. structure element.
     *
     *  @param property The widget property.
     *  @return List of property names
     */
    public static final List<String> expandPropertyNames(final WidgetProperty<?> property)
    {
        final List<String> names = new ArrayList<>();
        doAddPropertyNames(names, property.getName(), property);
        return names;
    }

    private static final void doAddPropertyNames(final List<String> names, String path, final WidgetProperty<?> property)
    {
        if (property instanceof ArrayWidgetProperty)
        {
            final ArrayWidgetProperty<?> array = (ArrayWidgetProperty<?>) property;
            for (int i=0; i<array.size(); ++i)
                doAddPropertyNames(names, path + "[" + i + "]", array.getElement(i));
        }
        else if (property instanceof StructuredWidgetProperty)
        {
            final StructuredWidgetProperty struct = (StructuredWidgetProperty) property;
            for (int i=0; i<struct.size(); ++i)
            {
                final WidgetProperty<?> item = struct.getElement(i);
                doAddPropertyNames(names, path + "." + item.getName(), item);
            }
        }
        else
            names.add(path);
    }

    /** Check if widget has a given property.
     *
     *  <p>This is called by code that needs to
     *  test if a widget has a certain property.
     *
     *  <p>Only checks for direct properties of
     *  the widget, neither mapping legacy property names
     *  nor allowing for complex property paths.
     *
     *  @param name Property name
     *  @param <PT> Type of the property's value.
     *  @return Optional {@link WidgetProperty}
     *  @see #getProperty(String)
     */
    public final <PT> Optional<WidgetProperty<PT>> checkProperty(final String name)
    {
        @SuppressWarnings("unchecked")
        final WidgetProperty<PT> property = (WidgetProperty<PT>) property_map.get(name);
        return Optional.ofNullable(property);
    }

    /** Check if widget has a given property.
     *  @param property_description Property descriptor
     *  @param <PT> Type of the property's value.
     *  @return Optional {@link WidgetProperty}
     *  @see #checkProperty(WidgetPropertyDescriptor)
     */
    public final <PT> Optional<WidgetProperty<PT>> checkProperty(final WidgetPropertyDescriptor<PT> property_description)
    {
        return checkProperty(property_description.getName());
    }

    /** Get widget property.
     *
     *  <p>Property access based on property description allows
     *  type-safe access.
     *
     *  @param property_description Property description
     *  @param <PT> Type of the property's value.
     *  @return {@link WidgetProperty}
     *  @throws IllegalArgumentException if property is unknown
     *  @see #checkProperty(WidgetPropertyDescriptor)
     */
    @SuppressWarnings("unchecked")
    public final <PT> WidgetProperty<PT> getProperty(final WidgetPropertyDescriptor<PT> property_description)
    {
        final WidgetProperty<?> property = getProperty(property_description.getName());
        return (WidgetProperty<PT>)property;
    }

    /** Get widget property.
     *
     *  <p>Meant for rules, scripts and similar code
     *  which does not know the exact widget type
     *  and thus fetches properties by name.
     *
     *  <p>Supports access to complex properties by path name,
     *  for example "y_axes[1].minimum" to get the minimum
     *  property of the second Y axis of a plot.
     *
     *  <p>To allow use of legacy scripts and rules,
     *  the widget implementation may override to
     *  handle deprecated property names.
     *
     *  <p>Caller presumes that the widget actually
     *  has the requested property, otherwise throwing Exception.
     *
     *  @param name Property name
     *  @return {@link WidgetProperty}
     *  @throws IllegalArgumentException if property is unknown
     *  @see #checkProperty(String)
     *  @throws IllegalArgumentException if path includes invalid elements,
     *          IndexOutOfBoundsException for access to array beyond size
     */
    public WidgetProperty<?> getProperty(final String name) throws IllegalArgumentException, IndexOutOfBoundsException
    {   // Is name a path "struct_prop.array_prop[2].element" ?
        if (name.indexOf('.') >=0  ||  name.indexOf('[') >= 0)
            return getPropertyByPath(name, false);
        // Plain property name
        final WidgetProperty<?> property = property_map.get(name);
        if (property == null)
            throw new IllegalArgumentException(toString() + " has no '" + name + "' property");
        return property;
    }

    /** Get property via path
     *  @param path_name "struct_prop.array_prop[2].element"
     *  @param create_elements Create missing array elements?
     *  @return Property for "element"
     *  @throws IllegalArgumentException if path includes invalid elements,
     *          IndexOutOfBoundsException for access to array beyond size
     */
    @SuppressWarnings("rawtypes")
    public WidgetProperty<?> getPropertyByPath(final String path_name, final boolean create_elements) throws IllegalArgumentException, IndexOutOfBoundsException
    {
        final String[] path = path_name.split("\\.");
        WidgetProperty<?> property = null;
        for (String item : path)
        {   // Does item refer to array element?
            final String name;
            final int index;
            final int braces = item.indexOf('[');
            if (braces >= 0)
            {
                if (! item.endsWith("]"))
                    throw new IllegalArgumentException("Missing ']' for end of array element");
                name = item.substring(0, braces);
                index = Integer.parseInt(item.substring(braces+1, item.length() - 1));
            }
            else
            {
                name = item;
                index = -1;
            }
            // Get property for the 'name'.
            // For first item, from widget. Later descent into structure.
            if (property == null)
            {
                property = property_map.get(name);
                if (property == null)
                    throw new IllegalArgumentException("Cannot locate '" + name + "' for '" + path_name + "'");
            }
            else if (property instanceof StructuredWidgetProperty)
                property = ((StructuredWidgetProperty)property).getElement(name);
            else
                throw new IllegalArgumentException("Cannot locate '" + name + "' for '" + path_name + "'");
            // Fetch individual array element?
            if (index >= 0)
                if (property instanceof ArrayWidgetProperty)
                {
                    final ArrayWidgetProperty array = (ArrayWidgetProperty)property;
                    // Add array elements?
                    if (create_elements)
                    {
                        while (array.size() <= index)
                            array.addElement();
                    }
                    else
                        if (array.size() < index)
                            throw new IndexOutOfBoundsException("'" + name + "' of '" + path_name +
                                                                "' has only " + array.size() + " elements");
                    property = array.getElement(index);
                }
                else
                    throw new IllegalArgumentException("'" + name + "' of '" + path_name + "' it not an array");
        }
        return property;
    }

    /** Get widget property value.
     *
     *  <p>Property access based on property description allows
     *  type-safe access.
     *
     *  @param property_description Property description
     *  @param <PT> Type of the property's value.
     *  @return Value of the property
     *  @throws IllegalArgumentException if property is unknown
     */
    public final <PT> PT getPropertyValue(final WidgetPropertyDescriptor<PT> property_description)
    {
        return getProperty(property_description).getValue();
    }

    /** Get widget property value.
     *
     *  <p>Property access based on property name returns generic
     *  WidgetProperty without known type.
     *  Data is cast to the receiver type, but that cast may fail
     *  if actual data type differs.
     *
     *  @param name Property name, may also be path like "struct_prop.array_prop[2].element"
     *  @param <TYPE> Data is cast to the receiver's type
     *  @return Value of the property
     *  @throws IllegalArgumentException if property is unknown
     *  @throws IndexOutOfBoundsException for array access beyond last element
     *
     */
    @SuppressWarnings("unchecked")
    public final <TYPE> TYPE getPropertyValue(final String name)
    {
        return (TYPE) getProperty(name).getValue();
    }

    /** Set widget property value.
     *
     *  <p>Property access based on property description allows
     *  type-safe access.
     *
     *  @param property_description Property description
     *  @param <PT> Type of the property's value.
     *  @param value New value of the property
     *  @throws IllegalArgumentException if property is unknown
     */
    public final <PT> void setPropertyValue(final WidgetPropertyDescriptor<PT> property_description,
            final PT value)
    {
        getProperty(property_description).setValue(value);
    }

    /** Set widget property value.
     *
     *  <p>Property access based on property name returns generic
     *  WidgetProperty without known type.
     *
     *  @param name Property name
     *  @param value New value of the property
     *  @throws IllegalArgumentException if property is unknown
     *  @throws Exception if value is unsuitable for this property
     */
    public final void setPropertyValue(final String name,
            final Object value) throws Exception
    {
        getProperty(name).setValueFromObject(value);
    }

    /** Determine effective macros.
     *
     *  <p>Default implementation requests macros
     *  from parent.
     *
     *  <p>Macros will be <code>null</code> while
     *  the widget is loaded until it is included in a model.
     *
     *  @return {@link Macros}
     */
    public Macros getEffectiveMacros()
    {
        final Optional<Widget> the_parent = getParent();
        if (! the_parent.isPresent())
            return null;
        return the_parent.get().getEffectiveMacros();
    }

    /** @return Macro provider for effective macros, falling back to properties */
    public MacroValueProvider getMacrosOrProperties()
    {
        return new MacroOrPropertyProvider(this);
    }

    /** Set user data
     *
     *  <p>User code can attach arbitrary data to a widget.
     *  This data is _not_ persisted with the model,
     *  and there is no change notification.
     *
     *  <p>User code should avoid using reserved keys
     *  which start with an underscore "_...".
     *
     *  @param key Key
     *  @param data Data
     */
    public final void setUserData(final String key, final Object data)
    {
        user_data.put(key, data);
    }

    /** @param key Key
     *  @param <TYPE> Data is cast to the receiver's type
     *  @return User data associated with key, or <code>null</code>
     *  @see #setUserData(String, Object)
     */
    @SuppressWarnings("unchecked")
    public final <TYPE> TYPE getUserData(final String key)
    {
        if (key == null)
        {   // Debug gimmick:
            // null is not supported as valid key,
            // but triggers dump of all user properties
            logger.info(this + " user data: " + user_data.entrySet());
            return null;
        }
        final Object data = user_data.get(key);
        return (TYPE)data;
    }

    /** Remove a user data entry
     *  @param key Key for which to remove user data
     *  @param <TYPE> User data is cast to the receiver's type
     *  @return User data associated with key that has been removed, or <code>null</code>
     */
    @SuppressWarnings("unchecked")
    public final <TYPE> TYPE clearUserData(final String key)
    {
        return (TYPE)user_data.remove(key);
    }

    @Override
    public String toString()
    {
        // Show name's specification, not value, because otherwise
        // a plain debug printout can trigger macro resolution for the name
        return "Widget '" + ((MacroizedWidgetProperty<?>)name).getSpecification() + "' (" + getType() + ")";
    }
}

/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newBooleanPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newColorPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newIntegerPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propMacros;

import java.util.List;

import org.csstudio.display.builder.model.macros.Macros;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.widgets.EmbeddedDisplayWidget;
import org.csstudio.display.builder.model.widgets.NavigationTabsWidget;

/** Display Model.
 *
 *  <p>Describes overall size of display, global settings,
 *  and holds widgets.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DisplayModel extends Widget
{
    /** Version
     *
     *  <ul>
     *  <li>1.0.0 -
     *      Legacy BOY format,
     *      and initial display builder files where
     *      widgets started to use their new XML,
     *      but no support for classes.
     *  </li>
     *  <li>2.0.0 -
     *      Supports widget classes and honors the
     *      'use_class' attribute of properties.
     *  </li>
     *  </ul>
     */
    public static final Version VERSION = new Version(2, 0, 0);

    /** File extension used for display files */
    public static final String FILE_EXTENSION = "bob";

    /** File extension used for legacy display files */
    public static final String LEGACY_FILE_EXTENSION = "opi";

    /** Widget type used by the display model */
    public static final String WIDGET_TYPE = "display";

    /** Reserved DisplayModel user data key for name of input file */
    public static final String USER_DATA_INPUT_FILE = "_input_file";

    /** Reserved DisplayModel user data key for version of input file
     *
     *  <p>Holds Version.
     */
    public static final String USER_DATA_INPUT_VERSION = "_input_version";

    /** Reserved DisplayModel user data key for storing toolkit used as representation.
     *
     *  <p>Holds ToolkitRepresentation.
     */
    public static final String USER_DATA_TOOLKIT = "_toolkit";

    /** Widget user data key for storing the embedding widget.
     *
     *  <p>For a {@link DisplayModel} that is held by an {@link EmbeddedDisplayWidget} or {@link NavigationTabsWidget},
     *  this user data element of the model points to the embedding widget.
     */
    public static final String USER_DATA_EMBEDDING_WIDGET = "_embedding_widget";

    /** Macros set in preferences
     *
     *  <p>Fetched once on display creation to
     *  use latest preference settings on newly opened display,
     *  while not fetching preferences for each macro evaluation
     *  within a running display to improve performance
     */
    private final Macros preference_macros = Preferences.getMacros();

    /** 'grid_visible' property */
    public static final WidgetPropertyDescriptor<Boolean> propGridVisible = newBooleanPropertyDescriptor(WidgetPropertyCategory.MISC, "grid_visible", Messages.WidgetProperties_GridVisible);

    /** 'grid_color' property */
    public static final WidgetPropertyDescriptor<WidgetColor> propGridColor = newColorPropertyDescriptor(WidgetPropertyCategory.MISC, "grid_color", Messages.WidgetProperties_GridColor);

    /** 'grid_step_x' property */
    public static final WidgetPropertyDescriptor<Integer> propGridStepX =
        newIntegerPropertyDescriptor(WidgetPropertyCategory.MISC, "grid_step_x", Messages.WidgetProperties_GridStepX,
                                     4, Integer.MAX_VALUE);

    /** 'grid_step_y' property */
    public static final WidgetPropertyDescriptor<Integer> propGridStepY =
        newIntegerPropertyDescriptor(WidgetPropertyCategory.MISC, "grid_step_y", Messages.WidgetProperties_GridStepY,
                                     4, Integer.MAX_VALUE);

    private volatile WidgetProperty<Macros> macros;
    private volatile WidgetProperty<WidgetColor> background;
    private volatile WidgetProperty<Boolean> gridVisible;
    private volatile WidgetProperty<WidgetColor> gridColor;
    private volatile WidgetProperty<Integer> gridStepX;
    private volatile WidgetProperty<Integer> gridStepY;
    private volatile ChildrenProperty children;

    /** Create display model */
    public DisplayModel()
    {
        super(WIDGET_TYPE, 800, 600);
    }

    @Override
    public Version getVersion()
    {
        return VERSION;
    }

    /** Get display name
     *
     *  <p>Provides the configured 'name',
     *  falling back to the input file
     *  if name is empty.
     *
     *  @return Display name
     */
    public String getDisplayName()
    {
        String name = super.getName();
        if (name.isEmpty())
        {
            name = getUserData(USER_DATA_INPUT_FILE);
            if (name == null)
                name = "<No name>";
        }
        return name;
    }

    /** Is this display model for a class file? */
    public final boolean isClassModel()
    {
        final String filename = getUserData(USER_DATA_INPUT_FILE);
        return filename != null &&
               filename.endsWith(WidgetClassSupport.FILE_EXTENSION);
    }

    /** @return <code>true</code> if this display is a top-level display,
     *          <code>false</code> if it's held by an embedded widget
     */
    public final boolean isTopDisplayModel()
    {
        final Widget embedder = getUserData(DisplayModel.USER_DATA_EMBEDDING_WIDGET);
        return embedder == null;
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(macros = propMacros.createProperty(this, new Macros()));
        properties.add(background = propBackgroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.BACKGROUND)));
        properties.add(gridVisible = propGridVisible.createProperty(this, true));
        properties.add(gridColor = propGridColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.GRID)));
        properties.add(gridStepX = propGridStepX.createProperty(this, 10));
        properties.add(gridStepY = propGridStepY.createProperty(this, 10));
        properties.add(children = new ChildrenProperty(this));
    }

    /** @return 'macros' property */
    public WidgetProperty<Macros> propMacros()
    {
        return macros;
    }

    /** @return Runtime 'children' */
    public ChildrenProperty runtimeChildren()
    {
        return children;
    }

    /** Get read-only list of children
     *
     *  <p>Convenience method.
     *  Use <code>runtimeChildren()</code>
     *  for full access.
     *
     *  @return Child widgets
     */
    public List<Widget> getChildren()
    {
        return children.getValue();
    }

    @Override
    protected void setParent(final Widget parent)
    {
        throw new IllegalStateException("Display cannot have parent widget " + parent);
    }

    /** Display model provides macros for all its widgets.
     *  @return {@link Macros}
     */
    @Override
    public Macros getEffectiveMacros()
    {
        // 1) Lowest priority are either
        // 1.a) .. global macros from preferences
        // 1.b) .. macros from embedding widget,
        //      which may in turn be embedded elsewhere,
        //      ultimately fetching the macros from preferences.
        final Widget embedder = getUserData(DisplayModel.USER_DATA_EMBEDDING_WIDGET);
        Macros result = (embedder == null)
            ? preference_macros
            : embedder.getEffectiveMacros();

        // 2) This display may provide added macros or replacement values
        result = Macros.merge(result, propMacros().getValue());
        return result;
    }

    /** @return 'background_color' property */
    public WidgetProperty<WidgetColor> propBackgroundColor()
    {
        return background;
    }

    /** @return 'grid_color' property */
    public WidgetProperty<WidgetColor> propGridColor()
    {
        return gridColor;
    }

    /** @return 'grid_step_x' property */
    public WidgetProperty<Integer> propGridStepX()
    {
        return gridStepX;
    }

    /** @return 'grid_step_y' property */
    public WidgetProperty<Integer> propGridStepY()
    {
        return gridStepY;
    }

    /** @return 'grid_visible' property */
    public WidgetProperty<Boolean> propGridVisible()
    {
        return gridVisible;
    }

    @Override
    public String toString()
    {
        // Show name's specification, not value, because otherwise
        // a plain debug printout can trigger macro resolution for the name
        return "DisplayModel '" + ((MacroizedWidgetProperty<?>)propName()).getSpecification() + "'";
    }
}

/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.properties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.csstudio.display.builder.editor.DisplayEditor;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.representation.javafx.AutocompleteMenu;

import javafx.scene.control.ScrollPane;

/** Property UI
 *  @author Kay Kasemir
 *  @author Claudio Rosati
 */
@SuppressWarnings("nls")
public class PropertyPanel extends ScrollPane
{
    private final DisplayEditor editor;
    private final PropertyPanelSection section;

    /** @param selection Selection handler
     *  @param undo 'Undo' manager
     */
    public PropertyPanel(final DisplayEditor editor)
    {
        this.editor = editor;
        section = new PropertyPanelSection();

        setFitToWidth(true);
        setContent(section);
        setMinHeight(0);

        // Track currently selected widgets
        editor.getWidgetSelectionHandler().addListener(this::setSelectedWidgets);
    }

    /** Populate UI with properties of widgets
     *  @param widgets Widgets to configure
     */
    private void setSelectedWidgets(final List<Widget> widgets)
    {
        final DisplayModel model = editor.getModel();
    	section.clear();
    	section.setClassMode(model != null  && model.isClassModel());

        if (widgets.size() < 1)
        {   // Use the DisplayModel
            if (model != null)
                section.fill(editor.getUndoableActionManager(), model.getProperties(), Collections.emptyList());
        }
        else
        {   // Determine common properties
            final List<Widget> other = new ArrayList<>(widgets);
            final Widget primary = other.remove(0);
            final Set<WidgetProperty<?>> properties = commonProperties(primary, other);
            section.fill(editor.getUndoableActionManager(), properties, other);
        }
    }

    public AutocompleteMenu getAutocompleteMenu()
    {
        return section.getAutocompleteMenu();
    }

    /** Determine common properties
     *  @param primary Primary widget, the one selected first
     *  @param other Zero or more 'other' widgets
     *  @return Common properties
     */
    private Set<WidgetProperty<?>> commonProperties(final Widget primary, final List<Widget> other)
    {
        if (other.contains(primary))
            throw new IllegalArgumentException("Primary widget " + primary + " included in 'other'");

        // Start with properties of primary widget
        final Set<WidgetProperty<?>> common = new LinkedHashSet<>(primary.getProperties());
        // Keep properties shared by other widgets, i.e. remove those _not_ in other
        for (Widget o : other)
            common.removeIf(prop  ->  ! o.checkProperty(prop.getName()).isPresent());
        return common;
    }
}

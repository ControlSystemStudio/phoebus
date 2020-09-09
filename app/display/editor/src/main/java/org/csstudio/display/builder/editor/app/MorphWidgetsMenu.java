/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.app;

import static org.csstudio.display.builder.editor.Plugin.logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

import org.csstudio.display.builder.editor.DisplayEditor;
import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.editor.Preferences;
import org.csstudio.display.builder.editor.WidgetSelectionHandler;
import org.csstudio.display.builder.editor.undo.AddWidgetAction;
import org.csstudio.display.builder.editor.undo.RemoveWidgetsAction;
import org.csstudio.display.builder.editor.util.WidgetIcons;
import org.csstudio.display.builder.model.ArrayWidgetProperty;
import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.MacroizedWidgetProperty;
import org.csstudio.display.builder.model.RuntimeWidgetProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetFactory;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.widgets.ArrayWidget;
import org.csstudio.display.builder.model.widgets.MorphWidgetSupport;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.undo.CompoundUndoableAction;

import javafx.collections.ObservableList;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.ImageView;

/** Menu to morph widgets
 *  (replace widgets with a particular type of widget) in editor.
 *
 *  Intended as a sub-menu in editor's main context menu.
 *
 *  @author Amanda Carpenter - Original SWT version 'MorphWidgetMenuSupport'
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MorphWidgetsMenu extends Menu
{
    private final DisplayEditor editor;

    private class MorphAction extends MenuItem
    {
        public MorphAction(final WidgetDescriptor descriptor)
        {
            super(descriptor.getName(), new ImageView(WidgetIcons.getIcon(descriptor.getType())));
            setOnAction(event -> morph(descriptor));
        }
    };

    public MorphWidgetsMenu(final DisplayEditor editor)
    {
        super(Messages.ReplaceWith,
              ImageCache.getImageView(DisplayEditor.class, "/icons/replace.png"));

        this.editor = editor;

        // Create menu that lists all widget types
        final ObservableList<MenuItem> items = getItems();
        WidgetCategory category = null;
        for (WidgetDescriptor descriptor : WidgetFactory.getInstance().getWidgetDescriptions())
        {
            if (Preferences.hidden_widget_types.contains(descriptor.getType()))
                continue;
            // Header for start of each category
            if (descriptor.getCategory() != category)
            {
                category = descriptor.getCategory();
                // Use disabled, empty action to show category name
                final MenuItem info = new MenuItem(category.getDescription());
                info.setDisable(true);
                items.add(new SeparatorMenuItem());
                items.add(info);
            }
            items.add(new MorphAction(descriptor));
        }
    }

    public void morph(final WidgetDescriptor descriptor)
    {
        final WidgetSelectionHandler selection = editor.getWidgetSelectionHandler();

        // Copy selected widgets.
        // List may be modified during iteration for widgets inside an ArrayWidget
        final List<Widget> widgets = new ArrayList<>(selection.getSelection());
        final List<Widget> replacements = new ArrayList<>();

        final CompoundUndoableAction steps = new CompoundUndoableAction("Morph to " + descriptor.getName());

        // Iterate in a way that allows modification of 'widgets' inside the loop
        for (int i=0;  i<widgets.size();  /**/)
        {
            final Widget widget = widgets.get(i);
            // Already of correct type?
            if (widget.getType().equals(descriptor.getType()))
            {
                ++i;
                continue;
            }
            final ChildrenProperty target = ChildrenProperty.getParentsChildren(widget);
            // All array elements of an ArrayWidget must have the same type
            // to avoid errors with matching element properties.
            if (target.getWidget() instanceof ArrayWidget)
            {
                // Replace _all_ children of ArrayWidget, not just the selected ones
                final List<Widget> children = new ArrayList<>(target.getValue());
                steps.execute(new RemoveWidgetsAction(selection, children));
                for (Widget child : children)
                {
                    final Widget replacement = createNewWidget(descriptor, child);
                    steps.execute(new AddWidgetAction(selection, target, replacement));
                    replacements.add(replacement);
                }

                // Remove _all_ potentially selected array elements
                // from the widgets to be replaced
                widgets.removeAll(children);
                // No need for ++i since `widgets` has been updated
            }
            else
            {
                final Widget replacement = createNewWidget(descriptor, widget);
                final int index = ChildrenProperty.getParentsChildren(widget).getValue().indexOf(widget);
                steps.execute(new RemoveWidgetsAction(selection, List.of(widget)));
                steps.execute(new AddWidgetAction(selection, target, replacement, index));
                replacements.add(replacement);
                ++i;
            }
        }

        // Add to undo (steps have already been executed)
        editor.getUndoableActionManager().add(steps);

        // Change selection from removed widgets to replacements
        selection.setSelection(replacements);
    }

    @SuppressWarnings("unchecked")
    public static <W extends Widget> W createNewWidget(final WidgetDescriptor descriptor, final Widget widget)
    {
        final Widget new_widget = descriptor.createWidget();
        final Set<WidgetProperty<?>> props = widget.getProperties();
        final MorphWidgetSupport mws = new MorphWidgetSupport(widget, new_widget);
        for (WidgetProperty<?> prop : props)
        {
            final Optional<WidgetProperty<Object>> check = mws.morphProperty(prop);
            if (! check.isPresent())
                continue;
            final WidgetProperty<?> new_prop = check.get();
            if (new_prop.isReadonly())
                continue;
            if (new_prop instanceof RuntimeWidgetProperty)
                continue;
            try
            {
                // Get the first element of an ArrayWidgetProperty if the new widget's property is not an array
                if (prop instanceof ArrayWidgetProperty<?> && ! (new_prop instanceof ArrayWidgetProperty<?>))
                    prop = ((ArrayWidgetProperty<?>)prop).getElement(0);

                if (new_prop instanceof MacroizedWidgetProperty<?>)
                {
                    if (prop instanceof MacroizedWidgetProperty<?>)
                        ((MacroizedWidgetProperty<?>)new_prop).setSpecification(((MacroizedWidgetProperty<?>) prop).getSpecification());
                    else
                        ((MacroizedWidgetProperty<?>)new_prop).setSpecification((String)prop.getValue());
                }
                else
                    new_prop.setValueFromObject(prop.getValue());
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot morph " + prop, ex);
            }
        }
        return (W)new_widget;
    }
}

/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm;

import static org.csstudio.display.converter.edm.Converter.logger;

import java.awt.geom.Rectangle2D;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.properties.ActionInfos;
import org.csstudio.display.builder.model.widgets.ActionButtonWidget;
import org.csstudio.display.converter.edm.widgets.ConverterBase;
import org.csstudio.opibuilder.converter.model.EdmDisplay;
import org.csstudio.opibuilder.converter.model.EdmEntity;
import org.csstudio.opibuilder.converter.model.EdmWidget;

/** Convert {@link EdmDisplay} to {@link DisplayModel}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class EdmConverter
{
    private final DisplayModel model = new DisplayModel();

    private final AtomicInteger next_group = new AtomicInteger();

    private int offset_x = 0, offset_y = 0;

    private final Set<String> linked_displays = new HashSet<>();

    public EdmConverter(final String name, final EdmDisplay edm)
    {
        model.propName().setValue(name);
        model.propX().setValue(edm.getX());
        model.propY().setValue(edm.getY());
        model.propWidth().setValue(edm.getW());
        model.propHeight().setValue(edm.getH());

        // TODO Global edm.getFont()?
        ConverterBase.convertColor(edm.getBgColor(), model.propBackgroundColor());

        if (edm.getTitle() != null)
            model.propName().setValue(edm.getTitle());

        model.propGridVisible().setValue(edm.isShowGrid());
        if (edm.getGridSize() > 0)
        {
            model.propGridStepX().setValue(edm.getGridSize());
            model.propGridStepY().setValue(edm.getGridSize());
        }

        for (EdmEntity edm_widget : edm.getWidgets())
            convertWidget(model, edm_widget);
        correctChildWidgets(model);
    }

    /** @return {@link DisplayModel} */
    public DisplayModel getDisplayModel()
    {
        return model;
    }

    /** @return Number of next group */
    public int nextGroup()
    {
        return next_group.getAndIncrement();
    }

    /** @return Displays that were linked from this display */
    public Collection<String> getLinkedDisplays()
    {
        return linked_displays.stream().sorted().collect(Collectors.toList());
    }

    /** @param x X offset and
     *  @param y Y offset of widgets within currently handled container
     */
    public void addPositionOffset(final int x, final int y)
    {
        offset_x += x;
        offset_y += y;
    }

    /** @return X offset of widgets within currently handled container */
    public int getOffsetX()
    {
        return offset_x;
    }

    /** @return Y offset of widgets within currently handled container */
    public int getOffsetY()
    {
        return offset_y;
    }

    /** Convert one widget
     *  @param parent Parent
     *  @param edm EDM widget to convert
     */
    public void convertWidget(final Widget parent, final EdmEntity edm)
    {
        // Given an EDM Widget type like "activeXTextClass",
        // locate the matching "Convert_activeXTextClass"
        final Class<?> clazz;
        try
        {
            final String wc_name = ConverterBase.class.getPackageName() +
                                   ".Convert_" +
                                   edm.getType().replace(':', '_');
            clazz = Class.forName(wc_name);
        }
        catch (ClassNotFoundException ex)
        {
            logger.log(Level.WARNING, "No converter for EDM " + edm.getType());
            return;
        }

        try
        {
            for (Constructor<?> c : clazz.getConstructors())
            {   // Look for suitable constructor
                final Class<?>[] parms = c.getParameterTypes();
                if (parms.length == 3  &&
                    parms[0] == EdmConverter.class &&
                    parms[1] == Widget.class       &&
                    EdmWidget.class.isAssignableFrom(parms[2]))
                {
                    // Simply constructing the converter will perform the conversion
                    c.newInstance(this, parent, edm);
                    return;
                }
            }
            throw new Exception(clazz.getSimpleName() + " lacks required constructor");
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot convert " + edm.getType(), ex);
        }
    }


    /** Correct widget issues
     *
     *  <p>Called after all widgets have been added to a parent
     *  @param parent
     */
    public void correctChildWidgets(final Widget parent)
    {
        final ChildrenProperty children = ChildrenProperty.getChildren(parent);
        mergeButtons(children);
        raiseTransparentButtons(children);
    }

    /** Merge action buttons that are overlapping into one button.
     *  EDM supports 'invisible' buttons which react to left or
     *  right mouse button to occupy the same space.
     *  In display builder, only the left button triggers an action.
     *  The right button opens the context menu.
     *  So merge actions from all overlapping buttons into one.
     *
     *  @param children Child widgets to correct
     */
    private void mergeButtons(final ChildrenProperty children)
    {
        // Start with the topmost button, i.e. end of list
        final List<Widget> copy = new ArrayList<>(children.getValue());
        for (int i=copy.size()-1;  i>=0;  --i)
        {
            final Widget widget = copy.get(i);
            if (! (widget instanceof ActionButtonWidget))
                continue;
            // Look for buttons below that occupy roughly the same space
            for (int o=i-1;  o>=0;  --o)
            {
                final Widget other = copy.get(o);
                if (! (other instanceof ActionButtonWidget  &&  doWidgetsOverlap(widget, other)))
                    continue;

                logger.log(Level.WARNING, "Merging actions from overlapping " + widget + " and " + other + " into one:");
                logger.log(Level.WARNING, "1) " + widget.propActions().getValue());
                logger.log(Level.WARNING, "2) " + other.propActions().getValue());
                final List<ActionInfo> actions = new ArrayList<>(widget.propActions().getValue().getActions());
                actions.addAll(other.propActions().getValue().getActions());
                widget.propActions().setValue(new ActionInfos(actions));

                children.removeChild(other);
                copy.remove(o);
                --i;
            }
        }
    }

    /** @param widget
     *  @param other
     *  @return Do the widgets overlap by a considerable amount?
     */
    private boolean doWidgetsOverlap(final Widget widget, final Widget other)
    {
        final Rectangle2D w = new Rectangle2D.Double(widget.propX().getValue(),
                                                     widget.propY().getValue(),
                                                     widget.propWidth().getValue(),
                                                     widget.propHeight().getValue());
        final Rectangle2D o = new Rectangle2D.Double(other.propX().getValue(),
                                                     other.propY().getValue(),
                                                     other.propWidth().getValue(),
                                                     other.propHeight().getValue());
        final Rectangle2D common = w.createIntersection(o);
        if (common.getWidth() <= 0  ||  common.getHeight() <= 0)
            return false;

        final int overlap = (int) (common.getWidth() * common.getHeight());
        final int avg_area = (int) (w.getWidth() * w.getHeight() +
                                    o.getWidth() * o.getHeight()) / 2;
        // Overlap by at least a 5th??
        return overlap > avg_area / 5;
    }

    /** Move transparent buttons to front.
     *  In EDM, transparent buttons may be placed behind text etc.
     *  In display builder, normal widget order would
     *  then have text block mouse events from button.
     *  @param children Child widgets to correct
     */
    private void raiseTransparentButtons(final ChildrenProperty children)
    {
        final List<Widget> copy = new ArrayList<>(children.getValue());
        for (Widget widget : copy)
        {
            if (widget instanceof ActionButtonWidget)
            {
                final ActionButtonWidget b = (ActionButtonWidget) widget;
                if (b.propTransparent().getValue())
                {
                    children.removeChild(widget);
                    children.addChild(widget);
                }
            }
        }
    }

    /** @param linked_display Register a display that was linked from the currently converted file */
    public void addLinkedDisplay(final String linked_display)
    {
        linked_displays.add(linked_display);
    }
}

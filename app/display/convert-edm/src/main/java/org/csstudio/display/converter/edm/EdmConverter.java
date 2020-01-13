/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm;

import static org.csstudio.display.converter.edm.Converter.logger;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.properties.ActionInfos;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.widgets.ActionButtonWidget;
import org.csstudio.display.converter.edm.widgets.ConverterBase;
import org.csstudio.opibuilder.converter.model.EdmDisplay;
import org.csstudio.opibuilder.converter.model.EdmEntity;
import org.csstudio.opibuilder.converter.model.EdmWidget;
import org.csstudio.opibuilder.converter.parser.EdmDisplayParser;

/** Convert one {@link EdmDisplay} to {@link DisplayModel}
 *
 *  <p>Tracks the referenced displays for caller to potentially
 *  convert them as well, both 'included' displays from embedded
 *  screens or symbols, and 'linked' displays from buttons that
 *  open related displays.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class EdmConverter
{
    private final AssetLocator asset_locator;

    private final DisplayModel model = new DisplayModel();

    private final AtomicInteger next_group = new AtomicInteger();

    private int offset_x = 0, offset_y = 0;

    private final Set<String> included_displays = new HashSet<>();
    private final Set<String> linked_displays = new HashSet<>();

    /** Parse EDL input and convert into {@link DisplayModel}
     *
     *  <p>Optional {@link AssetLocator} allows for example image
     *  widgets to locate their images, automatically downloading
     *  it from a http:/.. search path.
     *
     *  @param input EDL input file
     *  @param asset_locator Optional {@link AssetLocator} or <code>null</code>
     *  @throws Exception on error
     */
    public EdmConverter(final File input, final AssetLocator asset_locator) throws Exception
    {
        this.asset_locator = asset_locator;

        logger.log(Level.FINE, "Parsing EDM " + input);
        // Parse EDM file
        final EdmDisplayParser parser = new EdmDisplayParser(input.getPath(), new FileInputStream(input));
        final EdmDisplay edm = new EdmDisplay(parser.getRoot());

        // Create Display Model
        final String name = input.getName()
                                  .replace(".edl", "")
                                  .replace('_', ' ');
        model.propName().setValue(name);
        model.propX().setValue(edm.getX());
        model.propY().setValue(edm.getY());
        model.propWidth().setValue(edm.getW());
        model.propHeight().setValue(edm.getH());

        ConverterBase.convertColor(edm.getBgColor(), model.propBackgroundColor());

        if (edm.getTitle() != null)
            model.propName().setValue(edm.getTitle());

        model.propGridVisible().setValue(edm.isShowGrid());
        if (edm.getGridSize() > 0)
        {
            model.propGridStepX().setValue(edm.getGridSize());
            model.propGridStepY().setValue(edm.getGridSize());
        }

        // Convert all widgets
        for (EdmEntity edm_widget : edm.getWidgets())
            convertWidget(model, edm_widget);
        correctChildWidgets(model);
    }

    /** @return {@link DisplayModel} */
    public DisplayModel getDisplayModel()
    {
        return model;
    }

    /** @param output File to write with Display Builder model
     *  @throws Exception on error
     */
    public void write(final File output) throws Exception
    {
        logger.log(Level.FINE, "Writing " + output);
        final ModelWriter writer = new ModelWriter(new FileOutputStream(output));
        writer.writeModel(model);
        writer.close();
    }

    /** @return Number of next group */
    public int nextGroup()
    {
        return next_group.getAndIncrement();
    }

    /** Request download of asset.
     *
     *  <p>NOP when this converter doesn't have an {@link AssetLocator}.
     *
     *  @param asset Asset of a widget, for example PNG file for Image widget
     *  @throws Exception
     */
    public void downloadAsset(final String asset) throws Exception
    {
        if (asset_locator != null)
            asset_locator.locate(asset);
    }

    /** @return Displays that were included by this display (embedded, symbol) */
    public Collection<String> getIncludedDisplays()
    {
        return included_displays.stream().sorted().collect(Collectors.toList());
    }

    /** @return Displays that were linked from this display (related display) */
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
        if (edm instanceof EdmWidget)
        {
            final EdmWidget w = (EdmWidget) edm;
            if (w.getX() + w.getW() <= 0  ||
                w.getY() + w.getH() <= 0)
            {
                logger.log(Level.WARNING, "Skipping off-screen widget " + edm.getType() +
                           " @ " + w.getX() + "," + w.getY() + " sized " + w.getW() + " x " + w.getH());
                return;
            }
        }

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
        fixCoveredButtons(children);
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
            final ActionButtonWidget bw = (ActionButtonWidget) widget;

            // Look for buttons below that occupy roughly the same space
            for (int o=i-1;  o>=0;  --o)
            {
                final Widget other = copy.get(o);
                if (! (other instanceof ActionButtonWidget  &&  doWidgetsOverlap(widget, other)))
                    continue;
                final ActionButtonWidget ob = (ActionButtonWidget) other;

                logger.log(Level.INFO, "Merging actions from overlapping " + widget + " and " + other + " into one:");
                logger.log(Level.INFO, "1) " + widget.propActions().getValue());
                logger.log(Level.INFO, "2) " + other.propActions().getValue());
                final List<ActionInfo> actions = new ArrayList<>(widget.propActions().getValue().getActions());
                actions.addAll(other.propActions().getValue().getActions());
                widget.propActions().setValue(new ActionInfos(actions));

                // When merging buttons, as soon as one button is visible.
                // the remaining (merged) button must be visible
                if (! ob.propTransparent().getValue())
                {
                    bw.propTransparent().setValue(false);
                    bw.propText().setValue(ob.propText().getValue());
                    bw.propForegroundColor().setValue(ob.propForegroundColor().getValue());
                    bw.propBackgroundColor().setValue(ob.propBackgroundColor().getValue());
                    bw.propFont().setValue(ob.propFont().getValue());
                }

                // Remove 'other' both from original children (by value) and our working copy (by index)
                children.removeChild(other);
                copy.remove(o);
                // Correct index since we removed a widget
                --i;
            }
        }
    }

    /** Do two widgets overlap, no matter which one covers the other?
     *  @param widget
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

    /** Make covered buttons 'transparent'.
     *  EDM supports buttons in the back of rectangles etc.
     *  They're not visible, but they react to mouse clicks.
     *  In display builder, covered buttons are, well, covered.
     *  Make them 'transparent', and a follow-up operation then raises
     *  all transparent buttons to the top so they get mouse clicks.
     *
     *  @param children Child widgets to correct
     */
    private void fixCoveredButtons(final ChildrenProperty children)
    {
        final List<Widget> list = children.getValue();
        // Start of list = lowest widget
        for (int i=0;  i<list.size();  ++i)
        {
            final Widget widget = list.get(i);
            if (! (widget instanceof ActionButtonWidget))
                continue;
            final ActionButtonWidget bottom = (ActionButtonWidget) widget;

            // Look for widgets on top that cover it
            for (int o=i+1;  o<list.size();  ++o)
            {
                final Widget other = list.get(o);

                // Ignore transparent...
                Optional<WidgetProperty<Boolean>> check = other.checkProperty(CommonWidgetProperties.propTransparent);
                if (check.isPresent()  && check.get().getValue())
                    continue;

                // .. or invisible widgets
                check = other.checkProperty(CommonWidgetProperties.propVisible);
                if (check.isPresent()  && !check.get().getValue())
                    continue;

                // Does other widget cover this one?
                if (! isWidgetCovered(bottom, other))
                    continue;

                logger.log(Level.INFO, bottom + " is covered by " + other + ". Making it 'transparent' so it'll be raised.");
                bottom.propBackgroundColor().setValue(NamedWidgetColors.TRANSPARENT);
                bottom.propText().setValue("");
                bottom.propTransparent().setValue(true);
                break;
            }
        }
    }

    /** Does one widget cover the other?
     *  @param bottom
     *  @param top
     *  @return Does top widget cover the one at the bottom by a considerable amount?
     */
    private boolean isWidgetCovered(final Widget bottom, final Widget top)
    {
        final Rectangle2D w = new Rectangle2D.Double(bottom.propX().getValue(),
                                                     bottom.propY().getValue(),
                                                     bottom.propWidth().getValue(),
                                                     bottom.propHeight().getValue());
        final Rectangle2D o = new Rectangle2D.Double(top.propX().getValue(),
                                                     top.propY().getValue(),
                                                     top.propWidth().getValue(),
                                                     top.propHeight().getValue());
        final Rectangle2D common = w.createIntersection(o);
        if (common.getWidth() <= 0  ||  common.getHeight() <= 0)
            return false;

        final int overlap = (int) (common.getWidth() * common.getHeight());
        final int bottom_area = (int) (w.getWidth() * w.getHeight());
        // Overlap at least half of the bottom?
        return overlap > bottom_area / 2;
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
                    logger.log(Level.INFO, "Raising transparent " + b);
                    children.removeChild(widget);
                    children.addChild(widget);
                }
            }
        }
    }

    /** @param included_display Register a display that's included by the currently converted file */
    public void addIncludedDisplay(final String included_display)
    {
        included_displays.add(included_display);
    }

    /** @param linked_display Register a display that was linked from the currently converted file */
    public void addLinkedDisplay(final String linked_display)
    {
        linked_displays.add(linked_display);
    }
}

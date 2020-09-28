/*******************************************************************************
 * Copyright (c) 2010-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.model;

import static org.csstudio.trends.databrowser3.Activator.logger;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.csstudio.javafx.rtplot.util.RGBFactory;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.imports.ImportArchiveReaderFactory;
import org.csstudio.trends.databrowser3.preferences.Preferences;
import org.phoebus.framework.macros.MacroHandler;
import org.phoebus.framework.macros.MacroValueProvider;
import org.phoebus.framework.macros.Macros;
import org.phoebus.util.time.TimeInterval;
import org.phoebus.util.time.TimeParser;
import org.phoebus.util.time.TimeRelativeInterval;
import org.phoebus.util.time.TimestampFormats;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/** Data Browser model
 *
 *  <p>Maintains a list of {@link ModelItem}s
 *
 *  @author Kay Kasemir
 *  @author Takashi Nakamoto changed the model to accept multiple items with
 *                           the same name so that Data Browser can show the
 *                           trend of the same PV in different axes or with
 *                           different waveform indexes.
 *  @author Megan Grodowitz ported from databrowser 2 (SWT) to databrowser 3 (JFX under RCP)
 */
@SuppressWarnings("nls")
public class Model
{
    /** Matcher for "Value N" axis name */
    private static final Pattern axis_name_pattern = Pattern.compile(Messages.Plot_ValueAxisName + " \\d+", Pattern.MULTILINE);

    /** Should UI ask to save changes to the model? */
    final private AtomicBoolean save_changes = new AtomicBoolean(true);

    /** Default colors for newly added item */
    final private RGBFactory default_colors = new RGBFactory();

    /** Macros */
    private volatile MacroValueProvider macros = new Macros();

    /** Listeners to model changes */
    final private List<ModelListener> listeners = new CopyOnWriteArrayList<>();

    /** Title */
    private volatile Optional<String> title = Optional.empty();

    /** Axes configurations */
    final private List<AxisConfig> axes = new CopyOnWriteArrayList<>();

    /** All the items in this model */
    final private List<ModelItem> items = new CopyOnWriteArrayList<>();

    /** 'run' flag
     *  @see #start()
     *  @see #stop()
     */
    private volatile boolean is_running = false;

    /** Period in seconds for scrolling or refreshing */
    private volatile double update_period = Preferences.update_period;

    /** Scroll steps */
    private volatile Duration scroll_step = Preferences.scroll_step;

    /** Time span of data in seconds */
    private volatile TimeRelativeInterval time_range = TimeRelativeInterval.of(Preferences.time_span, Duration.ZERO);

    /** Show time axis grid line? */
    private volatile boolean show_grid = false;

    /** Foreground color */
    private volatile Color foreground = Color.BLACK;

    /** Background color */
    private volatile Color background = Color.WHITE;

    /** Title font */
    private volatile Font title_font = Font.font("Liberation Sans", FontWeight.BOLD, 20);

    /** Label font */
    private volatile Font label_font = Font.font("Liberation Sans", FontWeight.BOLD, 14);

    /** Scale font */
    private volatile Font scale_font = Font.font("Liberation Sans", 12);

    /** Legend font */
    private volatile Font legend_font = Font.font("Liberation Sans", 14);

    /** Annotations */
    private volatile List<AnnotationInfo> annotations = List.of();

    /** How should plot re-scale when archived data arrives? */
    private volatile ArchiveRescale archive_rescale = Preferences.archive_rescale;

    /** Show toolbar*/
    private boolean show_toolbar = true;

    /** Show legend*/
    private boolean show_legend = false;

    /** Remove all items and axes */
    public void clear()
    {
        for (ModelItem item : items)
        {
            item.dispose();
            removeItem(item);
        }

        while (getAxisCount() > 0)
            removeAxis(getAxis(getAxisCount()-1));

        annotations = List.of();
        macros = new Macros();
    }

    /** Load state from another model
     *
     *  <p>Takes shortcuts by directly copying data
     *  from other model.
     *  Only permitted when this model has no items.
     *  The other Model should not be used after
     *  it has been loaded into this model.
     *
     *  @param other Other model to load
     *  @throws Exception
     */
    public void load(final Model other) throws Exception
    {
        if (! items.isEmpty())
            throw new IllegalStateException("Can only load into an empty model with no items");
        if (! axes.isEmpty())
            throw new IllegalStateException("Can only load into an empty model with no axes");

        setSaveChanges(other.save_changes.get());
        macros = other.macros;

        setTitle(other.getTitle().orElse(null));
        setUpdatePeriod(other.update_period);
        setScrollStep(other.scroll_step);
        setTimerange(other.time_range);
        setGridVisible(other.show_grid);
        setPlotForeground(other.foreground);
        setPlotBackground(other.background);
        setTitleFont(other.title_font);
        setLabelFont(other.label_font);
        setScaleFont(other.scale_font);
        setLegendFont(other.legend_font);
        setArchiveRescale(other.archive_rescale);
        setToolbarVisible(other.show_toolbar);
        setLegendVisible(other.show_legend);

        for (AxisConfig axis : other.axes)
            addAxis(axis);
        for (ModelItem item : other.items)
            addItem(item);
        setAnnotations(other.annotations);
    }


    /** @return Should UI ask to save changes to the model? */
    public boolean shouldSaveChanges()
    {
        return save_changes.get();
    }

    /** @param save_changes Should UI ask to save changes to the model? */
    public void setSaveChanges(final boolean save_changes)
    {
        if (this.save_changes.getAndSet(save_changes) != save_changes)
            for (ModelListener listener : listeners)
                listener.changedSaveChangesBehavior(save_changes);
    }

    /** @param other Macros to use in this model */
    public void setMacros(final MacroValueProvider other)
    {
       this.macros = other;
    }

    /** Resolve macros
     *  @param text Text that might contain "$(macro)"
     *  @return Text with all macros replaced by their value
     */
    public String resolveMacros(final String text)
    {
        try
        {
            return MacroHandler.replace(macros, text);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Problem in macro " + text, ex);
            return text;
        }
    }

    /** @param listener New listener to notify */
    public void addListener(final ModelListener listener)
    {
        listeners.add(Objects.requireNonNull(listener));
    }

    /** @param listener Listener to remove */
    public void removeListener(final ModelListener listener)
    {
        listeners.remove(Objects.requireNonNull(listener));
    }

    /** @param title Title, may be <code>null</code> or empty */
    public void setTitle(final String title)
    {
        if (title == null  ||   title.isEmpty())
            this.title = Optional.empty();
        else
            this.title = Optional.of(title);
        for (ModelListener listener : listeners)
            listener.changedTitle();
    }

    /** @return Title */
    public Optional<String> getTitle()
    {
        return title;
    }

    /** @return Read-only, thread safe {@link AxisConfig}s */
    public List<AxisConfig> getAxes()
    {
        return Collections.unmodifiableList(axes);
    }

    /** Get number of axes
     *
     *  <p>Thread-safe access to multiple axes should use <code>getAxes()</code>
     *
     *  @return Number of axes
     */
    public int getAxisCount()
    {
        return axes.size();
    }

    /** Get specific axis. If the axis for the specified index does not exist, method returns null.
     *
     *  <p>Thread-safe access to multiple axes should use <code>getAxes()</code>
     *
     *  @param index Axis index
     *  @return {@link AxisConfig} or null if the config for the given index does not exist
     */
    public AxisConfig getAxis(final int index)
    {
        try
        {
            return axes.get(index);
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            return null;
        }
    }

    /** Locate index of value axis
     *  @param axis Value axis configuration
     *  @return Index of axis (0, ...) or -1 if not in Model
     */
    public int getAxisIndex(final AxisConfig axis)
    {
        return axes.indexOf(Objects.requireNonNull(axis));
    }

    /** @param axis Axis to test
     *  @return First ModelItem that uses the axis, visible or not.
     *          <code>null</code> if axis is empty
     */
    public ModelItem getFirstItemOnAxis(final AxisConfig axis)
    {
        Objects.requireNonNull(axis);
        for (ModelItem item : items)
            if (item.getAxis() == axis)
                return item;
        return null;
    }

    /** @param axis Axis to test
     *  @return <code>true</code> if there is any visible item on the axis
     */
    public boolean hasAxisActiveItems(final AxisConfig axis)
    {
        Objects.requireNonNull(axis);
        for (ModelItem item : items)
            if (item.getAxis() == axis && item.isVisible())
                return true;
        return false;
    }

    /** @return First unused axis (no items on axis) */
    public Optional<AxisConfig> getEmptyAxis()
    {
        for (AxisConfig axis : axes)
            if (getFirstItemOnAxis(axis) == null)
                return Optional.of(axis);
        return Optional.empty();
    }


    /** Add value axis with default settings
     *  Sets name of new axis to Value N
     *  N is found by searching for all the existing axes with the name Value X; N is set to the highest value of X found + 1
     *  (this scheme should avoid the creation of axes with duplicate names of the format Value N)
     *  @return Newly added axis configuration
     */
    public AxisConfig addAxis()
    {
        final AxisConfig axis = new AxisConfig("");
        axis.setColor(Color.BLACK);
        addAxis(axis);
        return axis;
    }

    /** @param axis New axis to add */
    public void addAxis(final AxisConfig axis)
    {
        if (axis.getName().isEmpty())
        {
            int max_default_axis_num = 0;
            for (AxisConfig a : axes)
            {
                String existing_axis_name = a.getName();
                Matcher matcher = axis_name_pattern.matcher(existing_axis_name);
                while (matcher.find())
                {
                    final int default_axis_num = Integer.parseInt(matcher.group(0).replace(Messages.Plot_ValueAxisName+" ",""));
                    if (default_axis_num > max_default_axis_num)
                        max_default_axis_num = default_axis_num;
                }
            }
            axis.setName(MessageFormat.format(Messages.Plot_ValueAxisNameFMT, max_default_axis_num + 1));
        }
        axes.add(Objects.requireNonNull(axis));
        axis.setModel(this);
        fireAxisChangedEvent(Optional.empty());
    }

    /** Add axis at given index.
     *  Adding at '1' means the new axis will be at index '1',
     *  and what used to be at '1' will be at '2' and so on.
     *  @param index Index where axis will be placed.
     *  @param axis New axis to add
     */
    public void addAxis(final int index, final AxisConfig axis)
    {
        axes.add(index, Objects.requireNonNull(axis));
        axis.setModel(this);
        fireAxisChangedEvent(Optional.empty());
    }

    /** @param axis Axis to remove
     *  @throws Error when axis not in model, or axis in use by model item
     */
    public void removeAxis(final AxisConfig axis)
    {
        if (! axes.contains(Objects.requireNonNull(axis)))
            throw new Error("Unknown AxisConfig");
        for (ModelItem item : items)
            if (item.getAxis() == axis)
                throw new Error("Cannot removed AxisConfig while in use");
        axis.setModel(null);
        axes.remove(axis);
        fireAxisChangedEvent(Optional.empty());
    }

    /** @return How should plot rescale after archived data arrived? */
    public ArchiveRescale getArchiveRescale()
    {
        return archive_rescale;
    }

    /** @param archive_rescale How should plot rescale after archived data arrived? */
    public void setArchiveRescale(final ArchiveRescale archive_rescale)
    {
        if (this.archive_rescale == archive_rescale)
            return;
        this.archive_rescale = archive_rescale;
        for (ModelListener listener : listeners)
            listener.changedArchiveRescale();
    }

    /** @return {@link ModelItem}s as thread-safe read-only {@link List} */
    public List<ModelItem> getItems()
    {
        return Collections.unmodifiableList(items);
    }

    /** Locate item by name.
     *
     *  <p>Note that the model may contain multiple items for the same
     *  name. The first occurrence will be returned.
     *  If no item is found with the given
     *  name, <code>null</code> will be returned.
     *  Now that this model may have different items with the same name,
     *  this method is not recommended to locate an item. This method
     *  just returns an item which just happens to have the given name.
     *  @param name
     *  @return ModelItem by that name or <code>null</code>
     */
    public ModelItem getItem(final String name)
    {
        for (ModelItem item : items)
            if (item.getName().equals(name) || item.getResolvedName().equals(name))
                return item;
        return null;
    }

    /** Find a formula that uses a model item as an input.
     *  @param item Item that's potentially used in a formula
     *  @return First {@link FormulaItem} that uses this item
     */
    public Optional<FormulaItem> getFormulaWithInput(final ModelItem item)
    {
        Objects.requireNonNull(item);
        for (ModelItem i : items)
        {
            if (! (i instanceof FormulaItem))
                continue;
            final FormulaItem formula = (FormulaItem) i;
            if (formula.usesInput(item))
                return Optional.of(formula);
        }
        return Optional.empty();
    }

    /** Called by items to set their initial color
     *  @return 'Next' suggested item color
     */
    private Color getNextItemColor()
    {
        boolean already_used;
        Color color;
        int attempts = 10;
        do
        {
            -- attempts;
            color = default_colors.next();
            already_used = false;
            for (ModelItem item : items)
                if (color.equals(item.getPaintColor()))
                {
                    already_used = true;
                    break;
                }
        }
        while (attempts > 0  &&  already_used);
        return color;
    }

    /** Add item to the model.
     *  <p>
     *  If the item has no color, this will define its color based
     *  on the model's next available color.
     *  <p>
     *  If the model is already 'running', the item will be 'start'ed.
     *
     *  @param item {@link ModelItem} to add
     *  @throws RuntimeException if item is already in model
     */
    public void addItem(final ModelItem item) throws Exception
    {
        Objects.requireNonNull(item);
        // A new item with the same PV name are allowed to be added in the
        // model. This way Data Browser can show the trend of the same PV
        // in different axes or with different waveform indexes. For example,
        // one may want to show the first element of epics://aaa:bbb in axis 1
        // while showing the third element of the same PV in axis 2 to compare
        // their trends in one chart.
        // But, if exactly the same instance of the given ModelItem already exists in this
        // model, it will not be added.
        if (items.indexOf(item) != -1)
            throw new RuntimeException("Item " + item.getName() + " already in Model");

        // Assign default color
        if (item.getPaintColor() == null)
            item.setColor(getNextItemColor());

        // Force item to be on an axis
        if (item.getAxis() == null)
        {
            // Assert there is at least one axis
            if (getAxisCount() <= 0)
                addAxis();
            // Place item on first axis
            item.setAxis(axes.get(0));
        }
        // Check item axis
        if (! axes.contains(item.getAxis()))
            throw new Exception("Item " + item.getName() + " added with invalid axis " + item.getAxis());

        // Add to model
        items.add(item);
        item.setModel(this);
        // Notify listeners of new item
        // This allows controller to add item to plot
        for (ModelListener listener : listeners)
            listener.itemAdded(item);
        // Now start PV, which might update the plot's labels to
        // reflect units and thus must happen after listeners have been called
        if (is_running  &&  item instanceof PVItem)
            ((PVItem)item).start();
    }

    /** Remove item from the model.
     *  <p>
     *  If the model and thus item are 'running',
     *  the item will be 'stopped'.
     *  @param item
     *  @throws RuntimeException if item not in model
     */
    public void removeItem(final ModelItem item)
    {
        Objects.requireNonNull(item);
        if (is_running  &&  item instanceof PVItem)
        {
            final PVItem pv = (PVItem)item;
            pv.stop();
            // Delete its samples:
            // For one, so save memory.
            // Also, in case item is later added back in, its old samples
            // will have gaps because the item was stopped
            pv.getSamples().clear();
        }
        if (! items.remove(item))
            throw new RuntimeException("Unknown item " + item.getName());
        // Detach item from model
        item.setModel(null);

        // Notify listeners of removed item
        for (ModelListener listener : listeners)
            listener.itemRemoved(item);
    }

    /** Move item in model.
     *  <p>
     *  @param item
     *  @param up Up? Otherwise down
     *  @throws RuntimeException if item null or not in model
     */
    public void moveItem(final ModelItem item, final boolean up)
    {
        final int pos = items.indexOf(Objects.requireNonNull(item));
        if (pos < 0)
            throw new RuntimeException("Unknown item " + item.getName());
        if (up)
        {
            if (pos == 0)
                return;
            items.remove(pos);
            items.add(pos-1, item);
        }
        else
        {    // Move down
            if (pos >= items.size() -1)
                return;
            items.remove(pos);
            items.add(pos+1, item);
        }

        // Notify listeners of moved item
        for (ModelListener listener : listeners)
        {
            listener.itemRemoved(item);
            listener.itemAdded(item);
        }
    }

    /** @return Period in seconds for scrolling or refreshing */
    public double getUpdatePeriod()
    {
        return update_period;
    }

    /** @param period_secs New update period in seconds */
    public void setUpdatePeriod(final double period_secs)
    {
        // Don't allow updates faster than 10Hz (0.1 seconds)
        if (period_secs < 0.1)
            update_period = 0.1;
        else
            update_period = period_secs;
        // Notify listeners
        for (ModelListener listener : listeners)
            listener.changedTiming();
    }

    /** @return Scroll step size */
    public Duration getScrollStep()
    {
        return scroll_step;
    }

    /** @param step New scroll step
     *  @throws Exception if step size cannot be used
     */
    public void setScrollStep(final Duration step) throws Exception
    {
        if (step.compareTo(Duration.ofSeconds(1)) < 0)
            throw new Exception("Scroll steps are too small: " + step);
        if (step.compareTo(scroll_step) == 0)
            return;
        scroll_step = step;
        for (ModelListener listener : listeners)
            listener.changedTiming();
    }

    /** @return time range */
    public TimeRelativeInterval getTimerange()
    {
        return time_range;
    }

    /** @return Start and end specification as text */
    public String[] getTimerangeText()
    {
        return getTimerangeText(time_range);
    }

    /** @param range {@link TimeRelativeInterval}
     *  @return Start and end specification as text
     */
    public static String[] getTimerangeText(final TimeRelativeInterval range)
    {
        if (range.isStartAbsolute())
        {
            final TimeInterval abs = range.toAbsoluteInterval();
            return new String[]
            {
                TimestampFormats.MILLI_FORMAT.format(abs.getStart()),
                TimestampFormats.MILLI_FORMAT.format(abs.getEnd())
            };
        }
        else
        {
            return new String[]
            {
                TimeParser.format(range.getRelativeStart().get()),
                TimeParser.NOW
            };
        }
    }

    /** @param range Time range */
    public void setTimerange(final TimeRelativeInterval range)
    {
        // Assert that start < end. Not empty, not inverted
        final TimeInterval abs = range.toAbsoluteInterval();
        if (! abs.getStart().isBefore(abs.getEnd()))
            return;

        time_range = range;
        // Notify listeners
        for (ModelListener listener : listeners)
            listener.changedTimerange();
    }

    /** @return foreground color */
    public Color getPlotForeground()
    {
        return foreground;
    }

    /** @param rgb New foreground color */
    public void setPlotForeground(final Color rgb)
    {
        if (foreground.equals(Objects.requireNonNull(rgb)))
            return;
        foreground = rgb;
        for (ModelListener listener : listeners)
            listener.changedColorsOrFonts();
    }

    /** @return Background color */
    public Color getPlotBackground()
    {
        return background;
    }

    /** @param rgb New background color */
    public void setPlotBackground(final Color rgb)
    {
        if (background.equals(Objects.requireNonNull(rgb)))
            return;
        background = rgb;
        for (ModelListener listener : listeners)
            listener.changedColorsOrFonts();
    }

    /** @return <code>true</code> if toolbar is visible*/
    public boolean isToolbarVisible()
    {
        return show_toolbar;
    }

    /** @param toolbar Should toolbar be visible? */
    public void setToolbarVisible(final boolean toolbar)
    {
        if (show_toolbar == toolbar)
            return;
        show_toolbar = toolbar;
        for (ModelListener listener : listeners)
            listener.changedLayout();
    }

    /** @return <code>true</code> if legend is visible*/
    public boolean isLegendVisible()
    {
        return show_legend;
    }

    /** @param legend Should legend be visible? */
    public void setLegendVisible(final boolean legend)
    {
        if (show_legend == legend)
            return;
        show_legend = legend;
        for (ModelListener listener : listeners)
            listener.changedLayout();
    }

    /** @return <code>true</code> if grid lines are drawn */
    public boolean isGridVisible()
    {
        return show_grid;
    }

    /** @param grid Should grid be visible? */
    public void setGridVisible(final boolean grid)
    {
        if (show_grid == grid)
            return;
        show_grid = grid;
        for (ModelListener listener : listeners)
            listener.changedTimeAxisConfig();
    }

    /** @return Title font */
    public Font getTitleFont()
    {
        return title_font;
    }

    /** @param font Title font */
    public void setTitleFont(final Font font)
    {
        title_font = font;
        for (ModelListener listener : listeners)
            listener.changedColorsOrFonts();
    }

    /** @return Label font */
    public Font getLabelFont()
    {
        return label_font;
    }

    /** @param font Label font */
    public void setLabelFont(final Font font)
    {
        label_font = font;
        for (ModelListener listener : listeners)
            listener.changedColorsOrFonts();
    }

    /** @return Scale font */
    public Font getScaleFont()
    {
        return scale_font;
    }

    /** @param font Scale font */
    public void setScaleFont(final Font font)
    {
        scale_font = font;
        for (ModelListener listener : listeners)
            listener.changedColorsOrFonts();
    }

    /** @return Legend font */
    public Font getLegendFont()
    {
        return legend_font;
    }

    /** @param font Scale font */
    public void setLegendFont(final Font font)
    {
        legend_font = font;
        for (ModelListener listener : listeners)
            listener.changedColorsOrFonts();
    }


    /** @param annotations Annotations to keep in model */
    public void setAnnotations(final List<AnnotationInfo> annotations)
    {
        this.annotations = Objects.requireNonNull(annotations);
        for (ModelListener listener : listeners)
            listener.changedAnnotations();
    }

    /** @return Annotation infos of model */
    public List<AnnotationInfo> getAnnotations()
    {
        return annotations;
    }

    /** Start all items: Connect PVs, initiate scanning, ...
     *  @throws Exception on error
     */
    public void start() throws Exception
    {
        if (is_running)
            throw new RuntimeException("Model already started");
        for (ModelItem item : items)
        {
            if (!(item instanceof PVItem))
                continue;
            final PVItem pv_item = (PVItem) item;
            pv_item.start();
        }
        is_running = true;
    }

    /** Stop all items: Disconnect PVs, ... */
    public void stop()
    {
        if (!is_running)
        {
            // Warn. Throwing exception would prevent closing when there was an error during start
            logger.log(Level.WARNING, "Data Browser Model wasn't started");
            return;
        }
        is_running = false;
        for (ModelItem item : items)
        {
            if (!(item instanceof PVItem))
                continue;
            final PVItem pv_item = (PVItem) item;
            pv_item.stop();
            ImportArchiveReaderFactory.removeCachedArchives(pv_item.getArchiveDataSources());
        }
    }

    /** Test if any ModelItems received new samples,
     *  if formulas need to be re-computed,
     *  since the last time this method was called.
     *  @return <code>true</code> if there were new samples
     */
    public boolean updateItemsAndCheckForNewSamples()
    {
        boolean anything_new = false;
        // Update any formulas
        for (ModelItem item : items)
        {
            if (item instanceof FormulaItem  &&
                ((FormulaItem)item).reevaluate())
                anything_new = true;
        }
        // Check and reset PV Items
        for (ModelItem item : items)
        {
            if (item instanceof PVItem  &&
                    item.getSamples().testAndClearNewSamplesFlag())
                anything_new = true;
        }
        return anything_new;
    }

    /** Notify listeners of changed axis configuration
     *  @param axis Axis that changed, empty to add/remove
     */
    public void fireAxisChangedEvent(final Optional<AxisConfig> axis)
    {
        for (ModelListener listener : listeners)
            listener.changedAxis(axis);
    }

    /** Notify listeners of changed item visibility
     *  @param item Item that changed
     */
    void fireItemVisibilityChanged(final ModelItem item)
    {
        for (ModelListener listener : listeners)
            listener.changedItemVisibility(item);
    }

    /** Notify listeners of changed item configuration
     *  @param item Item that changed
     */
    void fireItemLookChanged(final ModelItem item)
    {
        for (ModelListener listener : listeners)
            listener.changedItemLook(item);
    }

    void fireItemUnitsChanged(final ModelItem item)
    {
        for (ModelListener listener : listeners)
            listener.changedItemUnits(item);
    }

    /** Notify listeners of changed item data source configuration
     *  @param item Item with changed data sources
     *  @param archive_invalid Was a data source added, do we need to get new archived data?
     *                         Or does the change not affect archived data?
     */
    void fireItemDataConfigChanged(final PVItem item, final boolean archive_invalid)
    {
        for (ModelListener listener : listeners)
            listener.changedItemDataConfig(item, archive_invalid);
    }

    void fireItemRefreshRequested(final PVItem item)
    {
        for (ModelListener listener : listeners)
            listener.itemRefreshRequested(item);
    }

    public void fireSelectedSamplesChanged()
    {
        for (ModelListener listener : listeners)
            listener.selectedSamplesChanged();
    }

    /** Dispose all items, remove all listeners */
    public void dispose()
    {
        // Remove all listeners so they're no longer
        // called..
        listeners.clear();
        // .. as all items are removed:
        clear();
    }

    /**
     * @param uniqueId Non-null unique id.
     * @return A {@link ModelItem} matching the specified unique id, or null.
     */
    public ModelItem getItemByUniqueId(String uniqueId){
        if(uniqueId == null){
            return null;
        }
        return items.stream().filter(item -> uniqueId.equals(item.getUniqueId())).findAny().orElse(null);
    }
}

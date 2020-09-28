/*******************************************************************************
 * Copyright (c) 2014-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

import java.awt.Rectangle;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.javafx.rtplot.data.ValueRange;
import org.csstudio.javafx.rtplot.internal.AxisPart;
import org.csstudio.javafx.rtplot.internal.ImageConfigDialog;
import org.csstudio.javafx.rtplot.internal.ImagePlot;
import org.csstudio.javafx.rtplot.internal.ImageToolbarHandler;
import org.csstudio.javafx.rtplot.internal.MouseMode;
import org.csstudio.javafx.rtplot.internal.YAxisImpl;
import org.csstudio.javafx.rtplot.internal.undo.ChangeImageZoom;
import org.csstudio.javafx.rtplot.internal.util.GraphicsUtils;
import org.epics.util.array.ListNumber;
import org.epics.vtype.VImageType;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.ToolbarHelper;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/** Real-time plot
 *
 *  @param <XTYPE> Data type used for the {@link PlotDataItem}
 *  @author Kay Kasemir
 */
public class RTImagePlot extends BorderPane
{
    final protected ImagePlot plot;
    final protected ImageToolbarHandler toolbar;
    private boolean handle_keys = false;
	private TextField axisLimitsField; //Field for adjusting the limits of the axes
	private final Pane center = new Pane();

    /** Constructor
     *  @param active Active mode where plot reacts to mouse/keyboard?
     *  @param type Type of X axis
     */
    public RTImagePlot(final boolean active)
    {
        plot = new ImagePlot(active);
        toolbar = new ImageToolbarHandler(this, active);

        // Canvas, i.e. plot, is not directly size-manageable by a layout.
        // --> Let BorderPane resize 'center', then plot binds to is size.
        center.getChildren().add(plot);
        final ChangeListener<? super Number> resize_listener = (p, o, n) -> plot.setSize(center.getWidth(), center.getHeight());
        center.widthProperty().addListener(resize_listener);
        center.heightProperty().addListener(resize_listener);
        setCenter(center);
        showToolbar(active);

        if (active)
        {
            addEventFilter(KeyEvent.KEY_PRESSED, this::keyPressed);
            // Need focus to receive key events. Get focus when mouse moves.
            // (tried mouse _entered_, but can then loose focus while mouse still in widget)
            addEventFilter(MouseEvent.MOUSE_MOVED, event ->
            {
                handle_keys = true;
                if (!axisLimitsField.isVisible()) requestFocus();
            } );
            // Don't want to handle key events when mouse is outside the widget.
            // Cannot 'loose focus', so using flag to ignore them
            addEventFilter(MouseEvent.MOUSE_EXITED, event -> handle_keys = false);
            setOnMouseClicked(this::mouseClicked);
    		axisLimitsField = constructAxisLimitsField();
    		center.getChildren().add(axisLimitsField);
        }
    }

    /** Update the dormant time between updates
     *  @param dormant_time How long throttle remains dormant after a trigger
     *  @param unit Units for the dormant period
     */
    public void setUpdateThrottle(final long dormant_time, final TimeUnit unit)
    {
        plot.setUpdateThrottle(dormant_time, unit);
    }

    private TextField constructAxisLimitsField()
    {
    	final TextField field = new TextField();
    	//prevent mouse-clicks in TextField from triggering MouseClicked event for RTPlot
    	field.addEventFilter(MouseEvent.MOUSE_CLICKED, (event)->event.consume());
    	field.focusedProperty().addListener((prop, oldval, newval)->
    	{
    		if (!newval) hideAxisLimitsField();
    	});
    	field.setVisible(false);
    	field.setManaged(false); //false because we manage layout, not the Parent
    	return field;
    }

	private void showAxisLimitsField(AxisPart<Double> axis, boolean isHigh, Rectangle area)
    {
		axisLimitsField.setOnKeyPressed((KeyEvent event)->
		{
			if (event.getCode().equals(KeyCode.ENTER))
			{
				hideAxisLimitsField();
				if (axisLimitsField.getText().isEmpty()) return;
				try
				{
					Double value = Double.parseDouble(axisLimitsField.getText());
					changeAxisLimit(axis, isHigh, value);
				} catch (NumberFormatException e) {}
			}
			else if (event.getCode().equals(KeyCode.ESCAPE))
			{
				hideAxisLimitsField();
			}
		});

		String tip = isHigh ? axis.getValueRange().getHigh().toString() :
			axis.getValueRange().getLow().toString();
    	axisLimitsField.setText(tip);
    	axisLimitsField.setTooltip(new Tooltip(tip));
		axisLimitsField.setVisible(true);
		axisLimitsField.relocate(area.getX(), area.getY());
		axisLimitsField.resize(area.getWidth(), area.getHeight());
		axisLimitsField.requestFocus();
		axisLimitsField.layout(); //force text to appear in field
	}

	protected void changeAxisLimit(AxisPart<Double> axis, boolean isHigh, Double value)
	{
		AxisRange<Double> old_range = axis.getValueRange();
		AxisRange<Double> new_range = isHigh ? new AxisRange<>(old_range.getLow(), value) :
			new AxisRange<>(value, old_range.getHigh());
		if (axis instanceof YAxisImpl<?>) //Y axis?
		{
			getUndoableActionManager().execute(new ChangeImageZoom(Messages.Set_Axis_Range,
					null, null, null, axis, old_range, new_range));
		}
		else //X axis
		{
			getUndoableActionManager().execute(new ChangeImageZoom(Messages.Set_Axis_Range,
					axis, old_range, new_range, null, null, null));
		}
	}

    private void hideAxisLimitsField()
    {
		axisLimitsField.setVisible(false);
    }

    /** Configuration dialog
     *
     *  <p>Dialog is non-modal and sets to <code>null</code> when closed
     */
    private ImageConfigDialog config_dialog = null;


    /** Show the configuration dialog or bring existing dialog to front */
    public void showConfigurationDialog()
    {
        if (config_dialog == null)
        {
            config_dialog = new ImageConfigDialog(this);
            config_dialog.setOnHiding(evt ->  config_dialog = null);
            DialogHelper.positionDialog(config_dialog, this, 30 - (int) getWidth()/2, 30 - (int) getHeight()/2);
            config_dialog.show();
        }
        else
        {   // Raise existing dialog
            final Stage stage = (Stage) config_dialog.getDialogPane().getContent().getScene().getWindow();
            stage.toFront();
        }
    }

    /** onKeyPressed */
    private void keyPressed(final KeyEvent event)
    {
        if (! handle_keys || axisLimitsField.isVisible())
            return;
        if (event.getCode() == KeyCode.Z)
            plot.getUndoableActionManager().undoLast();
        else if (event.getCode() == KeyCode.Y)
            plot.getUndoableActionManager().redoLast();
        else if (event.getCode() == KeyCode.O)
            showConfigurationDialog();
        else if (event.getCode() == KeyCode.C)
            toolbar.toggleCrosshair();
        else if (event.getCode() == KeyCode.T)
            showToolbar(! isToolbarVisible());
        else if (event.isControlDown())
            toolbar.selectMouseMode(MouseMode.ZOOM_IN);
        else if (event.isAltDown())
            toolbar.selectMouseMode(MouseMode.ZOOM_OUT);
        else if (event.isShiftDown())
            toolbar.selectMouseMode(MouseMode.PAN);
        else
            toolbar.selectMouseMode(MouseMode.NONE);
        event.consume();
    }

    @SuppressWarnings("unchecked")
	private void mouseClicked(MouseEvent event)
    {
    	Object [] info = plot.axisClickInfo(event);
    	if (info != null)
    		showAxisLimitsField((AxisPart<Double>)info[0], (boolean)info[1], (Rectangle)info[2]);
    }

    /** @param plot_listener Plot listener */
    public void setListener(final RTImagePlotListener plot_listener)
    {
        plot.setListener(plot_listener);
    }

    /** Not meant to be public API, for internal use only */
    public ImagePlot internalGetImagePlot()
    {
        return plot;
    }

    /** @return <code>true</code> if toolbar is visible */
    public boolean isToolbarVisible()
    {
        return getTop() != null;
    }

    /** @param show <code>true</code> if toolbar should be displayed */
    public void showToolbar(final boolean show)
    {
        if (isToolbarVisible() == show)
            return;
        plot.removeROITracker();
        if (show)
            setTop(toolbar.getToolBar());
        else
            setTop(null);

        // Force layout to reclaim space used by hidden toolbar,
        // or make room for the visible toolbar
        layoutChildren();
        if (show)
            Platform.runLater(() -> ToolbarHelper.refreshHack(toolbar.getToolBar()));
    }

    /** @param mode New {@link MouseMode}
     *  @throws IllegalArgumentException if mode is internal
     */
    public void setMouseMode(final MouseMode mode)
    {
         plot.setMouseMode(mode);
    }

    /** Zoom 'in' or 'out' from center
     *  @param zoom_in Zoom 'in' or 'out'?
     */
    public void zoomInOut(final boolean zoom_in)
    {
        plot.zoomInOut(zoom_in);
    }

    /** @return {@link UndoableActionManager} for this plot */
    public UndoableActionManager getUndoableActionManager()
    {
        return plot.getUndoableActionManager();
    }

    /** @param interpolation How to interpolate from image to screen pixels */
    public void setInterpolation(final Interpolation interpolation)
    {
        plot.setInterpolation(interpolation);
    }

    /** @return Auto-scale the color mapping? */
    public boolean isAutoscale()
    {
        return plot.isAutoscale();
    }

    /** @param autoscale  Auto-scale the color mapping? */
    public void setAutoscale(final boolean autoscale)
    {
        plot.setAutoscale(autoscale);
    }

    /** @return Use log scale for color mapping? */
    public boolean isLogscale()
    {
        return plot.isLogscale();
    }

    /** @param logscale  Use log scale for color mapping? */
    public void setLogscale(final boolean logscale)
    {
        plot.setLogscale(logscale);
    }

    /** @return X axis */
    public Axis<Double> getXAxis()
    {
        return plot.getXAxis();
    }

    /** @return Y axis */
    public Axis<Double> getYAxis()
    {
        return plot.getYAxis();
    }

    /** Add region of interest
     *  @param name
     *  @param color
     *  @param visible
     *  @param interactive
     *  @return {@link RegionOfInterest}
     */
    public RegionOfInterest addROI(final String name, final javafx.scene.paint.Color color,
                                   final boolean visible, final boolean interactive)
    {
        return plot.addROI(name, color, visible, interactive);
    }

    /** @return Regions of interest */
    public List<RegionOfInterest> getROIs()
    {
        return plot.getROIs();
    }

    /** @param index Index of R.O.I. to remove
     *  @throws IndexOutOfBoundsException
     */
    public void removeROI(final int index)
    {
        plot.removeROI(index);
    }

    /** If there is a ROI tracker, remove it */
    public void removeROITracker()
    {
        plot.removeROITracker();
    }

    // Used to request a complete redraw of the plot with new layout of node,
    // but that creates loops:
    // layout -> compute new image -> set image -> trigger another layout
    // @Override
    // public void requestLayout()
    // {
    //     plot.requestLayout();
    // }

    /** Request a complete redraw of the plot */
    public void requestUpdate()
    {
        plot.requestUpdate();
    }

    /** @param color Background color */
    public void setBackground(final javafx.scene.paint.Color color)
    {
        plot.setBackground(GraphicsUtils.convert(Objects.requireNonNull(color), (int) (255*color.getOpacity())));
    }

    /** @param color_mapping Function that returns color for value 0.0 .. 1.0 */
    public void setColorMapping(final ColorMappingFunction color_mapping)
    {
        plot.setColorMapping(color_mapping);
    }

    /** @return Show color map? */
    public boolean isShowingColorMap()
    {
        return plot.isShowingColorMap();
    }

    /** @param show Show color map? */
    public void showColorMap(final boolean show)
    {
        plot.showColorMap(show);
    }

    /** @param size Color bar size in pixels */
    public void setColorMapSize(final int size)
    {
        plot.setColorMapSize(size);
    }

    /** @param size Color bar size in pixels */
    public void setColorMapFont(final Font font)
    {
        plot.setColorMapFont(font);
    }

    /** @param foreground Color bar text color. */
    public void setColorMapForeground(final Color foreground)
    {
        plot.setColorMapForeground(foreground);
    }

    /** @param show Show crosshair, moved on click?
     *              Or update cursor listener with each mouse move,
     *              not showing a persistent crosshair?
     */
    public void showCrosshair(final boolean show)
    {
        if (plot.isCrosshairVisible() == show)
            return;
        toolbar.showCrosshair(show);
        plot.showCrosshair(show);
    }

    /** @return Is crosshair enabled? */
    public boolean isCrosshairVisible()
    {
        return plot.isCrosshairVisible();
    }

    /** Set location of crosshair
     *  @param x_val Mouse X position
     *  @param y_val .. Y ..
     */
    public void setCrosshairLocation(final double x_val, final double y_val)
    {
        plot.setCrosshairLocation(x_val, y_val, false);
    }

    /** Set axis range for 'full' image
     *  @param min_x
     *  @param max_x
     *  @param min_y
     *  @param max_y
     */
    public void setAxisRange(final double min_x, final double max_x,
                             final double min_y, final double max_y)
    {
        plot.setAxisRange(min_x, max_x, min_y, max_y);
    }

    /** Get color mapping value range
     *  @return {@link ValueRange}
     */
    public ValueRange getValueRange()
    {
        return plot.getValueRange();
    }

    /** Set color mapping value range
     *  @param min
     *  @param max
     */
    public void setValueRange(final double min, final double max)
    {
        plot.setValueRange(min, max);
    }

    /** Set the data to display
     *  @param width Number of elements in one 'row' of data
     *  @param height Number of data rows
     *  @param data Image elements, starting in 'top left' corner,
     *              proceeding along the row, then to next rows
     *  @param unsigned Is the data meant to be treated as 'unsigned'
     */
    public void setValue(final int width, final int height, final ListNumber data, final boolean unsigned, final VImageType type)
    {
        plot.setValue(width, height, data, unsigned, type);
    }

    /** Should be invoked when plot no longer used to release resources */
    public void dispose()
    {
    	plot.dispose();
    }
}

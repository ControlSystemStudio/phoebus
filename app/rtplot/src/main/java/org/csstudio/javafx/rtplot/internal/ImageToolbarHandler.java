/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import static org.csstudio.javafx.rtplot.Activator.logger;

import java.util.logging.Level;

import org.csstudio.javafx.rtplot.Activator;
import org.csstudio.javafx.rtplot.Messages;
import org.csstudio.javafx.rtplot.RTImagePlot;
import org.phoebus.ui.undo.UndoButtons;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** Tool bar for {@link RTImagePlot}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ImageToolbarHandler
{
    public enum ToolIcons
    {
        CONFIGURE,
        ZOOM_IN,
        ZOOM_OUT,
        PAN,
        CROSSHAIR,
        POINTER
    };

    final private RTImagePlot plot;

    final private ToolBar toolbar;
    private ToggleButton zoom_in, zoom_out, pan, crosshair, pointer;

    /** Have any custom items been added? */
    private boolean have_custom_items = false;

    /** Construct tool bar
     *  @param plot {@link RTImagePlot} to control from tool bar
     */
    public ImageToolbarHandler(final RTImagePlot plot, final boolean active)
    {
        this.plot = plot;
        toolbar = new ToolBar();
        makeGUI(active);
    }

    /** @return The actual toolbar for {@link RTImagePlot} to handle its layout */
    public ToolBar getToolBar()
    {
        return toolbar;
    }

    /** Add a custom tool bar item
     *  @param icon Icon {@link Image}
     *  @param tool_tip Tool tip text
     *  @return {@link ToolItem}
     */
    public Button addItem(final Image icon, final String tool_tip)
    {
        if (!have_custom_items)
        {
            toolbar.getItems().add(new Separator());
            have_custom_items = true;
        }
        final Button item = new Button();
        item.setGraphic(new ImageView(icon));
        item.setTooltip(new Tooltip(tool_tip));
        item.setMinSize(ToolbarHandler.BUTTON_WIDTH, ToolbarHandler.BUTTON_HEIGHT);
        toolbar.getItems().add(item);
        return item;
    }

    private void makeGUI(final boolean active)
    {
        addMouseModes(active);
        toolbar.getItems().add(new Separator());
        addUndo(active);

        // Initially, zooming is selected
        selectMouseMode(zoom_in);
    }

    private void addMouseModes(final boolean active)
    {
        final Button configure = newButton(ToolIcons.CONFIGURE, Messages.ImageOptions);
        zoom_in = newToggleButton(ToolIcons.ZOOM_IN, Messages.Zoom_In_TT);
        zoom_out = newToggleButton(ToolIcons.ZOOM_OUT, Messages.Zoom_Out_TT);
        pan = newToggleButton(ToolIcons.PAN, Messages.Pan_TT);
        crosshair = newToggleButton(ToolIcons.CROSSHAIR, Messages.Crosshair_Cursor);
        pointer = newToggleButton(ToolIcons.POINTER, Messages.Plain_Pointer);

        if (active)
        {
            configure.setOnAction(event -> plot.showConfigurationDialog());
            zoom_in.setOnAction(event ->
            {
                selectMouseMode(zoom_in);
                plot.setMouseMode(MouseMode.ZOOM_IN);
            });
            zoom_out.setOnAction(event ->
            {
                selectMouseMode(zoom_out);
                plot.setMouseMode(MouseMode.ZOOM_OUT);
                plot.zoomInOut(false);
            });
            pan.setOnAction(event ->
            {
                selectMouseMode(pan);
                plot.setMouseMode(MouseMode.PAN);
            });
            crosshair.setOnAction(event ->
            {
                plot.showCrosshair(crosshair.isSelected());
            });
            pointer.setOnAction(event ->
            {
                selectMouseMode(pointer);
                plot.setMouseMode(MouseMode.NONE);
            });
        }
    }

    private void addUndo(final boolean active)
    {
        final Button[] undo_redo = UndoButtons.createButtons(plot.getUndoableActionManager());
        toolbar.getItems().addAll(undo_redo);
    }

    private Button newButton(final ToolIcons icon, final String tool_tip)
    {
    	return (Button) newItem(false, icon, tool_tip);
    }

    private ToggleButton newToggleButton(final ToolIcons icon, final String tool_tip)
    {
    	return (ToggleButton) newItem(true, icon, tool_tip);
    }

    private ButtonBase newItem(final boolean toggle, final ToolIcons icon, final String tool_tip)
    {
    	final ButtonBase item = toggle ? new ToggleButton() : new Button();
		try
		{
			item.setGraphic(new ImageView(Activator.getIcon(icon.name().toLowerCase())));
		}
		catch (Exception ex)
		{
			logger.log(Level.WARNING, "Cannot get icon" + icon, ex);
			item.setText(icon.toString());
		}
        item.setTooltip(new Tooltip(tool_tip));
        item.setMinSize(ToolbarHandler.BUTTON_WIDTH, ToolbarHandler.BUTTON_HEIGHT);

        toolbar.getItems().add(item);
        return item;
    }

    /** @param mode {@link MouseMode} ZOOM_IN, ZOOM_OUT, PAN or NONE */
    public void selectMouseMode(final MouseMode mode)
    {
        if (mode == MouseMode.ZOOM_IN)
        {
            selectMouseMode(zoom_in);
            plot.setMouseMode(mode);
        }
        else if (mode == MouseMode.ZOOM_OUT)
        {
            selectMouseMode(zoom_out);
            plot.setMouseMode(mode);
        }
        else if (mode == MouseMode.PAN)
        {
            selectMouseMode(pan);
            plot.setMouseMode(mode);
        }
        else
        {
            selectMouseMode(pointer);
            plot.setMouseMode(MouseMode.NONE);
        }
    }

    /** @param item Tool item to select, all others will be de-selected */
    private void selectMouseMode(final ToggleButton item)
    {
        for (ToggleButton ti : new ToggleButton[] { zoom_in, zoom_out, pan, pointer })
        	ti.setSelected(ti == item);
    }

    /** @param show Show crosshair, moved on click? */
    public void showCrosshair(final boolean show)
    {
        crosshair.setSelected(show);
    }

    /** Turn crosshair on/off */
    public void toggleCrosshair()
    {
        final boolean show = ! plot.isCrosshairVisible();
        showCrosshair(show);
        plot.showCrosshair(show);
    }
}

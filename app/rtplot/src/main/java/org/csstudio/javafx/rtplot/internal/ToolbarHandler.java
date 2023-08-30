/*******************************************************************************
 * Copyright (c) 2014-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import static org.csstudio.javafx.rtplot.Activator.logger;

import java.util.logging.Level;

import org.csstudio.javafx.rtplot.Activator;
import org.csstudio.javafx.rtplot.Annotation;
import org.csstudio.javafx.rtplot.Messages;
import org.csstudio.javafx.rtplot.RTPlot;
import org.csstudio.javafx.rtplot.RTPlotListener;
import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.undo.UndoButtons;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** Tool bar for {@link Plot}
 *  @param <XTYPE> Data type used for the {@link PlotDataItem}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ToolbarHandler<XTYPE extends Comparable<XTYPE>>
{
    // Button's auto-sizing doesn't work when toolbar is initially hidden.
    // Fixed size seems only way around that.
    static final int BUTTON_WIDTH = 32, BUTTON_HEIGHT = 26;

    public enum ToolIcons
    {
        CONFIGURE,
        ADD_ANNOTATION,
        EDIT_ANNOTATION,
        CROSSHAIR,
        STAGGER,
        ZOOM_IN,
        ZOOM_OUT,
        PAN,
        POINTER
    };

    final private RTPlot<XTYPE> plot;

    final private ToolBar toolbar;
    private ToggleButton crosshair, zoom_in, zoom_out, pan, pointer;
    private Button edit_annotation;

    /** Have any custom items been added? */
    private boolean have_custom_items = false;

    /** Construct tool bar
     *  @param plot {@link RTPlot} to control from tool bar
     *  @param active React to mouse clicks?
     */
    public ToolbarHandler(final RTPlot<XTYPE> plot, final boolean active)
    {
        this.plot = plot;
        toolbar = new ToolBar();
        makeGUI(active);
    }

    /** @return The actual toolbar for {@link RTPlot} to handle its layout */
    public ToolBar getToolBar()
    {
        return toolbar;
    }

    /** Add a custom tool bar item
     *  @param icon Icon {@link Image}
     *  @param tool_tip Tool tip text
     *  @return {@link Button}
     */
    public Button addItem(final ImageView icon, final String tool_tip)
    {
        if (!have_custom_items)
        {
            toolbar.getItems().add(new Separator());
            have_custom_items = true;
        }
        final Button item = new Button();
        item.setGraphic(icon);
        item.setTooltip(new Tooltip(tool_tip));

        // Buttons should size based on the icon, but
        // without explicit size, they sometimes start out zero-sized.
        // setMinSize tends to have icon end up in top-left corner.
        // setPrefSize tends to show just the icon, no button.
        // minSize with visible button is better than no button.
        // Icon gets positioned once the button is pressed.
        item.setMinSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        toolbar.getItems().add(item);
        return item;
    }

    private void makeGUI(final boolean active)
    {
        addOptions(active);
        addZoom(active);
        addMouseModes(active);
        toolbar.getItems().add(new Separator());
        addUndo(active);

        // Initially, pointer is selected
        selectMouseMode(pointer);
    }

    private void addOptions(final boolean active)
    {
        final Button configure = newButton(ToolIcons.CONFIGURE, Messages.PlotOptions);

        final Button add_annotation = newButton(ToolIcons.ADD_ANNOTATION, Messages.AddAnnotation);

        edit_annotation = newButton(ToolIcons.EDIT_ANNOTATION, Messages.EditAnnotation);
        // Enable if there are annotations to remove
        edit_annotation.setDisable(plot.getAnnotations().isEmpty());

        crosshair = newToggleButton(ToolIcons.CROSSHAIR, Messages.Crosshair_Cursor);

        if (active)
        {
            configure.setOnAction(event -> plot.showConfigurationDialog());
            add_annotation.setOnAction(event ->
            {
                final AddAnnotationDialog<XTYPE> dialog = new AddAnnotationDialog<>(plot);
                DialogHelper.positionDialog(dialog, add_annotation, 0, 0);
                dialog.showAndWait();
                edit_annotation.setDisable(! haveUserAnnotations());
            });
            edit_annotation.setOnAction(event ->
            {
                final EditAnnotationDialog<XTYPE> dialog = new EditAnnotationDialog<>(plot);
                DialogHelper.positionDialog(dialog, edit_annotation, 0, 0);
                dialog.showAndWait();
                edit_annotation.setDisable(! haveUserAnnotations());
            });
            plot.addListener(new RTPlotListener<XTYPE>()
            {
                @Override
                public void changedAnnotations()
                {
                    Platform.runLater(() -> edit_annotation.setDisable(! haveUserAnnotations()));
                }
            });
            crosshair.setOnAction(event ->  plot.showCrosshair(crosshair.isSelected()));
        }
    }

    /** @return Are there any user (non-internal) annotations? */
    private boolean haveUserAnnotations()
    {
        for (Annotation<XTYPE> annotation : plot.getAnnotations())
            if (!annotation.isInternal())
                return true;
        return false;
    }

    private void addZoom(final boolean active)
    {
        final Button stagger = newButton(ToolIcons.STAGGER, Messages.Zoom_Stagger_TT);
        if (active)
            stagger.setOnAction(event -> plot.stagger(true));
    }

    private void addMouseModes(final boolean active)
    {
        zoom_in = newToggleButton(ToolIcons.ZOOM_IN, Messages.Zoom_In_TT);
        zoom_out = newToggleButton(ToolIcons.ZOOM_OUT, Messages.Zoom_Out_TT);
        pan = newToggleButton(ToolIcons.PAN, Messages.Pan_TT);
        pointer = newToggleButton(ToolIcons.POINTER, Messages.Plain_Pointer);

        if (active)
        {

            zoom_in.setOnAction(event ->
            {
                selectMouseMode(zoom_in);
                plot.setMouseMode(MouseMode.ZOOM_IN);
            });
            zoom_out.setOnAction(event ->
            {
                selectMouseMode(zoom_out);
                plot.setMouseMode(MouseMode.ZOOM_OUT);
            });

            pan.setOnAction(event ->
            {
                selectMouseMode(pan);
                plot.setMouseMode(MouseMode.PAN);
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
        final Button[] buttons = UndoButtons.createButtons(plot.getUndoableActionManager());
        for (Button button : buttons)
            button.setMinSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        toolbar.getItems().addAll(buttons);
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
        // setMinSize tends to have all icons end up in top-left corner?!
        item.setPrefSize(BUTTON_WIDTH, BUTTON_HEIGHT);

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

    /** Turn crosshair on/off */
    public void toggleCrosshair()
    {
        final boolean show = ! plot.isCrosshairVisible();
        crosshair.setSelected(show);
        plot.showCrosshair(show);
    }
}

/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.model.widgets.ChoiceButtonWidget;
import org.csstudio.display.builder.representation.javafx.Cursors;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.epics.vtype.VEnum;
import org.epics.vtype.VNumber;
import org.epics.vtype.VType;
import org.phoebus.ui.javafx.Styles;
import org.phoebus.ui.vtype.FormatOption;
import org.phoebus.ui.vtype.FormatOptionHandler;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.TilePane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 *  @author Amanda Carpenter original RadioButton implementation
 */
@SuppressWarnings("nls")
public class ChoiceButtonRepresentation extends JFXBaseRepresentation<TilePane, ChoiceButtonWidget>
{
    private volatile boolean active = false;
    private final ToggleGroup toggle = new ToggleGroup();

    private final DirtyFlag dirty_size = new DirtyFlag();
    private final DirtyFlag dirty_style = new DirtyFlag();
    private final DirtyFlag dirty_content = new DirtyFlag();
    private final UntypedWidgetPropertyListener contentChangedListener = this::contentChanged;
    private final WidgetPropertyListener< List<WidgetProperty<String> > > itemsChangedListener = this::itemsChanged;
    private final UntypedWidgetPropertyListener sizeChangedListener = this::sizeChanged;
    private final UntypedWidgetPropertyListener styleChangedListener = this::styleChanged;

    private volatile List<String> items = Collections.emptyList();
    private volatile int index = -1;
    protected volatile boolean enabled = true;

    private static final double GAP = 1.0;

    @Override
    public TilePane createJFXNode() throws Exception
    {
        final TilePane pane = new TilePane(GAP, GAP, createButton(null));
        pane.setTileAlignment(Pos.BASELINE_LEFT);
        return pane;
    }

    private ButtonBase createButton(final String text)
    {
        final ToggleButton b = new ToggleButton(text);
        b.getStyleClass().add("action_button");
        b.setToggleGroup(toggle);
        b.setMnemonicParsing(false);
        return b;
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.propWidth().addUntypedPropertyListener(sizeChangedListener);
        model_widget.propHeight().addUntypedPropertyListener(sizeChangedListener);
        model_widget.propHorizontal().addUntypedPropertyListener(sizeChangedListener);

        model_widget.propFont().addUntypedPropertyListener(styleChangedListener);
        model_widget.propForegroundColor().addUntypedPropertyListener(styleChangedListener);
        model_widget.propBackgroundColor().addUntypedPropertyListener(styleChangedListener);
        model_widget.propEnabled().addUntypedPropertyListener(styleChangedListener);
        model_widget.runtimePropPVWritable().addUntypedPropertyListener(styleChangedListener);

        // When using items-from-pv, each value update can require an update of the buttons,
        // so one 'content' change handler for any of these events
        model_widget.runtimePropValue().addUntypedPropertyListener(contentChangedListener);
        model_widget.propItemsFromPV().addUntypedPropertyListener(contentChangedListener);
        // Changing the items also triggers content change,
        // after having each item trigger a content change
        model_widget.propItems().addPropertyListener(itemsChangedListener);

        if (! toolkit.isEditMode())
            toggle.selectedToggleProperty().addListener(this::selectionChanged);

        // Initially populate pane with radio buttons
        itemsChanged(model_widget.propItems(), null, model_widget.propItems().getValue());
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propWidth().removePropertyListener(sizeChangedListener);
        model_widget.propHeight().removePropertyListener(sizeChangedListener);
        model_widget.propHorizontal().removePropertyListener(sizeChangedListener);
        model_widget.propFont().removePropertyListener(styleChangedListener);
        model_widget.propForegroundColor().removePropertyListener(styleChangedListener);
        model_widget.propBackgroundColor().removePropertyListener(styleChangedListener);
        model_widget.propEnabled().removePropertyListener(styleChangedListener);
        model_widget.runtimePropPVWritable().removePropertyListener(styleChangedListener);
        model_widget.runtimePropValue().removePropertyListener(contentChangedListener);
        model_widget.propItemsFromPV().removePropertyListener(contentChangedListener);
        model_widget.propItems().removePropertyListener(itemsChangedListener);
        for (WidgetProperty<String> item : model_widget.propItems().getValue())
            item.removePropertyListener(contentChangedListener);

        super.unregisterListeners();
    }

    @Override
    protected boolean isFilteringEditModeClicks()
    {
        return true;
    }

    private void selectionChanged(final ObservableValue<? extends Toggle> obs, final Toggle oldval, final Toggle newval)
    {
        if (!active  &&  newval != null)
        {
            active = true;
            try
            {
                // For now reset to old value.
                // New value will be shown if the PV accepts it and sends a value update.
                toggle.selectToggle(oldval);

                if (enabled)
                {
                    final Object value;
                    final VType pv_value = model_widget.runtimePropValue().getValue();
                    if (pv_value instanceof VEnum  ||  pv_value instanceof VNumber)
                        // PV uses enumerated or numeric type, so write the index
                        value = toggle.getToggles().indexOf(newval);
                    else // PV uses text
                        value = FormatOptionHandler.parse(pv_value,
                                                          ((ButtonBase) newval).getText(),
                                                          FormatOption.DEFAULT);
                    logger.log(Level.FINE, "Writing " + value);
                    Platform.runLater(() -> confirm(value));
                }
            }
            finally
            {
                active = false;
            }
        }
    }

    private void confirm(final Object value)
    {
        if (model_widget.propConfirmDialog().getValue())
        {
            final String message = model_widget.propConfirmMessage().getValue();
            final String password = model_widget.propPassword().getValue();
            if (password.length() > 0)
            {
                if (toolkit.showPasswordDialog(model_widget, message, password) == null)
                    return;
            }
            else if (!toolkit.showConfirmationDialog(model_widget, message))
                return;
        }

        toolkit.fireWrite(model_widget, value);
    }

    private void styleChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_style.mark();
        toolkit.scheduleUpdate(this);
    }

    private void sizeChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_size.mark();
        toolkit.scheduleUpdate(this);
    }

    /** @param value Current value of PV
     *  @param fromPV Get list of items from PV (if possible)?
     *  @return List of radio button labels
     */
    private List<String> computeItems(final VType value, final boolean fromPV)
    {
        if (value instanceof VEnum  &&  fromPV)
            return ((VEnum)value).getDisplay().getChoices();

        final List<WidgetProperty<String>> itemProps = model_widget.propItems().getValue();
        final List<String> new_items = new ArrayList<>(itemProps.size());
        for (WidgetProperty<String> itemProp : itemProps)
            new_items.add(itemProp.getValue());
        return new_items;
    }

    private int determineIndex(final List<String> labels, final VType value)
    {
        if (value instanceof VEnum)
            return ((VEnum)value).getIndex();
        if (value instanceof VNumber)
            return ((VNumber)value).getValue().intValue();
        return labels.indexOf(VTypeUtil.getValueString(value, false));
    }

    private void itemsChanged(final WidgetProperty<List<WidgetProperty<String> >> property, final List<WidgetProperty<String> > removed, final List<WidgetProperty<String> > added)
    {
        if (removed != null)
            for (WidgetProperty<String> item : removed)
                item.removePropertyListener(contentChangedListener);
        if (added != null)
            for (WidgetProperty<String> item : added)
                item.addUntypedPropertyListener(contentChangedListener);
        contentChanged(null, null, null);
    }

    /** The value or how we treat the value changed */
    private void contentChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        final VType value = model_widget.runtimePropValue().getValue();
        final boolean fromPV = model_widget.propItemsFromPV().getValue() && value instanceof VEnum;
        items = computeItems(value, fromPV);
        index = determineIndex(items, value);
        dirty_content.mark();
        dirty_style.mark(); // Adjust colors
        toolkit.scheduleUpdate(this);
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_size.checkAndClear())
        {
            // Size
            jfx_node.setPrefSize(model_widget.propWidth().getValue(), model_widget.propHeight().getValue());
            jfx_node.setOrientation(model_widget.propHorizontal().getValue()
                                    ? Orientation.HORIZONTAL
                                    : Orientation.VERTICAL);
            sizeButtons();
        }
        if (dirty_content.checkAndClear())
        {
            active = true;
            try
            {
                // Copy volatile items before iteration
                final List<String> save_items = new ArrayList<>(items);
                final int save_index = index;

                final List<Node> buttons = jfx_node.getChildren();

                // Remove extra buttons
                while (buttons.size() > save_items.size())
                    buttons.remove(buttons.size() - 1);

                // Set text of buttons, adding new ones as needed
                for (int i = 0; i < save_items.size(); i++)
                    if (i < buttons.size())
                        ((ButtonBase) buttons.get(i)).setText(save_items.get(i));
                    else
                        buttons.add(createButton(save_items.get(i)));

                sizeButtons();

                // Select one of the buttons
                toggle.selectToggle(save_index < 0 || save_index >= buttons.size()
                                    ? null
                                    : (Toggle) buttons.get(save_index));
            }
            finally
            {
                active = false;
            }
        }
        if (dirty_style.checkAndClear())
        {
            final Font font = JFXUtil.convert(model_widget.propFont().getValue());
            final Color fg = JFXUtil.convert(model_widget.propForegroundColor().getValue());
            final String background = JFXUtil.shadedStyle(model_widget.propBackgroundColor().getValue());
            final String selected = JFXUtil.shadedStyle(model_widget.propSelectedColor().getValue());
            // Don't disable the widget, because that would also remove the
            // context menu etc.
            // Just apply a style that matches the disabled look.
            enabled = model_widget.propEnabled().getValue() &&
                      model_widget.runtimePropPVWritable().getValue();
            Styles.update(jfx_node, Styles.NOT_ENABLED, !enabled);
            jfx_node.setCursor(enabled ? Cursor.DEFAULT : Cursors.NO_WRITE);
            for (Node node : jfx_node.getChildren())
            {
                final ButtonBase b = (ButtonBase) node;
                b.setTextFill(fg);
                b.setFont(font);
                if (((Toggle)b).isSelected())
                    b.setStyle(selected);
                else
                    b.setStyle(background);
            }
        }
    }

    private void sizeButtons()
    {
        final int N = Math.max(1, jfx_node.getChildren().size());
        final int width, height;

        if (model_widget.propHorizontal().getValue())
        {
            jfx_node.setPrefColumns(N);
            width = (int) ((model_widget.propWidth().getValue() - (N-1)*jfx_node.getHgap()) / N);
            height = model_widget.propHeight().getValue();
        }
        else
        {
            jfx_node.setPrefRows(N);
            width = model_widget.propWidth().getValue();
            height = (int) ((model_widget.propHeight().getValue() - (N-1)*jfx_node.getVgap()) / N);
        }
        for (Node node : jfx_node.getChildren())
        {
            final ButtonBase b = (ButtonBase) node;
            b.setMinSize(width, height);
            b.setPrefSize(width, height);
            b.setMaxSize(width, height);
        }
    }
}

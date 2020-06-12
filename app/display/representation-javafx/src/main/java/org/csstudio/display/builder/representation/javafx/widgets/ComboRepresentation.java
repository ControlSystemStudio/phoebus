/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javafx.scene.input.MouseButton;
import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.model.widgets.ComboWidget;
import org.csstudio.display.builder.representation.javafx.Cursors;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.epics.vtype.VEnum;
import org.epics.vtype.VType;
import org.phoebus.ui.javafx.Styles;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.scene.Cursor;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;

/** Creates JavaFX item for model widget
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class ComboRepresentation extends RegionBaseRepresentation<ComboBox<String>, ComboWidget>
{
    private volatile boolean active = false;

    private final DirtyFlag dirty_style = new DirtyFlag();
    private final DirtyFlag dirty_content = new DirtyFlag();
    private final DirtyFlag dirty_enable = new DirtyFlag();
    private final UntypedWidgetPropertyListener contentChangedListener = this::contentChanged;
    private final UntypedWidgetPropertyListener enableChangedListener = this::enableChanged;
    private final UntypedWidgetPropertyListener styleChangedListener = this::styleChanged;

    private volatile List<String> items = Collections.emptyList();
    private volatile int index = -1;

    @Override
    public ComboBox<String> createJFXNode() throws Exception
    {
        final ComboBox<String> combo = new ComboBox<>();
        if (! toolkit.isEditMode())
        {
            // 'editable' cannot be changed at runtime
            combo.setEditable(model_widget.propEditable().getValue());

            // Handle user's selection
            combo.setOnAction((event)->
            {   // We are updating the UI, ignore
                if (active)
                    return;
                final String value = combo.getValue();
                if (value != null)
                {
                    // Restore current value
                    contentChanged(null, null, null);
                    // ... which should soon be replaced by updated value, if accepted
                    Platform.runLater(() -> confirm(value));
                }
            });

            // Also write to PV when user re-selects the current value
            combo.setCellFactory(list ->
            {
                final ListCell<String> cell = new ListCell<>()
                {
                    @Override
                    public void updateItem(final String item, final boolean empty)
                    {
                        super.updateItem(item, empty);
                        if ( !empty )
                            setText(item);
                    }
                };
                cell.addEventFilter(MouseEvent.MOUSE_PRESSED, e ->
                {
                    // Is this a click on the 'current' value which would by default be ignored?
                    if (Objects.equals(combo.getValue(), cell.getItem()))
                    {
                        combo.fireEvent(new ActionEvent());
                        //  Alternatively...
                        //combo.setValue(null);
                        //combo.getSelectionModel().select(cell.getItem());
                        e.consume();
                    }
                });

                return cell;
            });

            combo.setOnMouseClicked(event -> {
                // Secondary mouse button should bring up context menu,
                // but not show selections (i.e. not expand drop-down).
                if(event.getButton().equals(MouseButton.SECONDARY)){
                    combo.hide();
                }
            });

        }
        contentChanged(null, null, null);
        return combo;
    }

    @Override
    protected boolean isFilteringEditModeClicks()
    {   // Allow selecting the Combo in editor
        return true;
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.propWidth().addUntypedPropertyListener(styleChangedListener);
        model_widget.propHeight().addUntypedPropertyListener(styleChangedListener);
        model_widget.propForegroundColor().addUntypedPropertyListener(styleChangedListener);
        model_widget.propBackgroundColor().addUntypedPropertyListener(styleChangedListener);
        model_widget.propFont().addUntypedPropertyListener(styleChangedListener);

        model_widget.runtimePropValue().addUntypedPropertyListener(contentChangedListener);
        model_widget.propItemsFromPV().addUntypedPropertyListener(contentChangedListener);
        model_widget.propItems().addUntypedPropertyListener(contentChangedListener);
        model_widget.propEnabled().addUntypedPropertyListener(enableChangedListener);
        model_widget.runtimePropPVWritable().addUntypedPropertyListener(enableChangedListener);

        styleChanged(null, null, null);
        contentChanged(null, null, null);
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propWidth().removePropertyListener(styleChangedListener);
        model_widget.propHeight().removePropertyListener(styleChangedListener);
        model_widget.propForegroundColor().removePropertyListener(styleChangedListener);
        model_widget.propBackgroundColor().removePropertyListener(styleChangedListener);
        model_widget.propFont().removePropertyListener(styleChangedListener);
        model_widget.runtimePropValue().removePropertyListener(contentChangedListener);
        model_widget.propItemsFromPV().removePropertyListener(contentChangedListener);
        model_widget.propItems().removePropertyListener(contentChangedListener);
        model_widget.propEnabled().removePropertyListener(enableChangedListener);
        model_widget.runtimePropPVWritable().removePropertyListener(enableChangedListener);
        super.unregisterListeners();
    }

    private void confirm(final String value)
    {
        if ( model_widget.propConfirmDialog().getValue())
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

    private void enableChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_enable.mark();
        toolkit.scheduleUpdate(this);
    }


    /** @param value Current value of PV
     *  @param fromPV Use items from enum?
     *  @return List of items, potentially adding the current value to the items originally in the combo
     */
    private List<String> computeItems(final VType value, final boolean fromPV)
    {
        // System.out.println("computeItems(" + value + ", " + fromPV + "): ");
        if (fromPV)
        {
            index = ((VEnum)value).getIndex();
            return ((VEnum)value).getDisplay().getChoices();
        }
        else
        {
            final List<String> new_items = new ArrayList<>();
            for (WidgetProperty<String> itemProp : model_widget.propItems().getValue())
                new_items.add(itemProp.getValue());

            final String currValue = VTypeUtil.getValueString(value, false);
            int new_index = new_items.indexOf(currValue);
            if (new_index < 0)
            {   // User entered a custom value ('editable' combo).
                // Add to top of list and select it
                new_items.add(0, currValue);
                new_index = 0;
            }

            // System.out.println(new_items);
            // System.out.println(new_index);

            index = new_index;
            return new_items;
        }
    }

    private void contentChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        VType value = model_widget.runtimePropValue().getValue();
        boolean fromPV = model_widget.propItemsFromPV().getValue() && value instanceof VEnum;
        items = computeItems(value, fromPV); //also sets index
        dirty_content.mark();
        toolkit.scheduleUpdate(this);
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_style.checkAndClear())
        {

            jfx_node.setPrefSize(model_widget.propWidth().getValue(),
                                 model_widget.propHeight().getValue());

            Font f = JFXUtil.convert(model_widget.propFont().getValue());

            jfx_node.setStyle(MessageFormat.format(
                "-fx-body-color: linear-gradient(to bottom,ladder({0}, derive({0},8%) 75%, derive({0},10%) 80%), derive({0},-8%)); "
              + "-fx-text-base-color: ladder(-fx-color, -fx-light-text-color 45%, -fx-dark-text-color 46%, -fx-dark-text-color 59%, {1}); "
              + "-fx-font: {2} {3}px \"{4}\";",
                JFXUtil.webRGB(model_widget.propBackgroundColor().getValue()),
                JFXUtil.webRGB(model_widget.propForegroundColor().getValue()),
                f.getStyle().toLowerCase().replace("regular", "normal"),
                f.getSize(),
                f.getFamily()
            ));

        }
        if (dirty_content.checkAndClear()  &&  !toolkit.isEditMode())
        {
            active = true;
            try
            {
                jfx_node.setItems(FXCollections.observableArrayList(items));
                jfx_node.getSelectionModel().clearAndSelect(index);
            }
            finally
            {
                active = false;
            }
        }
        if (dirty_enable.checkAndClear()  &&  !toolkit.isEditMode())
        {
            final boolean enabled = model_widget.propEnabled().getValue()  &&
                                    model_widget.runtimePropPVWritable().getValue();
            // When truly disabled, the widget no longer reacts to context menu,
            // and the cursor will be ignored
            //  jfx_node.setDisable(! enabled);
            // So keep enabled, but indicate that trying to operate the widget is futile
            Styles.update(jfx_node, Styles.NOT_ENABLED, !enabled);
            jfx_node.setCursor(enabled ? Cursor.DEFAULT : Cursors.NO_WRITE);
        }
    }
}

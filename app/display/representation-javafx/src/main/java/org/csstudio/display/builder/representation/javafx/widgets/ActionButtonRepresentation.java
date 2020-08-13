/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.properties.ActionInfos;
import org.csstudio.display.builder.model.properties.OpenDisplayActionInfo;
import org.csstudio.display.builder.model.properties.RotationStep;
import org.csstudio.display.builder.model.properties.StringWidgetProperty;
import org.csstudio.display.builder.model.widgets.ActionButtonWidget;
import org.csstudio.display.builder.representation.javafx.Cursors;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.csstudio.display.builder.representation.javafx.Messages;
import org.phoebus.framework.macros.MacroHandler;
import org.phoebus.framework.macros.MacroValueProvider;
import org.phoebus.ui.javafx.Styles;
import org.phoebus.ui.javafx.TextUtils;
import org.phoebus.ui.vtype.FormatOption;
import org.phoebus.ui.vtype.FormatOptionHandler;

import javafx.application.Platform;
import javafx.geometry.Dimension2D;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/** Creates JavaFX item for model widget
 *  @author Megan Grodowitz
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ActionButtonRepresentation extends RegionBaseRepresentation<Pane, ActionButtonWidget>
{
    // Uses a Button if there is only one action,
    // otherwise a MenuButton so that user can select the specific action.
    //
    // These two types were chosen because they share the same ButtonBase base class.
    // ChoiceBox is not derived from ButtonBase, plus it has currently selected 'value',
    // and with action buttons it wouldn't make sense to select one of the actions.
    //
    // The 'base' button is wrapped in a 'pane'
    // to allow replacing the button as actions change from single actions (or zero)
    // to multiple actions.

    private final DirtyFlag dirty_representation = new DirtyFlag();
    private final DirtyFlag dirty_enablement = new DirtyFlag();
    private final DirtyFlag dirty_actionls = new DirtyFlag();

    private volatile ButtonBase base;
    private volatile String background;
    private volatile Color foreground;
    private volatile String button_text;
    private volatile boolean enabled = true;

    /** Was there ever any transformation applied to the jfx_node?
     *
     *  <p>Used to optimize:
     *  If there never was a rotation, don't even _clear()_ it
     *  to keep the Node's nodeTransformation == null
     */
    private boolean was_ever_transformed = false;

    /** Optional modifier of the open display 'target */
    private Optional<OpenDisplayActionInfo.Target> target_modifier = Optional.empty();

    private Pane pane;

    private final UntypedWidgetPropertyListener buttonChangedListener = this::buttonChanged;
    private final UntypedWidgetPropertyListener representationChangedListener = this::representationChanged;
    private final WidgetPropertyListener<Boolean> enablementChangedListener = this::enablementChanged;

    @Override
    protected boolean isFilteringEditModeClicks()
    {
        return true;
    }

    @Override
    public Pane createJFXNode() throws Exception
    {
        updateColors();
        base = makeBaseButton();

        pane = new Pane();
        pane.getChildren().add(base);

        return pane;
    }

    /** @param event Mouse event to check for target modifier keys */
    private void checkModifiers(final MouseEvent event)
    {
        // 'control' ('command' on Mac OS X)
        if (event.isShortcutDown())
            target_modifier = Optional.of(OpenDisplayActionInfo.Target.TAB);
        else if (event.isShiftDown())
            target_modifier = Optional.of(OpenDisplayActionInfo.Target.WINDOW);
        else
            target_modifier = Optional.empty();

        // At least on Linux, a Control-click or Shift-click
        // will not 'arm' the button, so the click is basically ignored.
        // Force the 'arm', so user can Control-click or Shift-click to
        // invoke the button
        if (target_modifier.isPresent())
        {
            logger.log(Level.FINE, "{0} modifier: {1}", new Object[] { model_widget, target_modifier.get() });
            base.arm();
        }
    }

//    private int calls = 0;

    /** Create <code>base</code>, either single-action button
     *  or menu for selecting one out of N actions
     */
    private ButtonBase makeBaseButton()
    {
        final ActionInfos actions = model_widget.propActions().getValue();
        final ButtonBase result;
        if (actions.isExecutedAsOne()  ||  actions.getActions().size() < 2)
        {
            final Button button = new Button();
            button.setOnAction(event -> confirm(() ->  handleActions(actions.getActions())));
            result = button;
        }
        else
        {
            final MenuButton button = new MenuButton();
            // Experimenting with ways to force update of popup location,
            // #226
            button.showingProperty().addListener((prop, old, showing) ->
            {
                if (showing)
                {
                    // System.out.println("Showing " + model_widget + " menu: " + showing);
//                    if (++calls > 2)
//                    {
//                        System.out.println("Hack!");
//                        if (button.getPopupSide() == Side.BOTTOM)
//                            button.setPopupSide(Side.LEFT);
//                        else
//                            button.setPopupSide(Side.BOTTOM);
//                        // button.layout();
//                    }
                }
            });
            for (final ActionInfo action : actions.getActions())
            {
                final MenuItem item = new MenuItem(makeActionText(action),
                                                   new ImageView(new Image(action.getType().getIconURL().toExternalForm()))
                                                  );
                item.getStyleClass().add("action_button_item");
                item.setOnAction(event -> confirm(() ->  handleAction(action)));
                button.getItems().add(item);
            }
            result = button;
        }
        result.setStyle(background);

        // In edit mode, show dashed border for transparent/invisible widget
        if (toolkit.isEditMode()  &&  model_widget.propTransparent().getValue())
            result.setBorder(new Border(new BorderStroke(Color.BLACK, GroupRepresentation.EDIT_NONE_DASHED, CornerRadii.EMPTY, GroupRepresentation.EDIT_NONE_BORDER)));
        result.getStyleClass().add("action_button");
        result.setMnemonicParsing(false);

        // Model has width/height, but JFX widget has min, pref, max size.
        // updateChanges() will set the 'pref' size, so make min use that as well.
        result.setMinSize(ButtonBase.USE_PREF_SIZE, ButtonBase.USE_PREF_SIZE);

        // Monitor keys that modify the OpenDisplayActionInfo.Target.
        // Use filter to capture event that's otherwise already handled.
        result.addEventFilter(MouseEvent.MOUSE_PRESSED, this::checkModifiers);

        // Need to attach TT to the specific button, not the common jfx_node Pane
        TooltipSupport.attach(result, model_widget.propTooltip());

        result.setCursor(Cursor.HAND);

        return result;
    }

    private void confirm(final Runnable action)
    {
        Platform.runLater(() ->
        {
            // If confirmation is requested..
            if (model_widget.propConfirmDialog().getValue())
            {
                final String message = model_widget.propConfirmMessage().getValue();
                final String password = model_widget.propPassword().getValue();
                // .. check either with password or generic Ok/Cancel prompt
                if (password.length() > 0)
                {
                    if (toolkit.showPasswordDialog(model_widget, message, password) == null)
                        return;
                }
                else
                    if (! toolkit.showConfirmationDialog(model_widget, message))
                        return;
            }

            action.run();
        });
    }

    /** @return Should 'label' show the PV's current value? */
    private boolean isLabelValue()
    {
        final StringWidgetProperty text_prop = (StringWidgetProperty)model_widget.propText();
        return ActionButtonWidget.VALUE_LABEL.equals(text_prop.getSpecification());
    }

    private String makeButtonText()
    {
        // If text is "$(actions)", evaluate the actions ourself because
        // a) That way we can format it beyond just "[ action1, action2, ..]"
        // b) Macro won't be re-evaluated as actions change,
        //    while this code will always use current actions
        final StringWidgetProperty text_prop = (StringWidgetProperty)model_widget.propText();
        if (isLabelValue())
            return FormatOptionHandler.format(model_widget.runtimePropValue().getValue(), FormatOption.DEFAULT, -1, true);
        else if ("$(actions)".equals(text_prop.getSpecification()))
        {
            final List<ActionInfo> actions = model_widget.propActions().getValue().getActions();
            if (actions.size() < 1)
                return Messages.ActionButton_NoActions;
            if (actions.size() > 1)
                return MessageFormat.format(Messages.ActionButton_N_ActionsFmt, actions.size());
            return makeActionText(actions.get(0));
        }
        else
            return text_prop.getValue();
    }

    private String makeActionText(final ActionInfo action)
    {
        String action_str = action.getDescription();
        if (action_str.isEmpty())
            action_str = action.toString();
        String expanded;
        try
        {
            final MacroValueProvider macros = model_widget.getMacrosOrProperties();
            expanded = MacroHandler.replace(macros, action_str);
        }
        catch (final Exception ex)
        {
            logger.log(Level.WARNING, model_widget + " action " + action + " cannot expand macros for " + action_str, ex);
            expanded = action_str;
        }
        return expanded;
    }

    /** @param actions Actions that the user invoked */
    private void handleActions(final List<ActionInfo> actions)
    {
        for (ActionInfo action : actions)
            handleAction(action);
    }

    /** @param action Action that the user invoked */
    private void handleAction(ActionInfo action)
    {
        if (! enabled)
            return;
        logger.log(Level.FINE, "{0} pressed", model_widget);
        if (action instanceof OpenDisplayActionInfo  &&  target_modifier.isPresent())
        {
            final OpenDisplayActionInfo orig = (OpenDisplayActionInfo) action;
            action = new OpenDisplayActionInfo(orig.getDescription(), orig.getFile(), orig.getMacros(), target_modifier.get(), orig.getPane());
        }
        toolkit.fireAction(model_widget, action);
    }

    @Override
    protected void registerListeners()
    {
        updateColors();
        super.registerListeners();

        model_widget.propWidth().addUntypedPropertyListener(representationChangedListener);
        model_widget.propHeight().addUntypedPropertyListener(representationChangedListener);
        model_widget.propText().addUntypedPropertyListener(representationChangedListener);
        model_widget.propFont().addUntypedPropertyListener(representationChangedListener);
        model_widget.propRotationStep().addUntypedPropertyListener(representationChangedListener);

        model_widget.propEnabled().addPropertyListener(enablementChangedListener);
        model_widget.runtimePropPVWritable().addPropertyListener(enablementChangedListener);

        model_widget.propBackgroundColor().addUntypedPropertyListener(buttonChangedListener);
        model_widget.propForegroundColor().addUntypedPropertyListener(buttonChangedListener);
        model_widget.propTransparent().addUntypedPropertyListener(buttonChangedListener);
        model_widget.propActions().addUntypedPropertyListener(buttonChangedListener);

        if (! toolkit.isEditMode()  &&  isLabelValue())
            model_widget.runtimePropValue().addUntypedPropertyListener(representationChangedListener);

        enablementChanged(null, null, null);
    }

    @Override
    protected void unregisterListeners()
    {
        if (! toolkit.isEditMode()  &&  isLabelValue())
            model_widget.runtimePropValue().removePropertyListener(representationChangedListener);
        model_widget.propWidth().removePropertyListener(representationChangedListener);
        model_widget.propHeight().removePropertyListener(representationChangedListener);
        model_widget.propText().removePropertyListener(representationChangedListener);
        model_widget.propFont().removePropertyListener(representationChangedListener);
        model_widget.propRotationStep().removePropertyListener(representationChangedListener);
        model_widget.propEnabled().removePropertyListener(enablementChangedListener);
        model_widget.runtimePropPVWritable().removePropertyListener(enablementChangedListener);
        model_widget.propBackgroundColor().removePropertyListener(buttonChangedListener);
        model_widget.propForegroundColor().removePropertyListener(buttonChangedListener);
        model_widget.propTransparent().removePropertyListener(buttonChangedListener);
        model_widget.propActions().removePropertyListener(buttonChangedListener);
        super.unregisterListeners();
    }

    @Override
    protected void attachTooltip()
    {
        // Cannot attach tool tip to the jfx_node (Pane).
        // Needs to be attached to actual button, which
        // is done in makeBaseButton()
    }

    /** Complete button needs to be updated */
    private void buttonChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_actionls.mark();
        representationChanged(property, old_value, new_value);
    }

    /** Only details of the existing button need to be updated */
    private void representationChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        updateColors();
        dirty_representation.mark();
        toolkit.scheduleUpdate(this);
    }

    /** enabled or pv_writable changed */
    private void enablementChanged(final WidgetProperty<Boolean> property, final Boolean old_value, final Boolean new_value)
    {
        enabled  = model_widget.propEnabled().getValue()  &&
                  model_widget.runtimePropPVWritable().getValue();
        dirty_enablement.mark();
        toolkit.scheduleUpdate(this);
    }


    private void updateColors()
    {
        foreground = JFXUtil.convert(model_widget.propForegroundColor().getValue());
        if (model_widget.propTransparent().getValue())
            // Set most colors to transparent, including the 'arrow' used by MenuButton
            background = "-fx-background: transparent; -fx-color: transparent; -fx-focus-color: rgba(3,158,211,0.1); -fx-mark-color: transparent; -fx-background-color: transparent;";
        else
            background = JFXUtil.shadedStyle(model_widget.propBackgroundColor().getValue());
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_actionls.checkAndClear())
        {
            base = makeBaseButton();
            jfx_node.getChildren().setAll(base);
        }
        if (dirty_representation.checkAndClear())
        {
            button_text = makeButtonText();
            base.setText(button_text);
            base.setTextFill(foreground);
            base.setFont(JFXUtil.convert(model_widget.propFont().getValue()));

            // If widget is not wide enough to show the label, hide menu button 'arrow'.
            if (base instanceof MenuButton)
            {
                // Assume that desired gap and arrow occupy similar space as "__VV_".
                // Check if the text exceeds the width.
                final Dimension2D size = TextUtils.computeTextSize(base.getFont(), button_text + "__VV_");
                final boolean hide = size.getWidth() >= model_widget.propWidth().getValue();
                Styles.update(base, "hide_arrow", hide);
            }

            final RotationStep rotation = model_widget.propRotationStep().getValue();
            final int width = model_widget.propWidth().getValue(),
                      height = model_widget.propHeight().getValue();
            // Button 'base' is inside 'jfx_node' Pane.
            // Rotation needs to be applied to the Pane,
            // which then auto-sizes to the 'base' Button dimensions.
            // If transforming the Button instead of the Pane,
            // it will still remain sensitive to mouse clicks in the
            // original, un-transformed rectangle. Unclear why.
            // Applying the transformation to the Pane does not exhibit this problem.
            switch (rotation)
            {
            case NONE:
                base.setPrefSize(width, height);
                if (was_ever_transformed)
                    jfx_node.getTransforms().clear();
                break;
            case NINETY:
                base.setPrefSize(height, width);
                jfx_node.getTransforms().setAll(new Rotate(-rotation.getAngle()),
                                                new Translate(-height, 0));
                was_ever_transformed = true;
                break;
            case ONEEIGHTY:
                base.setPrefSize(width, height);
                jfx_node.getTransforms().setAll(new Rotate(-rotation.getAngle()),
                                                new Translate(-width, -height));
                was_ever_transformed = true;
                               break;
            case MINUS_NINETY:
                base.setPrefSize(height, width);
                jfx_node.getTransforms().setAll(new Rotate(-rotation.getAngle()),
                                                new Translate(0, -width));
                was_ever_transformed = true;
                break;
            }
        }
        if (dirty_enablement.checkAndClear())
        {
            base.setDisable(! enabled);
            Styles.update(base, Styles.NOT_ENABLED, !enabled);
            jfx_node.setCursor(enabled ? Cursor.DEFAULT : Cursors.NO_WRITE);
        }
    }
}

/*******************************************************************************
 * Copyright (c) 2015-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.util.List;
import java.util.logging.Level;

import javafx.scene.control.Alert;
import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.ConfirmDialog;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.model.widgets.BoolButtonWidget;
import org.csstudio.display.builder.model.widgets.BoolButtonWidget.Mode;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.csstudio.display.builder.representation.javafx.Messages;
import org.epics.vtype.VEnum;
import org.epics.vtype.VType;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.Styles;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Ellipse;
import javafx.scene.text.TextAlignment;

/** Creates JavaFX item for model widget
 *  @author Megan Grodowitz
 */
@SuppressWarnings("nls")
public class BoolButtonRepresentation extends RegionBaseRepresentation<Pane, BoolButtonWidget>
{
    private final DirtyFlag dirty_representation = new DirtyFlag();
    private final DirtyFlag dirty_enablement = new DirtyFlag();
    private final DirtyFlag dirty_value = new DirtyFlag();
    /** State: 0 or 1 */
    private volatile int on_state = 1;
    private volatile int use_bit = 0;
    private volatile Integer rt_value = 0;

    // Design decision: Plain Button.
    // JFX ToggleButton appears natural to reflect two states,
    // but this type of button is updated by both the user
    // (press to 'push', 'release') and the PV.
    // When user pushes button, value is sent to PV
    // and the button should update its state as the
    // update is received from the PV.
    // The ToggleButton, however, will 'select' or not
    // just because of the user interaction.
    // If this is then in addition updated by the PV,
    // the ToggleButton tends to 'flicker'.

    private volatile Button button;
    private volatile Ellipse led;

    private volatile String background;
    private volatile Color foreground;
    private volatile Color[] state_colors;
    private volatile Color value_color;
    private volatile String[] state_labels;
    private volatile String value_label;
    private volatile ImageView[] state_images;
    private volatile ImageView value_image;

    private final UntypedWidgetPropertyListener imagesChangedListener = this::imagesChanged;
    private final UntypedWidgetPropertyListener representationChangedListener = this::representationChanged;
    private final WidgetPropertyListener<Integer> bitChangedListener = this::bitChanged;
    private final WidgetPropertyListener<Boolean> enablementChangedListener = this::enablementChanged;
    private final WidgetPropertyListener<VType> valueChangedListener = this::valueChanged;
    private final WidgetPropertyListener<Mode> modeChangeListener = this::modeChanged;
    private final WidgetPropertyListener<ConfirmDialog> confirmDialogWidgetPropertyListener = this::confirmationDialogChanged;
    private volatile Pos pos;

    @Override
    public Pane createJFXNode() throws Exception
    {
        led = new Ellipse();
        led.getStyleClass().add("led");
        button = new Button("BoolButton", led);
        button.getStyleClass().add("action_button");
        button.setMnemonicParsing(false);

        // Model has width/height, but JFX widget has min, pref, max size.
        // updateChanges() will set the 'pref' size, so make min use that as well.
        button.setMinSize(ButtonBase.USE_PREF_SIZE, ButtonBase.USE_PREF_SIZE);

        // Fix initial layout
        toolkit.execute(() -> Platform.runLater(button::requestLayout));

        if (! toolkit.isEditMode())
        {
            if (model_widget.propMode().getValue() == Mode.TOGGLE)
                button.setOnAction(event -> handlePress(true));
            else
            {
                final boolean inverted = model_widget.propMode().getValue() == Mode.PUSH_INVERTED;
                button.setOnMousePressed(event -> handlePress(! inverted));
                button.setOnMouseReleased(event -> handlePress(inverted));
            }
        }

        Pane pane = new Pane();
        pane.getChildren().setAll(button);
        return pane;
    }

    @Override
    protected void attachTooltip()
    {
        model_widget.checkProperty(CommonWidgetProperties.propTooltip)
                .ifPresent(prop -> TooltipSupport.attach(button, prop));
    }

    /** Respond to button press
     *  @param pressed Was button pressed or released?
     */
    private void handlePress(final boolean pressed)
    {
        logger.log(Level.FINE, "{0} pressed", model_widget);
        Platform.runLater(() -> confirm(pressed));
    }

    /** Check for confirmation, then perform the button action
     *  @param pressed Was button pressed or released?
     */
    private void confirm(final boolean pressed)
    {
        final boolean prompt;
        switch (model_widget.propConfirmDialog().getValue())
        {
        case BOTH:     prompt = true;            break;
        case PUSH:     prompt = on_state == 0;   break;
        case RELEASE:  prompt = on_state == 1;   break;
        case NONE:
        default:       prompt = false;
        }
        if (prompt)
        {   // Require password, or plain prompt?
            final String message = model_widget.propConfirmMessage().getValue();
            final String password = model_widget.propPassword().getValue();
            if (password.length() > 0)
            {
                if (toolkit.showPasswordDialog(model_widget, message, password) == null)
                    return;
            }
            else if (! toolkit.showConfirmationDialog(model_widget, message))
                    return;
        }

        final int bit = (use_bit < 0) ? 1 : (1 << use_bit);
        final int new_val;
        if (model_widget.propMode().getValue() == Mode.TOGGLE)
            new_val = rt_value ^ bit;
        else
        {
            if (pressed)
                new_val = rt_value | bit;
            else
                new_val = rt_value & ~bit;
        }
        toolkit.fireWrite(model_widget, new_val);
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        representationChanged(null,null,null);
        pos = JFXUtil.computePos(model_widget.propHorizontalAlignment().getValue(),
                model_widget.propVerticalAlignment().getValue());
        model_widget.propWidth().addUntypedPropertyListener(representationChangedListener);
        model_widget.propHeight().addUntypedPropertyListener(representationChangedListener);
        model_widget.propOffLabel().addUntypedPropertyListener(representationChangedListener);
        model_widget.propOffImage().addUntypedPropertyListener(imagesChangedListener);
        model_widget.propOffColor().addUntypedPropertyListener(representationChangedListener);
        model_widget.propOnLabel().addUntypedPropertyListener(representationChangedListener);
        model_widget.propOnImage().addUntypedPropertyListener(imagesChangedListener);
        model_widget.propOnColor().addUntypedPropertyListener(representationChangedListener);
        model_widget.propShowLED().addUntypedPropertyListener(representationChangedListener);
        model_widget.propFont().addUntypedPropertyListener(representationChangedListener);
        model_widget.propForegroundColor().addUntypedPropertyListener(representationChangedListener);
        model_widget.propBackgroundColor().addUntypedPropertyListener(representationChangedListener);
        model_widget.propHorizontalAlignment().addUntypedPropertyListener(representationChangedListener);
        model_widget.propVerticalAlignment().addUntypedPropertyListener(representationChangedListener);
        model_widget.propEnabled().addPropertyListener(enablementChangedListener);
        model_widget.runtimePropPVWritable().addPropertyListener(enablementChangedListener);
        model_widget.propBit().addPropertyListener(bitChangedListener);
        model_widget.runtimePropValue().addPropertyListener(valueChangedListener);
        model_widget.propMode().addPropertyListener(modeChangeListener);
        model_widget.propConfirmDialog().addPropertyListener(confirmDialogWidgetPropertyListener);

        imagesChanged(null, null, null);
        bitChanged(model_widget.propBit(), null, model_widget.propBit().getValue());
        enablementChanged(null, null, null);
        valueChanged(null, null, model_widget.runtimePropValue().getValue());

    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propWidth().removePropertyListener(representationChangedListener);
        model_widget.propHeight().removePropertyListener(representationChangedListener);
        model_widget.propOffLabel().removePropertyListener(representationChangedListener);
        model_widget.propOffImage().removePropertyListener(imagesChangedListener);
        model_widget.propOffColor().removePropertyListener(representationChangedListener);
        model_widget.propOnLabel().removePropertyListener(representationChangedListener);
        model_widget.propOnImage().removePropertyListener(imagesChangedListener);
        model_widget.propOnColor().removePropertyListener(representationChangedListener);
        model_widget.propShowLED().removePropertyListener(representationChangedListener);
        model_widget.propFont().removePropertyListener(representationChangedListener);
        model_widget.propForegroundColor().removePropertyListener(representationChangedListener);
        model_widget.propBackgroundColor().removePropertyListener(representationChangedListener);
        model_widget.propHorizontalAlignment().removePropertyListener(representationChangedListener);
        model_widget.propVerticalAlignment().removePropertyListener(representationChangedListener);
        model_widget.propEnabled().removePropertyListener(enablementChangedListener);
        model_widget.runtimePropPVWritable().removePropertyListener(enablementChangedListener);
        model_widget.propBit().removePropertyListener(bitChangedListener);
        model_widget.runtimePropValue().removePropertyListener(valueChangedListener);
        model_widget.propMode().removePropertyListener(modeChangeListener);
        model_widget.propConfirmDialog().removePropertyListener(confirmDialogWidgetPropertyListener);
        super.unregisterListeners();
    }

    private void computeBackground()
    {
        // When LED is off, use the on/off colors for the background
        if (model_widget.propShowLED().getValue() == false)
            background = JFXUtil.shadedStyle(on_state == 0 ? model_widget.propOffColor().getValue() : model_widget.propOnColor().getValue());
        else
            background = JFXUtil.shadedStyle(model_widget.propBackgroundColor().getValue());
    }

    private void stateChanged()
    {
        on_state = ((use_bit < 0) ? (rt_value != 0) : (((rt_value >> use_bit) & 1) == 1)) ? 1 : 0;
        value_color = state_colors[on_state];
        value_label = state_labels[on_state];
        value_image = state_images[on_state];

        computeBackground();

        dirty_value.mark();
        toolkit.scheduleUpdate(this);
    }

    private void bitChanged(final WidgetProperty<Integer> property, final Integer old_value, final Integer new_value)
    {
        use_bit = new_value;
        stateChanged();
    }

    private void valueChanged(final WidgetProperty<VType> property, final VType old_value, final VType new_value)
    {
        if ((new_value instanceof VEnum)  &&
            model_widget.propLabelsFromPV().getValue())
        {
            final List<String> labels = ((VEnum) new_value).getDisplay().getChoices();
            if (labels.size() == 2)
            {
                model_widget.propOffLabel().setValue(labels.get(0));
                model_widget.propOnLabel().setValue(labels.get(1));
            }
        }

        rt_value = VTypeUtil.getValueNumber(new_value).intValue();
        stateChanged();
    }

    private void modeChanged(final WidgetProperty<Mode> property, final Mode old_value, final Mode new_value){
        if(!new_value.equals(Mode.TOGGLE) && !model_widget.propConfirmDialog().getValue().equals(ConfirmDialog.NONE)){
            showUnsupportedConfigurationDialog();
            Platform.runLater(() -> model_widget.propMode().setValue(old_value));
        }
    }

    private void confirmationDialogChanged(final WidgetProperty<ConfirmDialog> property, final ConfirmDialog old_value, final ConfirmDialog new_value){
        if(!new_value.equals(ConfirmDialog.NONE) && !model_widget.propMode().getValue().equals(Mode.TOGGLE)){
            showUnsupportedConfigurationDialog();
            Platform.runLater(() -> model_widget.propConfirmDialog().setValue(old_value));
        }
    }

    private void imagesChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        state_images = new ImageView[]
        {
            loadImage(model_widget.propOffImage().getValue()),
            loadImage(model_widget.propOnImage().getValue())
        };
        // The 'value_image' needs to be updated
        stateChanged();
    }

    private ImageView loadImage(final String path)
    {
        if (path.isEmpty())
            return null;
        try
        {
            // Resolve image file relative to the source widget model (not 'top'!)
            final DisplayModel widget_model = model_widget.getDisplayModel();
            final String resolved = ModelResourceUtil.resolveResource(widget_model, path);
            return new ImageView(new Image(ModelResourceUtil.openResourceStream(resolved)));
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, model_widget + " cannot load image", ex);
        }
        return null;
    }

    private void representationChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        foreground = JFXUtil.convert(model_widget.propForegroundColor().getValue());

        state_colors = new Color[]
        {
            JFXUtil.convert(model_widget.propOffColor().getValue()),
            JFXUtil.convert(model_widget.propOnColor().getValue())
        };

        state_labels = new String[] { model_widget.propOffLabel().getValue(), model_widget.propOnLabel().getValue() };
        value_color = state_colors[on_state];
        value_label = state_labels[on_state];

        computeBackground();
        
        pos = JFXUtil.computePos(model_widget.propHorizontalAlignment().getValue(),
                model_widget.propVerticalAlignment().getValue());

        dirty_representation.mark();
        toolkit.scheduleUpdate(this);
    }

    private void enablementChanged(final WidgetProperty<Boolean> property, final Boolean old_value, final Boolean new_value)
    {
        dirty_enablement.mark();
        toolkit.scheduleUpdate(this);
    }

    private Paint computeEditColors()
    {
        final Color[] save_colors = state_colors;
        return new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            List.of(new Stop(0.0, save_colors[0]),
                    new Stop(0.5, save_colors[0]),
                    new Stop(0.5, save_colors[1]),
                    new Stop(1.0, save_colors[1])));
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        boolean update_value = dirty_value.checkAndClear();
        if (dirty_representation.checkAndClear())
        {
            final int wid = model_widget.propWidth().getValue(),
                      hei = model_widget.propHeight().getValue();
            button.setPrefSize(wid, hei);
            button.setFont(JFXUtil.convert(model_widget.propFont().getValue()));
            button.setTextFill(foreground);
            button.setStyle(background);

            if (model_widget.propShowLED().getValue())
            {
                led.setVisible(true);
                final int size = Math.min(wid, hei);
                led.setRadiusX(size / 3.7);
                led.setRadiusY(size / 3.7);
            }
            else
            {
                led.setVisible(false);
                // Give all room to label
                led.setRadiusX(0);
                led.setRadiusY(0);
            }
            update_value = true;
        }
        if (dirty_enablement.checkAndClear())
        {
            final boolean enabled = model_widget.propEnabled().getValue()  &&
                                    model_widget.runtimePropPVWritable().getValue();
            button.setDisable(! enabled);
            Styles.update(button, Styles.NOT_ENABLED, !enabled);
        }
        if (update_value)
        {
            button.setText(value_label);
            final ImageView image = value_image;
            if (image == null)
            {
                if (model_widget.propShowLED().getValue())
                {
                    button.setGraphic(led);
                    // Put highlight in top-left corner, about 0.2 wide,
                    // relative to actual size of LED
                    led.setFill(toolkit.isEditMode() ? computeEditColors() : value_color);
                }
                else
                {
                    button.setGraphic(null);
                    button.setStyle(background);
                }
            }
            else
                button.setGraphic(image);
        }
        button.setAlignment(pos);
        button.setTextAlignment(TextAlignment.values()[model_widget.propHorizontalAlignment().getValue().ordinal()]);
    }

    @Override
    protected boolean isFilteringEditModeClicks()
    {
        return true;
    }

    /**
     * Displays an error message. To be called when an unsupported
     * combination of mode and confirmation dialog is selected.
     */
    private void showUnsupportedConfigurationDialog(){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(Messages.BoolButtonError_Title);
        alert.setHeaderText(Messages.BoolButtonError_Body);
        DialogHelper.positionDialog(alert, jfx_node, 25, 25);
        alert.showAndWait();
    }
}

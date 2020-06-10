/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.util.logging.Level;

import javafx.scene.input.MouseButton;
import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.widgets.PVWidget;
import org.csstudio.display.builder.model.widgets.TextEntryWidget;
import org.csstudio.display.builder.representation.javafx.Cursors;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.epics.vtype.VType;
import org.phoebus.ui.javafx.Styles;
import org.phoebus.ui.vtype.FormatOptionHandler;

import javafx.scene.Cursor;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.Region;

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TextEntryRepresentation extends RegionBaseRepresentation<TextInputControl, TextEntryWidget>
{
    /** Is user actively editing the content, so updates should be suppressed?
     *
     *  <p>Only updated on the UI thread,
     *  but also read when receiving new value
     */
    private boolean active = false;

    private final DirtyFlag dirty_size = new DirtyFlag();
    private final DirtyFlag dirty_style = new DirtyFlag();
    private final DirtyFlag dirty_content = new DirtyFlag();
    private final UntypedWidgetPropertyListener sizeListener = this::sizeChanged;
    private final UntypedWidgetPropertyListener styleListener = this::styleChanged;
    private final UntypedWidgetPropertyListener contentListener = this::contentChanged;
    private final WidgetPropertyListener<String> pvNameListener = this::pvnameChanged;
    private volatile String value_text = "<?>";

    private static WidgetColor active_color = WidgetColorService.getColor(NamedWidgetColors.ACTIVE_TEXT);

    @Override
    public TextInputControl createJFXNode() throws Exception
    {
    	value_text = computeText(null);

    	// Note implementation choice:
    	// "multi_line" and "wrap_words" cannot change at runtime.
    	// In editor, there is no visible difference,
    	// and at runtime changes are simply not supported.
    	final TextInputControl text;
        if (model_widget.propMultiLine().getValue())
        {
            final TextArea area = new TextArea();
            area.setWrapText(model_widget.propWrapWords().getValue());
            text = area;
        }
        else
            text = new TextField();
        text.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        text.getStyleClass().add("text_entry");

        if (! toolkit.isEditMode())
        {
            // Initially used 'focus' to activate the widget, but
            // when a display is opened, one of the text fields likely has the focus.
            // That widget will then NOT show any value, because we're active as if the
            // user just navigated into the field to edit it.
            // Now requiring key press, including use of cursor keys, to activate.
            text.setOnKeyPressed(event ->
            {
                switch (event.getCode())
                {
                case TAB:
                    // For multiline, it's like any other entered key.
                    if (model_widget.propMultiLine().getValue()  &&  ! event.isShiftDown())
                        setActive(true);
                    // Otherwise results in lost focus and is handled as thus
                    break;
                case ESCAPE:
                    if (active)
                    {   // Revert original value, leave active state
                        restore();
                        setActive(false);
                    }
                    break;
                case ENTER:
                    // With Java 8, the main keyboard sent 'ENTER',
                    // but the numeric keypad's enter key sent UNDEFINED?!
                    // -> Was handled by checking for char 13 in onKeyTyped handler.
                    // With Java 9, always get ENTER in here,
                    // and _not_ receiving char 13 in onKeyTyped any more,
                    // so all enter keys handled in here.

                    // Single line mode uses plain ENTER.
                    // Multi line mode requires Control or Command-ENTER.
                    if (!isMultiLine()  ||  event.isShortcutDown())
                    {
                        // Submit value, leave active state
                        submit();
                        setActive(false);
                    }
                    break;
                default:
                    // Any other key results in active state
                    setActive(true);
                }
            });
            // Clicking into widget also activates
            text.setOnMouseClicked(event -> {
                // Secondary mouse button should bring up context menu
                // but not enable editing.
                if(event.getButton().equals(MouseButton.PRIMARY)){
                    setActive(true);
                }
                else{
                    text.setEditable(false);
                }
            });
            // While getting the focus does not activate the widget
            // (first need to type something or click),
            // _loosing_ focus de-activates the widget.
            // Otherwise widget where one moves the cursor, then clicks
            // someplace else would remain active and not show any updates
            text.focusedProperty().addListener((prop, old, focused) ->
            {
                if (active  &&  !focused)
                {
                    // For multi-line, submit on exit because users
                    // cannot remember Ctrl-Enter.
                    // For plain text field, require Enter to submit
                    // and cancel editing when focus is lost.
                    if (isMultiLine())
                        submit();
                    else
                        restore();
                    setActive(false);
                }
            });
        }

        // Non-managed widget reduces expensive Node.notifyParentOfBoundsChange() calls.
        // Code below is prepared to handle non-managed widget,
        // but multi-line version behaves oddly when not managed:
        // Cursor not shown, selection not shown,
        // unclear where entered text will appear.
        // Even when _only_ the single-line version is unmanaged,
        // the multi-line version will get into this state.
        // -> Keep managed.
        //        if (!isMultiLine())
        //            text.setManaged(false);

        return text;
    }

    @Override
    protected boolean isFilteringEditModeClicks()
    {
        return true;
    }

    private void setActive(final boolean active)
    {
        if (this.active == active)
            return;

        // When activated, start by selecting all in a plain text.
        // For multi-line, leave it to the user to click or cursor around,
        // because when all is selected, there's a larger risk of accidentally
        // replacing some long, carefully crafted text.
        if (active  &&  !isMultiLine())
            jfx_node.selectAll();

        // Don't enable when widget is disabled
        if (active  &&  !model_widget.propEnabled().getValue())
            return;
        this.active = active;
        dirty_style.mark();
        updateChanges();
    }

    /** @return Using the multi-line TextArea? */
    private boolean isMultiLine()
    {
        return jfx_node instanceof TextArea;
    }

    /** Restore representation to last known value,
     *  replacing what user might have entered
     */
    private void restore()
    {
        jfx_node.setText(value_text);
    }

    /** Submit value entered by user */
    private void submit()
    {
        // Strip 'units' etc. from text
        final String text = jfx_node.getText();

        final Object value = FormatOptionHandler.parse(model_widget.runtimePropValue().getValue(), text,
                                                       model_widget.propFormat().getValue());
        logger.log(Level.FINE, "Writing '" + text + "' as " + value + " (" + value.getClass().getName() + ")");
        toolkit.fireWrite(model_widget, value);

        // Wrote value. Expected is either
        // a) PV receives that value, PV updates to
        //    submitted value or maybe a 'clamped' value
        // --> We'll receive contentChanged() and display PV's latest.
        // b) PV doesn't receive the value and never sends
        //    an update. JFX control is stuck with the 'text'
        //    the user entered, not reflecting the actual PV
        // --> Request an update to the last known 'value_text'.
        //
        // This could result in a little flicker:
        // User enters "new_value".
        // We send that, but restore "old_value" to handle case b)
        // PV finally sends "new_value", and we show that.
        //
        // In practice, this rarely happens because we only schedule an update.
        // By the time it executes, we already have case a.
        // If it does turn into a problem, could introduce toolkit.scheduleDelayedUpdate()
        // so that case b) only restores the old 'value_text' after some delay,
        // increasing the chance of a) to happen.
        dirty_content.mark();
        toolkit.scheduleUpdate(this);
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.propWidth().addUntypedPropertyListener(sizeListener);
        model_widget.propHeight().addUntypedPropertyListener(sizeListener);

        model_widget.propForegroundColor().addUntypedPropertyListener(styleListener);
        model_widget.propBackgroundColor().addUntypedPropertyListener(styleListener);
        model_widget.propFont().addUntypedPropertyListener(styleListener);
        model_widget.propEnabled().addUntypedPropertyListener(styleListener);
        model_widget.runtimePropPVWritable().addUntypedPropertyListener(styleListener);

        model_widget.propFormat().addUntypedPropertyListener(contentListener);
        model_widget.propPrecision().addUntypedPropertyListener(contentListener);
        model_widget.propShowUnits().addUntypedPropertyListener(contentListener);
        model_widget.runtimePropValue().addUntypedPropertyListener(contentListener);

        model_widget.propPVName().addPropertyListener(pvNameListener);

        contentChanged(null, null, null);
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propWidth().removePropertyListener(sizeListener);
        model_widget.propHeight().removePropertyListener(sizeListener);
        model_widget.propForegroundColor().removePropertyListener(styleListener);
        model_widget.propBackgroundColor().removePropertyListener(styleListener);
        model_widget.propFont().removePropertyListener(styleListener);
        model_widget.propEnabled().removePropertyListener(styleListener);
        model_widget.runtimePropPVWritable().removePropertyListener(styleListener);
        model_widget.propFormat().removePropertyListener(contentListener);
        model_widget.propPrecision().removePropertyListener(contentListener);
        model_widget.propShowUnits().removePropertyListener(contentListener);
        model_widget.runtimePropValue().removePropertyListener(contentListener);
        model_widget.propPVName().removePropertyListener(pvNameListener);
        super.unregisterListeners();
    }

    @Override
    protected void attachTooltip()
    {
        // Use the formatted text for "$(pv_value)"
        TooltipSupport.attach(jfx_node, model_widget.propTooltip(), () -> value_text);
    }

    private void sizeChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_size.mark();
        toolkit.scheduleUpdate(this);
    }

    private void styleChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_style.mark();
        toolkit.scheduleUpdate(this);
    }

    /** @param value Current value of PV
     *  @return Text to show, "<pv name>" if disconnected (no value)
     */
    private String computeText(final VType value)
    {
        if (value == null)
            return "<" + model_widget.propPVName().getValue() + ">";
        if (value == PVWidget.RUNTIME_VALUE_NO_PV)
            return "";
        return FormatOptionHandler.format(value,
                                          model_widget.propFormat().getValue(),
                                          model_widget.propPrecision().getValue(),
                                          model_widget.propShowUnits().getValue());
    }

    private void pvnameChanged(final WidgetProperty<String> property, final String old_value, final String new_value)
    {   // PV name typically changes in edit mode.
        // -> Show new PV name.
        // Runtime could deal with disconnect/reconnect for new PV name
        // -> Also OK to show disconnected state until runtime
        //    subscribes to new PV, so we eventually get values from new PV.
        value_text = computeText(null);
        dirty_content.mark();
        toolkit.scheduleUpdate(this);
    }

    private void contentChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        value_text = computeText(model_widget.runtimePropValue().getValue());
        dirty_content.mark();
        if (! active)
            toolkit.scheduleUpdate(this);
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_size.checkAndClear())
        {
            if (jfx_node.isManaged())
                jfx_node.setPrefSize(model_widget.propWidth().getValue(),
                                     model_widget.propHeight().getValue());
            else
                jfx_node.resize(model_widget.propWidth().getValue(),
                                model_widget.propHeight().getValue());
        }
        if (dirty_style.checkAndClear())
        {
            final StringBuilder style = new StringBuilder(100);
            style.append("-fx-text-fill:");
            JFXUtil.appendWebRGB(style, model_widget.propForegroundColor().getValue()).append(";");

            // http://stackoverflow.com/questions/27700006/how-do-you-change-the-background-color-of-a-textfield-without-changing-the-border
            final WidgetColor back_color = active ? active_color : model_widget.propBackgroundColor().getValue();
            style.append("-fx-control-inner-background: ");
            JFXUtil.appendWebRGB(style, back_color).append(";");
            jfx_node.setStyle(style.toString());

            jfx_node.setFont(JFXUtil.convert(model_widget.propFont().getValue()));

            // Enable if enabled by user and there's write access
            final boolean enabled = model_widget.propEnabled().getValue()  &&
                                    model_widget.runtimePropPVWritable().getValue();
            // Don't disable the widget, because that would also remove the
            // context menu etc.
            // Just apply a style that matches the disabled look.
            jfx_node.setEditable(enabled);
            Styles.update(jfx_node, Styles.NOT_ENABLED, !enabled);
            jfx_node.setCursor(enabled ? Cursor.DEFAULT : Cursors.NO_WRITE);
        }
        if (! active)
        {
            if (dirty_content.checkAndClear())
                jfx_node.setText(value_text);
        }
        // When not managed, trigger layout
        if (!jfx_node.isManaged())
            jfx_node.layout();
    }
}

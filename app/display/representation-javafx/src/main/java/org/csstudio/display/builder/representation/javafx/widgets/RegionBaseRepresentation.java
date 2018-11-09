/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBorderAlarmSensitive;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBorderColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBorderWidth;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.runtimePropPVValue;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.widgets.PVWidget;
import org.csstudio.display.builder.model.widgets.VisibleWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.VType;

import javafx.geometry.Insets;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;

/** Base class for all JavaFX widgets that use a {@link Region}-derived JFX node representations
 *
 *  <p>Implements alarm-sensitive border based on the Region's border.
 *
 *  <p>For widgets with "border_alarm_sensitive" and "value" properties,
 *  the border is based on the alarm severity of that value,
 *  which in turn tends to be read from a primary PV.
 *
 *  <p>In addition, the "connected" runtime property is checked,
 *  which is updated by the runtime to indicate if _all_ PVs
 *  associated with the widget are connected.
 *  Whenever any PV is disconnected, the border reflects that.
 *
 *  @author Kay Kasemir
 */
abstract public class RegionBaseRepresentation<JFX extends Region, MW extends VisibleWidget> extends JFXBaseRepresentation<JFX, MW>
{
    // Like BorderStrokeStyle.SOLID except for OUTSIDE
    private static final BorderStrokeStyle solid =
        new BorderStrokeStyle(StrokeType.OUTSIDE, StrokeLineJoin.MITER, StrokeLineCap.BUTT, 10, 0, null);

    private static final BorderStrokeStyle dotted =
        new BorderStrokeStyle(StrokeType.OUTSIDE, StrokeLineJoin.MITER, StrokeLineCap.BUTT, 10, 0,
                              Collections.unmodifiableList(Arrays.asList(2.0, 2.0)));

    private static final BorderStrokeStyle dash_dotted =
            new BorderStrokeStyle(StrokeType.OUTSIDE, StrokeLineJoin.MITER, StrokeLineCap.BUTT, 10, 0,
                                  Collections.unmodifiableList(Arrays.asList(8.0, 2.0, 2.0, 2.0)));

    private static final BorderWidths thin = new BorderWidths(1);
    private static final BorderWidths normal = new BorderWidths(2);
    private static final BorderWidths wide = new BorderWidths(4);

    /** Colors used for the various alarm severities */
    protected static final Color[] alarm_colors = new Color[AlarmSeverity.values().length];

    /** Common border for each {@link AlarmSeverity} when not using custom radii */
    private static final Border[] alarm_borders = new Border[AlarmSeverity.values().length];

    /** Prepare alarm_borders
     *
     *  <p>Alarm borders are distinguished by color as well as style in case of color vision deficiency.
     *
     *  <p>They are drawn OUTSIDE the widget, so adding/removing border does not change the layout
     *  of the region's content.
     */
    static
    {
        alarm_colors[AlarmSeverity.NONE.ordinal()] = null;
        alarm_colors[AlarmSeverity.MINOR.ordinal()] = JFXUtil.convert(WidgetColorService.getColor(NamedWidgetColors.ALARM_MINOR));
        alarm_colors[AlarmSeverity.MAJOR.ordinal()] =  JFXUtil.convert(WidgetColorService.getColor(NamedWidgetColors.ALARM_MAJOR));
        alarm_colors[AlarmSeverity.INVALID.ordinal()] =  JFXUtil.convert(WidgetColorService.getColor(NamedWidgetColors.ALARM_INVALID));
        alarm_colors[AlarmSeverity.UNDEFINED.ordinal()] =  JFXUtil.convert(WidgetColorService.getColor(NamedWidgetColors.ALARM_DISCONNECTED));

        for (AlarmSeverity severity : AlarmSeverity.values())
            alarm_borders[severity.ordinal()] = createAlarmBorder(severity, CornerRadii.EMPTY);
    }

    private final DirtyFlag dirty_border = new DirtyFlag();
    private volatile WidgetProperty<VType> value_prop = null;
    private volatile WidgetProperty<Boolean> alarm_sensitive_border_prop = null;
    private volatile WidgetProperty<WidgetColor> border_color_prop = null;
    private volatile WidgetProperty<Integer> border_width_prop = null;
    private final AtomicReference<AlarmSeverity> current_alarm = new AtomicReference<>(AlarmSeverity.NONE);
    private volatile Border alarm_border = null, custom_border = null;


    /** Create alarm-based border
     *  @param severity AlarmSeverity
     *  @param corners CornerRadii
     *  @return Border
     */
    private static Border createAlarmBorder(final AlarmSeverity severity, final CornerRadii corners)
    {
        switch (severity)
        {
        case NONE:
            // No alarm -> no border
            return null;
        case MINOR:
            // Minor -> Simple border
            return new Border(new BorderStroke(alarm_colors[severity.ordinal()], solid, corners, normal));
        case MAJOR:
            // Major -> Double border
            return new Border(new BorderStroke(alarm_colors[severity.ordinal()], solid, corners, thin),
                              new BorderStroke(alarm_colors[severity.ordinal()], solid, corners, thin, new Insets(-2*thin.getTop())));
        case INVALID:
            // Invalid -> Border is cleverly interrupted just like the communication to the control system
            return new Border(new BorderStroke(alarm_colors[severity.ordinal()], dash_dotted, corners, normal));
        case UNDEFINED:
        default:
            // Disconnected -> Dotted
            return new Border(new BorderStroke(alarm_colors[severity.ordinal()], dotted, corners, wide));
        }
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();

        // Does widget have a custom border?
        final Optional<WidgetProperty<WidgetColor>> cust_col = model_widget.checkProperty(propBorderColor);
        final Optional<WidgetProperty<Integer>> cust_wid = model_widget.checkProperty(propBorderWidth);
        if (cust_col.isPresent()  &&  cust_wid.isPresent())
        {
            border_color_prop = cust_col.get();
            border_width_prop = cust_wid.get();
            border_color_prop.addUntypedPropertyListener(this::custom_border_changed);
            border_width_prop.addUntypedPropertyListener(this::custom_border_changed);
            custom_border_changed(null, null, null);
        }

        if (toolkit.isEditMode())
            return;

        // In runtime mode, handle alarm-sensitive border
        final Optional<WidgetProperty<Boolean>> alarm_sens = model_widget.checkProperty(propBorderAlarmSensitive);
        final Optional<WidgetProperty<VType>> value = model_widget.checkProperty(runtimePropPVValue);
        if (alarm_sens.isPresent()  &&  value.isPresent())
        {
            value_prop = value.get();
            alarm_sensitive_border_prop = alarm_sens.get();
            // runtimeValue should be a VType,
            // but some widgets may allow other data types (Table),
            // so use Object and then check for VType
            value_prop.addUntypedPropertyListener(this::connectionOrValueChanged);
        }

        // Indicate 'disconnected' state
        model_widget.runtimePropConnected().addUntypedPropertyListener(this::connectionOrValueChanged);

        // Allow middle-button click to copy PV name
        if (model_widget instanceof PVWidget)
            jfx_node.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> hookMiddleButtonCopy(event));
    }

    /** Copy PV name to clipboard when middle button clicked
     *  @param event Mouse pressed event
     */
    private void hookMiddleButtonCopy(final MouseEvent event)
    {
        if (event.getButton() != MouseButton.MIDDLE)
            return;

        final String pv_name = ((PVWidget)model_widget).propPVName().getValue();

        // Copy to copy/paste clipboard
        final ClipboardContent content = new ClipboardContent();
        content.putString(pv_name);
        Clipboard.getSystemClipboard().setContent(content);

        // Copy to the 'selection' buffer used by X11
        // for middle-button copy/paste
        // Note: This is AWT API!
        // JavaFX has no API, https://bugs.openjdk.java.net/browse/JDK-8088117
        Toolkit.getDefaultToolkit().getSystemSelection().setContents(new StringSelection(pv_name), null);
    }

    private void custom_border_changed(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        final Integer width = border_width_prop.getValue();
        if (width <= 0)
            custom_border = null;
        else
        {
            final Color color = JFXUtil.convert(border_color_prop.getValue());
            final CornerRadii corners = computeCornerRadii();
            custom_border = new Border(new BorderStroke(color, solid, corners, new BorderWidths(width)));
        }
        dirty_border.mark();
        toolkit.scheduleUpdate(this);
    }

    private void connectionOrValueChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        // Hope for the best
        AlarmSeverity severity = AlarmSeverity.NONE;

        // Ignore custom border and value of primary PV, show disconnected
        if (! model_widget.runtimePropConnected().getValue())
            severity = AlarmSeverity.UNDEFINED;
        else
        {   // Reflect severity of primary PV's value
            if (value_prop != null  && alarm_sensitive_border_prop.getValue())
            {
                final Object value = value_prop.getValue();
                final Alarm alarm = Alarm.alarmOf(value);
                if (alarm != null  &&  alarm.getSeverity() != AlarmSeverity.NONE)
                    // Have alarm info
                    severity = alarm.getSeverity();
                else if (value instanceof VType)
                    // VType that doesn't provide alarm, always OK
                    severity = AlarmSeverity.NONE;
                else if (value != null)
                    // Not a vtype, but non-null, assume OK
                    severity = AlarmSeverity.NONE;
                else // null
                    severity = AlarmSeverity.UNDEFINED;
            }
        }

        // Any change?
        if (current_alarm.getAndSet(severity) == severity)
            return;

        final CornerRadii corners = computeCornerRadii();
        if (corners == CornerRadii.EMPTY)
            // Use common alarm border
            alarm_border = alarm_borders[severity.ordinal()];
        else
            // Create a custom alarm border
            alarm_border = createAlarmBorder(severity, corners);

        dirty_border.mark();
        toolkit.scheduleUpdate(this);
    }

    private CornerRadii computeCornerRadii()
    {
        final int[] radii = getBorderRadii();
        if (radii == null)
            return CornerRadii.EMPTY;
        final int horiz = radii[0], vert = radii[1];
        // There's a bug in CornerRadii:
        // Even though horiz != vert, it considers them all 'uniform'
        // because it _separately_ compares all the horizontal and vertical radii,
        // never checking if horiz == vert.
        // Workaround: Make one of the horiz or vert radii a little different (+0.1).
        // Bug was in at least Java 1.8.0_101.
        return new CornerRadii(horiz, vert, vert, horiz, horiz, vert, vert, horiz+0.1,
                               false, false, false, false, false, false, false, false);
    }

    /** Get radii for the border of the widget.
     *
     *  <p>Default implementation returns <code>null</code>,
     *  i.e. no border radii, to use the shared default alarm
     *  border.
     *
     *  <p>Derived class can override to request alarm border
     *  which follows the border of the widget.
     *
     *  @return Array with horizontal and vertical border radius
     */
    public int[] getBorderRadii()
    {
        return null;
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_border.checkAndClear())
        {
            // If there's an alarm border, set it.
            // Otherwise, use custom border,
            // but leave property untouched if there's no change.
            // For the hopefully common case of never setting any border,
            // this avoids even setting it to 'null' and marking it as set-by-user.
            if (alarm_border != null)
                jfx_node.setBorder(alarm_border);
            else if (! Objects.equals(custom_border, jfx_node.getBorder()))
                jfx_node.setBorder(custom_border);
        }
    }
}

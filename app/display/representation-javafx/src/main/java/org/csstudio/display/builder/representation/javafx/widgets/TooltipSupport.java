/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.runtimePropPVValue;
import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.csstudio.display.builder.model.MacroizedWidgetProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.properties.StringWidgetProperty;
import org.csstudio.display.builder.representation.Preferences;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.Time;
import org.phoebus.framework.macros.MacroHandler;
import org.phoebus.framework.macros.MacroValueProvider;
import org.phoebus.util.time.TimestampFormats;

import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

/** Support for tooltips
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TooltipSupport
{
    /** Instead of tracking the tool tip attached to a node ourselves,
     *  we use this property that JavaFX 8+ happens to set,
     *  defined in Tooltip.TOOLTIP_PROP_KEY.
     *  If the JFX implementation changes, we'll need use our own property key.
     */
    private static final String TOOLTIP_PROP_KEY = "javafx.scene.control.Tooltip";

    /** Legacy tool tip: "$(pv_name)\n$(pv_value)" where number of '\n' can vary */
    private static final Pattern legacy_tooltip = Pattern.compile("\\$\\(pv_name\\)\\s*\\$\\(pv_value\\)");

    /** System property to disable tool tips (for debugging problems seen on Linux) */
    private static boolean disable_tooltips = Boolean.parseBoolean(System.getProperty("org.csstudio.display.builder.disable_tooltips"));

    /** Attach tool tip
     *  @param node Node that should have the tool tip
     *  @param tooltip_property Tool tip to show
     */
    public static void attach(final Node node, final WidgetProperty<String> tooltip_property)
    {
        attach(node, tooltip_property, null);
    }

    /** Attach tool tip
     *  @param node Node that should have the tool tip
     *  @param tooltip_property Tool tip to show
     *  @param pv_value Supplier of formatted "$(pv_value)". <code>null</code> to use toString of the property.
     */
    public static void attach(final Node node, final WidgetProperty<String> tooltip_property, final Supplier<String> pv_value)
    {
        if (disable_tooltips)
            return;

        // Patch legacy tool tips that defaulted to pv name & value,
        // even for static widgets
        final StringWidgetProperty ttp = (StringWidgetProperty)tooltip_property;
        if (legacy_tooltip.matcher(ttp.getSpecification()).matches()  &&
            ! tooltip_property.getWidget().checkProperty("pv_name").isPresent())
            ttp.setSpecification("");

        // Suppress tool tip if _initial_ text is empty.
        // In case a script changes the tool tip at runtime,
        // tool tip must have some initial non-empty value.
        // This was done for optimization:
        // Avoid listener and code to remove/add tooltip at runtime.
        if (tooltip_property.getValue().isEmpty())
            return;

        final Tooltip tooltip = new Tooltip();
        tooltip.setWrapText(true);
        // Evaluate the macros in tool tip specification each time
        // the tool tip is about to show
        tooltip.setOnShowing(event ->
        {
            String spec = ((MacroizedWidgetProperty<?>)tooltip_property).getSpecification();

            // Use custom supplier for $(pv_value)?
            // Otherwise replace like other macros, i.e. use toString of the property
            if (pv_value != null)
            {
                final StringBuilder buf = new StringBuilder();
                buf.append(pv_value.get());

                final Object vtype = tooltip_property.getWidget().getPropertyValue(runtimePropPVValue);
                final Alarm alarm = Alarm.alarmOf(vtype);
                if (alarm != null  &&  alarm.getSeverity() != AlarmSeverity.NONE)
                    buf.append(", ").append(alarm.getSeverity()).append(" - ").append(alarm.getName());

                final Time time = Time.timeOf(vtype);
                if (time != null)
                    buf.append(", ").append(TimestampFormats.FULL_FORMAT.format(time.getTimestamp()));

                spec = spec.replace("$(pv_value)", buf.toString());
            }
            final Widget widget = tooltip_property.getWidget();
            final MacroValueProvider macros = widget.getMacrosOrProperties();
            String expanded;
            try
            {
                expanded = MacroHandler.replace(macros, spec);
                if (expanded.length() > Preferences.tooltip_length)
                    expanded = expanded.substring(0, Preferences.tooltip_length) + "...";
                tooltip.setText(expanded);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot evaluate tooltip of " + widget, ex);
                tooltip.setText(spec);
            }
        });

        // Show after 250 instead of 1000 ms
        tooltip.setShowDelay(Duration.millis(250));

        // Hide after 30 instead of 5 secs
        tooltip.setShowDuration(Duration.seconds(30));

        Tooltip.install(node, tooltip);
        if (node.getProperties().get(TOOLTIP_PROP_KEY) != tooltip)
            throw new IllegalStateException("JavaFX Tooltip behavior changed");
    }

    /** Detach tool tip.
     *  @param node Node that should have the tool tip removed.
     */
    public static void detach(final Node node)
    {
        if (disable_tooltips)
            return;
        final Tooltip tooltip = (Tooltip) node.getProperties().get(TOOLTIP_PROP_KEY);
        if (tooltip != null)
            Tooltip.uninstall(node, tooltip);
    }
}

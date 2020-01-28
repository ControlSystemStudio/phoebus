/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import static org.csstudio.display.converter.edm.Converter.logger;

import java.util.List;
import java.util.logging.Level;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.properties.ActionInfos;
import org.csstudio.display.builder.model.properties.ConfirmDialog;
import org.csstudio.display.builder.model.properties.WritePVActionInfo;
import org.csstudio.display.builder.model.widgets.ActionButtonWidget;
import org.csstudio.display.builder.model.widgets.BoolButtonWidget;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.EdmWidget;
import org.csstudio.opibuilder.converter.model.Edm_activeMessageButtonClass;

/** Convert an EDM widget into Display Builder counterpart
 *  @author Kay Kasemir
 *  @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 */
@SuppressWarnings("nls")
public class Convert_activeMessageButtonClass extends ConverterBase<Widget>
{
    // No perfect match.
    //
    // When EDM message button is 'toggle', we use a BoolButtonWidget that writes 0, 1.
    // Cannot write arbitrary values.
    //
    // When EDM message button is 'push', we use ActionButtonWidget that writes the 'push' value,
    // falling back to the 'release' value, but can't write both.
    public Convert_activeMessageButtonClass(final EdmConverter converter, final Widget parent, final Edm_activeMessageButtonClass mb)
    {
        super(converter, parent, mb);

        if (is_boolean(mb))
        {
            // Create bool button that writes 0/1
            final BoolButtonWidget b = (BoolButtonWidget) widget;
            b.propShowLED().setValue(false);
            b.propOnLabel().setValue(mb.getOnLabel());
            b.propOffLabel().setValue(mb.getOffLabel());
            convertColor(mb.getOnColor(), b.propOnColor());
            convertColor(mb.getOffColor(), b.propOffColor());
            convertColor(mb.getFgColor(), b.propForegroundColor());
            convertFont(mb.getFont(), b.propFont());

            // EDM MB has no alarms sensitive border
            b.propBorderAlarmSensitive().setValue(false);

            if (mb.getPassword() != null)
            {
                b.propConfirmDialog().setValue(ConfirmDialog.BOTH);
                b.propPassword().setValue(mb.getPassword());
            }

            final String pv = convertPVName(mb.getControlPv());
            b.propPVName().setValue(pv);

            if (mb.isToggle())
            {
                b.propMode().setValue(BoolButtonWidget.Mode.TOGGLE);
                // Toggle will always write 1 in 'on', 0 in 'off' state.
                // If EDM widget reversed 1/0, swap the labels and colors.
                if ("0".equals(mb.getPressValue()))
                {
                    b.propOffLabel().setValue(mb.getOnLabel());
                    b.propOnLabel().setValue(mb.getOffLabel());
                    convertColor(mb.getOffColor(), b.propOnColor());
                    convertColor(mb.getOnColor(), b.propOffColor());
                }
            }
            else
                if ("0".equals(mb.getPressValue())  &&
                    "1".equals(mb.getReleaseValue()))
                    b.propMode().setValue(BoolButtonWidget.Mode.PUSH_INVERTED);
                else
                    b.propMode().setValue(BoolButtonWidget.Mode.PUSH);
        }
        else
        {
            // Create action button that writes a value on 'click'
            final ActionButtonWidget b = (ActionButtonWidget) widget;
            convertColor(mb.getOnColor(), b.propBackgroundColor());
            convertColor(mb.getFgColor(), b.propForegroundColor());
            convertFont(mb.getFont(), b.propFont());
            // Show the 'off' label in the idle state (with fallbacks).
            // When pressed, EDM would briefly show the 'on' label; we don't.
            String desc = mb.getOffLabel();
            if (desc == null)
                desc = mb.getOnLabel();
            if (desc == null)
                desc = "Write";
            b.propText().setValue(desc);

            if (mb.getControlPv() == null)
                logger.log(Level.WARNING, "Message button '" + desc + "' without PV");
            else
            {
                final String pv = convertPVName(mb.getControlPv());
                // Write a value when clicked.
                // Ordinarily, that's the 'press' value.
                // Could also be the 'release' value,
                // but can only be one of them.
                final boolean have_press_value   = mb.getPressValue()   != null  &&  !mb.getPressValue().isEmpty();
                final boolean have_release_value = mb.getReleaseValue() != null  &&  !mb.getReleaseValue().isEmpty();

                final String value;
                if (have_press_value)
                {
                    value = mb.getPressValue();
                    // If there is a release value, warn that it's ignored.
                    // OK to skip a release value that matches the press value,
                    // since we wrote it on press.
                    if (have_release_value  &&  !mb.getReleaseValue().equals(mb.getPressValue()))
                        logger.log(Level.WARNING, "Cannot convert EDM message 'push' button '" + desc + "' for release message '" + mb.getReleaseValue() +
                                "', will only write the 'press' message " + pv + " = '" + mb.getPressValue() + "'");
                }
                else if (have_release_value)
                    value = mb.getReleaseValue();
                else
                {
                    value = "";
                    logger.log(Level.WARNING, "EDM message 'push' button '" + desc + "' lacks both 'press' and 'release'; writing empty string");
                }

                b.propActions().setValue(new ActionInfos(List.of(new WritePVActionInfo(desc, pv, value))));
            }

            if (mb.getPassword() != null)
            {
                b.propConfirmDialog().setValue(true);
                b.propPassword().setValue(mb.getPassword());
            }

            // Turn invisible EDM button into transparent, no text action button
            if (mb.isInvisible())
            {
                b.propBackgroundColor().setValue(NamedWidgetColors.TRANSPARENT);
                b.propText().setValue("");
                b.propTransparent().setValue(true);
            }
        }
    }

    @Override
    protected Widget createWidget(final EdmWidget edm)
    {
        final Edm_activeMessageButtonClass mb = (Edm_activeMessageButtonClass) edm;
        if (is_boolean(mb))
            return new BoolButtonWidget();
        else
            return new ActionButtonWidget();
    }

    private boolean is_boolean(final Edm_activeMessageButtonClass mb)
    {
        // When EDM button writes 1/0 or 0/1, use a BoolButtonWidget
        return ("1".equals(mb.getPressValue()) && "0".equals(mb.getReleaseValue()))
               ||
               ("0".equals(mb.getPressValue()) && "1".equals(mb.getReleaseValue()));
    }
}

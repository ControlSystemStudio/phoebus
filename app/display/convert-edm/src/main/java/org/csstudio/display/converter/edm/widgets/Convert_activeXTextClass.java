/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.HorizontalAlignment;
import org.csstudio.display.builder.model.properties.VerticalAlignment;
import org.csstudio.display.builder.model.widgets.LabelWidget;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.EdmWidget;
import org.csstudio.opibuilder.converter.model.Edm_activeXTextClass;

/** Convert an EDM widget into Display Builder counterpart
 *  @author Kay Kasemir
 *  @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 */
@SuppressWarnings("nls")
public class Convert_activeXTextClass extends ConverterBase<LabelWidget>
{
    public Convert_activeXTextClass(final EdmConverter converter, final Widget parent, final Edm_activeXTextClass t)
    {
        super(converter, parent, t);

        widget.propTransparent().setValue(t.getAttribute("useDisplayBg").isExistInEDL() && t.isUseDisplayBg());

        // EDM uses '\r' as well as '\001' as line delimiter
        // EDM text that has been 'manually aligned' with leading/trailing spaces
        // often exceeds the widget size and doesn't show up at all  -> trim/strip.
        // If text should e.g. be centered, configure EDM label align="center"
        // instead of manually padding with spaces.
        widget.propText().setValue(t.getValue().get().replace('\001', '\n').replace('\r', '\n').strip());

        // Remove 2 pixels from height for each line, then find font that 'fits'
        final int lines = textLineCount(widget.propText().getValue());
        final int font_lim = (widget.propHeight().getValue()-2*lines) / lines;
        convertFont(t.getFont(), font_lim, widget.propFont());

        if (t.getAttribute("autoSize").isExistInEDL() && t.isAutoSize())
        {   // Autosize 'shrinks' the size; leave alignment left & top
            widget.propAutoSize().setValue(true);
        }
        else
        {   // Honor alignment settings
            widget.propVerticalAlignment().setValue(VerticalAlignment.MIDDLE);
            if ("right".equals(t.getFontAlign()))
                widget.propHorizontalAlignment().setValue(HorizontalAlignment.RIGHT);
            else if ("center".equals(t.getFontAlign()))
                widget.propHorizontalAlignment().setValue(HorizontalAlignment.CENTER);
        }

        // Alarm-sensitive color? Else use (optionally dynamic) color
        if (t.isBgAlarm())
            createAlarmColor(t.getAlarmPv(), widget.propBackgroundColor());
        else
            convertColor(t.getBgColor(), t.getAlarmPv(), widget.propBackgroundColor());
        if (t.isFgAlarm())
            createAlarmColor(t.getAlarmPv(), widget.propForegroundColor());
        else
            convertColor(t.getFgColor(), t.getAlarmPv(), widget.propForegroundColor());
    }

    /** @param text Text with potential newlines
     *  @return Number of lines in text, at least 1
     */
    public static int textLineCount(final String text)
    {
        int count = 1;
        int next = 0;
        while ((next = text.indexOf('\n', next) + 1) > 0)
            ++count;
        return count;
    }

    @Override
    protected LabelWidget createWidget(final EdmWidget edm)
    {
        return new LabelWidget();
    }
}

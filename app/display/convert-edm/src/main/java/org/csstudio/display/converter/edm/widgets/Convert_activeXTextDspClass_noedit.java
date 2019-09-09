/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import static org.csstudio.display.converter.edm.Converter.logger;

import java.util.logging.Level;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.HorizontalAlignment;
import org.csstudio.display.builder.model.properties.VerticalAlignment;
import org.csstudio.display.builder.model.widgets.TextUpdateWidget;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.EdmWidget;
import org.csstudio.opibuilder.converter.model.Edm_activeXTextDspClass_noedit;
import org.phoebus.ui.vtype.FormatOption;

/** Convert an EDM widget into Display Builder counterpart
 *  @author Kay Kasemir
 *  @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 */
@SuppressWarnings("nls")
public class Convert_activeXTextDspClass_noedit extends ConverterBase<TextUpdateWidget>
{
    public Convert_activeXTextDspClass_noedit(final EdmConverter converter, final Widget parent, final Edm_activeXTextDspClass_noedit r)
    {
        super(converter, parent, r);

        convertColor(r.getBgColor(), widget.propBackgroundColor());
        convertColor(r.getFgColor(), widget.propForegroundColor());
        convertFont(r.getFont(), widget.propFont());
        widget.propTransparent().setValue(r.isTransparent());

        if (r.getAttribute("controlPv").isExistInEDL())
            widget.propPVName().setValue(convertPVName(r.getControlPv()));
        else
        {
            logger.log(Level.WARNING, "Hiding Convert_activeXTextDspClass_noedit (Text Update) without PV");
            widget.propVisible().setValue(false);
        }

        if (! r.isLimitsFromDb()  && r.getAttribute("precision").isExistInEDL())
            widget.propPrecision().setValue(r.getPrecision());
        widget.propShowUnits().setValue(r.isShowUnits());

        if ("right".equals(r.getFontAlign()))
            widget.propHorizontalAlignment().setValue(HorizontalAlignment.RIGHT);
        else if ("center".equals(r.getFontAlign()))
            widget.propHorizontalAlignment().setValue(HorizontalAlignment.CENTER);
        widget.propVerticalAlignment().setValue(VerticalAlignment.MIDDLE);

        if (r.getFormat() != null)
        {
            if (r.getFormat().equals("exponential"))
                widget.propFormat().setValue(FormatOption.EXPONENTIAL);
            else if (r.getFormat().equals("decimal"))
                widget.propFormat().setValue(FormatOption.DECIMAL);
            else if (r.getFormat().equals("hex"))
                widget.propFormat().setValue(FormatOption.HEX);
            else if (r.getFormat().equals("string"))
                widget.propFormat().setValue(FormatOption.STRING);
        }
    }

    @Override
    protected TextUpdateWidget createWidget(final EdmWidget edm)
    {
        return new TextUpdateWidget();
    }
}

/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
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

        convertColor(t.getBgColor(), widget.propBackgroundColor());
        convertColor(t.getFgColor(), widget.propForegroundColor());

        convertFont(t.getFont(), widget.propFont());
        widget.propTransparent().setValue(t.getAttribute("useDisplayBg").isExistInEDL() && t.isUseDisplayBg());

        widget.propText().setValue(t.getValue().get());
        widget.propAutoSize().setValue(t.getAttribute("autoSize").isExistInEDL() && t.isAutoSize());

        widget.propVerticalAlignment().setValue(VerticalAlignment.MIDDLE);
        if ("right".equals(t.getFontAlign()))
            widget.propHorizontalAlignment().setValue(HorizontalAlignment.RIGHT);
        else if ("center".equals(t.getFontAlign()))
            widget.propHorizontalAlignment().setValue(HorizontalAlignment.CENTER);

        // TODO See Opi_activeXTextClass for alarm rules
    }

    @Override
    protected LabelWidget createWidget(final EdmWidget edm)
    {
        return new LabelWidget();
    }
}

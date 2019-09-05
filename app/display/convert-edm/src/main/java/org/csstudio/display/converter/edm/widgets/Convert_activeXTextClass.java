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
import org.csstudio.display.builder.model.widgets.LabelWidget;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.Edm_activeXTextClass;

@SuppressWarnings("nls")
public class Convert_activeXTextClass extends ConverterBase<LabelWidget>
{
    public Convert_activeXTextClass(final EdmConverter converter, final Widget parent, final Edm_activeXTextClass t)
    {
        super(converter, parent, t);

        widget.propText().setValue(t.getValue().get());

        widget.propAutoSize().setValue(t.getAttribute("autoSize").isExistInEDL() && t.isAutoSize());
        widget.propTransparent().setValue(t.getAttribute("useDisplayBg").isExistInEDL() && t.isUseDisplayBg());

        if (t.getFontAlign().equals("right"))
            widget.propHorizontalAlignment().setValue(HorizontalAlignment.RIGHT);
        else if (t.getFontAlign().equals("center"))
            widget.propHorizontalAlignment().setValue(HorizontalAlignment.CENTER);

        // TODO See Opi_activeXTextClass for alarm rules
    }

    @Override
    protected LabelWidget createWidget()
    {
        return new LabelWidget();
    }
}

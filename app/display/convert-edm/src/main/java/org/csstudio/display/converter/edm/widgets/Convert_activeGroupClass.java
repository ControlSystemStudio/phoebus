/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.widgets.GroupWidget;
import org.csstudio.display.builder.model.widgets.GroupWidget.Style;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.EdmWidget;
import org.csstudio.opibuilder.converter.model.Edm_activeGroupClass;

/** Convert an EDM widget into Display Builder counterpart
 *  @author Kay Kasemir
 *  @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 */
@SuppressWarnings("nls")
public class Convert_activeGroupClass extends ConverterBase<GroupWidget>
{
    static final String GROUP_NAME = "EDM Group ";

    public Convert_activeGroupClass(final EdmConverter converter, final Widget parent, final Edm_activeGroupClass g)
    {
        super(converter, parent, g);

        // Group name numbers groups within a file,
        // in case this file is used by the symbol widget
        widget.propName().setValue(GROUP_NAME + converter.nextGroup());

        widget.propStyle().setValue(Style.NONE);
        widget.propTransparent().setValue(true);

        // Expand size by 1px to prevent cropping
        widget.propWidth().setValue(widget.propWidth().getValue() + 1);
        widget.propHeight().setValue(widget.propHeight().getValue() + 1);

        // Add and later remove the container offset
        converter.addPositionOffset(widget.propX().getValue(), widget.propY().getValue());
        try
        {
            for (EdmWidget c : g.getWidgets())
                converter.convertWidget(widget, c);
            converter.correctChildWidgets(widget);
        }
        finally
        {
            converter.addPositionOffset(-widget.propX().getValue(), -widget.propY().getValue());
        }
    }

    @Override
    protected GroupWidget createWidget(final EdmWidget edm)
    {
        return new GroupWidget();
    }
}

/*******************************************************************************
 * Copyright (c) 2019-2022 Oak Ridge National Laboratory.
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

        // EDM group content uses absolute screen locations,
        // while the display builder places it relative to the group,
        // so content X,Y = 0,0 appears at the group's upper left corner.
        // To correct the group member locations, including recursive sub-groups,
        // a converter 'offset' is subtracted from group content.
        //
        // For some graphics like arcs, EDM will allow the line width to grow outside
        // of the widget outline, while the display builder keeps the line inside
        // the widget outline, basically moving the widget by half the line width.
        //
        // In combination, an EDM line element at the upper left edge of a group
        // will then result in negative X, Y coordinates relative to the group,
        // but negative X, Y is not permitted.
        //
        // We thus add a BUFFER to the EDM group outline to provide room for lines
        // outside the original group outline.
        // A BUFFER of 5 allows for line widths of up to 10.
        // This does enlarge all groups, but since EDM groups are only used for
        // widget organization without visual elements like border etc,
        // a change in group size compared to EDM is accepted.
        final int BUFFER = 5;
        final int x = widget.propX().getValue() - BUFFER;
        final int y = widget.propY().getValue() - BUFFER;
        widget.propX().setValue(x);
        widget.propY().setValue(y);

        // Expand size by 1px to prevent cropping, by BUFFER to allow for wider line content
        widget.propWidth().setValue(widget.propWidth().getValue() + 2*BUFFER);
        widget.propHeight().setValue(widget.propHeight().getValue() + 2*BUFFER);

        // Add and later remove the container offset
        converter.addPositionOffset(x, y);
        try
        {
            for (EdmWidget c : g.getWidgets())
                converter.convertWidget(widget, c);
            converter.correctChildWidgets(widget);
        }
        finally
        {
            converter.addPositionOffset(-x, -y);
        }
    }

    @Override
    protected GroupWidget createWidget(final EdmWidget edm)
    {
        return new GroupWidget();
    }
}

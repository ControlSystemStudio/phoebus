/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import java.util.ArrayList;
import java.util.List;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.properties.ActionInfos;
import org.csstudio.display.builder.model.properties.ExecuteCommandActionInfo;
import org.csstudio.display.builder.model.widgets.ActionButtonWidget;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.EdmString;
import org.csstudio.opibuilder.converter.model.EdmWidget;
import org.csstudio.opibuilder.converter.model.Edm_shellCmdClass;

/** Convert an EDM widget into Display Builder counterpart
 *  @author Kay Kasemir
 *  @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 */
@SuppressWarnings("nls")
public class Convert_shellCmdClass extends ConverterBase<ActionButtonWidget>
{
    public Convert_shellCmdClass(final EdmConverter converter, final Widget parent, final Edm_shellCmdClass t)
    {
        super(converter, parent, t);

        if (t.isInvisible())
        {
            widget.propBackgroundColor().setValue(NamedWidgetColors.TRANSPARENT);
            widget.propTransparent().setValue(true);
        }
        else
            convertColor(t.getBgColor(), widget.propBackgroundColor());
        convertColor(t.getFgColor(), widget.propForegroundColor());
        convertFont(t.getFont(), widget.propFont());

        List<ActionInfo> actions = new ArrayList<>();
        for (int i=0; i<t.getNumCmds(); ++i)
        {
            final String is = Integer.toString(i);
            final EdmString menuLabel = t.getCommandLabel().getEdmAttributesMap().get(is);
            final String description = menuLabel != null ? menuLabel.get() : "";

            String command = t.getCommand().getEdmAttributesMap().get(is).get();
            if (command.endsWith("&"))
                command = command.substring(0, command.length()-1).trim();

            actions.add(new ExecuteCommandActionInfo(description, command));
        }
        widget.propActions().setValue(new ActionInfos(actions));

        if (t.getButtonLabel() != null  &&  !t.isInvisible())
            widget.propText().setValue(t.getButtonLabel());
        else
            widget.propText().setValue("");
    }

    @Override
    protected ActionButtonWidget createWidget(final EdmWidget edm)
    {
        return new ActionButtonWidget();
    }
}

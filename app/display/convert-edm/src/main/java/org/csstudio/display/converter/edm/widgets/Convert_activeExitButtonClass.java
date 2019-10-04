/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import java.util.Collections;
import java.util.List;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ActionInfos;
import org.csstudio.display.builder.model.properties.ExecuteScriptActionInfo;
import org.csstudio.display.builder.model.properties.ScriptInfo;
import org.csstudio.display.builder.model.widgets.ActionButtonWidget;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.EdmWidget;
import org.csstudio.opibuilder.converter.model.Edm_activeExitButtonClass;

/** Convert an EDM widget into Display Builder counterpart
 *  @author Kay Kasemir
 *  @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 */
@SuppressWarnings("nls")
public class Convert_activeExitButtonClass extends ConverterBase<ActionButtonWidget>
{
    private static final String exit =
        "from org.csstudio.display.builder.runtime.script import ScriptUtil\n" +
        "ScriptUtil.closeDisplay(widget)\n";
    private static final ScriptInfo script = new ScriptInfo(ScriptInfo.EMBEDDED_PYTHON, exit, false, Collections.emptyList());

    public Convert_activeExitButtonClass(final EdmConverter converter, final Widget parent, final Edm_activeExitButtonClass t)
    {
        super(converter, parent, t);
        convertColor(t.getBgColor(), widget.propBackgroundColor());
        convertColor(t.getFgColor(), widget.propForegroundColor());
        convertFont(t.getFont(), widget.propFont());
        widget.propText().setValue(t.getLabel());
        widget.propActions().setValue(new ActionInfos(List.of(new ExecuteScriptActionInfo("close", script))));
    }

    @Override
    protected ActionButtonWidget createWidget(final EdmWidget edm)
    {
        return new ActionButtonWidget();
    }
}

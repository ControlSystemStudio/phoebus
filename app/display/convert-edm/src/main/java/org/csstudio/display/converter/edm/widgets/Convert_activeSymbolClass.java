/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import static org.csstudio.display.converter.edm.Converter.logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ScriptInfo;
import org.csstudio.display.builder.model.properties.ScriptPV;
import org.csstudio.display.builder.model.widgets.EmbeddedDisplayWidget;
import org.csstudio.display.builder.model.widgets.EmbeddedDisplayWidget.Resize;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.EdmDouble;
import org.csstudio.opibuilder.converter.model.EdmWidget;
import org.csstudio.opibuilder.converter.model.Edm_activeSymbolClass;

/** Convert an EDM widget into Display Builder counterpart
 *  @author Kay Kasemir
 *  @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 */
@SuppressWarnings("nls")
public class Convert_activeSymbolClass extends ConverterBase<EmbeddedDisplayWidget>
{
    public Convert_activeSymbolClass(final EdmConverter converter, final Widget parent, final Edm_activeSymbolClass g)
    {
        super(converter, parent, g);

        if (g.getFile() != null)
            widget.propFile().setValue(convertDisplayPath(g.getFile()));
        widget.propResize().setValue(Resize.ResizeContent);
        widget.propGroupName().setValue(Convert_activeGroupClass.GROUP_NAME + "0");

        if (g.getNumPvs() == 1  &&  !g.isTruthTable())
        {
            // Set symbol (group_name) from PV and list of min <= value < max ranges
            final String pv = convertPVName(g.getControlPvs().getEdmAttributesMap().get("0").get());
            final Map<String, EdmDouble> minMap = g.getMinValues().getEdmAttributesMap();
            final Map<String, EdmDouble> maxMap = g.getMaxValues().getEdmAttributesMap();
            final StringBuilder script = new StringBuilder();
            script.append("# pvs[0] = ").append(pv).append("\n");
            script.append("from org.csstudio.display.builder.runtime.script import PVUtil\n");
            script.append("pv0 = PVUtil.getDouble(pvs[0])\n");
            for (int i=0; i<g.getNumStates(); ++i)
            {
                final String si = Integer.toString(i);
                double min = 0.0, max = 0.0;
                if (minMap.get(si) != null)
                    min = minMap.get(si).get();
                if (maxMap.get(si) != null)
                    max = maxMap.get(si).get();
                // if or elif?
                if (i > 0)
                    script.append("el");
                script.append("if ").append(min).append(" <= pv0 < ").append(max).append(":\n");
                script.append("    widget.setPropertyValue('group_name', '").append(Convert_activeGroupClass.GROUP_NAME).append(i).append("')\n");
            }
            script.append("else:\n");
            script.append("    widget.setPropertyValue('group_name', '").append(Convert_activeGroupClass.GROUP_NAME).append("0')\n");

            final List<ScriptInfo> scripts = new ArrayList<>(widget.propScripts().getValue());
            scripts.add(new ScriptInfo(ScriptInfo.EMBEDDED_PYTHON, script.toString(), true, List.of(new ScriptPV(pv))));
            widget.propScripts().setValue(scripts);
        }
        else
            logger.log(Level.WARNING, "Symbol " + g.getFile() + " can only handle plain PV, not binary truth table");

    }

    @Override
    protected EmbeddedDisplayWidget createWidget(final EdmWidget edm)
    {
        return new EmbeddedDisplayWidget();
    }
}

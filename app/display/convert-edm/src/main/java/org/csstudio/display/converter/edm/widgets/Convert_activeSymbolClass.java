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
import org.csstudio.opibuilder.converter.model.EdmInt;
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
        {
            widget.propFile().setValue(convertDisplayPath(g.getFile()));
            converter.addIncludedDisplay(widget.propFile().getValue());
        }
        else
        {
            logger.log(Level.WARNING, "EDM Symbol without file");
            return;
        }
        widget.propResize().setValue(Resize.StretchContent);
        widget.propGroupName().setValue(Convert_activeGroupClass.GROUP_NAME + "0");

        final StringBuilder script = new StringBuilder();
        final List<ScriptPV> pvs = new ArrayList<>();
        if (g.getNumPvs() == 1  &&  !g.isTruthTable())
        {
            // Value directly read from PV
            final String pv = convertPVName(g.getControlPvs().getEdmAttributesMap().get("0").get());
            pvs.add(new ScriptPV(pv));
            widget.propTooltip().setValue(pv);
            script.append("# pvs[0] = ").append(pv).append("\n");
            script.append("from org.csstudio.display.builder.runtime.script import PVUtil\n");
            script.append("val = PVUtil.getDouble(pvs[0])\n");
        }
        else if (g.isTruthTable() && g.getNumPvs() >= 1)
        {
            // Each PV sets a bit in a value
            for (int i=0; i<g.getNumPvs(); ++i)
            {
                final String pv = convertPVName(g.getControlPvs().getEdmAttributesMap().get(Integer.toString(i)).get());
                pvs.add(new ScriptPV(pv));
                script.append("# pvs[").append(i).append("] = ").append(pv).append("\n");
            }
            script.append("from org.csstudio.display.builder.runtime.script import PVUtil\n");
            script.append("val = 0\n");
            for (int i=0; i<pvs.size(); ++i)
            {
                script.append("if PVUtil.getInt(pvs[").append(i).append("]) != 0:\n");
                script.append("    val += ").append(1<<i).append("\n");
            }
        }
        else
        {
            // Value of each PV is masked and shifted
            for (int i=0; i<g.getNumPvs(); ++i)
            {
                final String pv = convertPVName(g.getControlPvs().getEdmAttributesMap().get(Integer.toString(i)).get());
                pvs.add(new ScriptPV(pv));
                script.append("# pvs[").append(i).append("] = ").append(pv).append("\n");
            }
            script.append("from org.csstudio.display.builder.runtime.script import PVUtil\n");
            script.append("val = 0\n");

            final Map<String, EdmInt> andMap = g.getAndMask().getEdmAttributesMap();
            final Map<String, EdmInt> xorMap = g.getXorMask().getEdmAttributesMap();
            final Map<String, EdmInt> shiftMap = g.getShiftCount().getEdmAttributesMap();
            for (int i=0; i<pvs.size(); ++i)
            {
                final String si = Integer.toString(i);
                int and=0, xor=0, shift=0;
                if (andMap.containsKey(si))
                    and = andMap.get(si).get();
                if (xorMap.containsKey(si))
                    xor = xorMap.get(si).get();
                if (shiftMap.containsKey(si))
                    shift = shiftMap.get(si).get();

                script.append("val += ( PVUtil.getInt(pvs[").append(i).append("])");
                if (and > 0)
                    script.append(" & ").append(and);
                if (xor > 0)
                    script.append(" ^ ").append(xor);
                script.append(")");
                if (shift > 0)
                    script.append(" << ").append(shift);
                script.append("\n");
            }
        }

        // Set symbol (group_name) from list of min <= val < max ranges
        final Map<String, EdmDouble> minMap = g.getMinValues().getEdmAttributesMap();
        final Map<String, EdmDouble> maxMap = g.getMaxValues().getEdmAttributesMap();
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
            script.append("if ").append(min).append(" <= val < ").append(max).append(":\n");
            script.append("    widget.setPropertyValue('group_name', '").append(Convert_activeGroupClass.GROUP_NAME).append(i).append("')\n");
        }
        script.append("else:\n");
        script.append("    widget.setPropertyValue('group_name', '").append(Convert_activeGroupClass.GROUP_NAME).append("0')\n");

        // Attach that script to widget
        final List<ScriptInfo> scripts = new ArrayList<>(widget.propScripts().getValue());
        scripts.add(new ScriptInfo(ScriptInfo.EMBEDDED_PYTHON, script.toString(), true, pvs));
        widget.propScripts().setValue(scripts);
    }

    @Override
    protected EmbeddedDisplayWidget createWidget(final EdmWidget edm)
    {
        return new EmbeddedDisplayWidget();
    }
}

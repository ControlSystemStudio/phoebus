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
import org.csstudio.display.builder.model.widgets.NavigationTabsWidget;
import org.csstudio.display.builder.model.widgets.NavigationTabsWidget.TabProperty;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.EdmString;
import org.csstudio.opibuilder.converter.model.EdmWidget;
import org.csstudio.opibuilder.converter.model.Edm_activePipClass;
import org.phoebus.framework.macros.Macros;

/** Convert an EDM widget into Display Builder counterpart
 *  @author Kay Kasemir
 *  @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 */
@SuppressWarnings("nls")
public class Convert_activePipClass extends ConverterBase<Widget>
{
    public Convert_activePipClass(final EdmConverter converter, final Widget parent, final Edm_activePipClass pip)
    {
        super(converter, parent, pip);

        if (widget instanceof NavigationTabsWidget)
        {
            // Use nav. tabs for 'menu' that selects a display based on numeric PV
            final NavigationTabsWidget w = (NavigationTabsWidget) widget;
            // Hide the tabs
            w.propTabWidth().setValue(0);
            w.propTabHeight().setValue(0);
            // List displays
            if (pip.getDisplayFileName() != null)
            {
                final Map<String, EdmString> displays = pip.getDisplayFileName().getEdmAttributesMap();
                final Map<String, EdmString> symbols = pip.getSymbols() != null ? pip.getSymbols().getEdmAttributesMap() : null;
                int i=0;
                String si = Integer.toString(i);
                EdmString display = displays.get(si);
                while (display != null)
                {
                    while (w.propTabs().size() <= i)
                        w.propTabs().addElement();
                    final TabProperty tab = w.propTabs().getElement(i);

                    final String path = convertDisplayPath(display.get());
                    converter.addIncludedDisplay(path);
                    tab.file().setValue(path);

                    final EdmString edm_macros = symbols == null ? null : symbols.get(si);
                    if (edm_macros != null)
                        try
                        {
                            tab.macros().setValue(Macros.fromSimpleSpec(edm_macros.get()));
                        }
                        catch (Exception ex)
                        {
                            logger.log(Level.WARNING, "Cannot parse macros for embedded display " + path + " from '" + edm_macros.get() + "'", ex);
                        }

                    ++i;
                    si = Integer.toString(i);
                    display = displays.get(si);
                }
            }

            // Connect active_tab to PV
            final List<ScriptInfo> scripts = new ArrayList<>(w.propScripts().getValue());
            final String script =
                "from org.csstudio.display.builder.runtime.script import PVUtil\n" +
                "widget.setPropertyValue('active_tab', PVUtil.getInt(pvs[0]))\n";
            scripts.add(new ScriptInfo(ScriptInfo.EMBEDDED_PYTHON, script, true, List.of(new ScriptPV(convertPVName(pip.getFilePv())))));
            w.propScripts().setValue(scripts);
        }
        else if ((pip.getFile() != null && pip.getDisplayFileName().getValueCount() == 0)
                 ||
                 pip.getDisplayFileName().getValueCount() == 1)
        {
            // Single display file, either from 'file' or list of displays with only first entry & macros
            final EmbeddedDisplayWidget w = (EmbeddedDisplayWidget) widget;
            String path = pip.getDisplayFileName().getValueCount() == 1
                        ? pip.getDisplayFileName().getEdmAttributesMap().get("0").get()
                        : pip.getFile();
            path = convertDisplayPath(path);
            w.propFile().setValue(path);

            if (pip.getSymbols() != null)
            {
                final EdmString edm_macros = pip.getSymbols().getEdmAttributesMap().get("0");
                if (edm_macros != null)
                    try
                    {
                        w.propMacros().setValue(Macros.fromSimpleSpec(edm_macros.get()));
                    }
                    catch (Exception ex)
                    {
                        logger.log(Level.WARNING, "Cannot parse macros for embedded display " + path + " from '" + edm_macros.get() + "'", ex);
                    }
            }

            w.propTransparent().setValue(true);
        }
        else
        {
            // Not handling 'String PV'
            logger.log(Level.WARNING, "Can only handle EDM Embedded Window (pip) for single display or 'menu'");
        }
    }

    @Override
    protected Widget createWidget(final EdmWidget edm)
    {
        final Edm_activePipClass pip = (Edm_activePipClass) edm;
        // If there's a menu and more than one display, use tabs (hiding the tabs)
        if ("menu".equals(pip.getDisplaySource())  &&  pip.getDisplayFileName().getValueCount() > 1)
            return new NavigationTabsWidget();
        // Otherwise use embedded display
        else
            return new EmbeddedDisplayWidget();
    }
}

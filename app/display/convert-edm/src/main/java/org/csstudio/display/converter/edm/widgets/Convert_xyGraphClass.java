/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import java.util.Map.Entry;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.LineStyle;
import org.csstudio.display.builder.model.widgets.plots.PlotWidgetPointType;
import org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.TraceWidgetProperty;
import org.csstudio.display.builder.model.widgets.plots.XYPlotWidget;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.EdmBoolean;
import org.csstudio.opibuilder.converter.model.EdmColor;
import org.csstudio.opibuilder.converter.model.EdmInt;
import org.csstudio.opibuilder.converter.model.EdmString;
import org.csstudio.opibuilder.converter.model.EdmWidget;
import org.csstudio.opibuilder.converter.model.Edm_xyGraphClass;

/** Convert an EDM widget into Display Builder counterpart
 *  @author Kay Kasemir
 *  @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 */
@SuppressWarnings("nls")
public class Convert_xyGraphClass extends ConverterBase<XYPlotWidget>
{
    public Convert_xyGraphClass(final EdmConverter converter, final Widget parent, final Edm_xyGraphClass r)
    {
        super(converter, parent, r);

        widget.propToolbar().setValue(false);
        widget.propLegend().setValue(false);
        if (r.getGraphTitle() != null)
            widget.propTitle().setValue(r.getGraphTitle());

        convertColor(r.getBgColor(), widget.propBackground());
        convertColor(r.getFgColor(), widget.propForeground());
        convertColor(r.getFgColor(), widget.propGridColor());

        // X Axis
        if (r.getXLabel() == null)
            widget.propXAxis().title().setValue("");
        else
            widget.propXAxis().title().setValue(r.getXLabel());
        widget.propXAxis().visible().setValue(r.isShowXAxis());
        widget.propXAxis().grid().setValue(r.isxShowLabelGrid());
        if ("AutoScale".equals(r.getxAxisSrc()))
            widget.propXAxis().autoscale().setValue(true);
        else
        {
            widget.propXAxis().autoscale().setValue(false);
            widget.propXAxis().logscale().setValue("log10".equals(r.getXAxisStyle()));
            widget.propXAxis().minimum().setValue(r.getxMin());
            widget.propXAxis().maximum().setValue(r.getxMax());
        }

        // Y Axis
        if (r.getYLabel() == null)
            widget.propYAxes().getElement(0).title().setValue("");
        else
            widget.propYAxes().getElement(0).title().setValue(r.getYLabel());
        widget.propYAxes().getElement(0).visible().setValue(r.isShowYAxis());
        widget.propYAxes().getElement(0).grid().setValue(r.isyShowLabelGrid());
        if ("AutoScale".equals(r.getyAxisSrc()))
            widget.propYAxes().getElement(0).autoscale().setValue(true);
        else
        {
            widget.propYAxes().getElement(0).autoscale().setValue(false);
            widget.propYAxes().getElement(0).logscale().setValue("log10".equals(r.getYAxisStyle()));
            widget.propYAxes().getElement(0).minimum().setValue(r.getyMin());
            widget.propYAxes().getElement(0).maximum().setValue(r.getyMax());
        }

        // Create 2nd Y axis
        widget.propYAxes().addElement();
        if (r.getY2Label() == null)
            widget.propYAxes().getElement(1).title().setValue("");
        else
            widget.propYAxes().getElement(1).title().setValue(r.getY2Label());
        widget.propYAxes().getElement(1).visible().setValue(r.isShowY2Axis());
        widget.propYAxes().getElement(1).grid().setValue(r.isY2ShowLabelGrid());
        if ("AutoScale".equals(r.getY2AxisSrc()))
            widget.propYAxes().getElement(1).autoscale().setValue(true);
        else
        {
            widget.propYAxes().getElement(1).autoscale().setValue(false);
            widget.propYAxes().getElement(1).logscale().setValue("log10".equals(r.getY2AxisStyle()));
            widget.propYAxes().getElement(1).minimum().setValue(r.getY2Min());
            widget.propYAxes().getElement(1).maximum().setValue(r.getY2Max());
        }

        // All fonts on EDM graphs are the same
        if (r.getFont().isExistInEDL())
        {
            convertFont(r.getFont(), widget.propTitleFont());
            convertFont(r.getFont(), widget.propXAxis().titleFont());
            convertFont(r.getFont(), widget.propXAxis().scaleFont());
            convertFont(r.getFont(), widget.propYAxes().getElement(0).titleFont());
            convertFont(r.getFont(), widget.propYAxes().getElement(0).scaleFont());
            convertFont(r.getFont(), widget.propYAxes().getElement(1).titleFont());
            convertFont(r.getFont(), widget.propYAxes().getElement(1).scaleFont());
        }

        // Traces
        while (widget.propTraces().size() < r.getNumTraces())
            widget.propTraces().addElement();

        // EDM keeps separate sparse maps with stringified index as key for each trace property...
        if (r.getYPv().isExistInEDL())
            for (Entry<String, EdmString> entry : r.getYPv().getEdmAttributesMap().entrySet())
            {
                final int i = Integer.parseInt(entry.getKey());
                final TraceWidgetProperty trace = widget.propTraces().getElement(i);
                trace.traceYPV().setValue(entry.getValue().get());
                trace.traceName().setValue("");

                // Is there an X PV?
                if (r.getXPv().isExistInEDL())
                {
                    final EdmString name = r.getXPv().getEdmAttributesMap().get(Integer.toString(i));
                    if (name != null)
                        trace.traceXPV().setValue(name.get());
                }
            }

        if (r.getPlotColor().isExistInEDL())
            for (Entry<String, EdmColor> entry : r.getPlotColor().getEdmAttributesMap().entrySet())
            {
                final int i = Integer.parseInt(entry.getKey());
                final TraceWidgetProperty trace = widget.propTraces().getElement(i);
                convertColor(entry.getValue(), trace.traceColor());
            }

        if(r.getLineThickness().isExistInEDL())
            for (Entry<String, EdmInt> entry : r.getLineThickness().getEdmAttributesMap().entrySet())
            {
                final int i = Integer.parseInt(entry.getKey());
                final TraceWidgetProperty trace = widget.propTraces().getElement(i);
                trace.traceWidth().setValue(entry.getValue().get());
            }

        if (r.getLineStyle().isExistInEDL())
            for (Entry<String, EdmString> entry : r.getLineStyle().getEdmAttributesMap().entrySet())
            {
                final int i = Integer.parseInt(entry.getKey());
                final TraceWidgetProperty trace = widget.propTraces().getElement(i);
                if (entry.getValue().get().equals("dash"))
                    trace.traceLineStyle().setValue(LineStyle.DASH);
            }

        if(r.getPlotSymbolType().isExistInEDL())
            for (Entry<String, EdmString> entry : r.getPlotSymbolType().getEdmAttributesMap().entrySet())
            {
                final int i = Integer.parseInt(entry.getKey());
                final TraceWidgetProperty trace = widget.propTraces().getElement(i);
                if (entry.getValue().get().equals("circle"))
                    trace.tracePointType().setValue(PlotWidgetPointType.CIRCLES);
                else if (entry.getValue().get().equals("square"))
                    trace.tracePointType().setValue(PlotWidgetPointType.SQUARES);
                else if (entry.getValue().get().equals("diamond"))
                    trace.tracePointType().setValue(PlotWidgetPointType.DIAMONDS);
                else
                    continue;
                trace.tracePointSize().setValue(6);
            }

        boolean show_second = false;
        if (r.getUseY2Axis().isExistInEDL())
            for (Entry<String, EdmBoolean> entry : r.getUseY2Axis().getEdmAttributesMap().entrySet())
            {
                final int i = Integer.parseInt(entry.getKey());
                final TraceWidgetProperty trace = widget.propTraces().getElement(i);
                if (entry.getValue().is())
                {
                    trace.traceYAxis().setValue(1);
                    show_second = true;
                }
            }

        widget.propYAxes().getElement(1).visible().setValue(show_second);
    }

    @Override
    protected XYPlotWidget createWidget(final EdmWidget edm)
    {
        return new XYPlotWidget();
    }
}

/*************************************************************************\
* Copyright (c) 2010  UChicago Argonne, LLC
* This file is distributed subject to a Software License Agreement found
* in the file LICENSE that is included with this distribution.
/*************************************************************************/

package org.csstudio.opibuilder.adl2boy.translator;

import java.util.List;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.widgets.plots.StripchartWidget;
import org.csstudio.display.builder.model.widgets.plots.StripchartWidget.TraceWidgetProperty;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLPen;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLPlotcom;
import org.csstudio.utility.adlparser.fileParser.widgets.StripChart;

public class StripChart2Model extends AbstractADL2Model<StripchartWidget> {

    public StripChart2Model(ADLWidget adlWidget, WidgetColor[] colorMap, Widget parentModel) throws Exception {
        super(adlWidget, colorMap, parentModel);
    }

    @Override
    public void processWidget(ADLWidget adlWidget) throws Exception
    {
        widgetModel.propToolbar().setValue(false);
        final StripChart plotWidget = new StripChart(adlWidget);
        setADLObjectProps(plotWidget, widgetModel);
        ADLPlotcom plotcom = plotWidget.getAdlPlotcom();
        if (plotcom != null )
        {
            widgetModel.propTitle().setValue(plotcom.getTitle());
            widgetModel.propYAxes().getElement(0).autoscale().setValue(true);
            widgetModel.propYAxes().getElement(0).title().setValue("");
            setColor(plotcom.getForegroundColor(), CommonWidgetProperties.propForegroundColor);
            setColor(plotcom.getBackgroundColor(), CommonWidgetProperties.propBackgroundColor);
        }

        widgetModel.propTimeRange().setValue(Math.round(plotWidget.getPeriod()) + " " + plotWidget.getUnits());

        final List<ADLPen> pens = plotWidget.getPens();
        if (pens.size() > 0)
        {
            // Adjust trace count
            while (widgetModel.propTraces().size() < pens.size())
                widgetModel.propTraces().addElement();
            while (widgetModel.propTraces().size() > pens.size())
                widgetModel.propTraces().removeElement();

            for (int ii = 0; ii< pens.size(); ii++)
            {
                final ADLPen trace = pens.get(ii);
                final TraceWidgetProperty model_trace = widgetModel.propTraces().getElement(ii);
                model_trace.traceYPV().setValue(trace.getChannel());
                model_trace.traceColor().setValue(colorMap[trace.getLineColor()]);
            }
        }
    }

    @Override
    public void makeModel(ADLWidget adlWidget,
            Widget parentModel) {
        widgetModel = new StripchartWidget();
        ChildrenProperty.getChildren(parentModel).addChild(widgetModel);
    }
}

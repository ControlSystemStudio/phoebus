/*************************************************************************\
* Copyright (c) 2010  UChicago Argonne, LLC
* This file is distributed subject to a Software License Agreement found
* in the file LICENSE that is included with this distribution.
/*************************************************************************/

package org.csstudio.opibuilder.adl2boy.translator;

import java.util.ArrayList;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.TraceWidgetProperty;
import org.csstudio.display.builder.model.widgets.plots.XYPlotWidget;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLPlotTrace;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLPlotcom;
import org.csstudio.utility.adlparser.fileParser.widgets.CartesianPlot;

/** Turn CartesianPlot into XYPlot
 *
 *  <p>XYPlot handles waveform PVs.
 */
public class CartesianPlot2Model extends AbstractADL2Model<XYPlotWidget> {
//    XYGraphModel graphModel = new XYGraphModel();

    public CartesianPlot2Model(ADLWidget adlWidget, WidgetColor[] colorMap, Widget parentModel) throws Exception {
        super(adlWidget, colorMap, parentModel);
    }

    /**
     * @param adlWidget
     * @param colorMap
     */
    @Override
    public void processWidget(ADLWidget adlWidget) {
        CartesianPlot plotWidget = new CartesianPlot(adlWidget);
        setADLObjectProps(plotWidget, widgetModel);
        //Add Title & X/Y Labels
        ADLPlotcom plotcom = plotWidget.getAdlPlotcom();
        if (plotcom != null )
        {
            widgetModel.propTitle().setValue(plotcom.getTitle());
            widgetModel.propXAxis().title().setValue(plotcom.getXLabel());
            widgetModel.propYAxes().getElement(0).title().setValue(plotcom.getYLabel());

            // TODO check rangeStyle
            widgetModel.propXAxis().autoscale().setValue(true);
            widgetModel.propYAxes().getElement(0).autoscale().setValue(true);
            //TODO set foreground and background color
        }
        //Add Trace data to CartesianPlot2Model
        ArrayList<ADLPlotTrace> traces = plotWidget.getTraces();
        if (traces.size() > 0)
        {
            // Adjust trace count
            while (widgetModel.propTraces().size() < traces.size())
                widgetModel.propTraces().addElement();
            while (widgetModel.propTraces().size() > traces.size())
                widgetModel.propTraces().removeElement();

            for (int ii = 0; ii< traces.size(); ii++)
            {
                final ADLPlotTrace trace = traces.get(ii);
                final TraceWidgetProperty model_trace = widgetModel.propTraces().getElement(ii);
                model_trace.traceXPV().setValue(trace.getxData());
                model_trace.traceYPV().setValue(trace.getyData());
                model_trace.traceColor().setValue(colorMap[trace.getDataColor()]);

                // MEDM cartesian plot and BOY XY Graph support different modes:
                // 1) Plot one 'Y' array PV over array index
                // 2) Plot 'X' and 'Y' array PVs over each other
                // 3) Plot scalar 'X' PV over array index, using fixed size buffer
                // 4) .. more
                // Some behavior is based on configuration,
                // other behavior based on type of received PV data,
                // which can only be determined at runtime.
            }
        }

        //TODO Add Point Style to CartesianPlot2Model
        //TODO CartesianPlot2Model - Add TriggerChannel
        //TODO CartesianPlot2Model - Add EraseChannel.  Not supported by XYGraph.
        //TODO CartesianPlot2Model - Add EraseMode.  Not supported by XYGraph.
    }

    @Override
    public void makeModel(ADLWidget adlWidget,
            Widget parentModel) {
        widgetModel = new XYPlotWidget();
        ChildrenProperty.getChildren(parentModel).addChild(widgetModel);
    }
}

/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;

/**
 * @author Lei Hu, Xihui Chen
 *
 */
public class Edm_xyGraphClass extends EdmWidget {

    @EdmAttributeAn @EdmOptionalAn private String graphTitle;
    @EdmAttributeAn @EdmOptionalAn private String xLabel;
    @EdmAttributeAn @EdmOptionalAn private String yLabel;
    @EdmAttributeAn @EdmOptionalAn private String y2Label;


    @EdmAttributeAn @EdmOptionalAn private EdmColor gridColor;

    @EdmAttributeAn @EdmOptionalAn private boolean border;
    @EdmAttributeAn @EdmOptionalAn private boolean plotAreaBorder;
    //------------
    @EdmAttributeAn @EdmOptionalAn private boolean autoScaleBothDirections;
    @EdmAttributeAn @EdmOptionalAn private double autoScaleThreshPct;
    @EdmAttributeAn @EdmOptionalAn private String plotMode;
    @EdmAttributeAn @EdmOptionalAn private int nPts;
    @EdmAttributeAn @EdmOptionalAn private int updateTimerMs; //Update Delay
    @EdmAttributeAn @EdmOptionalAn private String triggerPv;
    @EdmAttributeAn @EdmOptionalAn private String resetPv;
    @EdmAttributeAn @EdmOptionalAn private String resetMode;

    @EdmAttributeAn @EdmOptionalAn private double xMin;
    @EdmAttributeAn @EdmOptionalAn private double yMin;
    @EdmAttributeAn @EdmOptionalAn private double y2Min;

    @EdmAttributeAn @EdmOptionalAn private double xMax;
    @EdmAttributeAn @EdmOptionalAn private double yMax;
    @EdmAttributeAn @EdmOptionalAn private double y2Max;

    @EdmAttributeAn @EdmOptionalAn private boolean xShowMajorGrid;
    @EdmAttributeAn @EdmOptionalAn private boolean yShowMajorGrid;
    @EdmAttributeAn @EdmOptionalAn private boolean y2ShowMajorGrid;

    @EdmAttributeAn @EdmOptionalAn private boolean xShowMinorGrid;
    @EdmAttributeAn @EdmOptionalAn private boolean yShowMinorGrid;
    @EdmAttributeAn @EdmOptionalAn private boolean y2ShowMinorGrid;

    @EdmAttributeAn @EdmOptionalAn private boolean xShowLabelGrid;
    @EdmAttributeAn @EdmOptionalAn private boolean yShowLabelGrid;
    @EdmAttributeAn @EdmOptionalAn private boolean y2ShowLabelGrid;

    @EdmAttributeAn @EdmOptionalAn private String xAxisSrc; //AutoScale, fromUser
    @EdmAttributeAn @EdmOptionalAn private String yAxisSrc;
    @EdmAttributeAn @EdmOptionalAn private String y2AxisSrc;




    //------------
    @EdmAttributeAn @EdmOptionalAn private boolean showXAxis;
    @EdmAttributeAn @EdmOptionalAn private boolean showYAxis;
    @EdmAttributeAn @EdmOptionalAn private boolean showY2Axis;

    @EdmAttributeAn @EdmOptionalAn private String xAxisStyle;
    @EdmAttributeAn @EdmOptionalAn private String xAxisTimeFormat;
    @EdmAttributeAn @EdmOptionalAn private String yAxisStyle;
    @EdmAttributeAn @EdmOptionalAn private String y2AxisStyle;


    // trace properties
    @EdmAttributeAn @EdmOptionalAn private int numTraces;
    @EdmAttributeAn @EdmOptionalAn private EdmMultiStrings xPv;
    @EdmAttributeAn @EdmOptionalAn private EdmMultiStrings yPv;
    @EdmAttributeAn @EdmOptionalAn private EdmMultiColors plotColor;

    //----
    @EdmAttributeAn @EdmOptionalAn private EdmMultiStrings plotStyle;
    @EdmAttributeAn @EdmOptionalAn private EdmMultiInts lineThickness;
    @EdmAttributeAn @EdmOptionalAn private EdmMultiStrings lineStyle;
    @EdmAttributeAn @EdmOptionalAn private EdmMultiStrings plotUpdateMode;
    @EdmAttributeAn @EdmOptionalAn private EdmMultiStrings plotSymbolType;
    @EdmAttributeAn @EdmOptionalAn private EdmMultiBooleans useY2Axis;
    @EdmAttributeAn @EdmOptionalAn private EdmMultiStrings opMode;

    public Edm_xyGraphClass(EdmEntity genericEntity) throws EdmException {
        super(genericEntity);
    }

    public EdmMultiStrings getOpMode() {
        return opMode;
    }


    public String getxLabel() {
        return xLabel;
    }







    public String getyLabel() {
        return yLabel;
    }







    public boolean isAutoScaleBothDirections() {
        return autoScaleBothDirections;
    }







    public double getAutoScaleThreshPct() {
        return autoScaleThreshPct;
    }







    public String getPlotMode() {
        return plotMode;
    }







    public int getnPts() {
        return nPts;
    }







    public int getUpdateTimerMs() {
        return updateTimerMs;
    }







    public String getResetPv() {
        return resetPv;
    }







    public String getResetMode() {
        return resetMode;
    }







    public double getxMin() {
        return xMin;
    }







    public double getyMin() {
        return yMin;
    }







    public double getY2Min() {
        return y2Min;
    }







    public double getxMax() {
        return xMax;
    }







    public double getyMax() {
        return yMax;
    }







    public double getY2Max() {
        return y2Max;
    }







    public boolean isxShowMajorGrid() {
        return xShowMajorGrid;
    }







    public boolean isyShowMajorGrid() {
        return yShowMajorGrid;
    }







    public boolean isY2ShowMajorGrid() {
        return y2ShowMajorGrid;
    }






    public boolean isxShowMinorGrid() {
        return xShowMinorGrid;
    }







    public boolean isyShowMinorGrid() {
        return yShowMinorGrid;
    }







    public boolean isY2ShowMinorGrid() {
        return y2ShowMinorGrid;
    }






    public boolean isxShowLabelGrid() {
        return xShowLabelGrid;
    }







    public boolean isyShowLabelGrid() {
        return yShowLabelGrid;
    }







    public boolean isY2ShowLabelGrid() {
        return y2ShowLabelGrid;
    }






    public String getxAxisSrc() {
        return xAxisSrc;
    }







    public String getyAxisSrc() {
        return yAxisSrc;
    }







    public String getY2AxisSrc() {
        return y2AxisSrc;
    }







    public String getxAxisStyle() {
        return xAxisStyle;
    }







    public String getxAxisTimeFormat() {
        return xAxisTimeFormat;
    }







    public String getyAxisStyle() {
        return yAxisStyle;
    }







    public EdmMultiStrings getxPv() {
        return xPv;
    }







    public EdmMultiStrings getyPv() {
        return yPv;
    }







    public EdmMultiStrings getPlotStyle() {
        return plotStyle;
    }







    public EdmMultiInts getLineThickness() {
        return lineThickness;
    }







    public EdmMultiStrings getLineStyle() {
        return lineStyle;
    }







    public EdmMultiStrings getPlotUpdateMode() {
        return plotUpdateMode;
    }







    public EdmMultiStrings getPlotSymbolType() {
        return plotSymbolType;
    }







    public EdmMultiBooleans getUseY2Axis() {
        return useY2Axis;
    }







    /**
     * @return the lineAlarm
     */
    public final String getGraphTitle() {
        return graphTitle;
    }
    public final String getXLabel() {
        return xLabel;
    }
    public final String getYLabel() {
        return yLabel;
    }
    public final String getY2Label() {
        return y2Label;
    }


    public EdmColor getGridColor() {
        return gridColor;
    }

    public boolean isBorder(){
        return border;
    }
    public boolean isPlotAreaBorder(){
        return plotAreaBorder;
    }
    //axis
    public boolean isShowXAxis(){
        return showXAxis;
    }
    public boolean isShowYAxis(){
        return showYAxis;
    }
    public boolean isShowY2Axis(){
        return showY2Axis;
    }

    public final String getXAxisStyle() {
        return xAxisStyle;
    }
    public final String getXAxisTimeFormat() {
        return xAxisTimeFormat;
    }
    public final String getYAxisStyle() {
        return yAxisStyle;
    }

    public String getY2AxisStyle() {
        return y2AxisStyle;
    }

    public final EdmMultiColors getPlotColor() {
        return plotColor;
    }
    public final EdmMultiStrings getXPv() {
        return xPv;
    }
    public final EdmMultiStrings getYPv() {
        return yPv;
    }
    public int getNumTraces() {
        return numTraces;
    }

    public final String getTriggerPv() {
        return triggerPv;
    }
}

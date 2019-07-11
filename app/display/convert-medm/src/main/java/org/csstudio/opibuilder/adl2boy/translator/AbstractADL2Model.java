/*************************************************************************\
 * Copyright (c) 2010  UChicago Argonne, LLC
 * This file is distributed subject to a Software License Agreement found
 * in the file LICENSE that is included with this distribution.
/*************************************************************************/

package org.csstudio.opibuilder.adl2boy.translator;

import static org.csstudio.display.converter.medm.Converter.logger;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.LineStyle;
import org.csstudio.display.builder.model.properties.ScriptPV;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.rules.RuleInfo;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLBasicAttribute;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLConnected;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLDynamicAttribute;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLObject;
import org.csstudio.utility.adlparser.fileParser.widgets.ADLAbstractWidget;
import org.csstudio.utility.adlparser.fileParser.widgets.IWidgetWithColorsInBase;
import org.phoebus.framework.macros.Macros;

/**
 * @author John Hammonds, Argonne National Laboratory
 *
 */
public abstract class AbstractADL2Model<WM extends Widget>
{
    WM widgetModel;
    final WidgetColor colorMap[];
    protected String className = new String();

    public AbstractADL2Model(final ADLWidget adlWidget, WidgetColor colorMap[], Widget parentModel) throws Exception
    {
        this.colorMap = colorMap;
        makeModel(adlWidget, parentModel);
        processWidget(adlWidget);
    }

    /**
     * Does the work of converting the adlWidget into the Widget
     *
     * @param adlWidget
     */
    abstract public void processWidget(ADLWidget adlWidget) throws Exception;

    /**
     * Creates the widgetModel appropriate to the adlWidget. Adds the
     * widgetModel as a child to the parentModel
     *
     * @param adlWidget
     * @param parentModel
     */
    abstract public void makeModel(ADLWidget adlWidget, Widget parentModel);

    /**
     *
     * @return
     */
    public WM getWidgetModel()
    {
        return widgetModel;
    }

    /**
     * set the properties contained in the ADL basic properties section in the
     * created widgetModel
     *
     * @param adlWidget
     * @param widgetModel
     */
    protected void setADLObjectProps(ADLAbstractWidget adlWidget, Widget widgetModel) {
        widgetModel.propName().setValue(adlWidget.getName());
        if (adlWidget.hasADLObject()) {
            ADLObject adlObj = adlWidget.getAdlObject();
            widgetModel.propX().setValue(adlObj.getX());
            widgetModel.propY().setValue(adlObj.getY());
            widgetModel.propHeight().setValue(adlObj.getHeight());
            widgetModel.propWidth().setValue(adlObj.getWidth());
        }
    }

    /**
     * set the properties contained in the ADL basic properties section in the
     * created widgetModel
     *
     * @param adlWidget
     * @param widgetModel
     * @throws Exception
     */
    protected void setADLBasicAttributeProps(ADLAbstractWidget adlWidget,
            Widget widgetModel, boolean colorForeground) throws Exception {
        ADLBasicAttribute basAttr;
        if (adlWidget.hasADLBasicAttribute()) {
            basAttr = adlWidget.getAdlBasicAttribute();
        } else {
            basAttr = TranslatorUtils.defaultBasicAttribute;
            adlWidget.setAdlBasicAttribute(basAttr);
        }
        if (basAttr.isColorDefined()) {
            if (colorForeground) {
                setColor(basAttr.getClr(),
                        CommonWidgetProperties.propForegroundColor.getName());
            } else {
                setColor(basAttr.getClr(),
                        CommonWidgetProperties.propBackgroundColor.getName());
            }
        } else {
            if (colorForeground) {
                setForegroundColorSameAsParent(widgetModel);
            } else {
                setBackgroundColorSameAsParent(widgetModel);
            }

        }

    }

    /**
     *
     * @param adlWidget
     * @param widgetModel
     */
    protected void setADLDynamicAttributeProps(ADLAbstractWidget adlWidget,
            Widget widgetModel) {
        ADLDynamicAttribute dynAttr;
        if (adlWidget.hasADLDynamicAttribute()) {
            dynAttr = adlWidget.getAdlDynamicAttribute();
        } else {
            dynAttr = TranslatorUtils.defaultDynamicAttribute;
            adlWidget.setAdlDynamicAttribute(dynAttr);
        }
        if (!(dynAttr.get_vis().equals("static"))) {
            if (dynAttr.get_chan() != null) {
                if (dynAttr.get_vis().equals("if not zero")) {
                    addSimpleVisibilityRule("pv0!=0", dynAttr.get_chan(),
                            widgetModel);
                } else if (dynAttr.get_vis().equals("if zero")) {
                    addSimpleVisibilityRule("pv0==0", dynAttr.get_chan(),
                            widgetModel);
                }
                else if (dynAttr.get_vis().equals("calc"))
                {
                    // Make widget visible, then add rule to make invisible
                    WidgetProperty<Boolean> visible = widgetModel.getProperty(CommonWidgetProperties.propVisible);
                    visible.setValue(true);

                    visible = visible.clone();
                    visible.setValue(false);

                    final List<ScriptPV> pvs = new ArrayList<>();
                    for (String pv : List.of(dynAttr.get_chan(), dynAttr.get_chanb(), dynAttr.get_chanc(), dynAttr.get_chand()))
                        if (! pv.isEmpty())
                            pvs.add(new ScriptPV(pv, true));

                    final String newExpr = translateExpression(dynAttr.get_calc());
                    final RuleInfo rule = new RuleInfo("visible",
                            CommonWidgetProperties.propVisible.getName(),
                            false,
                            List.of(new RuleInfo.ExprInfoValue<>("!("+ newExpr + ")", visible)),
                            pvs);

                    widgetModel.propRules().setValue(List.of(rule));
                }
            }
        }
    }

    /** Create a simple visibility rule.  This places a simple logical
     * expression for one channel
     * @param booleanExpression
     * @param chan
     * @param widgetModel
     * @return
     */
    private void addSimpleVisibilityRule(final String booleanExpression,
                                         final String chan, final Widget widgetModel)
    {
        // Make widget visible, then add rule to make invisible
        WidgetProperty<Boolean> visible = widgetModel.getProperty(CommonWidgetProperties.propVisible);
        visible.setValue(true);

        visible = visible.clone();
        visible.setValue(false);

        final RuleInfo rule = new RuleInfo("visible",
                CommonWidgetProperties.propVisible.getName(),
                false,
                List.of(new RuleInfo.ExprInfoValue<>("!("+ booleanExpression + ")", visible)),
                List.of(new ScriptPV(chan, true)));

        widgetModel.propRules().setValue(List.of(rule));
    }

    /**
     * Perform a translation between an MEDM style
     * calc expression for visibility rules.  Makes
     * the following assumptions
     * 1. The rule is fairy simple. The only
     *    alpha characters are A, B, C & D
     * 2. That the pv fields A, B, C & D are
     *    used sequentially. (i.e. If B is used
     *    A is used, if C is used A & Bare used).
     *    This allows the substitutions
     *        A = pv0
     *        B = pv1
     *        C = pv2
     *        D = PV3
     * 3. Only basic desion maiking is hapenning
     *    ()+-/*=<># were used.  No use of math
     *    functions like ABS, SIN, ...
     * 4. The characters = and # are replaced by
     *    == and != respecively.
     * @param adlExpr
     * @return
     */
    private String translateExpression(String adlExpr) {
        String opiExpr = adlExpr;
        opiExpr = replaceChannel("A", "pv0", opiExpr);
        opiExpr = replaceChannel("B", "pv1", opiExpr);
        opiExpr = replaceChannel("C", "pv2", opiExpr);
        opiExpr = replaceChannel("D", "pv3", opiExpr);
        opiExpr = replaceString("=", "==", opiExpr);
        opiExpr = replaceString("#", "!=", opiExpr);

        // The above can result in "pv0====7".
        // Patch that back into a plain "pv0==7"
        opiExpr = opiExpr.replaceAll("==+", "==");
        return opiExpr.toString();
    }

    /**
     *
     * @param adlChanName
     * @param opiChanName
     * @param opiExpr
     * @return
     */
    private String replaceChannel(String adlChanName, String opiChanName,
            String opiExpr) {
        opiExpr = replaceString(adlChanName, opiChanName, opiExpr);
        opiExpr = replaceString(adlChanName.toLowerCase(), opiChanName, opiExpr);
        return opiExpr;
    }

    private String replaceString(String inName, String outName, String expr) {
        String retExpr = expr;
        if (retExpr.contains(inName)){
            StringBuffer tempExpr = new StringBuffer();
            String[] parts = retExpr.split(inName);
            tempExpr.append(parts[0]);
            for (int occur = 0; occur<(parts.length-1); occur++){
                if (!inName.equals("=")) {
                    if (inName.equals("=")
                            && (tempExpr.toString().endsWith(">") ||
                                    tempExpr.toString().endsWith("<") ))

                    {
                        tempExpr.append("=");
                        tempExpr.append(parts[occur + 1]);
                    } else {
                        tempExpr.append(outName);
                        tempExpr.append(parts[occur + 1]);
                    }
                }
                else {
                    tempExpr.append(outName);
                    tempExpr.append(parts[occur + 1]);
                }
            }
            retExpr = tempExpr.toString();
        }
        return retExpr;
    }

    /**
     * set the properties contained in the ADL basic properties section in the
     * created widgetModel
     *
     * @param adlWidget
     * @param widgetModel
     * @throws Exception
     */
    protected void setADLControlProps(ADLAbstractWidget adlWidget,
            Widget widgetModel) throws Exception {
        if (adlWidget.hasADLControl()) {
            ADLConnected adlConnected = adlWidget.getAdlControl();
            setADLConnectedProps(widgetModel, adlConnected);
        }
    }

    /**
     * set the properties contained in the ADL basic properties section in the
     * created widgetModel
     *
     * @param adlWidget
     * @param widgetModel
     * @throws Exception
     */
    protected void setADLMonitorProps(ADLAbstractWidget adlWidget,
            Widget widgetModel) throws Exception {
        if (adlWidget.hasADLMonitor()) {
            ADLConnected adlConnected = adlWidget.getAdlMonitor();
            setADLConnectedProps(widgetModel, adlConnected);
        }
    }

    /**
     * @param widgetModel
     * @param adlConnected
     * @throws Exception
     */
    public void setADLConnectedProps(Widget widgetModel,
            ADLConnected adlConnected) throws Exception
    {
        if (adlConnected.isForeColorDefined())
            setColor(adlConnected.getForegroundColor(),
                    CommonWidgetProperties.propForegroundColor.getName());
        else
            setForegroundColorSameAsParent(widgetModel);

        if (adlConnected.isBackColorDefined())
            setColor(adlConnected.getBackgroundColor(),
                    CommonWidgetProperties.propBackgroundColor.getName());
        else
            setBackgroundColorSameAsParent(widgetModel);

        String channel = adlConnected.getChan();
        if (channel != null && !channel.isEmpty())
            widgetModel.setPropertyValue(CommonWidgetProperties.propPVName.getName(),
                    channel);
    }

    /**
     * @param widgetModel
     * @throws Exception
     */
    public void setBackgroundColorSameAsParent(Widget widgetModel) throws Exception {
        widgetModel.setPropertyValue(
                CommonWidgetProperties.propBackgroundColor.getName(),
                widgetModel.getParent().get().getPropertyValue(
                        CommonWidgetProperties.propBackgroundColor.getName()));
    }

    /**
     * @param widgetModel
     * @throws Exception
     */
    public void setForegroundColorSameAsParent(Widget widgetModel) throws Exception {
        widgetModel.setPropertyValue(
                CommonWidgetProperties.propForegroundColor.getName(),
                widgetModel.getParent().get().getPropertyValue(
                        CommonWidgetProperties.propForegroundColor.getName()));
    }

    /**
     * @param color_index
     * @param propertyName
     * @throws Exception
     */
    public void setColor(int color_index, String propertyName) throws Exception {
        if (color_index >= 0  &&  color_index < colorMap.length)
            widgetModel.setPropertyValue(propertyName, colorMap[color_index]);
        else
            throw new Exception("Invalid color map index " + color_index);
    }

    protected void setShapesColorFillLine(ADLAbstractWidget shapeWidget) throws Exception {
        if (shapeWidget.getAdlBasicAttribute().getFill().equals("solid") )
        {
            widgetModel.checkProperty(CommonWidgetProperties.propTransparent).ifPresent(prop -> prop.setValue(false));
        }
        else if (shapeWidget.getAdlBasicAttribute().getFill().equals("outline"))
        {
            widgetModel.checkProperty(CommonWidgetProperties.propTransparent).ifPresent(prop -> prop.setValue(true));

            WidgetColor fColor = widgetModel.getPropertyValue(CommonWidgetProperties.propBackgroundColor.getName());
            widgetModel.setPropertyValue(CommonWidgetProperties.propLineColor.getName(), fColor);

            if ( shapeWidget.getAdlBasicAttribute().getStyle().equals("solid") )
                widgetModel.setPropertyValue(CommonWidgetProperties.propLineStyle.getName(), LineStyle.SOLID);
            if ( shapeWidget.getAdlBasicAttribute().getStyle().equals("dash") )
                widgetModel.setPropertyValue(CommonWidgetProperties.propLineStyle.getName(), LineStyle.DASH);

            int lineWidth = shapeWidget.getAdlBasicAttribute().getWidth();
            if (lineWidth <= 0)
                lineWidth = 1;
            widgetModel.setPropertyValue(CommonWidgetProperties.propLineWidth.getName(), lineWidth );
        }
    }

    /**
     * @param args
     * @return
     */
    public Macros makeMacros(String args)
    {
        String resArgs = removeParentMacros(args);
        Macros macIn = null;
        try
        {
            macIn = Macros.fromSimpleSpec(resArgs);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Error in macros " + args, ex);
        }
        return macIn;
    }

    /**
     * Remove parent macros (i.e. P=$(P))from the list. We can now pass parent
     * Macros.
     *
     * @param args
     * @return
     */
    public String removeParentMacros(String args) {
        String[] argList = args.split(",");
        StringBuffer strBuff = new StringBuffer();
        for (int ii = 0; ii < argList.length; ii++) {
            String[] argParts = argList[ii].split("=");
            if (!argParts[1].replaceAll(" ", "").equals(
                    "$(" + argParts[0].trim() + ")")) {
                if (strBuff.length() != 0)
                    strBuff.append(", ");
                strBuff.append(argList[ii]);
            }
        }
        String resArgs = strBuff.toString();
        return resArgs;
    }

    /**
     * @param rdWidget
     * @throws Exception
     */
    public void setWidgetColors(IWidgetWithColorsInBase rdWidget) throws Exception {
        if (rdWidget.isForeColorDefined()) {
            setColor(rdWidget.getForegroundColor(),
                    CommonWidgetProperties.propForegroundColor.getName());
        }
        if (rdWidget.isBackColorDefined()) {
            setColor(rdWidget.getBackgroundColor(),
                    CommonWidgetProperties.propBackgroundColor.getName());
        }
    }
}

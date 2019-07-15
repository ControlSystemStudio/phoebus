package org.csstudio.utility.adlparser.fileParser.widgets;

import java.util.ArrayList;

import org.csstudio.utility.adlparser.fileParser.ADLResource;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.FileLine;
import org.csstudio.utility.adlparser.fileParser.WrongADLFormatException;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLObject;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLPen;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLPlotcom;
import org.csstudio.utility.adlparser.internationalization.Messages;

/**
 *
 * @author hammonds
 *
 */
public class StripChart extends ADLAbstractWidget {
    private String units = new String("second");
    private int period = 60;
    private ArrayList<ADLPen> pens = new ArrayList<>();
    public StripChart(ADLWidget adlWidget) {
        super(adlWidget);
        name = new String("strip chart");
        try {
            for (ADLWidget childWidget : adlWidget.getObjects()) {
                if (childWidget.getType().equals("object")){
                    _adlObject = new ADLObject(childWidget);
                    if (_adlObject != null){
                        _hasObject = true;
                    }

                }
                if (childWidget.getType().equals("plotcom")){
                    _adlPlotcom = new ADLPlotcom(childWidget);
                    if (_adlPlotcom != null){
                        _hasPlotcom = true;
                    }
                }
                if (childWidget.getType().startsWith("pen[")){
                    pens.add(new ADLPen(childWidget));
                }
            }
            for (FileLine fileLine : adlWidget.getBody()){
                String bodyPart = fileLine.getLine();
                String[] row = bodyPart.trim().split("=");
                if (row.length < 2){
                    throw new WrongADLFormatException(Messages.Label_WrongADLFormatException_Parameter_Begin + bodyPart + Messages.Label_WrongADLFormatException_Parameter_End);
                }
                if (FileLine.argEquals(row[0], "period")){
                    setPeriod(Math.round(FileLine.getFloatValue(row[1])));
                }
                else if (FileLine.argEquals(row[0], "units")){
                    setUnits(FileLine.getTrimmedValue(row[1]));
                }
            }
        }

        catch (WrongADLFormatException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @param units the units to set
     */
    private void setUnits(String units) {
        this.units = units;
    }

    /**
     * @return the units
     */
    public String getUnits() {
        return units;
    }

    /**
     * @param period the period to set
     */
    private void setPeriod(int period) {
        this.period = period;
    }

    /**
     * @return the period
     */
    public float getPeriod() {
        return period;
    }

    /**
     * @return the pens
     */
    public ArrayList<ADLPen> getPens() {
        return pens;
    }

    @Override
    public Object[] getChildren() {
        ArrayList<Object> ret = new ArrayList<>();
        if (_adlObject != null) ret.add( _adlObject);
        if (_adlPlotcom != null) ret.add( _adlPlotcom);
        for (ADLPen pen : pens){
            if (pen!=null){
                ret.add(pen);
            }
        }
        if (!(units.equals(""))) ret.add(new ADLResource(ADLResource.UNITS, units));
        ret.add(new ADLResource(ADLResource.PERIOD, period));
        return ret.toArray();
    }

}

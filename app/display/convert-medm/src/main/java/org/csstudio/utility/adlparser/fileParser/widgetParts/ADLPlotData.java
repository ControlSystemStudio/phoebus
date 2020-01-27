package org.csstudio.utility.adlparser.fileParser.widgetParts;

import static  org.csstudio.display.converter.medm.Converter.logger;

import java.util.ArrayList;
import java.util.logging.Level;

import org.csstudio.utility.adlparser.fileParser.ADLResource;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.FileLine;
import org.csstudio.utility.adlparser.fileParser.WrongADLFormatException;
import org.csstudio.utility.adlparser.internationalization.Messages;

public class ADLPlotData extends WidgetPart {
    private String axisStyle;
    private String rangeStyle;
    private float minRange;
    private float maxRange;

    public ADLPlotData(ADLWidget widgetPart) throws WrongADLFormatException
    {
        // This super-constructor will check name,
        // but actual widget part will be called "x_axis" etc.,
        // not "plot data"
        // super(widgetPart);
        super();
        parseWidgetPart(widgetPart);
    }

    /**
     * Default Constructor
     */
    public ADLPlotData(){
        super();
    }

    @Override
    public Object[] getChildren() {
        ArrayList<Object> ret = new ArrayList<>();
        ret.add(new ADLResource(ADLResource.PLOT_AXIS_STYLE, axisStyle));
        ret.add(new ADLResource(ADLResource.PLOT_RANGE_STYLE, rangeStyle));
        ret.add(new ADLResource(ADLResource.PLOT_RANGE_MIN, new Float(minRange)));
        ret.add(new ADLResource(ADLResource.PLOT_RANGE_MAX, new Float(maxRange)));
        return ret.toArray();
    }

    @Override
    void init() {
        name = new String("plot data");
        axisStyle = new String("linear");
        rangeStyle = new String("from channel");
    }

    @Override
    void parseWidgetPart(ADLWidget widgetPart) throws WrongADLFormatException {
           assert widgetPart.getType().startsWith("trace[") : Messages.ADLObject_AssertError_Begin+widgetPart.getType()+Messages.ADLObject_AssertError_End+"\r\n"+widgetPart; //$NON-NLS-1$
            for (FileLine parameter : widgetPart.getBody()) {
                if(parameter.getLine().trim().startsWith("//")){ //$NON-NLS-1$
                    continue;
                }
                String[] row = parameter.getLine().split("="); //$NON-NLS-1$
                if(row.length!=2){
                    throw new WrongADLFormatException(Messages.ADLControl_WrongADLFormatException_Begin+parameter+Messages.ADLControl_WrongADLFormatException_End);
                }
                if(FileLine.argEquals(row[0], "minRange")){ //$NON-NLS-1$
                    setMinRange(FileLine.getFloatValue(row[1]));
                }else if(FileLine.argEquals(row[0], "maxRange")){ //$NON-NLS-1$
                        setMaxRange(FileLine.getFloatValue(row[1]));
                }else if(FileLine.argEquals(row[0], "axisStyle")){ //$NON-NLS-1$
                    setAxisStyle(FileLine.getTrimmedValue(row[1]));
                }else if(FileLine.argEquals(row[0], "rangeStyle")){ //$NON-NLS-1$
                    setRangeStyle(FileLine.getTrimmedValue(row[1]));
                }else if(FileLine.argEquals(row[0], "timeFormat")){ //$NON-NLS-1$
                    // Ignore
                    logger.log(Level.WARNING, "Ignoring " + parameter.getLine());
                }else {
                    throw new WrongADLFormatException(Messages.ADLControl_WrongADLFormatException_Parameter_Begin+parameter+Messages.ADLControl_WrongADLFormatException_Parameter_End);
                }
            }

    }

    /**
     * @param axisStyle the axisStyle to set
     */
    public void setAxisStyle(String axisStyle) {
        this.axisStyle = axisStyle;
    }

    /**
     * @return the axisStyle
     */
    public String getAxisStyle() {
        return axisStyle;
    }

    /**
     * @param rangeStyle the rangeStyle to set
     */
    public void setRangeStyle(String rangeStyle) {
        this.rangeStyle = rangeStyle;
    }

    /**
     * @return the rangeStyle
     */
    public String getRangeStyle() {
        return rangeStyle;
    }

    /**
     * @param minRange the minRange to set
     */
    public void setMinRange(float minRange) {
        this.minRange = minRange;
    }

    /**
     * @return the minRange
     */
    public float getMinRange() {
        return minRange;
    }

    /**
     * @param maxRange the maxRange to set
     */
    public void setMaxRange(float maxRange) {
        this.maxRange = maxRange;
    }

    /**
     * @return the maxRange
     */
    public float getMaxRange() {
        return maxRange;
    }


}

package org.csstudio.utility.adlparser.fileParser.widgetParts;

import java.util.ArrayList;

import org.csstudio.utility.adlparser.fileParser.ADLResource;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.FileLine;
import org.csstudio.utility.adlparser.fileParser.WrongADLFormatException;
import org.csstudio.utility.adlparser.internationalization.Messages;

public class ADLPlotTrace extends WidgetPart {
    private String xData;
    private String yData;
    private int dataColor;

    public ADLPlotTrace(ADLWidget widgetPart) throws WrongADLFormatException {
        super(widgetPart);
    }

    /**
     * Default constructor
     */
    public ADLPlotTrace(){
        super();
    }

    @Override
    public Object[] getChildren() {
        ArrayList<Object> ret = new ArrayList<Object>();
        if (!xData.equals(""))ret.add(new ADLResource(ADLResource.PLOT_XDATA, xData));
        if (!yData.equals(""))ret.add(new ADLResource(ADLResource.PLOT_YDATA, yData));
        ret.add(new ADLResource(ADLResource.PLOT_DATA_COLOR, new Integer(dataColor)));
        return ret.toArray();
    }

    @Override
    void init() {
        name = new String("trace");
        setxData(new String());
        setyData(new String());
        setDataColor(0);

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
                if(FileLine.argEquals(row[0], "data_clr")){ //$NON-NLS-1$
                    setDataColor(FileLine.getIntValue(row[1]));
                }else if(FileLine.argEquals(row[0], "xdata")){ //$NON-NLS-1$
                    setxData(FileLine.getTrimmedValue(row[1]));
                }else if(FileLine.argEquals(row[0], "ydata")){ //$NON-NLS-1$
                    setyData(FileLine.getTrimmedValue(row[1]));
                }else {
                    System.out.println("Ignoring " + parameter);
                }
            }


    }
    /**
     * @param xData the xData to set
     */
    private void setxData(String xData) {
        this.xData = xData;
    }

    /**
     * @return the xData
     */
    public String getxData() {
        return xData;
    }

    /**
     * @param yData the yData to set
     */
    private void setyData(String yData) {
        this.yData = yData;
    }

    /**
     * @return the yData
     */
    public String getyData() {
        return yData;
    }

    /**
     * @param dataColor the dataColor to set
     */
    private void setDataColor(int dataColor) {
        this.dataColor = dataColor;
    }

    /**
     * @return the dataColor
     */
    public int getDataColor() {
        return dataColor;
    }

}

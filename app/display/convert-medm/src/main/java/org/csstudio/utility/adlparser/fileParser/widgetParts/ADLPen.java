package org.csstudio.utility.adlparser.fileParser.widgetParts;

import org.csstudio.utility.adlparser.fileParser.ADLResource;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.FileLine;
import org.csstudio.utility.adlparser.fileParser.WrongADLFormatException;
import org.csstudio.utility.adlparser.internationalization.Messages;

public class ADLPen extends WidgetPart {
    private ADLLimits _adlLimit;
    private boolean _hasLimits;
    private int lineColor;
    private String channel;

    public ADLPen(ADLWidget widgetPart) throws WrongADLFormatException {
        super(widgetPart);
    }

    /**
     * Default constructor
     */
    public ADLPen(){
        super();
    }

    @Override
    public Object[] getChildren() {
        Object[] ret;
        if (_hasLimits){
            ret = new Object[3];
        }
        else {
            ret = new Object[2];
        }
        ret[0] = new ADLResource(ADLResource.PEN_COLOR, new Integer(lineColor));
        ret[1] = new ADLResource(ADLResource.CHANNEL, channel);
        if (_hasLimits){
            ret[2] = new ADLResource(ADLResource.ADL_LIMITS, _adlLimit);
        }

        return ret;
    }

    @Override
    void init() {
        name = new String("pen");
        _adlLimit = null;
        _hasLimits = false;
        lineColor = 0;
        channel = new String();
        }

    @Override
    void parseWidgetPart(ADLWidget widgetPart) throws WrongADLFormatException {
        try {
            for (ADLWidget childWidget : widgetPart.getObjects()) {
                if (childWidget.getType().equals("limits")){
                    _adlLimit = new ADLLimits(childWidget);
                    if (_adlLimit != null){
                        _hasLimits = true;
                    }

                }
            }
            for (FileLine fileLine : widgetPart.getBody()) {
                String parameter = fileLine.getLine();
                if(parameter.trim().startsWith("//")){ //$NON-NLS-1$
                    continue;
                }
                String[] row = parameter.split("="); //$NON-NLS-1$
                if(row.length!=2){
                    throw new WrongADLFormatException(Messages.ADLMonitor_WrongADLFormatException_Begin+parameter+Messages.ADLMonitor_WrongADLFormatException_End);
                }
                if(FileLine.argEquals(row[0], "clr")){ //$NON-NLS-1$
                    setLineColor(FileLine.getIntValue(row[1]));
                }else if(FileLine.argEquals(row[0], "chan")){ //$NON-NLS-1$
                    setChannel(FileLine.getTrimmedValue(row[1]));
                }else if(FileLine.argEquals(row[0], "utilChan")){ //$NON-NLS-1$
                    // Ignore
                }else {
                    throw new WrongADLFormatException(Messages.ADLMonitor_WrongADLFormatException_Parameter_Begin+row[0]+Messages.ADLMonitor_WrongADLFormatException_Parameter_End+parameter);
                }
            }
        }

        catch (WrongADLFormatException ex) {
            ex.printStackTrace();
        }

    }

    /**
     * @param lineColor the lineColor to set
     */
    private void setLineColor(int lineColor) {
        this.lineColor = lineColor;
    }

    /**
     * @return the lineColor
     */
    public int getLineColor() {
        return lineColor;
    }

    /**
     * @return the _hasLimits
     */
    public boolean is_hasLimits() {
        return _hasLimits;
    }

    public ADLLimits getAdlLimits(){
        return _adlLimit;
    }
    /**
     * @return the channel
     */
    public String getChannel() {
        return channel;
    }

    /**
     * @param channel the channel to set
     */
    private void setChannel(String channel) {
        this.channel = channel;
    }

}

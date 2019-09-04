package org.csstudio.utility.adlparser.fileParser.widgetParts;

import org.csstudio.utility.adlparser.fileParser.ADLResource;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.FileLine;
import org.csstudio.utility.adlparser.fileParser.WrongADLFormatException;
import org.csstudio.utility.adlparser.internationalization.Messages;

public class ADLLimits extends WidgetPart {
    /**Source of LOPR */
    private String loprSrc;
    /**Default value for LOPR */
    private float loprDefault;
    /**Source of HOPR */
    private String hoprSrc;
    /**Default value for HOPR */
    private float hoprDefault;
    /**Source of PREC */
    private String precSrc;
    /**Default value for PREC */
    private int precDefault;

    public ADLLimits(ADLWidget widgetPart) throws WrongADLFormatException {
        super(widgetPart);
    }

    /**
     * Default constructor
     */
    public ADLLimits(){
        super();
    }

    @Override
    public Object[] getChildren() {
        Object[] ret = new Object[6];
        ret[0] = new ADLResource(ADLResource.LOPR_SRC, new String(loprSrc));
        ret[1] = new ADLResource(ADLResource.LOPR_DEFAULT, new Float(loprDefault));
        ret[2] = new ADLResource(ADLResource.HOPR_SRC, new String(hoprSrc));
        ret[3] = new ADLResource(ADLResource.HOPR_DEFAULT, new Float(hoprDefault));
        ret[4] = new ADLResource(ADLResource.PREC_SRC, new String(precSrc));
        ret[5] = new ADLResource(ADLResource.PREC_DEFAULT, new Float(precDefault));

        return ret;
    }

    @Override
    void init() {
        name = new String("limits");
        loprSrc = new String("Channel");
        hoprSrc = new String("Channel");
        precSrc = new String("Channel");
        loprDefault = 0.0f;
        hoprDefault = 0.0f;
        precDefault = 0;
    }

    @Override
    void parseWidgetPart(ADLWidget adlObject) throws WrongADLFormatException {
        assert adlObject.isType("limits") : Messages.ADLObject_AssertError_Begin+adlObject.getType()+Messages.ADLObject_AssertError_End+"\r\n"+adlObject; //$NON-NLS-1$
        for (FileLine fileLine : adlObject.getBody()) {
            String parameter = fileLine.getLine();
            if(parameter.trim().startsWith("//")){ //$NON-NLS-1$
                continue;
            }
            String[] row = parameter.split("=");
            if (row.length !=2){
                throw new WrongADLFormatException(Messages.ADLObject_WrongADLFormatException_Begin+parameter+Messages.ADLObject_WrongADLFormatException_End);
            }
            row[1] = row[1].replaceAll("\"", "").trim();
            if (FileLine.argEquals(row[0], "loprsrc")){
                if (row[1].startsWith("$")){
                    //TODO ADLLimits Figure out what to do with macro in loprSrc
                    loprSrc = "Channel";
                }
                else {
                    loprSrc = FileLine.getTrimmedValue(row[1]);
                }
            }
            if (FileLine.argEquals(row[0], "loprdefault")){
                if (row[1].startsWith("$")){
                    //TODO ADLLimits Figure out what to do with macro in loprSrc
                    loprDefault = 0;
                }
                else {
                    loprDefault = FileLine.getFloatValue(row[1]);
                }
            }
            if (FileLine.argEquals(row[0], "hoprsrc")){
                if (row[1].startsWith("$")){
                    //TODO ADLLimits Figure out what to do with macro in hoprSrc
                    hoprSrc = "Channel";
                }
                else {
                    setHoprSrc(FileLine.getTrimmedValue(row[1]));
                }
            }
            if (FileLine.argEquals(row[0], "hoprdefault")){
                if (row[1].startsWith("$")){
                    //TODO ADLLimits Figure out what to do with macro in hoprSrc
                    hoprDefault = 0;
                }
                else {
                    hoprDefault = FileLine.getFloatValue(row[1]);
                }
            }
            if (FileLine.argEquals(row[0], "precsrc")){
                if (row[1].startsWith("$")){
                    //TODO ADLLimits Figure out what to do with macro in precSrc
                    precSrc = "Channel";
                }
                else {
                    precSrc = FileLine.getTrimmedValue(row[1]);
                }
            }
            if (FileLine.argEquals(row[0], "precdefault")){
                if (row[1].startsWith("$")){
                    //TODO Figure out what to do with macro in precDefault
                    precDefault = 0;
                }
                else {
                    precDefault = FileLine.getIntValue(row[1]);
                }
            }
        }
    }

    public float getLoprDefault() {
        return loprDefault;
    }

    public void setLoprDefault(float loprDefault) {
        this.loprDefault = loprDefault;
    }

    public float getHoprDefault() {
        return hoprDefault;
    }

    public void setHoprDefault(float hoprDefault) {
        this.hoprDefault = hoprDefault;
    }

    public int getPrecDefault() {
        return precDefault;
    }

    public void setPrecDefault(int precDefault) {
        this.precDefault = precDefault;
    }

    public String getLoprSrc() {
        return loprSrc;
    }

    public void setLoprSrc(String _loprSrc) {
        this.loprSrc = _loprSrc;
    }

    public String getHoprSrc() {
        return hoprSrc;
    }

    public void setHoprSrc(String _hoprSrc) {
        this.hoprSrc = _hoprSrc;
    }

    public String getPrecSrc() {
        return precSrc;
    }

    public void setPrecSrc(String _precSrc) {
        this.precSrc = _precSrc;
    }

}

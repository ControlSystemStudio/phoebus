package org.csstudio.utility.adlparser.fileParser.widgetParts;

import org.csstudio.utility.adlparser.fileParser.ADLResource;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.FileLine;
import org.csstudio.utility.adlparser.fileParser.WrongADLFormatException;
import org.csstudio.utility.adlparser.internationalization.Messages;

public class ADLPlotcom extends WidgetPart {
    /**
     * The foreground color.
     */
    private int _clr;
    /**
     * The background color.
     */
    private int _bclr;
    private String title;
    private String xLabel;
    private String yLabel;
    private boolean _isBackColorDefined;
    private boolean _isForeColorDefined;


    /**
     *
     * @param widgetPart
     * @throws WrongADLFormatException
     */
    public ADLPlotcom(ADLWidget widgetPart) throws WrongADLFormatException {
        super(widgetPart);
    }

    /**
     * Default constructor
     */
    public ADLPlotcom(){
        super();
    }

    @Override
    public Object[] getChildren() {
        Object[] ret = new Object[5];
        ret[0] = new ADLResource(ADLResource.FOREGROUND_COLOR, new Integer(_clr));
        ret[1] = new ADLResource(ADLResource.BACKGROUND_COLOR, new Integer(_bclr));
        ret[2] = new ADLResource(ADLResource.CHANNEL, title);
        ret[3] = new ADLResource(ADLResource.PLOT_XLABEL, xLabel);
        ret[4] = new ADLResource(ADLResource.PLOT_YLABEL, yLabel);

        return ret;
    }

    @Override
    void init() {
        name = new String("plotcom");
        title = "";
        xLabel = "";
        yLabel = "";
    }

    @Override
    void parseWidgetPart(ADLWidget widgetPart) throws WrongADLFormatException {
           assert widgetPart.isType("object") : Messages.ADLObject_AssertError_Begin+widgetPart.getType()+Messages.ADLObject_AssertError_End+"\r\n"+widgetPart; //$NON-NLS-1$
             for (FileLine fileLine : widgetPart.getBody()) {
                 String parameter = fileLine.getLine();
                 if(parameter.trim().startsWith("//")){ //$NON-NLS-1$
                     continue;
                 }
                 String[] row = parameter.split("="); //$NON-NLS-1$
                 if(row.length!=2){
                     throw new WrongADLFormatException(Messages.ADLObject_WrongADLFormatException_Begin+parameter+Messages.ADLObject_WrongADLFormatException_End);
                 }
                 row[1] = row[1].replaceAll("\"", "").trim();
                if (FileLine.argEquals(row[0], "title")){
                    setTitle(FileLine.getTrimmedValue(row[1]));
                }
                else if (FileLine.argEquals(row[0], "xlabel")){
                    setXLabel(FileLine.getTrimmedValue(row[1]));
                }
                else if (FileLine.argEquals(row[0], "ylabel")){
                    setYLabel(FileLine.getTrimmedValue(row[1]));
                } else if(FileLine.argEquals(row[0], "clr")){ //$NON-NLS-1$
                    set_clr(FileLine.getIntValue(row[1]));
                    set_isForeColorDefined(true);
                }else if(FileLine.argEquals(row[0], "bclr")){ //$NON-NLS-1$
                    set_bclr(FileLine.getIntValue(row[1]));
                    set_isBackColorDefined(true);
                }else if(FileLine.argEquals(row[0], "package")){ //$NON-NLS-1$
                    // Ignore
                }else {
                     throw new WrongADLFormatException(Messages.ADLObject_WrongADLFormatException_Parameter_Begin+fileLine+Messages.ADLObject_WrongADLFormatException_Parameter_End);
                }
             }

    }

    /**
     * @param _clr the _clr to set
     */
    public void set_clr(int _clr) {
        this._clr = _clr;
    }

    /**
     * @return the _clr
     */
    public int getForegroundColor() {
        return _clr;
    }

    /**
     * @param _bclr the _bclr to set
     */
    public void set_bclr(int _bclr) {
        this._bclr = _bclr;
    }

    /**
     * @return the _bclr
     */
    public int getBackgroundColor() {
        return _bclr;
    }

    /**
     * @param _isBackColorDefined the _isBackColorDefined to set
     */
    public void set_isBackColorDefined(boolean _isBackColorDefined) {
        this._isBackColorDefined = _isBackColorDefined;
    }

    /**
     * @return the _isBackColorDefined
     */
    public boolean isBackColorDefined() {
        return _isBackColorDefined;
    }

    /**
     * @param _isForeColorDefined the _isForeColorDefined to set
     */
    public void set_isForeColorDefined(boolean _isForeColorDefined) {
        this._isForeColorDefined = _isForeColorDefined;
    }

    /**
     * @return the _isForeColorDefined
     */
    public boolean isForeColorDefined() {
        return _isForeColorDefined;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param xLabel the xLabel to set
     */
    public void setXLabel(String xLabel) {
        this.xLabel = xLabel;
    }

    /**
     * @return the xLabel
     */
    public String getXLabel() {
        return xLabel;
    }

    /**
     * @param yLabel the yLabel to set
     */
    public void setYLabel(String yLabel) {
        this.yLabel = yLabel;
    }

    /**
     * @return the yLabel
     */
    public String getYLabel() {
        return yLabel;
    }

}

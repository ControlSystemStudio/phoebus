package org.csstudio.utility.adlparser.fileParser.widgetParts;

import org.csstudio.utility.adlparser.fileParser.ADLResource;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.FileLine;
import org.csstudio.utility.adlparser.fileParser.WrongADLFormatException;

public abstract class ADLConnected extends WidgetPart {

    private int _clr;
    private int _bclr;
    protected String _chan;
    private boolean _isBackColorDefined;
    private boolean _isForeColorDefined;
    protected String assertBeginMsg;
    protected String assertEndMsg;
    protected String exceptionBeginMsg;
    protected String exceptionEndMsg;
    protected String exceptionBeginParameterMsg;
    protected String exceptionEndParameterMsg;
    protected String oldChannelName;

    public ADLConnected(ADLWidget widgetPart) throws WrongADLFormatException {
        super(widgetPart);
    }

    public ADLConnected() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void parseWidgetPart(final ADLWidget adlWidget)
            throws WrongADLFormatException {
                assert adlWidget.isType(name) : assertBeginMsg+adlWidget.getType()+assertEndMsg; //$NON-NLS-1$

                for (FileLine fileLine : adlWidget.getBody()) {
                    String parameter = fileLine.getLine();
                    if(parameter.trim().startsWith("//")){ //$NON-NLS-1$
                        continue;
                    }
                    String[] row = parameter.split("="); //$NON-NLS-1$
                    if(row.length!=2){
                        throw new WrongADLFormatException(exceptionBeginMsg+parameter+exceptionEndMsg);
                    }
                    if(FileLine.argEquals(row[0], "clr")){ //$NON-NLS-1$
                        _clr=FileLine.getIntValue(row[1]);
                        set_isForeColorDefined(true);
                    }else if(FileLine.argEquals(row[0], "bclr")){ //$NON-NLS-1$
                        _bclr=FileLine.getIntValue(row[1]);
                        set_isBackColorDefined(true);
                    }else if(FileLine.argEquals(row[0], "chan")){   //$NON-NLS-1$
                        _chan = FileLine.getTrimmedValue(row[1]);
                    }else if(FileLine.argEquals(row[0], oldChannelName)){ // Name was changed to chan later.//$NON-NLS-1$
                        _chan = FileLine.getTrimmedValue(row[1]);
                    }else {
                        throw new WrongADLFormatException(exceptionBeginParameterMsg+row[0]+exceptionEndParameterMsg);
                    }
                }
            }

    /**
     * @return child objects
     */
    @Override
    public Object[] getChildren() {
        Object[] ret = new Object[3];
        ret[0] = new ADLResource(ADLResource.FOREGROUND_COLOR, new Integer(_clr));
        ret[1] = new ADLResource(ADLResource.BACKGROUND_COLOR, new Integer(_bclr));
        ret[2] = new ADLResource(ADLResource.CHANNEL, _chan);
        return ret;
    }

    /**
     * @return background Color
     */
    public int getBackgroundColor() {
        return _bclr;
    }

    /**
     * @return background Color
     */
    public int getForegroundColor() {
        return _clr;
    }

    /**
     * @return the channel
     */
    public String getChan() {
        return _chan;
    }

    /**
     * @param _isBackColorDefined the _isBackColorDefined to set
     */
    protected void set_isBackColorDefined(boolean _isBackColorDefined) {
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
    protected void set_isForeColorDefined(boolean _isForeColorDefined) {
        this._isForeColorDefined = _isForeColorDefined;
    }

    /**
     * @return the _isForeColorDefined
     */
    public boolean isForeColorDefined() {
        return _isForeColorDefined;
    }

}
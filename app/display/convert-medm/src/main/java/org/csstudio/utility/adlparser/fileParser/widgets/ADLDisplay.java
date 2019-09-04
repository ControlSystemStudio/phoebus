package org.csstudio.utility.adlparser.fileParser.widgets;

import java.util.ArrayList;

import org.csstudio.utility.adlparser.fileParser.ADLResource;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.FileLine;
import org.csstudio.utility.adlparser.fileParser.WrongADLFormatException;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLObject;
import org.csstudio.utility.adlparser.internationalization.Messages;

/**
 *
 * @author hammonds
 *
 */
public class ADLDisplay extends ADLAbstractWidget {
    private int _clr;
    private int _bclr;
    private boolean _snapToGrid = false;
    private boolean _gridOn = false;
    private int _gridSpacing = 5;
    private boolean _isBackColorDefined;
    private boolean _isForeColorDefined;

    public ADLDisplay(ADLWidget adlWidget) {
        super(adlWidget);
        set_isBackColorDefined(false);
        set_isForeColorDefined(false);
        name = String.valueOf("display");
        try {
            for (ADLWidget childWidget : adlWidget.getObjects()) {
                if (childWidget.getType().equals("object")){
                    _adlObject = new ADLObject(childWidget);
                    if (_adlObject != null){
                        _hasObject = true;
                    }

                }
            }
            for (FileLine fileLine : adlWidget.getBody()) {
                String parameter = fileLine.getLine();
                if(parameter.trim().startsWith("//")){ //$NON-NLS-1$
                    continue;
                }
                String[] row = parameter.split("="); //$NON-NLS-1$
                if(row.length!=2){
                    throw new WrongADLFormatException(Messages.ADLMonitor_WrongADLFormatException_Begin+parameter+Messages.ADLMonitor_WrongADLFormatException_End);
                }
                if(FileLine.argEquals(row[0], "clr")){ //$NON-NLS-1$
                    set_clr(FileLine.getIntValue(row[1]));
                    set_isForeColorDefined(true);
                }else if(FileLine.argEquals(row[0], "bclr")){ //$NON-NLS-1$
                    set_bclr(FileLine.getIntValue(row[1]));
                    set_isBackColorDefined(true);
                }else if(FileLine.argEquals(row[0], "gridSpacing")){ //$NON-NLS-1$
                    set_gridSpacing(FileLine.getIntValue(row[1]));
                }else if(FileLine.argEquals(row[0], "gridOn")){ //$NON-NLS-1$
                    set_gridOn(FileLine.getBooleanValue(row[1]));
                }else if(FileLine.argEquals(row[0], "snapToGrid")){ //$NON-NLS-1$
                    set_snapToGrid(FileLine.getBooleanValue(row[1]));
                }else if(FileLine.argEquals(row[0], "cmap")){ //$NON-NLS-1$
                    //TODO figure out cmap in ADLDisplay
                }else {
                    throw new WrongADLFormatException("\n"+Messages.ADLMonitor_WrongADLFormatException_Parameter_Begin+" "+row[0]+"\n "+Messages.ADLMonitor_WrongADLFormatException_Parameter_End+" "+row[1]);
                }
            }
        }

        catch (WrongADLFormatException ex) {
            ex.printStackTrace();
        }
    }


    /**
     * @param _snapToGrid the _snapToGrid to set
     */
    public void set_snapToGrid(boolean _snapToGrid) {
        this._snapToGrid = _snapToGrid;
    }

    /**
     * @return the _snapToGrid
     */
    public boolean is_snapToGrid() {
        return _snapToGrid;
    }

    /**
     * @param _gridOn the _gridOn to set
     */
    public void set_gridOn(boolean _gridOn) {
        this._gridOn = _gridOn;
    }

    /**
     * @return the _gridOn
     */
    public boolean is_gridOn() {
        return _gridOn;
    }

    /**
     * @param _gridSpacing the _gridSpacing to set
     */
    public void set_gridSpacing(int _gridSpacing) {
        this._gridSpacing = _gridSpacing;
    }

    /**
     * @return the _gridSpacing
     */
    public int get_gridSpacing() {
        return _gridSpacing;
    }


    /**
     * @param _clr the _clr to set
     */
    private void set_clr(int _clr) {
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
    private void set_bclr(int _bclr) {
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
    private void set_isBackColorDefined(boolean _isBackColorDefined) {
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
    private void set_isForeColorDefined(boolean _isForeColorDefined) {
        this._isForeColorDefined = _isForeColorDefined;
    }


    /**
     * @return the _isForeColorDefined
     */
    public boolean isForeColorDefined() {
        return _isForeColorDefined;
    }


    @Override
    public Object[] getChildren() {
        ArrayList<Object> ret = new ArrayList<>();
        if (_hasObject) ret.add( _adlObject);
        if (_isBackColorDefined) ret.add(new ADLResource(ADLResource.BACKGROUND_COLOR, Integer.valueOf(_bclr)) );
        if (_isForeColorDefined) ret.add(new ADLResource(ADLResource.FOREGROUND_COLOR, Integer.valueOf(_bclr)) );
        ret.add(new ADLResource(ADLResource.SNAP_TO_GRID, Boolean.valueOf(_snapToGrid)) );
        ret.add(new ADLResource(ADLResource.GRID_ON, Boolean.valueOf(_gridOn)));
        ret.add(new ADLResource(ADLResource.GRID_SPACING, Integer.valueOf(_gridSpacing)));
        return ret.toArray();
    }


}

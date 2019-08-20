package org.csstudio.utility.adlparser.fileParser.widgets;

import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLBasicAttribute;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLControl;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLDynamicAttribute;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLLimits;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLMonitor;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLObject;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLPlotcom;
import org.csstudio.utility.adlparser.fileParser.widgetParts.ADLPoints;
import org.csstudio.utility.adlparser.fileParser.widgetParts.RelatedDisplayItem;

/**
 *
 * @author hammonds
 *
 */
public abstract class ADLAbstractWidget {
    protected ADLObject _adlObject= null;
    protected ADLBasicAttribute _adlBasicAttribute= null;
    protected ADLControl _adlControl= null;
    protected ADLMonitor _adlMonitor= null;
    protected ADLPoints _adlPoints= null;
    protected RelatedDisplayItem[] _relatedDisplayItem = null;
    protected ADLDynamicAttribute _adlDynamicAttribute = null;
    protected ADLLimits _adlLimits = null;
    protected ADLPlotcom _adlPlotcom = null;

    protected boolean _hasBasicAttribute = false;
    protected boolean _hasObject = false;
    protected boolean _hasControl = false;
    protected boolean _hasMonitor = false;
    protected boolean _hasPoints = false;
    protected boolean _hasRelatedDisplayItem = false;
    protected boolean _hasDynamicAttribute = false;
    protected boolean _hasLimits = false;
    protected boolean _hasPlotcom = false;

    protected String name = new String();

    private final int _objectNr;

    public ADLAbstractWidget(final ADLWidget adlWidget){
        _objectNr = adlWidget.getObjectNr();
    }

    public final int getObjectNr() {
        return _objectNr;
    }

    public final String getName(){
        return name;
    }
    abstract public Object[] getChildren();

    public boolean hasADLObject(){
        return _hasObject;
    }

    public boolean hasADLBasicAttribute(){
        return _hasBasicAttribute;
    }

    public boolean hasADLControl(){
        return _hasControl;
    }

    public boolean hasADLMonitor(){
        return _hasMonitor;
    }

    public boolean hasADLPoints(){
        return _hasPoints;
    }

    public boolean hasRelatedDisplayItem(){
        return _hasRelatedDisplayItem;
    }

    public boolean hasADLDynamicAttribute(){
        return _hasDynamicAttribute;
    }

    public boolean hasADLLimits(){
        return _hasLimits;
    }

    public boolean hasADLPlotcom(){
        return _hasPlotcom;
    }


    /**
     * @return ADLObject
     */
    public ADLObject getAdlObject(){
        return _adlObject;
    }

    /**
     * @return ADLBasicAttribute
     */
    public ADLBasicAttribute getAdlBasicAttribute(){
        return _adlBasicAttribute;
    }

    /**
     * @return ADLControl
     */
    public ADLControl getAdlControl(){
        return _adlControl;
    }

    /**
     * @return ADLMonitor
     */
    public ADLMonitor getAdlMonitor(){
        return _adlMonitor;
    }

    /**
     * @return ADLPoints
     */
    public ADLPoints getAdlPoints(){
        return _adlPoints;
    }

    /**
     * @return RelatedDisplayItem[]
     */
    public RelatedDisplayItem[] getRelatedDisplayItem(){
        return _relatedDisplayItem;
    }

    /**
     * @return ADLDynamicAttribute
     */
    public ADLDynamicAttribute getAdlDynamicAttribute(){
        return _adlDynamicAttribute;
    }

    /**
     * @return ADLLimits
     */
    public ADLLimits getAdlLimits(){
        return _adlLimits;
    }

    /**
     * @return ADLPlotcom
     */
    public ADLPlotcom getAdlPlotcom(){
        return _adlPlotcom;
    }

    public void setAdlBasicAttribute(ADLBasicAttribute basAttr){
        _adlBasicAttribute = basAttr;
        _hasBasicAttribute = true;
    }

    public void setAdlDynamicAttribute(ADLDynamicAttribute dynAttr){
        _adlDynamicAttribute = dynAttr;
        _hasDynamicAttribute = true;
    }
}

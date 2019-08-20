/*
 * Copyright (c) 2007 Stiftung Deutsches Elektronen-Synchrotron,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS.
 * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE
 * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR
 * CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE.
 * NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
 * DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS,
 * OR MODIFICATIONS.
 * THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION, MODIFICATION,
 * USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY
 * AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
 */
/*
 * $Id$
 */
package org.csstudio.utility.adlparser.fileParser.widgetParts;

import org.csstudio.utility.adlparser.fileParser.ADLResource;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.FileLine;
import org.csstudio.utility.adlparser.fileParser.WrongADLFormatException;
import org.csstudio.utility.adlparser.internationalization.Messages;

/**
 * @author hrickens
 * @author $Author$
 * @version $Revision$
 * @since 11.09.2007
 */
@SuppressWarnings("nls")
public class ADLDynamicAttribute extends WidgetPart{
    //TODO Strip out old code lines that refer to SDS implementations

    /**
     * The Color.
     */
    private String clrMode;
    /**
     * Visibility of the Widget.
     */
    private String _vis;
    /**
     * The Channel.
     */
    private String _chan;
    private String _chanb;
    private String _chanc;
    private String _chand;
    private String _calc;

    /**
     * The Color rule.
     */
    private String _colorRule;
    /**
     * If the Dynamic Attribute a boolean Attribute.
     */
    private boolean _bool;
    /**
     * If the Dynamic Attribute a color Attribute.
     */
    private boolean _color;

    /**
     * The default constructor.
     *
     * @param adlDynamicAttribute An ADLWidget that correspond a ADL Dynamic Attribute.
     * @param parentWidgetModel The Widget that set the parameter from ADLWidget.
     * @throws WrongADLFormatException Wrong ADL format or untreated parameter found.
     */
    public ADLDynamicAttribute(final ADLWidget adlDynamicAttribute) throws WrongADLFormatException {
        super(adlDynamicAttribute);
    }

    public ADLDynamicAttribute() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void init() {
        name = String.valueOf("dynamic attribute");
        set_vis("static");
        setClrMode("static");
//        set_isColorDefined(false);
        _chan = String.valueOf("");
        _chanb = String.valueOf("");
        _chanc = String.valueOf("");
        _chand = String.valueOf("");
        _calc = String.valueOf("");

    }

    /**
     * {@inheritDoc}
     */
    @Override
    final void parseWidgetPart(final ADLWidget adlDynamicAttribute) throws WrongADLFormatException {

        assert adlDynamicAttribute.isType("dynamic attribute") : Messages.ADLDynamicAttribute_AssertError_Begin+adlDynamicAttribute.getType()+Messages.ADLDynamicAttribute_AssertError_End;
        for (ADLWidget adlWidget : adlDynamicAttribute.getObjects()) {
            if(adlWidget.getType().equals("attr")){
                for (FileLine fileLine : adlWidget.getBody()) {
                    adlDynamicAttribute.addBody(fileLine);
                }
            }
        }

        _bool=false;
        _color=false;

        for (FileLine parameter : adlDynamicAttribute.getBody()) {
            if(parameter.getLine().trim().startsWith("//")){
                continue;
            }
            String head = parameter.getLine().replaceAll("\"", "").split("=")[0];
            String[] row = {parameter.getLine().replaceAll("\"", "").substring(head.length()+1)};
            head=head.trim().toLowerCase();
            if(head.equals("clr")){
                clrMode=FileLine.getTrimmedValue(row[0]);
            }else if(head.equals("vis")){
                set_vis(FileLine.getTrimmedValue(row[0]));
            }else if(head.equals("chan")){
                set_chan(FileLine.getTrimmedValue(row[0]));
            }else if(head.equals("chanb")){
                set_chanb(FileLine.getTrimmedValue(row[0]));
            }else if(head.equals("chanc")){
                set_chanc(FileLine.getTrimmedValue(row[0]));
            }else if(head.equals("chand")){
                set_chand(FileLine.getTrimmedValue(row[0]));
            }else if(head.equals("colorrule")){
                _colorRule=FileLine.getTrimmedValue(row[0]);
            }else if(head.equals("calc")){
                set_calc(FileLine.getTrimmedValue(row[0]));
            }else {
                throw new WrongADLFormatException(Messages.ADLDynamicAttribute_WrongADLFormatException_Parameter_Begin+parameter+Messages.ADLDynamicAttribute_WrongADLFormatException_Parameter_End);
            }
        }
    }


    /**
     * @return true when the DynamicAttribute have a <b>boolean rule</b> otherwise false.
     */
    public final boolean isBoolean() {
        return _bool;
    }

    /**
     * @return true when the DynamicAttribute have a <b>color rule</b> otherwise false.
     */
    public final boolean isColor() {
        return _color;
    }

    @Override
    public Object[] getChildren() {
        Object[] ret = new Object[4];
        ret[0] = new ADLResource(ADLResource.FOREGROUND_COLOR, clrMode);
        ret[1] = new ADLResource(ADLResource.VISIBILITY, _vis);
        ret[2] = new ADLResource(ADLResource.CHANNEL, _chan);
        ret[2] = new ADLResource(ADLResource.CHANNELB, _chanb);
        ret[2] = new ADLResource(ADLResource.CHANNELC, _chanc);
        ret[2] = new ADLResource(ADLResource.CHANNELD, _chand);
        ret[2] = new ADLResource(ADLResource.CALC, _chan);
        ret[3] = new ADLResource(ADLResource.COLOR_RULE, _colorRule);
        return ret;
    }

    public String getClrMode() {
        return clrMode;
    }

    protected void setClrMode(String clrMode) {
        this.clrMode = clrMode;
    }

//    /**
//     * @param _isColorDefined the _isColorDefined to set
//     */
//    private void set_isColorDefined(boolean _isColorDefined) {
//        this._isColorDefined = _isColorDefined;
//    }
//
//    /**
//     * @return the _isColorDefined
//     */
//    public boolean isColorDefined() {
//        return _isColorDefined;
//    }

    /**
     * @param _chanb the _chanb to set
     */
    public void set_chanb(String _chanb) {
        this._chanb = _chanb;
    }

    /**
     * @return the _chanb
     */
    public String get_chanb() {
        return _chanb;
    }

    /**
     * @param _chanc the _chanc to set
     */
    public void set_chanc(String _chanc) {
        this._chanc = _chanc;
    }

    /**
     * @return the _chanc
     */
    public String get_chanc() {
        return _chanc;
    }

    /**
     * @param _chand the _chand to set
     */
    public void set_chand(String _chand) {
        this._chand = _chand;
    }

    /**
     * @return the _chand
     */
    public String get_chand() {
        return _chand;
    }

    /**
     * @param _calc the _calc to set
     */
    public void set_calc(String _calc) {
        this._calc = _calc;
    }

    /**
     * @return the _calc
     */
    public String get_calc() {
        return _calc;
    }

    /**
     * @param _vis the _vis to set
     */
    public void set_vis(String _vis) {
        this._vis = _vis;
    }

    /**
     * @return the _vis
     */
    public String get_vis() {
        return _vis;
    }

    /**
     * @param _chan the _chan to set
     */
    public void set_chan(String _chan) {
        this._chan = _chan;
    }

    /**
     * @return the _chan
     */
    public String get_chan() {
        return _chan;
    }
}

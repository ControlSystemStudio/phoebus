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

//**import org.csstudio.sds.components.model.ArcModel;
//**import org.csstudio.sds.components.model.RectangleModel;
//**import org.csstudio.sds.model.AbstractWidgetModel;
//**import org.csstudio.sds.model.DynamicsDescriptor;
import org.csstudio.utility.adlparser.internationalization.Messages;
//**import org.csstudio.utility.adlparser.fileParser.ADLHelper;
import org.csstudio.utility.adlparser.fileParser.ADLResource;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.FileLine;
import org.csstudio.utility.adlparser.fileParser.WrongADLFormatException;

/**
 * @author hrickens
 * @author $Author$
 * @version $Revision$
 * @since 05.09.2007
 */
public class ADLBasicAttribute extends WidgetPart{

    /**
     * The default constructor.
     *
     * @param adlBasicAttribute An ADLWidget that correspond a ADL Basic Attribute.
     * @param parentWidgetModel The Widget that set the parameter from ADLWidget.
     * @throws WrongADLFormatException Wrong ADL format or untreated parameter found.
     */
    public ADLBasicAttribute(final ADLWidget adlBasicAttribute) throws WrongADLFormatException {
        super(adlBasicAttribute);
    }

    public ADLBasicAttribute() {
        super();
    }

    /** The Color of (front?) Object. */
    private int _clr;
    /** width of the Border. */
    private int _width;
    /** The style of border. */
    private String _style;
    /** Is type of fill.*/
    private String _fill;

    private boolean _isColorDefined;

    /**
     * {@inheritDoc}
     */
    @Override
    void init() {
        name = String.valueOf("basic attribute");
        _clr = 0;
        _width = 0;
        _style =  String.valueOf("solid");
        _fill = String.valueOf("solid");
        set_isColorDefined(false);
    }

    /**
     * @param adlBasicAttribute The ADL String for an ADL Basic Attribute.
     * @throws WrongADLFormatException this exception was thrown the String not an valid ADL Basic Attribute String.
     */
    @Override
    final void parseWidgetPart(final ADLWidget adlBasicAttribute) throws WrongADLFormatException {
        assert adlBasicAttribute.isType("basic attribute") : Messages.ADLBasicAttribute_AssertError_Begin+adlBasicAttribute.getType()+Messages.ADLBasicAttribute_AssertError_End; //$NON-NLS-1$
        for (ADLWidget adlWidget : adlBasicAttribute.getObjects()) {
            if(adlWidget.getType().equals("attr")){
                for (FileLine fileLine : adlWidget.getBody()) {
                    //TODO Refactor this so that it is not necessary to copy the lines to from the attr object to the atrribute object
                    adlBasicAttribute.addBody(fileLine);
                }
            }
        }
        for (FileLine parameter : adlBasicAttribute.getBody()) {
            if(parameter.getLine().trim().startsWith("//")){ //$NON-NLS-1$
                continue;
            }
            String[] row = parameter.getLine().split("="); //$NON-NLS-1$
            if(row.length!=2){
                throw new WrongADLFormatException(Messages.ADLBasicAttribute_WrongADLFormatException_Begin+parameter+Messages.ADLBasicAttribute_WrongADLFormatException_End);
            }
            if(FileLine.argEquals(row[0],"clr")){ //$NON-NLS-1$
                _clr=FileLine.getIntValue(row[1]);
                set_isColorDefined(true);
            }else if(FileLine.argEquals(row[0],"width")){ //$NON-NLS-1$
                _width=FileLine.getIntValue(row[1]);
            }else if(FileLine.argEquals(row[0],"style")){ //$NON-NLS-1$
                _style=FileLine.getTrimmedValue(row[1]);
            }else if(FileLine.argEquals(row[0],"fill")){ //$NON-NLS-1$
                _fill=FileLine.getTrimmedValue(row[1]);
            }else {
                throw new WrongADLFormatException(Messages.ADLBasicAttribute_WrongADLFormatException_Parameter_Begin+parameter+Messages.ADLBasicAttribute_WrongADLFormatException_Parameter_End);
            }
        }
    }


    /** @return the Color */
    public final int getClr() {
        return _clr;
    }


    /** @return the width */
    public final int getWidth() {
        return _width;
    }


    /** @return the style */
    public final String getStyle() {
        return _style;
    }

    /** @return the fill */
    public final String getFill() {
        return _fill;
    }


    /**
     * @return child objects
     */
    @Override
    public Object[] getChildren(){
        Object[] ret = new Object[4];
        ret[0] = new ADLResource(ADLResource.FOREGROUND_COLOR, new Integer(_clr));
        ret[1] = new ADLResource(ADLResource.LINE_WIDTH, new Integer(_width));
        ret[2] = new ADLResource(ADLResource.STYLE, _style);
        ret[3] = new ADLResource(ADLResource.FILL, _fill);

        return ret;
    }

    /**
     * @param _isColorDefined the _isColorDefined to set
     */
    public void set_isColorDefined(boolean _isColorDefined) {
        this._isColorDefined = _isColorDefined;
    }

    /**
     * @return the _isColorDefined
     */
    public boolean isColorDefined() {
        return _isColorDefined;
    }
}

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

//**import org.csstudio.sds.internal.rules.ParameterDescriptor;
//**import org.csstudio.sds.model.AbstractWidgetModel;
//**import org.csstudio.sds.model.DynamicsDescriptor;
import org.csstudio.utility.adlparser.internationalization.Messages;
//**import org.csstudio.utility.adlparser.fileParser.ADLHelper;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.FileLine;
import org.csstudio.utility.adlparser.fileParser.WrongADLFormatException;

/**
 * @author hrickens
 * @author $Author$
 * @version $Revision$
 * @since 08.11.2007
 */
public class ADLSensitive extends WidgetPart {
    //TODO Strip out old code lines that refer to SDS implementations
    /**
     * The channel.
     */
    private String _chan;
    /**
     * The used rule to set the Sensitive.
     */
    private String _sensitiveMode;
    /**
     * The Record property/Feldname.
     */
    private String _postfix;

    /**
     * The default constructor.
     *
     * @param sensitive An ADLWidget that correspond a ADL Sensitive Item.
     * @param parentWidgetModel The Widget that set the parameter from ADLWidget.
     * @throws WrongADLFormatException Wrong ADL format or untreated parameter found.
     */
    public ADLSensitive(final ADLWidget sensitive) throws WrongADLFormatException {
        super(sensitive);
    }

    public ADLSensitive(){
        super();
    }

//**    /**
  //**     * {@inheritDoc}
  //**     */
  //**    @Override
  //**    final void generateElements() {
  //**        if(_chan!=null&&_chan[0].length()>0){
  //**            _postfix = ADLHelper.setChan(_widgetModel,_chan);
  //**        }else{
  //**            _postfix="";
  //**        }
  //**        if(_sensitiveMode!=null && _sensitiveMode.equals("if not zero")){ //$NON-NLS-1$
  //**            DynamicsDescriptor adlBooleanDynamicAttribute = new DynamicsDescriptor("org.css.sds.color.if_not_zero"); //$NON-NLS-1$
  //**            adlBooleanDynamicAttribute.addInputChannel(new ParameterDescriptor("$channel$"+_postfix,"")); //$NON-NLS-1$
  //**            _widgetModel.setDynamicsDescriptor(AbstractWidgetModel.PROP_ENABLED, adlBooleanDynamicAttribute);
  //**        }else if(_sensitiveMode!=null && _sensitiveMode.equals("if zero")){ //$NON-NLS-1$
  //**            DynamicsDescriptor adlBooleanDynamicAttribute = new DynamicsDescriptor("org.css.sds.color.if_zero"); //$NON-NLS-1$
  //**            adlBooleanDynamicAttribute.addInputChannel(new ParameterDescriptor("$channel$"+_postfix,"")); //$NON-NLS-1$
  //**            _widgetModel.setDynamicsDescriptor(AbstractWidgetModel.PROP_ENABLED, adlBooleanDynamicAttribute);
  //**        }
  //**    }

    /**
     * {@inheritDoc}
     */
    @Override
    void init() {
        name = new String("sensitive");

    }

    /**
     * {@inheritDoc}
     */
    @Override
    final void parseWidgetPart(final ADLWidget sensitive) throws WrongADLFormatException {
        assert sensitive.isType("sensitive") :  Messages.ADLSensitive_AssertError_Begin+sensitive.getType()+Messages.ADLSensitive_AssertError_End; //$NON-NLS-1$

        for (FileLine fileLine : sensitive.getBody()) {
            String parameter = fileLine.getLine();
            if(parameter.trim().startsWith("//")){ //$NON-NLS-1$
                continue;
            }
            String[] row = parameter.split("="); //$NON-NLS-1$
//            if(row.length!=2){
//                throw new Exception("This "+parameter+" is a wrong ADL Menu Item");
//            }
            if(FileLine.argEquals(row[0], "chan")){ //$NON-NLS-1$
//**                _chan=ADLHelper.cleanString(row[1]);
                _chan=FileLine.getTrimmedValue(row[1]);
            }else if(FileLine.argEquals(row[0], "sensitive_mode")){ //$NON-NLS-1$
                _sensitiveMode = FileLine.getTrimmedValue(row[1]);
            }else {
                throw new WrongADLFormatException(Messages.ADLSensitive_WrongADLFormatException_Begin+fileLine+Messages.ADLSensitive_WrongADLFormatException_End);
            }
        }

//        if(_chan==null&&_sensitiveMode!=null){
//            CentralLogger.getInstance().warn(this,Messages.ADLSensitive_No_Channel_Warning + sensitive);
//        }
    }

    public String getPostfix() {
        return _postfix;
    }

    @Override
    public Object[] getChildren() {
        // TODO Auto-generated method stub
        return null;
    }



}

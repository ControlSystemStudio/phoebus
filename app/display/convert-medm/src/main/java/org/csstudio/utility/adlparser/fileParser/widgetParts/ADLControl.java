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

import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.WrongADLFormatException;
import org.csstudio.utility.adlparser.internationalization.Messages;

/**
 * @author hrickens
 * @author $Author$
 * @version $Revision$
 * @since 20.09.2007
 */
public class ADLControl extends ADLConnected {

    /**
     * The default constructor.
     *
     * @param adlWidget
     *            An ADLWidget that correspond a ADL Control.
     * @param parentWidgetModel
     *            The Widget that set the parameter from ADLWidget.
     * @throws WrongADLFormatException
     *             Wrong ADL format or untreated parameter found.
     */
    public ADLControl(final ADLWidget adlWidget) throws WrongADLFormatException {
        super(adlWidget);
    }

    /**
     * Default constructor
     */
    public ADLControl() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    final void init() {
        name = new String("control");
        _chan = String.valueOf("");
        set_isForeColorDefined(false);
        set_isBackColorDefined(false);
        assertBeginMsg = Messages.ADLControl_AssertError_Begin;
        assertEndMsg = Messages.ADLControl_AssertError_End;
        exceptionBeginMsg = Messages.ADLControl_WrongADLFormatException_Begin;
        exceptionEndMsg = Messages.ADLControl_WrongADLFormatException_End;
        exceptionBeginParameterMsg = Messages.ADLControl_WrongADLFormatException_Parameter_Begin;
        exceptionEndParameterMsg = Messages.ADLControl_WrongADLFormatException_Parameter_End;
        oldChannelName = "ctrl";
    }
}

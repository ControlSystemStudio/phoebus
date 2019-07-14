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
 * @since 12.09.2007
 */
public class ADLMonitor extends ADLConnected {

    /**
     * The default constructor.
     *
     * @param adlWidget
     *            An ADLWidget that correspond a ADL Monitor.
     * @param parentWidgetModel
     *            The Widget that set the parameter from ADLWidget.
     * @throws WrongADLFormatException
     *             Wrong ADL format or untreated parameter found.
     */
    public ADLMonitor(final ADLWidget adlWidget) throws WrongADLFormatException {
        super(adlWidget);
    }

    /**
     * Default constructor
     */
    public ADLMonitor() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void init() {
        name = new String("monitor");
        _chan = String.valueOf("");
        set_isForeColorDefined(false);
        set_isBackColorDefined(false);
        assertBeginMsg = Messages.ADLMonitor_AssertError_Begin;
        assertEndMsg = Messages.ADLMonitor_AssertError_End;
        exceptionBeginMsg = Messages.ADLMonitor_WrongADLFormatException_Begin;
        exceptionEndMsg = Messages.ADLMonitor_WrongADLFormatException_End;
        exceptionBeginParameterMsg = Messages.ADLMonitor_WrongADLFormatException_Parameter_Begin;
        exceptionEndParameterMsg = Messages.ADLMonitor_WrongADLFormatException_Parameter_End;
        oldChannelName = "rdbk";
    }
}

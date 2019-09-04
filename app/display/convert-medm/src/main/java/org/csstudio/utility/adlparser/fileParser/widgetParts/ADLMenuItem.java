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

import org.csstudio.utility.adlparser.internationalization.Messages;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.FileLine;
import org.csstudio.utility.adlparser.fileParser.WrongADLFormatException;

/**
 * @author hrickens
 * @author $Author$
 * @version $Revision$
 * @since 21.09.2007
 */
public class ADLMenuItem extends WidgetPart {
    //TODO Strip out old code lines that refer to SDS implementations

    /**
     * The displayed text and description of the Action.
     */
    private String _label;
    /**
     * The type of Action.
     */
    private String _type;
    /**
     * The relative path to faceplate, script, ...
     */
    private String _command;
    /**
     * The arguments. Normal a Channel names.
     */
    private String _args;
    /**
     * The root path for Widget.
     */
    private String _path;
    /**
     * The root path for Trends.
     */
    private String _trendPath;

    /**
     * The default constructor.
     *
     * @param menuItem
     *            An ADLWidget that correspond a ADL Menu Item.
     * @param parentWidgetModel
     *            The Widget that set the parameter from ADLWidget.
     * @throws WrongADLFormatException
     *             Wrong ADL format or untreated parameter found.
     */
    public ADLMenuItem(final ADLWidget menuItem)
            throws WrongADLFormatException {
        super(menuItem);
    }

    /**
     * Default constructor
     */
    public ADLMenuItem(){
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    final void init() {
        name = new String("menu item");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    final void parseWidgetPart(final ADLWidget menuItem) throws WrongADLFormatException {
        assert menuItem.isType("menuItem") : Messages.ADLMenuItem_AssertError_Begin + menuItem.getType() + Messages.ADLMenuItem_AssertError_End; //$NON-NLS-1$

        for (FileLine fileLine : menuItem.getBody()) {
            String parameter = fileLine.getLine();
            if (parameter.trim().startsWith("//")) { //$NON-NLS-1$
                continue;
            }
            String[] row = parameter.split("="); //$NON-NLS-1$
            // if(row.length!=2){
            // throw new Exception("This "+parameter+" is a wrong ADL Menu Item");
            // }
            if (FileLine.argEquals(row[0], "label")) { //$NON-NLS-1$
                _label = FileLine.getTrimmedValue(row[1]);
            } else if (FileLine.argEquals(row[0], "type")) { //$NON-NLS-1$
                _type = FileLine.getTrimmedValue(row[1]);
            } else if (FileLine.argEquals(row[0], "command")) { //$NON-NLS-1$
                _command = FileLine.getTrimmedValue(row[1]);
            } else if (FileLine.argEquals(row[0], "args")) { //$NON-NLS-1$
                _args = parameter.substring(parameter.indexOf("=") + 1).replaceAll("\"", "").trim(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            } else {
                throw new WrongADLFormatException(
                        Messages.ADLMenuItem_WrongADLFormatException_Begin + fileLine
                                + Messages.ADLMenuItem_WrongADLFormatException_end);
            }
        }
    }


    @Override
    public Object[] getChildren() {
        // TODO Auto-generated method stub
        return null;
    }
}

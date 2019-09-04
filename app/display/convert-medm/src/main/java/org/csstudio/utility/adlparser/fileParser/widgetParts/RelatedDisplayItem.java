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
 * @since 20.09.2007
 */
public class RelatedDisplayItem extends WidgetPart {
    //TODO Add LineParser routines to get commonly used entries

    /**
     * The Button Label Text.
     */
    private String _label;
    /**
     * The display to open.
     */
    private String _fileName;
    /**
     * the record for the new Display.
     */
    private String _args;
    /**
     * The root path for Widget.
     */
    private String _path;
    private String _policy;

    /**
     * The default constructor.
     *
     * @param display
     *            An ADLWidget that correspond a ADL Related Display Item.
     * @param parentWidgetModel
     *            The Widget that set the parameter from ADLWidget.
     * @throws WrongADLFormatException
     *             Wrong ADL format or untreated parameter found.
     */
    public RelatedDisplayItem(final ADLWidget display)
            throws WrongADLFormatException {
            super(display);
    }

    /**
     * Default Constructor
     */
    public RelatedDisplayItem(){
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    final void init() {
        name = String.valueOf("display");
        _label = String.valueOf("");
        _fileName = String.valueOf("");
        _args = String.valueOf("");
        _policy = String.valueOf("false");
    }



    /**
     * {@inheritDoc}
     */
    @Override
    final void parseWidgetPart(final ADLWidget display) throws WrongADLFormatException {
        // assert !display.isType("display[n]") :
        // "This "+display.getType()+" is not a ADL displayItem";

        for (FileLine fileLine : display.getBody()) {
            String parameter = fileLine.getLine();
            if (parameter.trim().startsWith("//")) { //$NON-NLS-1$
                continue;
            }
            String head = parameter.split("=")[0]; //$NON-NLS-1$
            String tmp = "";
            try {
                tmp = parameter.substring(head.length() + 1);
            } catch (StringIndexOutOfBoundsException exp) {
                throw new WrongADLFormatException(
                        Messages.RelatedDisplayItem_WrongADLFormatException_Begin + head
                                + Messages.RelatedDisplayItem_WrongADLFormatException_Middle
                                + fileLine + "(" + display.getObjectNr() + ":" + display.getType()
                                + ")[" + parameter + "]");
            }
            String row = tmp;
            head = head.trim().toLowerCase();
            if (head.equals("label")) { //$NON-NLS-1$
                _label = row;
            } else if (head.equals("name")) { //$NON-NLS-1$
                _fileName = row;
            } else if (head.equals("args")) { //$NON-NLS-1$
                  _args = row;
            } else if (head.equals("policy")) { //$NON-NLS-1$
                setPolicy(row.replaceAll("\"", ""));
            } else if (head.equals("x")) { //$NON-NLS-1$
                // Do Nothing
                // SDS not support this Property
            } else if (head.equals("y")) { //$NON-NLS-1$
                // Do Nothing
                // SDS not support this Property
            } else if (head.equals("width")) { //$NON-NLS-1$
                // Do Nothing
                // SDS not support this Property
            } else if (head.equals("height")) { //$NON-NLS-1$
                // Do Nothing
                // SDS not support this Property
            } else {
                throw new WrongADLFormatException(
                        Messages.RelatedDisplayItem_WrongADLFormatException_Begin + head
                                + Messages.RelatedDisplayItem_WrongADLFormatException_Middle
                                + fileLine + "(" + display.getObjectNr() + ":" + display.getType()
                                + ")");
            }
        }
    }

    /**
     *
     * @return the Label of the Related Display Item.
     */
    public final String getLabel() {
        return _label;
    }

    /**
     *
     * @return the filename of the Related Display Item.
     */
    public final String getFileName() {
        return _fileName;
    }

    /**
     *
     * @return the arguments(macros) of the Related Display Item.
     */
    public final String getArgs() {
        return _args;
    }

@Override
public Object[] getChildren() {
    Object[] ret = new Object[3];
    ret[0] = new ADLResource(ADLResource.RD_LABEL, _label);
    ret[1] = new ADLResource(ADLResource.RD_NAME, _fileName);
    ret[2] = new ADLResource(ADLResource.RD_ARGS, _args);

    return ret;
}

/**
 * @param _policy the _policy to set
 */
public void setPolicy(String _policy) {
    this._policy = _policy;
}

/**
 * @return the _policy
 */
public String getPolicy() {
    return _policy;
}



}

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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.utility.adlparser.fileParser.ADLResource;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.FileLine;
import org.csstudio.utility.adlparser.fileParser.ParserADL;
import org.csstudio.utility.adlparser.fileParser.WrongADLFormatException;
import org.csstudio.utility.adlparser.internationalization.Messages;

/**
 * @author hrickens
 * @author $Author$
 * @version $Revision$
 * @since 04.09.2007
 */
public class ADLObject extends WidgetPart{
    //TODO Strip out old code lines that refer to SDS implementations

    /** The x-coordinate of the Object.*/
    private int _x;
    /** The y-coordinate of the Object.*/
    private int _y;
    /** The width of the Object.*/
    private int _width;
    /** The height of the Object.*/
    private int _height;

    /**
     * The default constructor.
     *
     * @param adlObject An ADLWidget that correspond a ADL Object.
     * @param parentWidgetModel The Widget that set the parameter from ADLWidget.
     * @throws WrongADLFormatException Wrong ADL format or untreated parameter found.
     */
    public ADLObject(final ADLWidget adlObject) throws WrongADLFormatException {
       super(adlObject);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void init() {
        name = new String("object");
        /* Not to initialization*/
    }

    private static final int HUGE = 4000;

    /**
     * {@inheritDoc}
     */
    @Override
    final void parseWidgetPart(final ADLWidget adlObject) throws WrongADLFormatException {
        assert adlObject.isType("object") : Messages.ADLObject_AssertError_Begin+adlObject.getType()+Messages.ADLObject_AssertError_End+"\r\n"+adlObject; //$NON-NLS-1$
        for (FileLine fileLine : adlObject.getBody()) {
            String parameter = fileLine.getLine();
            if(parameter.trim().startsWith("//")){ //$NON-NLS-1$
                continue;
            }
            String[] row = parameter.split("="); //$NON-NLS-1$
            if(row.length!=2){
                throw new WrongADLFormatException(Messages.ADLObject_WrongADLFormatException_Begin+parameter+Messages.ADLObject_WrongADLFormatException_End);
            }
            row[1] = row[1].replaceAll("\"", "").trim();
            if(FileLine.argEquals(row[0], "x")){ //$NON-NLS-1$
                if(row[1].startsWith("$")){
                    //TODO: ADLObject --> Dynamic x coordinate
                    _x=0;
                }else{
                    _x=Integer.parseInt(row[1]);
                    if (Math.abs(_x) > HUGE)
                    {
                        Logger.getLogger(ParserADL.class.getName()).log(Level.INFO, "Limiting ridiculous x=" + row[1]);
                        _x = HUGE;
                    }
                }
            }else if(FileLine.argEquals(row[0], "y")){ //$NON-NLS-1$
                if(row[1].startsWith("$")){
                    //TODO: ADLObject --> Dynamic y coordinate
                    _y=0;
                }else {
                    _y=Integer.parseInt(row[1]);
                    if (Math.abs(_y) > HUGE)
                    {
                        Logger.getLogger(ParserADL.class.getName()).log(Level.INFO, "Limiting ridiculous y=" + row[1]);
                        _y = HUGE;
                    }
                }
            }else if(FileLine.argEquals(row[0], "width")){ //$NON-NLS-1$
                _width=Integer.parseInt(row[1]);
                if (Math.abs(_width) > HUGE)
                {
                    Logger.getLogger(ParserADL.class.getName()).log(Level.INFO, "Limiting ridiculous width=" + row[1]);
                    _width = HUGE;
                }
            }else if(FileLine.argEquals(row[0], "height")){ //$NON-NLS-1$
                _height=Integer.parseInt(row[1]);
                if (Math.abs(_height) > HUGE)
                {
                    Logger.getLogger(ParserADL.class.getName()).log(Level.INFO, "Limiting ridiculous height=" + row[1]);
                    _height = HUGE;
                }
            }else if(FileLine.argEquals(row[0], "groupid")){ //$NON-NLS-1$
                // TODO: ADLObject->groupid
                Logger.getLogger(ParserADL.class.getName()).log(Level.INFO, "Unhandled Parameter: " + fileLine);
            }else {
                throw new WrongADLFormatException(Messages.ADLObject_WrongADLFormatException_Parameter_Begin+fileLine+Messages.ADLObject_WrongADLFormatException_Parameter_End);
            }
        }
    }

//**    /**
//**     * {@inheritDoc}
//**     */
//**    @Override
//**    final void generateElements() {
//**            setX(_x);
//**            setY(_y);
//**            setWidth(_width);
//**            setHeight(_height);
//**
//**    }

    /**
     * @return the x-coordinate.
     */
    public final int getX() {
        return _x;
    }
    /**
     * @return the y-coordinate.
     */
    public final int getY() {
        return _y;
    }
    /**
     * @return the width.
     */
    public final int getWidth() {
        return _width;
    }

    /**
     * @return the height.
     */
    public final int getHeight() {
        return _height;
    }

//**    /**
//**     * @param x set the x-coordinate.
//**     */
    //**    public final void setX(final int x) {
//**        _x = x;
//**        _widgetModel.setX(_x);
//**    }
//**
//**    /**
//**     * @param y set the y-coordinate
//**     */
    //**    public final void setY(final int y) {
//**        _y = y;
//**        _widgetModel.setY(_y);
//**    }
//**
//**    /**
//**     * @param width set the width-coordinate
//**     */
    //**    public final void setWidth(final int width) {
//**        _width = width;
//**        _widgetModel.setWidth(_width);
//**    }
//**
//**    /**
//**     * @param height set the height-coordinate.
//**     */


//**        public final void setHeight(final int height) {
//**        _height = height;
//**        _widgetModel.setHeight(_height);
//**    }


    /**
     * @return child objects
     */
    @Override
    public Object[] getChildren(){
        Object[] ret = new Object[4];
        ret[0] = new ADLResource(ADLResource.X, new Integer(_x));
        ret[1] = new ADLResource(ADLResource.Y, new Integer(_y));
        ret[2] = new ADLResource(ADLResource.WIDTH, new Integer(_width));
        ret[3] = new ADLResource(ADLResource.HEIGHT, new Integer(_height));

        return ret;
    }
}

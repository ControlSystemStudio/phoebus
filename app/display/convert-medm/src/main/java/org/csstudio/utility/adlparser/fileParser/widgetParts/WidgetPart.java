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


//**import org.csstudio.sds.components.model.MenuButtonModel;
//**import org.csstudio.sds.components.model.PolygonModel;
//**import org.csstudio.sds.components.model.RectangleModel;
//**import org.csstudio.sds.model.AbstractWidgetModel;
//**import org.csstudio.sds.model.initializers.WidgetInitializationService;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.WrongADLFormatException;

/**
 * @author hrickens
 * @author $Author$
 * @version $Revision$
 * @since 24.10.2007
 */
public abstract class WidgetPart {

    /**
     * The Widget that set the parameter from ADLWidget.
     */
    protected String name = new String();
    /**
     * The default constructor.
     *
     * @param widgetPart An ADLWidget that correspond to the Child Widget Part.
     * @param parentWidgetModel The Widget that set the parameter from ADLWidget.
     * @throws WrongADLFormatException Wrong ADL format or untreated parameter found.
     */
    public WidgetPart(final ADLWidget widgetPart) throws WrongADLFormatException {
        init();
        if (!widgetPart.getType().startsWith(getName())){
            throw new WrongADLFormatException("part type does not match widget name(part type, widget name): ("
                    +widgetPart.getType() + ", " + getName() + ")");
        }
        parseWidgetPart(widgetPart);
    }

    public WidgetPart(){
        init();
    }
    /**
     * Initialization.
     */
    abstract void init();

    /**
     * Pars the {@link ADLWidget}.
     *
     * @param widgetPart the widget Part to pars.
     * @throws WrongADLFormatException Wrong ADL format or untreated parameter found.
     */
    abstract void parseWidgetPart(ADLWidget widgetPart) throws WrongADLFormatException;

    abstract public Object[] getChildren();


    public String getName(){
        return name;
    }

}

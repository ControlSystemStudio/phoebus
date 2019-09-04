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
package org.csstudio.utility.adlparser.fileParser;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hrickens
 * @author $Author$
 * @version $Revision$
 * @since 11.09.2007
 */
public class ADLWidget {

    /**
     * The Widget type.
     */
    private String _type;
    /** The parent Widget.*/
    private ADLWidget _parent;
    /** A list whit all body properties. */
    private ArrayList<FileLine> _body = new ArrayList<>();
    /** A list whit all ADLobjectparts. */
    private ArrayList<ADLWidget> _objects = new ArrayList<>();
    /** The Number of this object in the Display. useful for Debugging and Error handling.*/
    private int _objectNr;

    /**
     * @param type The Widgettype of this Object.
     * @param parent The parent widget.
     * @param objectNr The Number of this Object.
     */
    public ADLWidget(final String type, final ADLWidget parent, final int objectNr) {
        _objectNr=objectNr;
        setType(type);
        setParent(parent);
    }

    /**
     * @param type Widget  type
     */
    public final void setType(final String type){
        //TODO replacement of slashes does not work
        _type = type.replaceAll("[\\{\"]", "").trim().toLowerCase();  //$NON-NLS-1$ //$NON-NLS-2$
    }
    /**
     *
     * @return the Type of Widget;
     */
    public final String getType() {
        return _type;
    }
    /**
     * @return get a list of Parameter
     */
    public final ArrayList<FileLine> getBody() {
        return _body;
    }
    /**
     * @param parameter the added
     */
    public final void addBody(final FileLine parameter) {
        _body.add(parameter);
    }
    /**
     *
     * @return a list of children objects.
     */
    public final List<ADLWidget> getObjects() {
        return _objects;
    }

    /**
     * Add an object as children.
     * @param object the children object.
     */
    public final void addObject(final ADLWidget object) {
        _objects.add(object);
    }

    /**
     *
     * @return the parent Widget.
     */
    public final ADLWidget getParent() {
        return _parent;
    }

    /**
     *
     * @param parent set the parent widget.
     */
    public final void setParent(final ADLWidget parent) {
        _parent = parent;
    }

    /**
     * @param checkType The type to test.
     * @return if true when the Widget type = checkType
     */
    public final boolean isType(final String checkType){
        boolean equals = getType().equals(checkType.toLowerCase());
        return equals;
    }
    /**
     *
     * @return the number of this Object.
     */
    public final int getObjectNr() {
        return _objectNr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String toString() {
        String string = ""; //$NON-NLS-1$
        if(_parent!=null){
            string=string.concat(_parent.toString()+"->"); //$NON-NLS-1$
        }
        string = string.concat(getType()+":("+_objectNr); //$NON-NLS-1$
        FileLine fileLine = null;
        if(getBody().size()>0){
            fileLine = getBody().get(0);
        }
        if(fileLine!=null){
            string = string.concat(")["+Integer.toString(fileLine.getLineNumber()-1)+"]");
        }
        return string;
    }

}

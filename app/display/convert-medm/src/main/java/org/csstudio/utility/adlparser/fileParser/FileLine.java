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

/**
 * @author hrickens
 * @author $Author$
 * @version $Revision$
 * @since 04.11.2008
 */
public class FileLine {

    private static String _file;
    private String _line;
    private int _lineNumber;

    public FileLine(String line, int lineNumber){
        setLine(line);
        setLineNumber(lineNumber);
    }

    public final String getLine() {
        if(_line==null){
            _line="";
        }
        return _line;
    }
    public final void setLine(String line) {
        _line = line;
    }
    public final int getLineNumber() {
        return _lineNumber;
    }
    public final void setLineNumber(int lineNumber) {
        _lineNumber = lineNumber;
    }

    public static final String getFile() {
        return _file;
    }

    public static final void setFile(String file) {
        _file = file;
    }

    /** method used in parseWidgetPart methods to check the parameter string
     *
     * @param arg
     * @param toCompare
     * @return
     */
    public static boolean argEquals(String arg, String toCompare) {
        if(arg.trim().replaceAll( "\"", "" ).equalsIgnoreCase(toCompare)) {
            return true;
        }
        else {
            return false;
        }
    }
    /**
     * Method used in parseWidgetPart to get the value with quotes and leading and trailing spaces removed
     * @param input
     * @return
     */
    public static String getTrimmedValue(String input){
        return input.replaceAll( "\"", "" ).trim();
    }

    /**
     * Method used in parseWidgetPart to convert the string value into an integer
     * @param input
     * @return
     */
    public static int getIntValue(String input) throws NumberFormatException{
        return Integer.parseInt(FileLine.getTrimmedValue(input));
    }

    /**
     * Method used in parseWidgetPart to convert the string value into an integer
     * @param input
     * @return
     */
    public static float getFloatValue(String input) throws NumberFormatException {
        return Float.parseFloat(FileLine.getTrimmedValue(input));
    }

    /**
     * Method used in parseWidgetPart to convert the string value into an integer
     * @param input
     * @return
     */
    public static boolean getBooleanValue(String input) {
        return Boolean.parseBoolean(FileLine.getTrimmedValue(input));
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "\r\nFile: "+getFile()+"\r\n"+getLineNumber()+": "+getLine();
    }

}

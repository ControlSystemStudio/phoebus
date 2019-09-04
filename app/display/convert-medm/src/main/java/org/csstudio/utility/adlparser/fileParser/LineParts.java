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
 * @since 12.08.2008ls
 */
public class LineParts {

    /**
     * Have the raw line a File.
     */
    private boolean _isFile;
    /**
     * Have the raw line an Alias.
     */
    private boolean _haveAlias;

    /**
     * The unhandled ADL Line.
     */
    private String _rawLine;
    /**
     * An Alias.
     */
    private String _alias;
    /**
     * A File Name.
     */
    private String _fileName;
    /**
     * The File extension.
     */
    private String _fileEnd;

    /**
     * The default constructor.
     * @param rowLine unhandled ADL Line.
     */
    public LineParts(final String rowLine){
        _rawLine = rowLine;
    }

    public final boolean isFile() {
        return _isFile;
    }

    public final void setFile(boolean isFile) {
        _isFile = isFile;
    }

    public final boolean isHaveAlias() {
        return _haveAlias;
    }

    public final void setHaveAlias(boolean haveAlias) {
        _haveAlias = haveAlias;
    }

    public final String getRowLine() {
        return _rawLine;
    }

    public final void setRowLine(String rowLine) {
        _rawLine = rowLine;
    }

    public final String getAlias() {
        return _alias;
    }

    public final void setAlias(String alias) {
        _alias = alias;
    }

    public final String getFileName() {
        return _fileName;
    }

    public final void setFileName(String fileName) {
        _fileName = fileName;
    }

    public final String getFileEnd() {
        return _fileEnd;
    }

    public final void setFileEnd(String fileEnd) {
        _fileEnd = fileEnd;
    }




}

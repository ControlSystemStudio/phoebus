/*
 * Copyright (c) 2008 Stiftung Deutsches Elektronen-Synchrotron,
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
 package org.csstudio.utility.adlparser.fileParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Class which provides methods for parsing ADL files into a nested Map
 * with almost exactly the same structure as original ADL text file.
 * @author Tomaz Hocevar
 *
 * @author $Author$
 * @version $Revision$
 * @since 08.10.2007
 */
public final class ParserADL {

    /**
     * Default Constructor.
     */
    public ParserADL(){}

    /**
     * Main method of class ParserADL.<br/>
     * Reads form an adl file and creates a structure of ADLWidget.
     *
     * @param file The ADL File to parse.
     * @return the root Object with contain the structure of the Widget.
     */
    public static ADLWidget getNextElement(final File file) {
        int lineNr=0;
        final ADLWidget root = new ADLWidget(file.getAbsolutePath(),null,lineNr++);
        FileLine.setFile(file.getAbsolutePath());
        ADLWidget children= root;
        BufferedReader buffRead = null;
        try {

            buffRead = new BufferedReader(new FileReader(file));
            String line;
            int lineNumber=0;
            String storeDirtyLine=null;
            int lastDirtyLine=0;
            while((line = buffRead.readLine()) != null){
                lineNumber++;
                line = line.trim();
                if(line.length()>0){
                    if(line.startsWith("#")){
                        // do nothing comment line
                    }else if(line.contains("{") && !line.contains("textix=")){ //$NON-NLS-1$
                        children = new ADLWidget(line,children, lineNr++);
                    }else if (line.contains("}")&& !line.contains("textix=")){ //$NON-NLS-1$
                        if (children.getParent() != null)
                        {
                            children.getParent().addObject(children);
                            children = children.getParent();
                        }
                    }else{
                        boolean dirtyLine = (line.length()-line.replaceAll("\"", "").length())%2==1;
                        if(storeDirtyLine!=null&&dirtyLine && lineNumber==(lastDirtyLine+1)){
                            line = storeDirtyLine.concat(line);
                            storeDirtyLine=null;
                        }else if(dirtyLine){
                            storeDirtyLine = line;
                            lastDirtyLine = lineNumber;
                            continue;
                        }
                        children.addBody(new FileLine(line,lineNumber));
                    }
                }
            }
        } catch (final Exception e) {
            Logger.getLogger(ParserADL.class.getName()).log(Level.WARNING, "ADL Parse error", e);
        } finally{
            try {
                if(buffRead!=null){
                    buffRead.close();
                }
            } catch (final IOException e) {
                Logger.getLogger(ParserADL.class.getName()).log(Level.WARNING, "Error closing reader", e);
            }
        }
        return root;
    }
}

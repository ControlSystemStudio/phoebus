/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.opibuilder.converter.model.EdmDisplay;
import org.csstudio.opibuilder.converter.parser.EdmDisplayParser;

/** EDM Converter
 *
 *  <p>Can be called as 'Main',
 *  also used by converter app.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Converter
{
    /** Logger for all the Display Builder generating code */
    public static final Logger logger = Logger.getLogger(Converter.class.getPackageName());

    public Converter(final File input, final File output) throws Exception
    {
        logger.log(Level.INFO, "Convert " + input + " -> " + output);
        final EdmDisplayParser parser = new EdmDisplayParser(input.getPath(), new FileInputStream(input));
        final EdmDisplay edm = new EdmDisplay(parser.getRoot());
        final EdmConverter converter = new EdmConverter(input.getName(), edm);
        final ModelWriter writer = new ModelWriter(new FileOutputStream(output));
        writer.writeModel(converter.getDisplayModel());
        writer.close();
    }

    public static void main(String[] args)
    {
        // TODO Convert files passed on the command line
    }
}

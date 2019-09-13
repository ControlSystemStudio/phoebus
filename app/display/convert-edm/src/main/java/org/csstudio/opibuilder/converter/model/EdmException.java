/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.model;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class handling exceptions when handling general and specific data in Edm data model.
 *
 * @author Matevz
 *
 */
@SuppressWarnings("serial")
public class EdmException extends Exception {

    private static Logger log = Logger.getLogger("org.csstudio.opibuilder.converter.model.EdmException");

    public static final String ATTRIBUTE_ALREADY_EXISTS = "ATTRIBUTE_ALREADY_EXISTS";
    public static final String FILE_NOT_FOUND = "FILE_NOT_FOUND";
    public static final String NESTING_ERROR = "NESTING_ERROR";
    public static final String COLOR_FORMAT_ERROR = "COLOR_FORMAT_ERROR";
    public static final String STRING_FORMAT_ERROR = "STRING_FORMAT_ERROR";
    public static final String INTEGER_FORMAT_ERROR = "INTEGER_FORMAT_ERROR";
    public static final String FONT_FORMAT_ERROR = "FONT_FORMAT_ERROR";
    public static final String BOOLEAN_FORMAT_ERROR = "BOOLEAN_FORMAT_ERROR";
    public static final String SPECIFIC_PARSING_ERROR = "SPECIFIC_PARSING_ERROR";
    public static final String DOUBLE_FORMAT_ERROR = "DOUBLE_FORMAT_ERROR";
    public static final String CLASS_NOT_DECLARED = "CLASS_NOT_DECLARED";
    public static final String REQUIRED_ATTRIBUTE_MISSING = "REQUIRED_ATTRIBUTE_MISSING";

    public static final String DOM_BUILDER_EXCEPTION = "DOM_BUILDER_EXCEPTION";
    public static final String OPI_WRITER_EXCEPTION = "OPI_WRITER_EXCEPTION";

    private String type;
    private String message;


    /**
     * Constructs an EdmException instance.
     * Message is piped untouched, except when there is a File_NOT_FOUND exception,
     * an standard message is composed and input parameter message is treated as file name.
     *
     * @param type EdmException type.
     * @param message EdmException message, error information.
     */
    public EdmException(String type, String message, Throwable throwable) {
        super(throwable);

        this.type = type;

        if (type.equals(FILE_NOT_FOUND))
            this.message = type + " exception: " + "File " + message + " does not exist.";
        else
            this.message = type + " exception: " + message;



        if(throwable !=null){
            this.message = this.message + "\n"+throwable.getMessage();
        }

        log.log(Level.SEVERE, getMessage(), this);
    }

    public String getType() {
        return type;
    }

    @Override
    public String getMessage() {
        return message;
    }
}

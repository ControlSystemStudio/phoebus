
package org.epics.pva.data.nt;

/**
 * Exception when trying to construct a PVAScalar without using
 * the name "description" for the description.
 */
public class PVAScalarDescriptionNameException extends Exception {
    public PVAScalarDescriptionNameException(String errorMessage) {
        super(errorMessage);
    }
}

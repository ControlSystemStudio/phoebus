package org.epics.pva.data.nt;

/**
 * Exception when trying to construct a PVAScalar without using
 * the name "value" for the scalar.
 */
public class PVAScalarValueNameException extends Exception {
    public PVAScalarValueNameException(String errorMessage) {
        super(errorMessage);
    }
}

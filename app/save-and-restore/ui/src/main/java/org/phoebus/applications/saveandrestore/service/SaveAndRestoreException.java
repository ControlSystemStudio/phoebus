package org.phoebus.applications.saveandrestore.service;

public class SaveAndRestoreException extends Exception {
    public SaveAndRestoreException(String errorMessage) {
        super(errorMessage);
    }

    public SaveAndRestoreException() {
    }

    public SaveAndRestoreException(String message, Throwable cause) {
        super(message, cause);
    }

    public SaveAndRestoreException(Throwable cause) {
        super(cause);
    }

    public SaveAndRestoreException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

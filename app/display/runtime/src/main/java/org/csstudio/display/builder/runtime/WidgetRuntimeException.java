package org.csstudio.display.builder.runtime;

public class WidgetRuntimeException extends Exception {
    public WidgetRuntimeException(String message) {
        super(message);
    }

    public WidgetRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}

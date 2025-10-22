package org.phoebus.pv.tango;

import org.tango.utils.DevFailedUtils;

import fr.esrf.Tango.DevError;
import fr.esrf.Tango.DevFailed;

/**
 * Tango DevFailed helper
 *
 * @author katy.saintin@cea.fr
 */
public class TangoExceptionHelper {
    private static final String DESCRIPTION_IS = "Description = ";
    private static final String ORIGIN_IS = "\nOrigin= ";
    private static final String REASON_IS = "\nReason= ";
    private static final String SEPARATOR = ": ";
    private static final String AT = "\n\tat";
    private static final String CAUSED_BY = "\nCaused by: ";

    public static final String REASON = "\nReason: ";

    private static final String IDL = "IDL";
    private static final String DESC = "desc:";

    public static String[] getErrorArray(DevFailed e) {
        String[] result = null;
        if (e != null) {
            DevError[] traces = e.errors;
            if (traces != null) {
                result = new String[traces.length];
                for (int i = 0; i < traces.length; i++) {
                    StringBuilder errorBuider = new StringBuilder(DESCRIPTION_IS);
                    errorBuider.append(traces[i].desc);
                    errorBuider.append(ORIGIN_IS).append(traces[i].origin);
                    errorBuider.append(REASON_IS).append(traces[i].reason);
                    result[i] = errorBuider.toString();
                }
            }
        }
        return result;
    }

    public static StringBuilder errorTraceToStringBuilder(Throwable t, StringBuilder builder) {
        return errorTraceToStringBuilder(t, builder, true);
    }

    public static StringBuilder errorTraceToStringBuilder(Throwable t, StringBuilder builder, boolean traceCause) {
        if (builder == null) {
            builder = new StringBuilder();
        }
        if (t != null) {
            if (t instanceof DevFailed) {
                builder.append(DevFailedUtils.toString((DevFailed) t));
            } else {
                builder.append(t.getClass().getName());
                String message = t.getLocalizedMessage();
                if (message != null) {
                    builder.append(SEPARATOR).append(message);
                }
                if (t.getStackTrace() != null) {
                    for (StackTraceElement element : t.getStackTrace()) {
                        builder.append(AT).append(element);
                    }
                }
                if (traceCause && (t.getCause() != null)) {
                    builder.append(CAUSED_BY);
                    errorTraceToStringBuilder(t.getCause(), builder);
                }
            }
        }
        return builder;
    }

    /**
     * Extracts the error message of a {@link Throwable}
     * 
     * @param t The {@link Throwable}
     * @return A {@link String}: the error message.
     */
    public static String getErrorMessage(Throwable t) {
        String message = null;
        if (t != null) {
            if (t instanceof DevFailed) {
                // Try to extract error message. 2 possible case
                // - Either e.getMessage() contains the expected message
                // - Or it contains a not user friendly message with "IDL...",
                // which means expected message is in description
                message = t.getMessage();
                if (message.indexOf(IDL) > -1) {
                    // 2nd case: search in description
                    message = DevFailedUtils.toString((DevFailed) t);
                    int index = message.indexOf(DESC);
                    if (index > -1) {
                        int index2 = message.indexOf('\n', index);
                        if (index2 > -1) {
                            message = message.substring(index + DESC.length(), index2).trim();
                        }
                    }
                }
            } else {
                message = t.getMessage();
            }
        }
        return message;
    }

}

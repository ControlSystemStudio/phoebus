package org.phoebus.channelfinder.utility;

import org.phoebus.channelfinder.ChannelFinderException;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

/** Helper for handling channel finder errors */
public class ChannelErrorHandler
{
    /** Given a nested exception, locate channel finder error
     *
     *  <p>Channel finder API wraps ChannelFinderException
     *  in RuntimeException. Recover the underlying error,
     *  or provide plain Exception that can be caught.
     *
     *  @param ex (Runtime)Exception thrown by API
     *  @return ChannelFinderException or a plain Exception
     */
    public static Exception findChannelFinderError(Throwable ex)
    {
        while (ex != null)
        {
            if (ex instanceof ChannelFinderException)
                return (ChannelFinderException) ex;
            ex = ex.getCause();
        }
        return new Exception(ex);
    }

    /** Display error dialog
     *  @param info Description of failed operation
     *  @param thrown Error thrown by API
     */
    public static void displayError(String info, Throwable thrown)
    {
        final Exception ex = findChannelFinderError(thrown);
        if (ex instanceof ChannelFinderException)
            info += "\nWeb service status: " + ((ChannelFinderException)ex).getStatus();
        ExceptionDetailsErrorDialog.openError("Channel Finder Error", info, ex);
    }
}

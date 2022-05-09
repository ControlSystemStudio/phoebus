package org.phoebus.channelfinder.utility;

import org.phoebus.channelfinder.ChannelFinderException;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.docking.DockStage;

import javafx.scene.Node;

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

        // Can't easily position the dialog on top of the channel table, channel tree etc.
        // because the JFX node was not passed into the *ChannelsJob.
        // Next best approach is to position it on top of the main window.
        final Node parent = DockStage.getDockStages().get(0).getScene().getRoot();
        ExceptionDetailsErrorDialog.openError(parent, "Channel Finder Error", info, ex);
    }
}

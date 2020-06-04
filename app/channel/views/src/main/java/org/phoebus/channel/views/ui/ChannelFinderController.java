package org.phoebus.channel.views.ui;

import java.util.Collection;
import java.util.logging.Logger;

import org.phoebus.channelfinder.Channel;
import org.phoebus.channelfinder.ChannelFinderClient;
import org.phoebus.channelfinder.utility.ChannelSearchJob;
import org.phoebus.framework.jobs.Job;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

import javafx.application.Platform;

/**
 * A basic controller for any UI performing channelfinder queries. The
 * controller takes care of performing the query off the UI thread using
 * {@link Job}s and then invokes the setChannels method on the UI thread after
 * the query has been completed.
 * 
 * @author Kunal Shroff
 *
 */
public abstract class ChannelFinderController {

    public static Logger logger = Logger.getLogger(ChannelFinderController.class.getName());
    private ChannelFinderClient client;

    private Job channelSearchJob;

    public void setClient(ChannelFinderClient client) {
        this.client = client;
    }

    public ChannelFinderClient getClient() {
        return this.client;
    }

    public void search(String searchString) {
        if (channelSearchJob != null) {
            channelSearchJob.cancel();
        }
        channelSearchJob = ChannelSearchJob.submit(this.client, searchString,
                channels -> Platform.runLater(() -> setChannels(channels)),
                (url, ex) -> ExceptionDetailsErrorDialog.openError("ChannelFinder Query Error", ex.getMessage(), ex));

    }

    /**
     * Set a new list of channels. This method is called after the successful execution of a channelfinder query. 
     * @param channels - the new list of channels
     */
    public abstract void setChannels(Collection<Channel> channels);
}

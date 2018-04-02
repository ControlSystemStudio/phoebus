package org.phoebus.channel.views.ui;

import java.util.Collection;

import org.phoebus.channelfinder.Channel;
import org.phoebus.channelfinder.ChannelFinderClient;
import org.phoebus.framework.jobs.Job;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

import javafx.application.Platform;

/**
 * A basic controller for any ui performing channelfinder queries. The
 * controller takes care of performing the query off the ui thread using
 * {@link Job}s and then invokes the setChannels method on the UI thread after
 * the query has been completed.
 * 
 * @author Kunal Shroff
 *
 */
public abstract class ChannelFinderController {

    private ChannelFinderClient client;

    private Job channelSearchJob;

    public void setClient(ChannelFinderClient client) {
        this.client = client;
    }

    public void search(String searchString) {
        if (channelSearchJob != null) {
            channelSearchJob.cancel();
        }
        channelSearchJob = ChannelSearchJob.submit(this.client, searchString,
                channels -> Platform.runLater(() -> setChannels(channels)),
                (url, ex) -> ExceptionDetailsErrorDialog.openError("ChannelFinder Query Error", ex.getMessage(), ex));

    }

    public abstract void setChannels(Collection<Channel> channels);
}

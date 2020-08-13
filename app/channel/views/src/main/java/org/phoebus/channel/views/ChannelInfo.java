package org.phoebus.channel.views;

import static javafx.scene.control.Alert.AlertType.INFORMATION;

import java.util.ArrayList;
import java.util.List;

import org.phoebus.channel.views.ui.ChannelInfoTreeController;
import org.phoebus.channelfinder.Channel;
import org.phoebus.channelfinder.ChannelFinderClient;
import org.phoebus.channelfinder.ChannelFinderService;
import org.phoebus.channelfinder.utility.ChannelSearchJob;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.ContextMenuEntry;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;

/**
 * Action which opens a dialog to display the channel information.
 */
public class ChannelInfo implements ContextMenuEntry {
    private static final String NAME = "Channel Info";
    private static final Image icon = ImageCache.getImage(ChannelTableApp.class, "/icons/channel_info.png");

    private static final Class<?> supportedTypes = Channel.class;

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public Image getIcon()
    {
        return icon;
    }

    @Override
    public Class<?> getSupportedType()
    {
        return supportedTypes;
    }

    private ChannelFinderClient client = ChannelFinderService.getInstance().getClient();

    @Override
    public void call(Selection selection) throws Exception
    {
        List<ProcessVariable> pvs = new ArrayList<>();
        List<Channel> channels = new ArrayList<>();
        SelectionService.getInstance().getSelection().getSelections().stream().forEach(s -> {
            if (s instanceof Channel)
            {
                channels.add((Channel)s);
            } else if (s instanceof ProcessVariable)
            {
                pvs.add((ProcessVariable) s);
            }
        });

        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(this.getClass().getResource("ui/ChannelInfoTree.fxml"));

        Alert alert = new Alert(INFORMATION);
        alert.setTitle(NAME);
        alert.setHeaderText(null);
        alert.setGraphic(null);
        alert.getDialogPane().setContent(loader.load());

        ChannelInfoTreeController controller = loader.getController();
        controller.setChannels(channels);

        // Query channelfinder for selected pvs on a separate thread.
        pvs.forEach(pv -> {
            ChannelSearchJob.submit(this.client,
                    pv.getName(),
                    result -> Platform.runLater(() -> {
                        controller.addChannels(result);
                    }),
                    (url, ex) -> ExceptionDetailsErrorDialog.openError("ChannelFinder Query Error", ex.getMessage(), ex));

        });
        alert.showAndWait();
    }
}

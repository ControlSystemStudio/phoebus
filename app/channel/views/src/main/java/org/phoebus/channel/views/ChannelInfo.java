package org.phoebus.channel.views;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Action which opens a dialog to display the channel information.
 */
public class ChannelInfo implements ContextMenuEntry {
    private static final String NAME = "Channel Info";
    private static final Image icon = ImageCache.getImage(ChannelTableApp.class, "/icons/channel_info.png");

    private static final Class<?> supportedTypes = ProcessVariable.class;

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

        Stage dialog = new Stage();
        dialog.setTitle(NAME);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.getIcons().add(icon);
        dialog.setScene(new Scene(loader.load()));
        dialog.setMinWidth(500);
        dialog.setMinHeight(500);

        ChannelInfoTreeController controller = loader.getController();
        controller.setChannels(channels);

        // Query channelfinder for selected pvs on a separate thread.
        pvs.forEach(pv -> {
            // X.DESC, X.VAL$, loc://X(3.14) or pva://X are all valid PV names,
            // but the channel finder tends to only know about a record "X",
            // so locate that part of the name
            String name = pv.getName();
            int sep = name.lastIndexOf('(');
            if (sep > 0)
                name = name.substring(0, sep);
            sep = name.lastIndexOf('.');
            if (sep > 0)
                name = name.substring(0, sep);
            sep = name.indexOf("://");
            if (sep >= 0)
                name = name.substring(sep + 3);
            ChannelSearchJob.submit(this.client,
                    name,
                    result -> Platform.runLater(() -> {
                        controller.addChannels(result);
                    }),
                    (url, ex) -> ExceptionDetailsErrorDialog.openError("ChannelFinder Query Error", ex.getMessage(), ex));

        });

        dialog.show();
    }
}

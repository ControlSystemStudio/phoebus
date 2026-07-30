package org.phoebus.channel.views.ui;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.phoebus.channelfinder.Channel;
import org.phoebus.channelfinder.XmlChannel;
import org.phoebus.ui.javafx.ApplicationWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ChannelTreeDemo extends ApplicationWrapper {

    public static void main(String[] args) {
        launch(ChannelTreeDemo.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        FXMLLoader loader = new FXMLLoader();

        loader.setLocation(this.getClass().getResource("ChannelTree.fxml"));
        loader.load();

        ChannelTreeController controller = loader.getController();
        controller.setChannels(testChannels());

        Parent root = loader.getRoot();
        primaryStage.setScene(new Scene(root, 400, 400));
        primaryStage.show();
    }

    private List<Channel> testChannels() throws IOException {
        List<Channel> channels = new ArrayList<>();

        final ObjectMapper mapper = new ObjectMapper();
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("testChannels.json")) {
            List<XmlChannel> xmlChannels = mapper.readValue(inputStream, new TypeReference<List<XmlChannel>>() {
            });
            for (XmlChannel xmlchannel : xmlChannels) {
                channels.add(new Channel(xmlchannel));
            }
        } catch (IOException ignored) {

        }
        return channels;
    }

}

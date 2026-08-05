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
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;

public class ChannelTableDemo extends ApplicationWrapper {

    public static void main(String[] args) {
        launch(ChannelTableDemo.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        FXMLLoader loader = new FXMLLoader();

        loader.setLocation(this.getClass().getResource("ChannelTable.fxml"));
        loader.load();

        ChannelTableController controller = loader.getController();
        controller.setChannels(testChannels());

        Parent root = loader.getRoot();
        primaryStage.setScene(new Scene(root, 400, 400));
        primaryStage.show();
    }

    private List<Channel> testChannels() {
        List<Channel> channels = new ArrayList<>();

        final JsonMapper mapper = JsonMapper.builder().build();
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("testChannels.json"))
        {
            if (inputStream == null) {
                throw new IllegalStateException("testChannels.json resource not found");
            }
            List<XmlChannel> xmlChannels = mapper.readValue(
                    inputStream.readAllBytes(),
                    new TypeReference<List<XmlChannel>>() {
                    }
            );
            for (XmlChannel xmlchannel : xmlChannels) {
                channels.add(new Channel(xmlchannel));
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return channels;
    }

}

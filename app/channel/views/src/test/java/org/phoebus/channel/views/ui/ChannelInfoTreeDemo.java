package org.phoebus.channel.views.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.phoebus.channelfinder.Channel;
import org.phoebus.ui.javafx.ApplicationWrapper;

import static org.phoebus.channelfinder.Property.Builder.property;
import static org.phoebus.channelfinder.Tag.Builder.tag;

import java.io.IOException;
import java.util.Arrays;

public class ChannelInfoTreeDemo extends ApplicationWrapper {

    public static void main(String[] args) {
        launch(ChannelInfoTreeDemo.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader();

        loader.setLocation(this.getClass().getResource("ChannelInfoTree.fxml"));
        loader.load();

        ChannelInfoTreeController controller = loader.getController();
        Channel testChannel1 = Channel.Builder.channel("testChannel1").owner("testOwner")
                .with(property("testProperty1", "value1"))
                .with(property("testProperty2", "value2"))
                .with(property("testProperty3", "value3"))
                .with(tag("testTag1"))
                .with(tag("testTag2"))
                .build();
        Channel testChannel2 = Channel.Builder.channel("testChannel2").owner("testOwner")
                .with(property("testProperty1", "value1"))
                .with(property("testProperty2", "value2"))
                .with(property("testProperty3", "value3"))
                .with(tag("testTag1"))
                .with(tag("testTag2"))
                .build();
        controller.setChannels(Arrays.asList(testChannel1, testChannel2));

        Parent root = loader.getRoot();
        primaryStage.setScene(new Scene(root, 400, 400));
        primaryStage.show();
    }

}

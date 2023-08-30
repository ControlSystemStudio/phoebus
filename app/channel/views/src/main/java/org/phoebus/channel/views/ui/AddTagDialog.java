package org.phoebus.channel.views.ui;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.phoebus.channelfinder.Tag;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import org.phoebus.ui.dialog.DialogHelper;
import static org.phoebus.channel.views.ui.ChannelFinderController.logger;
/**
 * A dialog for adding a tag to a list of channels
 * 
 * @author Kunal Shroff
 *
 */
public class AddTagDialog extends Dialog<Tag> {

    public AddTagDialog(final Node parent, final Collection<Tag> tags) {
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        setResizable(true);
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(this.getClass().getResource("SelectEntity.fxml"));
        try {
            getDialogPane().setContent(loader.load());
            SelectEntityController controller = loader.getController();
            Map<String, Tag> tagsMap = tags.stream().collect(Collectors.toMap(Tag::getName, Function.identity()));
            controller.setAvaibleOptions(tagsMap.keySet());
            setResultConverter(button -> {
                return button == ButtonType.OK
                        ? tagsMap.get(controller.getSelectedOption())
                        : null;
            });
            DialogHelper.positionDialog(this, parent, -250, -400);
        } catch (IOException e) {
            // TODO update the dialog
            logger.log(Level.WARNING, "Failed to add tag", e);
        }
    }
}

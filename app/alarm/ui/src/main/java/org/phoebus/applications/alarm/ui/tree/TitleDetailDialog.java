package org.phoebus.applications.alarm.ui.tree;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import org.phoebus.applications.alarm.model.TitleDetail;
import org.phoebus.applications.alarm.ui.Messages;
import org.phoebus.applications.alarm.ui.config.TitleDetailTableController;
import org.phoebus.framework.nls.NLS;

import java.util.List;


public class TitleDetailDialog extends Dialog<List<TitleDetail>> {

    public TitleDetailDialog(final String name, final String headerText, final List<TitleDetail> initialValue) {
        super();

        setTitle(name);
        setHeaderText(headerText);
        setResizable(true);
        getDialogPane().getButtonTypes().add(ButtonType.OK);

        final FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setResources(NLS.getMessages(Messages.class));
        fxmlLoader.setLocation(TitleDetailTableController.class.getResource("TitleDetailTable.fxml"));

        final Node node;
        try {
            node = fxmlLoader.load();
        } catch (final Exception ex) {
            throw new RuntimeException("Failed to load " + fxmlLoader.getLocation(), ex);
        }

        final TitleDetailTableController controller = fxmlLoader.getController();
        controller.setTitle(name);
        controller.setItems(initialValue);

        getDialogPane().setContent(node);

        setResultConverter(_ -> controller.getItems().stream().map(x -> (TitleDetail) x).toList());
    }
}

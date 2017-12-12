package org.phoebus.applications.probe;

import java.io.IOException;

import org.phoebus.applications.probe.view.ProbeController;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;

/**
 * This class describes and instance of the the probe application
 *
 * @author Kunal Shroff
 *
 */
public class ProbeInstance implements AppInstance {

    private final AppDescriptor appDescriptor;

    public ProbeInstance(AppDescriptor appDescriptor) {
        this.appDescriptor = appDescriptor;
    }

    @Override
    public AppDescriptor getAppDescriptor() {
        return this.appDescriptor;
    }

    private FXMLLoader loader;

    public Node create() {
        loader = new FXMLLoader();
        loader.setLocation(this.getClass().getResource("view/ProbeView.fxml"));
        try {
            TitledPane mainLayout = loader.load();
            return mainLayout;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public TextField getPVField()
    {
        ProbeController controller = (ProbeController) loader.getController();
        return controller.getPVField();
    }

    public String getPV() {
        ProbeController controller = (ProbeController) loader.getController();
        return controller.getPVName();
    }

    public void setPV(String pv) {
        ProbeController controller = (ProbeController) loader.getController();
        controller.setPVName(pv);
    }

    @Override
    public void restore(final Memento memento) {
        memento.getString("pv").ifPresent(name -> setPV(name));
    }

    @Override
    public void save(final Memento memento) {
        memento.setString("pv", getPV());
    }


}

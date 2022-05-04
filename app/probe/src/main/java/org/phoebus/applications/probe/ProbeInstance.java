package org.phoebus.applications.probe;

import static org.phoebus.applications.probe.Probe.logger;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;

import org.phoebus.applications.probe.view.ProbeController;
import org.phoebus.framework.nls.NLS;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.vtype.FormatOption;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.TextField;

/**
 * This class describes and instance of the the probe application
 *
 * @author Kunal Shroff
 *
 */
@SuppressWarnings("nls")
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
        try {
            final URL fxml = getClass().getResource("view/ProbeView.fxml");
            final ResourceBundle bundle = NLS.getMessages(ProbeInstance.class);
            loader = new FXMLLoader(fxml, bundle);
            return loader.load();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Cannot load FXML", e);
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
        ProbeController controller = (ProbeController) loader.getController();
        memento.getString("pv").ifPresent(name -> setPV(name));
        memento.getString("format").ifPresent(name -> controller.setFormat(FormatOption.valueOf(name)));
        memento.getNumber("precision").ifPresent(value -> controller.setPrecision(value.intValue()));
    }

    @Override
    public void save(final Memento memento) {
        ProbeController controller = (ProbeController) loader.getController();
        memento.setString("pv", getPV());
        memento.setString("format", controller.getFormat().name());
        memento.setNumber("precision", controller.getPrecision());
    }
}

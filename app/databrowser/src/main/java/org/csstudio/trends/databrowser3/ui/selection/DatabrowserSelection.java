package org.csstudio.trends.databrowser3.ui.selection;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.csstudio.trends.databrowser3.persistence.XMLPersistence;
import org.phoebus.util.time.TimeRelativeInterval;

import javafx.scene.image.Image;

/**
 * This represents the databrowser plot selection
 */
public class DatabrowserSelection {
    
    private final Model model;
    private final Image plot;
    
    /**
     * A instance representing a selection on the databrowser plot.
     * @param model the databrowser model
     * @param plot the databrowser plot
     */
    public DatabrowserSelection(Model model, Image plot) {
        this.model = model;
        this.plot = plot;
    }

    /**
     * Get the time range of the selected databrowser plot
     * @return time range of the plot
     */
    public TimeRelativeInterval getPlotTime() {
        return model.getTimerange();
    }

    /**
     * Get the list of all the pv's and formulas on the selected databrowser plot
     * @return list of pv's and formulas
     */
    public List<String> getPlotPVs() {
        return model.getItems().stream().map(ModelItem::getName).collect(Collectors.toList());
    }

    /**
     * The .plt file of the selected plot
     * @param outputStream
     */
    public void getPlotFile(OutputStream outputStream) {
        try (BufferedOutputStream out = new BufferedOutputStream(outputStream)) {
            XMLPersistence.write(model, out);
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    /**
     * The selected databrowser plot Image
     * @return Image
     */
    public Image getPlot() {
        return plot;
    }

}

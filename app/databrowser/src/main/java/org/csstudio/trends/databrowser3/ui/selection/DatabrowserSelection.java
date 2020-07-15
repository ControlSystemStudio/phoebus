package org.csstudio.trends.databrowser3.ui.selection;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.csstudio.trends.databrowser3.persistence.XMLPersistence;
import org.csstudio.trends.databrowser3.ui.plot.ModelBasedPlot;
import org.phoebus.util.time.TimeRelativeInterval;

import javafx.scene.image.Image;

/**
 * This represents the databrowser plot selection
 */
public class DatabrowserSelection {
    
    private final Model model;
    private final ModelBasedPlot plot;
    
    public static DatabrowserSelection of(Model model, ModelBasedPlot plot)
    {
        return new DatabrowserSelection(model, plot);
    }

    /**
     * A instance representing a selection on the databrowser plot.
     * @param model the databrowser model
     * @param plot the ModelBasedPlot
     */
    public DatabrowserSelection(Model model, ModelBasedPlot plot)
    {
        this.model = model;
        this.plot = plot;
    }

    /**
     * Get the time range of the selected databrowser plot
     * @return time range of the plot
     */
    public TimeRelativeInterval getPlotTime()
    {
        return model.getTimerange();
    }

    /**
     * Get the title of the selected databrowser plot
     * @return plot title
     */
    public Optional<String> getPlotTitle()
    {
        return model.getTitle();
    }

    /**
     * Get the list of all the pv's and formulas on the selected databrowser plot
     * @return list of pv's and formulas
     */
    public List<String> getPlotPVs()
    {
        return model.getItems().stream().map(ModelItem::getResolvedName).collect(Collectors.toList());
    }

    /**
     * The .plt file of the selected plot
     * @param outputStream
     */
    public void getPlotFile(OutputStream outputStream)
    {
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
    public Image getPlot()
    {
        return plot.getPlot().getImage();
    }

}

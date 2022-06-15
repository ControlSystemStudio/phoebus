package org.csstudio.trends.databrowser3.ui.selection;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.csstudio.trends.databrowser3.Activator;
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

    /** @param model Model
     *  @param plot Plot
     *  @return Selection handler
     */
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

    /** @param outputStream Stream where .plt content of selected plot is written
     */
    public void writePlotFile(OutputStream outputStream)
    {
        try (BufferedOutputStream out = new BufferedOutputStream(outputStream))
        {
            XMLPersistence.write(model, out);
        }
        catch (Exception ex)
        {
            Activator.logger.log(Level.WARNING, "Cannot write *.plt content", ex);
        }
    }

    /** @return Selected databrowser plot's image */
    public Image getPlot()
    {
        return plot.getPlot().getImage();
    }
}

package org.csstudio.display.builder.runtime.app.actionhandlers;

import org.csstudio.display.actions.OpenDataBrowserAction;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.spi.ActionHandler;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.csstudio.display.builder.representation.ToolkitRepresentation;
import org.csstudio.trends.databrowser3.DataBrowserApp;
import org.csstudio.trends.databrowser3.DataBrowserInstance;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.phoebus.framework.macros.MacroHandler;
import org.phoebus.framework.macros.MacroValueProvider;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.util.time.TimeParser;
import org.phoebus.util.time.TimeRelativeInterval;
import org.phoebus.util.time.TimestampFormats;

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OpenDataBrowserActionHandler implements ActionHandler {

    private final Logger logger = Logger.getLogger(OpenDataBrowserActionHandler.class.getName());

    @Override
    public void handleAction(Widget sourceWidget, ActionInfo pluggableActionInfo) {
        OpenDataBrowserAction action = (OpenDataBrowserAction) pluggableActionInfo;

        try
        {
            final DisplayModel model = sourceWidget.getTopDisplayModel();
            final ToolkitRepresentation<Object, Object> toolkit = ToolkitRepresentation.getToolkit(model);
            final MacroValueProvider macros = sourceWidget.getMacrosOrProperties();
            final String pvNames = action.getPVs().strip();
            final String timeframe = action.getTimeframe().strip();

            // Build URI from list of PVs
            String pvURI = "pv://?";
            String[] pvs = pvNames.split(" ");
            for (int i = 0; i < pvs.length; i++) {
                try {
                    pvs[i] = MacroHandler.replace(macros, pvs[i]);
                    pvURI = pvURI.concat(pvs[i]);
                    if (i < pvs.length - 1)
                        pvURI = pvURI.concat("&");
                } catch (Exception ignore) {
                    // NOP
                }
            }
            final String finalPvURI = pvURI;

            // Set timeframe
            TimeRelativeInterval timeInterval;
            if (timeframe.contains(":") || timeframe.contains("-")){
                // An absolute time has been provided
                Instant ints = TimestampFormats.parse(timeframe);
                timeInterval = TimeRelativeInterval.of(ints,
                        TimeParser.parseTemporalAmount(TimeParser.NOW));
            } else {
                // Temporal timeframe
                timeInterval = TimeRelativeInterval.of(TimeParser.parseTemporalAmount(timeframe),
                        TimeParser.parseTemporalAmount(TimeParser.NOW));
            }

            toolkit.submit(() ->
            {   // Create databrowser instance
                DataBrowserInstance instance = ApplicationService.createInstance(DataBrowserApp.NAME,
                        ResourceParser.createResourceURI(finalPvURI));

                // Set the default archiver
                for (String pv: pvs) {
                    PVItem pvItem = (PVItem) instance.getModel().getItem(pv);
                    pvItem.useDefaultArchiveDataSources();
                }

                // Set timeframe
                instance.getModel().setTimerange(timeInterval);

                return null;
            });
        }
        catch (Throwable ex)
        {
            logger.log(Level.WARNING, action+" failed. Cannot open data browser", ex);
        }
    }

    @Override
    public boolean matches(ActionInfo pluggableActionInfo) {
        return pluggableActionInfo.getType().equalsIgnoreCase(OpenDataBrowserAction.OPEN_DATA_BROWSER);
    }
}

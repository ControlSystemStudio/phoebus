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

    /**
     * Expand macros that may exist in the PV name
     * @param name PV name
     * @param macros Macros available
     * @return PV name with macros replaced
     */
    private String expandMacros(String name, MacroValueProvider macros) {
        try {
            name = MacroHandler.replace(macros, name);
        } catch (Exception ignore) {
            // NOP
        }
        return name;
    }

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

            String[] pvs = pvNames.split(" ");
            for (int i = 0; i < pvs.length; i++) {
                pvs[i] = expandMacros(pvs[i], macros);
            }
            // Build URI from list of PVs
            String pvURI = "pv://?" + String.join("&", pvs);

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
                        ResourceParser.createResourceURI(pvURI));

                // Set the default archiver
                for (String pv: pvs) {
                    PVItem pvItem = (PVItem) instance.getModel().getItem(pv);
                    pvItem.useDefaultArchiveDataSources();
                }

                // Set timeframe
                instance.getModel().setTimerange(timeInterval);

                // Disable saving dialog as we can relaunch from action
                instance.getModel().setSaveChanges(false);

                return null;
            });
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, ex, () -> action + " failed. Cannot open data browser");
        }
    }

    @Override
    public boolean matches(ActionInfo pluggableActionInfo) {
        return pluggableActionInfo.getType().equalsIgnoreCase(OpenDataBrowserAction.OPEN_DATA_BROWSER);
    }
}

package org.csstudio.display.builder.model.properties;

import org.phoebus.framework.spi.AppDescriptor;

/**
 * Wraps data needed to handle the {@link org.csstudio.display.builder.model.properties.ActionInfo.ActionType#OPEN_APPLICATION}.
 */
public class OpenApplicationActionInfo extends ActionInfo{

    /**
     * An (optional) input string like file:/my/file.plt.
     */
    private final String inputUri;

    private final AppDescriptor appDescriptor;

    /**
     *
     * @param description A description appearing in the UI
     * @param appDescriptor {@link AppDescriptor} of a Phoebus application.
     * @param resourceUri Optional input for the application, must be a valid {@link java.net.URI} string.
     */
    public OpenApplicationActionInfo(String description, AppDescriptor appDescriptor, String resourceUri){
        super(description);
        this.appDescriptor = appDescriptor;
        this.inputUri = resourceUri;
    }

    /**
     * @return The input uri string.
     */
    public String getInputUri(){
        return inputUri;
    }

    /**
     *
     * @return The {@link AppDescriptor} for the application to launch.
     */
    public AppDescriptor getAppDescriptor(){
        return appDescriptor;
    }

    @Override
    public ActionType getType() {
        return ActionType.OPEN_APPLICATION;
    }
}

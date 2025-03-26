/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore;

import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;

import java.net.URI;

/**
 * Application showing list of save-and-restore nodes matching a particular filter. Its UI is a subset of the
 * search and filter view of the {@link SaveAndRestoreApplication} UI.
 */
public class FilterViewApplication implements AppResourceDescriptor {

    public static final String NAME = "saveandrestorefilterview";
    public static final String DISPLAY_NAME = "SAR Filter";

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public AppInstance create() {
        return create(null);
    }

    @Override
    public AppInstance create(URI uri) {
        if(FilterViewInstance.INSTANCE == null){
            FilterViewInstance.INSTANCE = new FilterViewInstance(this);
        }
        else{
            FilterViewInstance.INSTANCE.raise();
        }

        if(uri != null){
            FilterViewInstance.INSTANCE.openResource(uri);
        }

        return FilterViewInstance.INSTANCE;
    }

    public FilterViewInstance getInstance(){
        return FilterViewInstance.INSTANCE;
    }
}

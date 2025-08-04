package org.phoebus.applications.queueserver.view;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public final class UiSignalEvent {

    private static final BooleanProperty ENV_DESTROY_ARMED =
            new SimpleBooleanProperty(false);

    private UiSignalEvent() {}

    public static BooleanProperty envDestroyArmedProperty() {
        return ENV_DESTROY_ARMED;
    }
}

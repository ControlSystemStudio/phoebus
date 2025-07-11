package org.phoebus.applications.queueserver.util;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public final class UiSignals {

    private static final BooleanProperty ENV_DESTROY_ARMED =
            new SimpleBooleanProperty(false);

    private UiSignals() {}

    public static BooleanProperty envDestroyArmedProperty() {
        return ENV_DESTROY_ARMED;
    }
}

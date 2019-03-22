package org.phoebus.app.diag;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;

@SuppressWarnings("nls")
public class DiagApp implements AppDescriptor {

    static final String NAME = "Phoebus Diagnostics";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public AppInstance create() {
        return new DiagAppInstance(this);
    }
}
